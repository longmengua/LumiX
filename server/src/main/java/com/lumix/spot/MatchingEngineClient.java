package com.lumix.spot;

import com.lumix.account.UserId;
import com.lumix.common.RequestId;

/**
 * 撮合核心接入層契約。
 * 正式流程必須對接 C++ Core，本介面不能替代正式撮合。
 */
public interface MatchingEngineClient {

    // TODO: requires high-reasoning review before production use
    String submitOrder(SpotOrderRequest request);

    // TODO: requires high-reasoning review before production use
    boolean cancelOrder(RequestId requestId, UserId userId, String orderId);
}
