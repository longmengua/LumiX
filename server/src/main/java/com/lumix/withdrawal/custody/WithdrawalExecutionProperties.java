package com.lumix.withdrawal.custody;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 提款執行的最後一道預設關閉設定；它不保存 provider credential 或任何簽章材料。 */
@ConfigurationProperties("lumix.withdrawal")
public class WithdrawalExecutionProperties {
    private boolean enabled = false;
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
