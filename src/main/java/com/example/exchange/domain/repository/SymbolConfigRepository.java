package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.entity.SymbolConfig;

import java.util.Optional;

public interface SymbolConfigRepository {

    Optional<SymbolConfig> findBySymbol(String symbol);
}
