/*
 * 檔案用途：外部 API client 抽象，隔離 Polymarket Gamma/CLOB 等遠端服務呼叫。
 */
package com.example.exchange.domain.repository.client;

import com.example.exchange.domain.model.dto.PredictionGammaEventDto;
import com.example.exchange.domain.model.dto.PredictionGammaMarketDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Polymarket Gamma Market Client。
 *
 * 用途：
 * 1. 從 Gamma /events 拉取 FIFA World Cup events
 * 2. 將 event 內 markets flatten 給 price refresh 使用
 * 3. 保留 search fallback 給單一 key retry 使用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PredictionGammaMarketClient {

    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;

    @Value("${polymarket.gamma.base-url:https://gamma-api.polymarket.com}")
    private String gammaBaseUrl;

    /**
     * 拉取 FIFA World Cup active / open events。
     *
     * Gamma 目前 sports event 的 canonical source 是 /events，
     * event 內會直接帶 markets。
     */
    public List<PredictionGammaEventDto> fetchFifaWorldCupEvents() {
        int limit = 100;
        int offset = 0;

        List<PredictionGammaEventDto> all = new ArrayList<>();

        while (true) {
            List<PredictionGammaEventDto> page =
                    fetchFifaWorldCupEventsPage(limit, offset);

            if (page.isEmpty()) {
                break;
            }

            all.addAll(page);

            log.info(
                    "Gamma FIFA events loaded, offset={}, pageSize={}, total={}",
                    offset,
                    page.size(),
                    all.size()
            );

            if (page.size() < limit) {
                break;
            }

            offset += limit;

            sleepQuietly(300);
        }

        return all;
    }

    public List<PredictionGammaMarketDto> fetchFifaWorldCupMarkets() {
        return fetchFifaWorldCupEvents()
                .stream()
                .filter(event -> event.getMarkets() != null)
                .flatMap(event -> event.getMarkets().stream())
                .filter(market -> market.getSlug() != null && !market.getSlug().isBlank())
                .toList();
    }

    public List<PredictionGammaEventDto> fetchFifaWorldCupEventsPage(
            int limit,
            int offset
    ) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(gammaBaseUrl)
                    .path("/events")
                    .queryParam("limit", limit)
                    .queryParam("offset", offset)
                    .queryParam("active", true)
                    .queryParam("closed", false)
                    .queryParam("series_slug", "soccer-fifwc")
                    .toUriString();

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .header("Accept", "application/json")
                    .header("User-Agent", "java21-exchange")
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                String body = response.body() == null ? "[]" : response.body().string();

                if (!response.isSuccessful()) {
                    log.warn(
                            "Gamma fetch FIFA events page failed, offset={}, code={}, body={}",
                            offset,
                            response.code(),
                            body
                    );
                    return Collections.emptyList();
                }

                if (body.isBlank()) {
                    return Collections.emptyList();
                }

                return objectMapper.readValue(
                        body,
                        new TypeReference<List<PredictionGammaEventDto>>() {
                        }
                );
            }
        } catch (Exception e) {
            log.warn(
                    "Gamma fetch FIFA events page error, limit={}, offset={}",
                    limit,
                    offset,
                    e
            );
            return Collections.emptyList();
        }
    }

    /**
     * fallback：模糊搜尋。
     *
     * 不建議主流程使用。
     * 因為 search 容易命中非 FIFA World Cup market。
     */
    public List<PredictionGammaMarketDto> searchMarkets(String keyword) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(gammaBaseUrl)
                    .path("/markets")
                    .queryParam("search", keyword)
                    .queryParam("active", true)
                    .queryParam("closed", false)
                    .queryParam("limit", 50)
                    .toUriString();

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .header("Accept", "application/json")
                    .header("User-Agent", "java21-exchange")
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                String body = response.body() == null ? "[]" : response.body().string();

                if (!response.isSuccessful()) {
                    log.warn(
                            "Gamma search failed, keyword={}, code={}, body={}",
                            keyword,
                            response.code(),
                            body
                    );
                    return Collections.emptyList();
                }

                if (body.isBlank()) {
                    return Collections.emptyList();
                }

                return objectMapper.readValue(
                        body,
                        new TypeReference<List<PredictionGammaMarketDto>>() {
                        }
                );
            }

        } catch (Exception e) {
            log.warn("Gamma search error, keyword={}", keyword, e);
            return Collections.emptyList();
        }
    }

    /**
     * 避免分頁請求打太快。
     */
    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
