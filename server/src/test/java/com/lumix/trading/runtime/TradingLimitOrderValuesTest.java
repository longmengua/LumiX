package com.lumix.trading.runtime;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 真正接單共用的精度不變量，不重複既有 sandbox 的描述文字與 capability gate。 */
class TradingLimitOrderValuesTest {
    /** 數值尾零等值，驗證結果仍保留具名政策版本，且兩類商品不能靠 symbol 推定為同一個。 */
    @Test
    void exactValuesUseExplicitInstrumentIdentityAndVersion() {
        var spot = rules(TradingInstrumentRules.Kind.SPOT);
        var perpetual = rules(TradingInstrumentRules.Kind.PERPETUAL);
        for (var rule : List.of(spot, perpetual)) {
            var value = TradingLimitOrderValues.validate(rule, "60000.00", "0.0100");
            assertEquals(0, new BigDecimal("600").compareTo(value.notional()));
            assertEquals(7, value.policyVersion());
            assertEquals(new BigDecimal("0.01"), value.quantity());
        }
        assertNotEquals(spot.instrumentId(), perpetual.instrumentId());
    }

    /** 在 BigDecimal 配置前拒絕 exponent、空白、NaN 與超長輸入，禁止不受限解析或默默改數值。 */
    @Test
    void boundedPlainDecimalRejectsAmbiguousAndOversizedInput() {
        for (String value : List.of("1e10", "1e-999999999", "NaN", "Infinity", " 1", "+1", "-1", "0", "1.", ".1", "9".repeat(10000), "0.0000000000000000001")) {
            assertThrows(IllegalArgumentException.class, () -> TradingDecimal.positive(value));
        }
        assertThrows(IllegalArgumentException.class, () -> TradingDecimal.positive(null));
        // 內部規則也可能由設定建立 BigDecimal，不能只保護 HTTP 字串；負 scale 不可使位數減法溢位。
        assertThrows(IllegalArgumentException.class, () -> TradingDecimal.ledgerAmount(new BigDecimal("1E+2147483647")));
    }

    /** 不合法的 tick、step、過小／過大數量或名義額都拒絕，不替使用者四捨五入委託。 */
    @Test
    void precisionAndSizeLimitsFailClosed() {
        var rule = rules(TradingInstrumentRules.Kind.SPOT);
        for (String[] input : List.of(new String[]{"60000.001", "0.01"}, new String[]{"60000", "0.00001"},
                new String[]{"60000", "101"}, new String[]{"1", "0.0001"}, new String[]{"999999999999999999", "100"})) {
            assertThrows(IllegalArgumentException.class, () -> TradingLimitOrderValues.validate(rule, input[0], input[1]));
        }
        assertThrows(NullPointerException.class, () -> TradingLimitOrderValues.validate(null, "1", "1"));
    }

    /** 商品政策本身必須可以精確結算，不能把不支援的本金小數精度留給撮合臨時猜測。 */
    @Test
    void incoherentPolicyAndMissingFeeRulesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TradingInstrumentRules("BAD", TradingInstrumentRules.Kind.SPOT,
                "BTC-USDT", "BTC", "USDT", 1, 2, 8, 6,
                new BigDecimal("0.01"), new BigDecimal("0.00000001"), new BigDecimal("0.00000001"), BigDecimal.ONE,
                BigDecimal.ONE, new BigDecimal("100000"), BigDecimal.ZERO, BigDecimal.ZERO, RoundingMode.DOWN));
        assertThrows(NullPointerException.class, () -> new TradingInstrumentRules("BAD", TradingInstrumentRules.Kind.SPOT,
                "BTC-USDT", "BTC", "USDT", 1, 2, 4, 6,
                new BigDecimal("0.01"), new BigDecimal("0.0001"), new BigDecimal("0.0001"), BigDecimal.ONE,
                BigDecimal.ONE, new BigDecimal("100000"), null, BigDecimal.ZERO, RoundingMode.DOWN));
    }

    private TradingInstrumentRules rules(TradingInstrumentRules.Kind kind) {
        // 數字僅為隔離測試政策，絕不可由 application runtime 載入或作為正式費率。
        return new TradingInstrumentRules("BTC-USDT:" + kind.name(), kind, "BTC-USDT", "BTC", "USDT", 7,
                2, 4, 6, new BigDecimal("0.01"), new BigDecimal("0.0001"), new BigDecimal("0.0001"),
                new BigDecimal("100"), BigDecimal.ONE, new BigDecimal("10000000"),
                new BigDecimal("0.001"), new BigDecimal("0.002"), RoundingMode.CEILING);
    }
}
