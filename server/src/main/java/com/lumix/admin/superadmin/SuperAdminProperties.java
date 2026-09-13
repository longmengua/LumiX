package com.lumix.admin.superadmin;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? "" : email;
    }
}
