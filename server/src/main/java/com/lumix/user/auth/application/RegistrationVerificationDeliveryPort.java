package com.lumix.user.auth.application;

import com.lumix.user.auth.domain.RegistrationVerificationSecret;

/**
 * 註冊 email 雙驗證碼的輸出邊界。
 *
 * <p>application layer 不知道 SMTP 或供應商細節；adapter 不得把兩組原始 code 寫進 log、例外或監控標籤。</p>
 */
public interface RegistrationVerificationDeliveryPort {

    boolean isAvailable();

    void deliver(String email, RegistrationVerificationSecret secret);
}
