package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.Account;

import java.util.Optional;

/**
 * Account 的持久化抽象（Domain 不關心具體用什麼 DB/Cache）
 *
 * - infra 層會提供實作（例如 Redis、JPA、MyBatis）
 * - Domain/Application 只透過此介面存取資料
 */
public interface AccountRepository {

    /** 依 uid 取得帳戶，若不存在則回傳 Optional.empty() */
    Optional<Account> findByUid(long uid);

    /** 儲存（新增或更新）帳戶狀態 */
    void save(Account account);
}
