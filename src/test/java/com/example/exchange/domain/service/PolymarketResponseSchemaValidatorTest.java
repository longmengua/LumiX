/*
 * 檔案用途：測試 Polymarket Gamma/CLOB response schema version wrapper。
 */
package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.PolymarketResponseSchemaReport;
import com.example.exchange.domain.util.PolymarketResponseSchemaValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolymarketResponseSchemaValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Gamma events schema wrapper 記錄版本並容忍新增欄位")
    /**
     * 流程：Gamma /events 回傳既有必填欄位，同時新增遠端欄位。
     * 期望：wrapper 標記 gamma.events.v1、compatible=true，並把新增欄位列入 drift report。
     */
    void gammaEventsWrapperReportsUnknownFieldsWithoutBreakingCompatibility() {
        String body = """
                [
                  {
                    "slug": "fifwc-mex-rsa-2026-06-11",
                    "title": "Mexico vs South Africa",
                    "seriesSlug": "soccer-fifwc",
                    "remoteNewField": "new-shape"
                  }
                ]
                """;

        PolymarketResponseSchemaReport report =
                PolymarketResponseSchemaValidator.validateGammaEventsPage(objectMapper, "/events", body);

        assertThat(report.schemaVersion()).isEqualTo(PolymarketResponseSchemaValidator.GAMMA_EVENTS_V1);
        assertThat(report.source()).isEqualTo("GAMMA");
        assertThat(report.itemCount()).isEqualTo(1);
        assertThat(report.compatible()).isTrue();
        assertThat(report.missingFields()).isEmpty();
        assertThat(report.unknownFields()).containsExactly("remoteNewField");
    }

    @Test
    @DisplayName("Gamma market schema wrapper 缺 slug 時標記 incompatible")
    /**
     * 流程：Gamma /markets 回傳沒有 slug 的 market，discovery 無法穩定建立 sync key。
     * 期望：wrapper 回報 gamma.markets.v1 不相容，讓 client log 可搜尋到 schema drift。
     */
    void gammaMarketsWrapperReportsMissingRequiredFields() {
        String body = """
                [
                  {
                    "conditionId": "0xabc",
                    "question": "Will Mexico win?"
                  }
                ]
                """;

        PolymarketResponseSchemaReport report =
                PolymarketResponseSchemaValidator.validateGammaMarketsPage(objectMapper, "/markets", body);

        assertThat(report.schemaVersion()).isEqualTo(PolymarketResponseSchemaValidator.GAMMA_MARKETS_V1);
        assertThat(report.compatible()).isFalse();
        assertThat(report.missingFields()).containsExactly("slug");
        assertThat(report.unknownFields()).isEmpty();
    }

    @Test
    @DisplayName("CLOB order operation schema wrapper 標記版本並容忍新增欄位")
    /**
     * 流程：CLOB /order 回成功 payload，遠端加上本地尚未使用的新欄位。
     * 期望：wrapper 使用 clob.order-operations.v1，保持 compatible 並回報 unknown field。
     */
    void clobOrderWrapperReportsVersionAndUnknownFields() {
        String body = """
                {
                  "orderID": "clob-1",
                  "status": "live",
                  "remoteFillPolicy": "ioc"
                }
                """;

        PolymarketResponseSchemaReport report =
                PolymarketResponseSchemaValidator.validateClobOrderOperation(objectMapper, "POST /order", body);

        assertThat(report.schemaVersion()).isEqualTo(PolymarketResponseSchemaValidator.CLOB_ORDER_OPERATIONS_V1);
        assertThat(report.source()).isEqualTo("CLOB");
        assertThat(report.endpoint()).isEqualTo("POST /order");
        assertThat(report.compatible()).isTrue();
        assertThat(report.unknownFields()).containsExactly("remoteFillPolicy");
    }
}
