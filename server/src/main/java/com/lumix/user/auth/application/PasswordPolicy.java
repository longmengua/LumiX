package com.lumix.user.auth.application;

import java.util.regex.Pattern;

/**
 * 新密碼的唯一字元與長度規則。
 *
 * <p>規則不依賴資料庫或 HTTP 層，讓 controller 的早期拒絕與 application service 的最終防線使用相同判斷；
 * 登入既有密碼不應套用此限制，避免歷史帳戶被新的建立規則意外鎖在門外。</p>
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 32;
    private static final Pattern ALLOWED_CHARACTERS = Pattern.compile("[A-Za-z0-9!@#$%^&*()_+\\-=\\[\\]{};:,.?]+");

    private PasswordPolicy() { }

    /** 僅允許產品 UI 已明示的 ASCII 字元，並以字元數限制避免 BCrypt 截斷風險。 */
    public static boolean isValidNewPassword(String password) {
        return password != null
            && password.length() >= MIN_LENGTH
            && password.length() <= MAX_LENGTH
            && ALLOWED_CHARACTERS.matcher(password).matches();
    }
}
