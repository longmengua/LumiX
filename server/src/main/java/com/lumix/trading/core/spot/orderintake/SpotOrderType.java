package com.lumix.trading.core.spot.orderintake;

/**
 * sandbox order 的類型。
 *
 * 目前只把 LIMIT 納入可受理範圍，MARKET 保留為後續 sandbox boundary 的擴充點。
 */
public enum SpotOrderType {
    LIMIT,
    MARKET
}
