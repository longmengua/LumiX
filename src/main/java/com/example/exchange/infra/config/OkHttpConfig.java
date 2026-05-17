/*
 * 檔案用途：基礎設施設定，建立 Spring Bean 並連接 Kafka、Redis、Web3j 或 HTTP client。
 */
package com.example.exchange.infra.config;

import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.example.exchange.infra.tracing.TraceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OkHttp Config。
 *
 * 用途：
 * 1. 提供全域 OkHttpClient Bean
 * 2. 給 Gamma API / 未來 CLOB API 共用
 * 3. 避免每次 request 都 new client
 */
@Configuration
public class OkHttpConfig {

    @Value("${external-api.retry-max-attempts:3}")
    private int retryMaxAttempts;

    @Value("${external-api.retry-backoff-ms:200}")
    private long retryBackoffMs;

    @Value("${external-api.min-interval-ms:0}")
    private long minIntervalMs;

    @Value("${external-api.circuit-breaker-enabled:true}")
    private boolean circuitBreakerEnabled;

    @Value("${external-api.circuit-breaker-failure-threshold:5}")
    private int circuitBreakerFailureThreshold;

    @Value("${external-api.circuit-breaker-open-ms:30000}")
    private long circuitBreakerOpenMs;

    private final AtomicLong nextRequestAtMillis = new AtomicLong(0);
    private final Map<String, CircuitState> circuitStates = new ConcurrentHashMap<>();

    /**
     * 全域 OkHttpClient Bean。
     */
    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()

                /**
                 * 建立 TCP timeout。
                 */
                .connectTimeout(Duration.ofSeconds(10))

                /**
                 * 讀取 timeout。
                 */
                .readTimeout(Duration.ofSeconds(30))

                /**
                 * 寫入 timeout。
                 */
                .writeTimeout(Duration.ofSeconds(30))

                /**
                 * Connection Pool。
                 *
                 * 最多保留 20 條 idle connection。
                 * 5 分鐘回收。
                 */
                .connectionPool(
                        new ConnectionPool(
                                20,
                                5,
                                TimeUnit.MINUTES
                        )
                )

                /**
                 * 發生 connection failure 時自動 retry。
                 */
                .retryOnConnectionFailure(true)

                .addInterceptor(traceHeaderInterceptor())

                .addInterceptor(circuitBreakerInterceptor())

                .addInterceptor(rateLimitInterceptor())

                .addInterceptor(retryInterceptor())

                .build();
    }

    private Interceptor traceHeaderInterceptor() {
        return chain -> {
            Request request = chain.request();
            Request.Builder builder = request.newBuilder();
            TraceContext.currentHeaders().forEach((name, value) -> {
                if (request.header(name) == null) {
                    builder.header(name, value);
                }
            });
            return chain.proceed(builder.build());
        };
    }

    private Interceptor circuitBreakerInterceptor() {
        return chain -> {
            Request request = chain.request();
            if (!circuitBreakerEnabled) {
                return chain.proceed(request);
            }

            String key = circuitKey(request);
            CircuitState state = circuitStates.computeIfAbsent(key, ignored -> new CircuitState());
            long now = System.currentTimeMillis();
            long openUntil = state.openUntilMillis.get();
            if (openUntil > now) {
                throw new IOException("HTTP circuit breaker open for " + key);
            }

            try {
                Response response = chain.proceed(request);
                if (isCircuitFailure(response.code())) {
                    recordCircuitFailure(key, state);
                } else {
                    recordCircuitSuccess(state);
                }
                return response;
            } catch (IOException ex) {
                recordCircuitFailure(key, state);
                throw ex;
            }
        };
    }

    private Interceptor rateLimitInterceptor() {
        return chain -> {
            waitForPermit();
            return chain.proceed(chain.request());
        };
    }

    private Interceptor retryInterceptor() {
        return chain -> {
            Request request = chain.request();
            int maxAttempts = Math.max(1, retryMaxAttempts);
            IOException lastException = null;

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    Response response = chain.proceed(request);
                    if (!shouldRetry(request, response.code()) || attempt == maxAttempts) {
                        return response;
                    }
                    response.close();
                } catch (IOException ex) {
                    lastException = ex;
                    if (!isRetryableRequest(request) || attempt == maxAttempts) {
                        throw ex;
                    }
                }
                sleepBeforeRetry(attempt);
            }

            throw lastException == null
                    ? new IOException("HTTP retry failed")
                    : lastException;
        };
    }

    private boolean shouldRetry(Request request, int statusCode) {
        if (!isRetryableRequest(request)) return false;
        return statusCode == 429 || statusCode >= 500;
    }

    private boolean isCircuitFailure(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private boolean isRetryableRequest(Request request) {
        String method = request.method().toUpperCase();
        if (Set.of("GET", "HEAD", "OPTIONS").contains(method)) return true;
        return request.header("Idempotency-Key") != null
                || request.header("X-Idempotency-Key") != null;
    }

    private void sleepBeforeRetry(int attempt) throws IOException {
        try {
            Thread.sleep(Math.max(0, retryBackoffMs) * attempt);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("HTTP retry interrupted", ex);
        }
    }

    private void waitForPermit() throws IOException {
        long interval = Math.max(0, minIntervalMs);
        if (interval == 0) return;

        while (true) {
            long now = System.currentTimeMillis();
            long currentNext = nextRequestAtMillis.get();
            long permittedAt = Math.max(now, currentNext);
            long newNext = permittedAt + interval;
            if (nextRequestAtMillis.compareAndSet(currentNext, newNext)) {
                sleepUntil(permittedAt);
                return;
            }
        }
    }

    private static void sleepUntil(long permittedAtMillis) throws IOException {
        long sleepMs = permittedAtMillis - System.currentTimeMillis();
        if (sleepMs <= 0) return;
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("HTTP rate limit interrupted", ex);
        }
    }

    private void recordCircuitFailure(String key, CircuitState state) {
        int failures = state.failures.incrementAndGet();
        if (failures >= Math.max(1, circuitBreakerFailureThreshold)) {
            state.openUntilMillis.set(System.currentTimeMillis() + Math.max(1, circuitBreakerOpenMs));
        }
    }

    private static void recordCircuitSuccess(CircuitState state) {
        state.failures.set(0);
        state.openUntilMillis.set(0);
    }

    private static String circuitKey(Request request) {
        return request.url().scheme() + "://" + request.url().host() + ":" + request.url().port();
    }

    private static final class CircuitState {
        private final AtomicInteger failures = new AtomicInteger();
        private final AtomicLong openUntilMillis = new AtomicLong();
    }
}
