package com.lumix.ethereum.scanner;

import com.lumix.ethereum.asset.EthereumAssetRegistry;
import java.util.Map;
import java.util.Optional;

/** 只處理官方合約 Transfer 到 active managed address 的 USDT evidence。 */
public final class EthereumUsdtDepositScanner {
    public Optional<EthereumDepositCandidate> detect(EthereumUsdtTransferLog transfer, Map<String, String> activeAddressAccounts) {
        String accountId = activeAddressAccounts.get(transfer.toAddress());
        if (accountId == null) return Optional.empty();
        return Optional.of(new EthereumDepositCandidate(accountId, "USDT", transfer.transactionHash(), transfer.logIndex(),
                transfer.blockNumber(), transfer.blockHash(), transfer.fromAddress(), transfer.toAddress(),
                Optional.of(EthereumAssetRegistry.USDT_CONTRACT), transfer.amountRaw()));
    }
}
