/*
 * 檔案用途：測試做市商 quote command 風控 baseline。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.event.MarketMakerQuoteDecisionRecorded;
import com.example.exchange.domain.model.dto.MarketMakerProfile;
import com.example.exchange.domain.model.dto.MarketMakerQuoteCommand;
import com.example.exchange.domain.model.dto.MarketMakerQuoteDecision;
import com.example.exchange.domain.model.dto.MarketMakerRiskLimit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarketMakerQuoteServiceTest {

    @Test
    @DisplayName("kill switch 會拒絕 quote command 並發布 audit event")
    void killSwitchRejectsQuoteCommand() {
        List<MarketMakerQuoteDecisionRecorded> published = new ArrayList<>();
        MarketMakerQuoteService service = new MarketMakerQuoteService(published::add);

        // 流程：做市商 kill switch 開啟時，即使價格合法，也不能繼續報價。
        MarketMakerQuoteDecision decision = service.validateQuote(profile(true), quote("99.00", "101.00"));

        assertThat(decision.accepted()).isFalse();
        assertThat(decision.reason()).isEqualTo("KILL_SWITCH_ENABLED");
        assertThat(published).extracting(MarketMakerQuoteDecisionRecorded::reason)
                .containsExactly("KILL_SWITCH_ENABLED");
    }

    @Test
    @DisplayName("bid >= ask 的 crossed quote 會被拒絕")
    void crossedQuoteIsRejected() {
        List<MarketMakerQuoteDecisionRecorded> published = new ArrayList<>();
        MarketMakerQuoteService service = new MarketMakerQuoteService(published::add);

        // 流程：bid 高於 ask 代表自成交/交叉報價風險，quote command 必須先拒絕。
        MarketMakerQuoteDecision decision = service.validateQuote(profile(false), quote("101.00", "100.00"));

        assertThat(decision.accepted()).isFalse();
        assertThat(decision.reason()).isEqualTo("CROSSED_QUOTE");
        assertThat(published).hasSize(1);
    }

    @Test
    @DisplayName("合法 quote command 會通過並留下 accepted audit event")
    void validQuoteIsAccepted() {
        List<MarketMakerQuoteDecisionRecorded> published = new ArrayList<>();
        MarketMakerQuoteService service = new MarketMakerQuoteService(published::add);

        // 流程：profile 啟用、risk limit 存在、bid < ask 且數量價格為正，quote baseline 可接受。
        MarketMakerQuoteDecision decision = service.validateQuote(profile(false), quote("99.00", "101.00"));

        assertThat(decision.accepted()).isTrue();
        assertThat(decision.reason()).isNull();
        assertThat(published.getFirst().accepted()).isTrue();
    }

    private static MarketMakerProfile profile(boolean killSwitch) {
        return new MarketMakerProfile(
                "mm-quote-1",
                9101,
                true,
                List.of(new MarketMakerRiskLimit(
                        "BTCUSDT",
                        new BigDecimal("1000000"),
                        new BigDecimal("1000000"),
                        new BigDecimal("10000"),
                        new BigDecimal("0.01"),
                        killSwitch
                ))
        );
    }

    private static MarketMakerQuoteCommand quote(String bidPrice, String askPrice) {
        return new MarketMakerQuoteCommand(
                "mm-quote-1",
                9101,
                "BTCUSDT",
                new BigDecimal(bidPrice),
                new BigDecimal("1.000"),
                new BigDecimal(askPrice),
                new BigDecimal("1.000"),
                "quote-ref-1"
        );
    }
}
