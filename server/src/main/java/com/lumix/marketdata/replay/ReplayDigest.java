package com.lumix.marketdata.replay;

import java.util.Objects;

/** SHA-256 hex digest；輸入由 coordinator 的 canonical plain-string state serialization 產生。 */
public record ReplayDigest(String sha256) {

    public ReplayDigest {
        sha256 = Objects.requireNonNull(sha256, "sha256 must not be null");
        if (!sha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("sha256 must be canonical lowercase hex");
        }
    }
}
