package com.lumix.deposit.address;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DepositAddressOwnershipPolicyTest {

    private final DepositAddressOwnershipPolicy policy = new DepositAddressOwnershipPolicy();

    @Test
    void canonicalNetworkAndEvmAddressMakeEquivalentInputOneOwnershipIdentity() {
        // EVM 大小寫不能讓同一地址繞過跨 user ownership 防線。
        DepositNetwork network = new DepositNetwork("eth_mainnet", DepositAddressFormat.EVM_HEX);
        DepositAddressOwnership first = ownership("user-a", "USDT", network, "0xAbCd000000000000000000000000000000000000");
        DepositAddressOwnership equivalent = ownership("user-a", "USDT", network, "0xabcd000000000000000000000000000000000000");

        assertEquals("ETH_MAINNET", network.code());
        assertEquals(first.networkAddressKey(), equivalent.networkAddressKey());
        assertEquals(DepositAddressOwnershipDecision.DUPLICATE_IGNORED,
                policy.evaluate(Map.of(first.networkAddressKey(), first), equivalent).decision());
    }

    @Test
    void differentOwnerCannotClaimTheSameCanonicalNetworkAddress() {
        // 所有權衝突必須在 credit 前 fail closed，不能以 asset 不同而交叉歸屬。
        DepositNetwork network = new DepositNetwork("ETH_MAINNET", DepositAddressFormat.EVM_HEX);
        DepositAddressOwnership first = ownership("user-a", "USDT", network, "0xabcdef0000000000000000000000000000000000");
        DepositAddressOwnership conflict = ownership("user-b", "USDC", network, "0xabcdef0000000000000000000000000000000000");

        assertEquals(DepositAddressOwnershipDecision.CONFLICTING_OWNER_REJECTED,
                policy.evaluate(Map.of(first.networkAddressKey(), first), conflict).decision());
    }

    @Test
    void invalidFormatAndWrongBech32CaseFailClosed() {
        // 沒有 provider/checksum runtime 時，格式不明確就拒絕，不嘗試猜測或修正地址。
        DepositNetwork evm = new DepositNetwork("ETH_MAINNET", DepositAddressFormat.EVM_HEX);
        DepositNetwork bech32 = new DepositNetwork("BTC_MAINNET", DepositAddressFormat.BECH32);
        assertThrows(IllegalArgumentException.class, () -> DepositAddress.from("0x1234", evm));
        assertThrows(IllegalArgumentException.class, () -> DepositAddress.from("BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KYGT080", bech32));
    }

    private static DepositAddressOwnership ownership(String user, String asset, DepositNetwork network, String address) {
        return new DepositAddressOwnership(
                new UserId(user), new AssetSymbol(asset), network, DepositAddress.from(address, network), DepositAddressLifecycle.ACTIVE
        );
    }
}
