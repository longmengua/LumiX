/*
 * 檔案用途：Spring Data JPA repository，提供 durable DLQ event 存取。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.DlqEventRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DlqEventRecordJpaRepository
        extends JpaRepository<DlqEventRecord, String> {

    List<DlqEventRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
