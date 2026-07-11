package com.lumix.trading.core.spot.orderintake;

/**
 * sandbox order 的有效時間條件。
 *
 * 目前只把 GTC 納入可受理範圍，IOC 保留為後續 sandbox boundary 的擴充點。
 */
public enum SpotTimeInForce {
    GTC,
    IOC
}
