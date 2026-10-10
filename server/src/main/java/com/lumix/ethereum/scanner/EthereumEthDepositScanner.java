package com.lumix.ethereum.scanner;

import java.util.Map;
import java.util.Optional;

/** 只將 managed address 的正值直接 ETH transaction 轉成 candidate。 */
public final class EthereumEthDepositScanner {
    public Optional<EthereumDepositCandidate> detect(EthereumDirectTransfer transfer, Map<String, String> activeAddressAccounts) {
        Optional<String> recipient = transfer.toAddress();
        if (transfer.valueWei().signum() == 0 || recipient.isEmpty()) return Optional.empty();
        String accountId = activeAddressAccounts.get(recipient.orElseThrow());
        if (accountId == null) return Optional.empty();
        return Optional.of(new EthereumDepositCandidate(accountId, "ETH", transfer.transactionHash(), 0L,
                transfer.blockNumber(), transfer.blockHash(), transfer.fromAddress(), recipient.orElseThrow(), Optional.empty(), transfer.valueWei()));
    }
}
