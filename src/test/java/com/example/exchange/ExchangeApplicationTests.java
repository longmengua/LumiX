/*
 * 檔案用途：Java 原始碼檔案，屬於 java21-match-hub 交易服務。
 */
package com.example.exchange;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 最小 Spring Boot application smoke test。
 *
 * <p>此測試只確認 application class 可被載入，避免基礎 package 或 classpath 破裂。</p>
 */
class ExchangeApplicationTests {

	@Test
	@DisplayName("ExchangeApplication class 可載入")
	void applicationClassIsLoadable() {
		assertNotNull(ExchangeApplication.class);
	}

}
