package com.lumix.user.auth.domain;

/**
 * 帳戶可綁定的裝置槽位類別。
 *
 * <p>類別只用於每帳戶每類別一台的安全限制，不保存完整 User-Agent，也不嘗試把它當作不可偽造的裝置指紋。</p>
 */
public enum BoundDevicePlatform {
    DESKTOP,
    TABLET,
    MOBILE
}
