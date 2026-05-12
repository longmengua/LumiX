package com.example.exchange.domain.repository.client;

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
 * 1. 從 Gamma API 拉取 active / open markets
 * 2. 支援分頁全量拉取
 * 3. 保留 marketSlug 精準查詢
 *
 * 注意：
 * - eventSlug 不能用 /markets/slug/{slug} 查
 * - /markets/slug/{slug} 只能查 marketSlug
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
     * 全量拉取 Gamma active / open markets。
     *
     * 對應 TS：
     * fetchAllMarkets()
     */
    public List<PredictionGammaMarketDto> fetchAllActiveMarkets() {
        int limit = 1000;
        int offset = 0;

        List<PredictionGammaMarketDto> all = new ArrayList<>();

        while (true) {
            List<PredictionGammaMarketDto> page = fetchMarketsPage(limit, offset);

            if (page.isEmpty()) {
                break;
            }

            all.addAll(page);

            log.info(
                    "Gamma markets loaded, offset={}, pageSize={}, total={}",
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

    /**
     * 分頁拉取 Gamma markets。
     *
     * API:
     * GET /markets?limit=1000&offset=0&active=true&closed=false
     */
    public List<PredictionGammaMarketDto> fetchMarketsPage(int limit, int offset) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(gammaBaseUrl)
                    .path("/markets")
                    .queryParam("limit", limit)
                    .queryParam("offset", offset)
                    .queryParam("active", true)
                    .queryParam("closed", false)
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
                            "Gamma fetch markets page failed, offset={}, code={}, body={}",
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
                        new TypeReference<List<PredictionGammaMarketDto>>() {
                        }
                );
            }

        } catch (Exception e) {
            log.warn(
                    "Gamma fetch markets page error, limit={}, offset={}",
                    limit,
                    offset,
                    e
            );
            return Collections.emptyList();
        }
    }

    /**
     * 用 marketSlug 精準查單一 market。
     *
     * 注意：
     * 這裡不能傳 eventSlug。
     *
     * 可以查：
     * fifwc-mex-rsa-2026-06-11-mex
     *
     * 不能查：
     * fifwc-mex-rsa-2026-06-11
     */
    public PredictionGammaMarketDto getMarketBySlug(String marketSlug) {
        try {
            String url = gammaBaseUrl + "/markets/slug/" + marketSlug;

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .header("Accept", "application/json")
                    .header("User-Agent", "java21-exchange")
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                String body = response.body() == null ? "" : response.body().string();

                if (!response.isSuccessful()) {
                    log.warn(
                            "Gamma get market by slug failed, marketSlug={}, code={}, body={}",
                            marketSlug,
                            response.code(),
                            body
                    );
                    return null;
                }

                if (body.isBlank()) {
                    return null;
                }

                return objectMapper.readValue(body, PredictionGammaMarketDto.class);
            }

        } catch (Exception e) {
            log.warn("Gamma get market by slug error, marketSlug={}", marketSlug, e);
            return null;
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