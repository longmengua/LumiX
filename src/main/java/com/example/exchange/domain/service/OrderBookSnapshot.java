package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.PriceLevel;

import java.util.List;

/**
 * 訂單簿快照：bids / asks 各自的價量列表（已按價格排序）
 * - bids：由高到低
 * - asks：由低到高
 */
public record OrderBookSnapshot(List<PriceLevel> bids, List<PriceLevel> asks) {}
