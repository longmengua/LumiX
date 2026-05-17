/*
 * 檔案用途：Web 驗證器，封裝 API request 的自訂驗證規則。
 */
package com.example.exchange.interfaces.web.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 簡單用正則判斷 symbol 是否符合「英數全大寫，且以 USDT 結尾」
 * - 例：BTCUSDT / ETHUSDT / BNBUSDT
 * - 若你需要更多 quote（如 USDⓈ-M, COIN-M），請擴充判斷
 */
public class ValidSymbolValidator implements ConstraintValidator<ValidSymbol, String> {

    private static final String PATTERN = "^[A-Z0-9]{2,}USDT$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;
        return value.matches(PATTERN);
    }
}
