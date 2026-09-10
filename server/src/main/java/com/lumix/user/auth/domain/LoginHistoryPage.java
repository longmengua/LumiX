package com.lumix.user.auth.domain;

import java.util.List;
import java.util.Objects;

/**
 * 本人登入紀錄的有界頁面。
 *
 * <p>cursor 僅以已公開的成功登入時間表示；頁面本身不攜帶 session ID、token digest 或其他可被用於
 * 認證的材料。</p>
 */
public record LoginHistoryPage(List<LoginHistoryEntry> records, boolean hasOlder, boolean hasNewer) {

    public LoginHistoryPage {
        records = List.copyOf(Objects.requireNonNull(records, "records must not be null"));
    }
}
