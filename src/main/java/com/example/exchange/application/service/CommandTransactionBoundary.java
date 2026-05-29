/*
 * 檔案用途：應用服務，為核心指令提供明確的資料庫交易邊界。
 */
package com.example.exchange.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 核心指令交易邊界。
 *
 * <p>所有會同時改動訂單、帳務、持倉與 outbox 的 command 都應從這裡進入。
 * 這讓資料庫寫入與 outbox row 保存共用同一個 transaction；真正的外部事件發布
 * 由 OutboxService 延後到 commit 後處理。</p>
 */
@Service
@RequiredArgsConstructor
public class CommandTransactionBoundary {

    private final TransactionTemplate transactionTemplate;

    public <T> T execute(String commandName, Supplier<T> operation) {
        Objects.requireNonNull(commandName, "commandName must not be null");
        Objects.requireNonNull(operation, "operation must not be null");

        return transactionTemplate.execute(status -> operation.get());
    }
}
