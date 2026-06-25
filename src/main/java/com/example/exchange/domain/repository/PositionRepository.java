/*
 * 檔案用途：Repository 介面，定義領域層需要的資料存取契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.Position;
import com.example.exchange.domain.model.dto.Symbol;

import java.util.List;
import java.util.Optional;

/**
 * Position 的持久化抽象
 */
public interface PositionRepository {

    /** 取得某使用者在某交易對的倉位（可能不存在） */
    Optional<Position> find(long uid, Symbol symbol);

    /** 儲存（新增或更新）倉位 */
    void save(Position position);

    /** 列出某使用者所有倉位 */
    List<Position> findAllByUid(long uid);

    /** 列出所有非零倉位，用於資金費、強平掃描與對帳。 */
    List<Position> findOpenPositions();
}
