package com.example.exchange.domain.model.dto;

import java.time.Instant;

public record RecoveryResult(
        long uid,
        boolean recovered,
        long snapshotSeq,
        long replayFromSeq,
        int replayedEvents,
        Instant recoveredAt
) {}
