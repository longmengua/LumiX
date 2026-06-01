/*
 * 檔案用途：insurance fund capital movement durable store contract。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.InsuranceFundMovement;

import java.util.List;

public interface InsuranceFundMovementStore {

    void save(InsuranceFundMovement movement);

    List<InsuranceFundMovement> findRecent(String asset, int limit);
}
