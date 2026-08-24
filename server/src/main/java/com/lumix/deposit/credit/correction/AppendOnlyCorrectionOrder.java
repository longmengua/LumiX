package com.lumix.deposit.credit.correction;

/**
 * 原 credit 與未來 reversal 的不可覆寫相對順序。
 */
public record AppendOnlyCorrectionOrder(long originalAppendSequence, long reversalAppendSequence) {

    public AppendOnlyCorrectionOrder {
        if (originalAppendSequence < 0 || reversalAppendSequence <= originalAppendSequence) {
            throw new IllegalArgumentException("reversal append sequence must strictly follow original append sequence");
        }
    }
}
