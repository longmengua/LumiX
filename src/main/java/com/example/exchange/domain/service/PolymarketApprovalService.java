package com.example.exchange.domain.service;

import com.example.exchange.infra.config.PolymarketConfigs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.math.BigInteger;
import java.util.List;

/**
 * Polymarket approval query service。
 *
 * Deposit Wallet + Session Signer 架構：
 *
 * - approval transaction 必須由前端 MetaMask 送出
 * - 後端不能代替使用者 approve
 * - 後端只負責：
 *   1. ERC20 allowance query
 *   2. ERC1155 approval query
 *   3. placeOrder 前 validation
 *
 * 注意：
 * owner 必須是 deposit wallet。
 * 不是 session signer。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolymarketApprovalService {

    private final Web3j web3j;

    private final PolymarketConfigs polymarketConfigs;

    /**
     * 查指定 owner 的 USDC allowance。
     *
     * BUY YES / BUY NO 需要。
     */
    public BigInteger getCollateralAllowance(
            String owner
    ) {
        try {
            String token =
                    polymarketConfigs.getChain().getCollateralToken();

            String spender =
                    polymarketConfigs.getChain().getNegRiskExchangeV2();

            log.info(
                    "[Approval] Query ERC20 allowance. token={}, owner={}, spender={}",
                    token,
                    owner,
                    spender
            );

            return getErc20Allowance(
                    token,
                    owner,
                    spender
            );

        } catch (Exception e) {
            log.error(
                    "Get collateral allowance failed. owner={}",
                    owner,
                    e
            );

            throw new IllegalStateException(
                    "Get collateral allowance failed: "
                            + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 查指定 owner 是否已 approve Conditional Tokens。
     *
     * SELL YES / SELL NO 需要。
     */
    public Boolean isConditionalTokensApproved(
            String owner
    ) {
        try {
            String token =
                    polymarketConfigs.getChain().getConditionalTokens();

            String operator =
                    polymarketConfigs.getChain().getNegRiskExchangeV2();

            log.info(
                    "[Approval] Query ERC1155 approval. token={}, owner={}, operator={}",
                    token,
                    owner,
                    operator
            );

            return isErc1155ApprovedForAll(
                    token,
                    owner,
                    operator
            );

        } catch (Exception e) {
            log.error(
                    "Get conditional token approval failed. owner={}",
                    owner,
                    e
            );

            throw new IllegalStateException(
                    "Get conditional token approval failed: "
                            + e.getMessage(),
                    e
            );
        }
    }

    /**
     * BUY 下單前檢查 USDC allowance。
     */
    public void requireCollateralAllowance(
            String owner,
            BigInteger requiredAmountAtomic
    ) {
        BigInteger allowance =
                getCollateralAllowance(owner);

        if (allowance.compareTo(requiredAmountAtomic) < 0) {
            throw new IllegalStateException(
                    "Insufficient collateral allowance. owner="
                            + owner
                            + ", required="
                            + requiredAmountAtomic
                            + ", allowance="
                            + allowance
                            + ". Please approve collateral from deposit wallet first."
            );
        }
    }

    /**
     * SELL 下單前檢查 ERC1155 approval。
     */
    public void requireConditionalTokensApproval(
            String owner
    ) {
        Boolean approved =
                isConditionalTokensApproved(owner);

        if (!Boolean.TRUE.equals(approved)) {
            throw new IllegalStateException(
                    "Conditional tokens not approved. owner="
                            + owner
                            + ". Please approve conditional tokens from deposit wallet first."
            );
        }
    }

    /**
     * ERC20 allowance(owner, spender)。
     */
    private BigInteger getErc20Allowance(
            String token,
            String owner,
            String spender
    ) throws Exception {

        validateAddress(token, "collateral token");
        validateAddress(owner, "owner");
        validateAddress(spender, "spender");

        Function function =
                new Function(
                        "allowance",
                        List.of(
                                addressParam(owner),
                                addressParam(spender)
                        ),
                        List.of(
                                new TypeReference<Uint256>() {
                                }
                        )
                );

        EthCall response =
                ethCall(
                        token,
                        FunctionEncoder.encode(function)
                );

        if (response.hasError()) {
            throw new IllegalStateException(
                    response.getError().getMessage()
            );
        }

        List<?> decoded =
                FunctionReturnDecoder.decode(
                        response.getValue(),
                        function.getOutputParameters()
                );

        if (decoded.isEmpty()) {
            return BigInteger.ZERO;
        }

        Uint256 value =
                (Uint256) decoded.get(0);

        return value.getValue();
    }

    /**
     * ERC1155 isApprovedForAll(owner, operator)。
     */
    private Boolean isErc1155ApprovedForAll(
            String token,
            String owner,
            String operator
    ) throws Exception {

        validateAddress(token, "conditional token");
        validateAddress(owner, "owner");
        validateAddress(operator, "operator");

        Function function =
                new Function(
                        "isApprovedForAll",
                        List.of(
                                addressParam(owner),
                                addressParam(operator)
                        ),
                        List.of(
                                new TypeReference<Bool>() {
                                }
                        )
                );

        EthCall response =
                ethCall(
                        token,
                        FunctionEncoder.encode(function)
                );

        if (response.hasError()) {
            throw new IllegalStateException(
                    response.getError().getMessage()
            );
        }

        List<?> decoded =
                FunctionReturnDecoder.decode(
                        response.getValue(),
                        function.getOutputParameters()
                );

        if (decoded.isEmpty()) {
            return false;
        }

        Bool value =
                (Bool) decoded.get(0);

        return value.getValue();
    }

    /**
     * 建立 ABI address 參數。
     *
     * 注意：
     * 使用 new Address(address)，不要使用 new Address(160, address)。
     */
    private Address addressParam(
            String address
    ) {
        validateAddress(address, "address");

        return new Address(address);
    }

    /**
     * address 基礎檢查。
     */
    private void validateAddress(
            String address,
            String name
    ) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException(
                    name + " address is blank"
            );
        }

        if (!address.matches("^0x[0-9a-fA-F]{40}$")) {
            throw new IllegalArgumentException(
                    name + " address invalid: " + address
            );
        }
    }

    /**
     * eth_call。
     *
     * from 可為 null。
     * to 必須是 contract address。
     */
    private EthCall ethCall(
            String contractAddress,
            String data
    ) throws Exception {

        validateAddress(contractAddress, "contract");

        Transaction transaction =
                Transaction.createEthCallTransaction(
                        null,
                        contractAddress,
                        data
                );

        return web3j.ethCall(
                transaction,
                DefaultBlockParameterName.LATEST
        ).send();
    }
}