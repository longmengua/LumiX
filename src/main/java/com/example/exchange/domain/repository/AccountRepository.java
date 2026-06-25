/*
 * 檔案用途：Repository 介面，定義領域層需要的資料存取契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.Account;

import java.util.List;
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

    /** 查詢已知帳戶；不支援掃描的實作可回傳空清單。 */
    default List<Account> findAll() {
        return List.of();
    }

    /** 儲存（新增或更新）帳戶狀態 */
    void save(Account account);
}
