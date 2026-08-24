package com.lumix.deposit.observation;

/**
 * provider 回報的 finality 事實快照，尚未構成 credit 決定。
 *
 * <p>後續確認數、reorg 與 health policy 必須自行驗證此觀測；不能因 provider 的 boolean 直接改帳。</p>
 */
public record ObservationFinality(long confirmationCount, boolean providerReportedFinal) {

    public ObservationFinality {
        if (confirmationCount < 0) {
            throw new IllegalArgumentException("confirmation count must not be negative");
        }
    }
}
