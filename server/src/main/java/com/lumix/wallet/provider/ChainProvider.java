package com.lumix.wallet.provider;

import com.lumix.wallet.ChainType;

import java.time.Instant;
import java.util.Set;
import java.util.List;

/**
 * 真實鏈上 provider 的最小隔離介面。
 *
 * <p>此 contract 不提供 default、mock 或 fake adapter。實作必須使用具名 provider 的受控 RPC、
 * secret isolation 與 rate-limit policy；一般 API controller 不得直接取得 provider credential。</p>
 */
public interface ChainProvider {

    /** 穩定的 provider 識別，例如 ALCHEMY、TRONGRID；不可使用 endpoint 或 secret 作為 identity。 */
    String providerId();

    /** provider 可服務的鏈集合；一個 provider adapter 可安全支援多鏈，但每筆 observation 仍須帶明確 chain identity。 */
    Set<ChainType> supportedChains();

    /** provider 已實作且可被 registry 選擇的能力集合。 */
    Set<ChainProviderCapability> capabilities();

    /** 回傳可稽核的 provider health，無法連線或資料過期時實作必須 fail-closed。 */
    ChainProviderHealth health();

    /**
     * 讀取自已確認 cursor 之後的 immutable chain observations。
     *
     * <p>回傳內容只能作為 P22 observation input，不能直接 credit ledger 或 balance。</p>
     */
    List<ChainProviderObservation> observeAfter(String cursor, Instant observedBefore);
}
