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
 * 正式 Deposit Wallet + Session Signer 架構：
 *
 * - approval transaction 必須由前端 MetaMask 自己送
 * - 後端不能代替使用者 approve
 * - 後端只負責：
 *   1. allowance query
 *   2. approval query
 *   3. 下單前 validation
 *
 * 注意：
 * owner 必須是：
 * - deposit wallet
 * - 真正持有資產的 wallet
 *
 * 不是 session signer。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolymarketApprovalService {

    private final Web3j web3j;

    private final PolymarketConfigs polymarketConfigs;

    /**
     * 查指定 owner 的 ERC20 allowance。
     *
     * BUY YES / BUY NO 需要。
     */
    public BigInteger getCollateralAllowance(
            String owner
    ) {
        try {
            return getErc20Allowance(
                    polymarketConfigs.getChain().getCollateralToken(),
                    owner,
                    polymarketConfigs.getChain().getNegRiskExchangeV2()
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
     * 查指定 owner 是否已 ERC1155 approve。
     *
     * SELL YES / SELL NO 需要。
     */
    public Boolean isConditionalTokensApproved(
            String owner
    ) {
        try {
            return isErc1155ApprovedForAll(
                    polymarketConfigs.getChain().getConditionalTokens(),
                    owner,
                    polymarketConfigs.getChain().getNegRiskExchangeV2()
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
     * 下單前檢查 BUY allowance。
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
     * 下單前檢查 SELL approval。
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
     * ERC20 allowance(owner, spender)
     */
    private BigInteger getErc20Allowance(
            String token,
            String owner,
            String spender
    ) throws Exception {

        Function function = new Function(
                "allowance",
                List.of(
                        new Address(owner),
                        new Address(spender)
                ),
                List.of(
                        new TypeReference<Uint256>() {
                        }
                )
        );

        EthCall response = ethCall(
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
     * ERC1155 isApprovedForAll(owner, operator)
     */
    private Boolean isErc1155ApprovedForAll(
            String token,
            String owner,
            String operator
    ) throws Exception {

        Function function = new Function(
                "isApprovedForAll",
                List.of(
                        new Address(owner),
                        new Address(operator)
                ),
                List.of(
                        new TypeReference<Bool>() {
                        }
                )
        );

        EthCall response = ethCall(
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
     * eth_call
     */
    private EthCall ethCall(
            String contractAddress,
            String data
    ) throws Exception {

        Transaction transaction =
                Transaction.createEthCallTransaction(
                        contractAddress,
                        contractAddress,
                        data
                );

        return web3j.ethCall(
                transaction,
                DefaultBlockParameterName.LATEST
        ).send();
    }
}