package com.lumix.withdrawal.signing;

/** signer capability 的部署型態標記；僅作 policy 分流，絕不承載或取得 secret。 */
public enum WithdrawalSignerProviderKind { HSM, MPC, EXTERNAL_SIGNER }
