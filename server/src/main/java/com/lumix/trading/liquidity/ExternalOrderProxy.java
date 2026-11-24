package com.lumix.trading.liquidity;

import java.util.List;

/** 多家外部交易 adapter 的統一入口；依 source 選取具名 provider，避免交易流程綁定單一交易所。 */
public interface ExternalOrderProxy {
    ExternalOrderRouter.Outcome submit(ExternalOrderRouter.Intent intent);
    ExternalOrderRouter.Outcome lookup(String source, String clientOrderId);
    List<String> sources();
}
