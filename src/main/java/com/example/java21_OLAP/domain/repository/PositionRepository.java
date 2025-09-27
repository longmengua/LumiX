package com.example.java21_OLAP.domain.repository;

import com.example.java21_OLAP.domain.model.Position;
import com.example.java21_OLAP.domain.model.Symbol;

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
}
