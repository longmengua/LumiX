package com.lumix.ethereum.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EthereumAssetRegistryTest {
    @Test
    void onlyOfficialMainnetUsdtContractIsAdmitted() {
        EthereumAssetRegistry registry = new EthereumAssetRegistry();

        assertEquals("USDT", registry.findErc20ByContract("0xdAC17F958D2ee523a2206206994597C13D831ec7").orElseThrow().assetCode());
        // 同名或任意 ERC-20 不得靠前端 contract 輸入混入 scanner。
        assertFalse(registry.findErc20ByContract("0x0000000000000000000000000000000000000001").isPresent());
        assertThrows(IllegalArgumentException.class, () -> registry.requireAsset("USDC"));
    }
}
