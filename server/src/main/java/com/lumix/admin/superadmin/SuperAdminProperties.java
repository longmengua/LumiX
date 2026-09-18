package com.lumix.admin.superadmin;

import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 部署者指定的唯一最高管理員信箱。
 *
 * <p>此值不是授權白名單；它僅能在尚未建立 principal 時決定啟用信寄送目標。啟用後若設定與資料庫
 * principal 不一致，服務必須拒絕啟動，避免透過改環境變數轉移最高權限。</p>
 */
@ConfigurationProperties("lumix.admin.super-admin")
public class SuperAdminProperties {

    private String email = "";
    private String locale = "en-US";

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? "" : email;
    }

    /**
     * bootstrap 信只提供已有受控文案的語系；未知值不可靜默猜測或回退，以免高權限啟用信使用錯誤語言。
     */
    public Locale getLocale() {
        String normalized = locale == null || locale.isBlank() ? "en-US" : locale.trim();
        return switch (normalized) {
            case "en-US" -> Locale.US;
            case "zh-TW" -> Locale.TAIWAN;
            default -> throw new IllegalStateException("LUMIX_ADMIN_SUPER_ADMIN_LOCALE 僅支援 en-US 或 zh-TW");
        };
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
