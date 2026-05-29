/*
 * 檔案用途：JPA Repository，提供 RPC transaction tracking record 查詢與寫入。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.RpcTransactionRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RpcTransactionRecordRepository
        extends JpaRepository<RpcTransactionRecordEntity, String> {

    List<RpcTransactionRecordEntity> findByCompletedFalseOrderByUpdatedAtAsc();
}
