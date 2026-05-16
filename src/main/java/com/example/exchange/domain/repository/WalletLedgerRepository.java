package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.entity.WalletLedgerEntry;

import java.util.List;

public interface WalletLedgerRepository {

    void append(WalletLedgerEntry entry);

    List<WalletLedgerEntry> findByUid(long uid);

    List<WalletLedgerEntry> findByRefId(String refId);
}
