package com.lumix.spot;

import com.lumix.account.UserId;
import com.lumix.common.RequestId;

/**
 * 撮合核心接入層契約。
 * 正式流程必須對接 C++ Core，本介面不能替代正式撮合。
 */
public interface MatchingEngineClient {

    // TODO(HUMAN_REVIEW_REQUIRED): 將訂單送往 matching core；正式串接前必須明確定義命令與回應格式。
    String submitOrder(SpotOrderRequest request);

    // TODO(HUMAN_REVIEW_REQUIRED): 請求取消訂單；正式版本需考慮重試與重複請求的處理。
    boolean cancelOrder(RequestId requestId, UserId userId, String orderId);
}
