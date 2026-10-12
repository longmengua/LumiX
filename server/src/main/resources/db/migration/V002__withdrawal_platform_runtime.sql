-- 提款平台 runtime 擴充。這份 migration 只延伸既有 withdrawals 與既有金融核心；不建立第二份提款主檔。
-- 回滾：不得直接 DROP。本 migration 含不可變提款、審核及鏈上證據，回滾需先由人工完成資料保全與停用流程。

ALTER TABLE withdrawals DROP CONSTRAINT ck_withdrawals_status;
ALTER TABLE withdrawals ADD COLUMN user_id VARCHAR(64);
ALTER TABLE withdrawals ADD COLUMN network VARCHAR(32) NOT NULL DEFAULT 'ETHEREUM';
ALTER TABLE withdrawals ADD COLUMN destination_address_id UUID;
ALTER TABLE withdrawals ADD COLUMN normalized_address VARCHAR(64);
ALTER TABLE withdrawals ADD COLUMN chain_id BIGINT;
ALTER TABLE withdrawals ADD COLUMN token_contract VARCHAR(64);
ALTER TABLE withdrawals ADD COLUMN reservation_id VARCHAR(64);
ALTER TABLE withdrawals ADD COLUMN approval_payload_hash CHAR(64);
ALTER TABLE withdrawals ADD COLUMN created_ip VARCHAR(64);
ALTER TABLE withdrawals ADD COLUMN created_device_id UUID;
ALTER TABLE withdrawals ADD COLUMN request_fingerprint CHAR(64);
ALTER TABLE withdrawals ADD COLUMN completed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE withdrawals ADD CONSTRAINT fk_withdrawals_user FOREIGN KEY (user_id) REFERENCES users(user_id);
ALTER TABLE withdrawals ADD CONSTRAINT fk_withdrawals_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id);
ALTER TABLE withdrawals ADD CONSTRAINT ck_withdrawals_status CHECK (status IN ('REQUESTED','FUNDS_HELD','RISK_PENDING','RISK_HOLD','MANUAL_REVIEW','APPROVED','REJECTED','FINAL_RISK_CHECK','SIGN_PENDING','SIGNING','SIGNED','BROADCASTING','BROADCASTED','CONFIRMING','COMPLETED','FAILED','CANCELLED'));
ALTER TABLE withdrawals ADD CONSTRAINT ck_withdrawals_network CHECK (network IN ('ETHEREUM'));
ALTER TABLE withdrawals ADD CONSTRAINT ck_withdrawals_runtime_chain CHECK (chain_id IS NULL OR chain_id IN (1,11155111));
CREATE UNIQUE INDEX uq_withdrawals_owner_request_fingerprint ON withdrawals(user_id, request_id, request_fingerprint) WHERE user_id IS NOT NULL;
CREATE INDEX idx_withdrawals_user_created ON withdrawals(user_id, created_at DESC);
CREATE INDEX idx_withdrawals_runtime_status ON withdrawals(status, created_at);
COMMENT ON TABLE withdrawals IS '提款唯一主表。保存不可變請求快照與受控 runtime 狀態，不得另建第二套提款主檔。';
COMMENT ON COLUMN withdrawals.request_fingerprint IS '使用者、資產、網路、金額與地址的 SHA-256 指紋，用於 durable idempotency 衝突判斷。';
COMMENT ON COLUMN withdrawals.approval_payload_hash IS '人工核准的 canonical payload SHA-256；簽章前必須重新計算並相等。';

CREATE TABLE withdrawal_addresses (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(user_id),
    network VARCHAR(32) NOT NULL,
    asset_symbol VARCHAR(32) NOT NULL REFERENCES assets(asset_symbol),
    address VARCHAR(256) NOT NULL,
    normalized_address VARCHAR(64) NOT NULL,
    label VARCHAR(128),
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMP WITH TIME ZONE,
    cooldown_until TIMESTAMP WITH TIME ZONE NOT NULL,
    activated_at TIMESTAMP WITH TIME ZONE,
    disabled_at TIMESTAMP WITH TIME ZONE,
    created_ip VARCHAR(64),
    created_device_id UUID,
    CONSTRAINT ck_withdrawal_address_network CHECK (network = 'ETHEREUM'),
    CONSTRAINT ck_withdrawal_address_status CHECK (status IN ('PENDING','COOLDOWN','ACTIVE','DISABLED')),
    CONSTRAINT uq_withdrawal_address_identity UNIQUE(user_id, network, asset_symbol, normalized_address)
);
CREATE INDEX idx_withdrawal_addresses_owner ON withdrawal_addresses(user_id, status);
COMMENT ON TABLE withdrawal_addresses IS '使用者提款地址簿；停用採狀態轉換，不可實體刪除，以保留提款歷史與稽核。';

CREATE TABLE withdrawal_risk_assessments (
    id UUID PRIMARY KEY, withdrawal_id VARCHAR(64) NOT NULL REFERENCES withdrawals(withdrawal_id),
    decision VARCHAR(16) NOT NULL, score INTEGER NOT NULL, evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    engine_version VARCHAR(64) NOT NULL,
    CONSTRAINT ck_withdrawal_risk_decision CHECK (decision IN ('ALLOW','MANUAL_REVIEW','DENY','HOLD'))
);
CREATE INDEX idx_withdrawal_risk_assessments_withdrawal ON withdrawal_risk_assessments(withdrawal_id, evaluated_at DESC);
CREATE TABLE withdrawal_risk_triggers (
    id UUID PRIMARY KEY, assessment_id UUID NOT NULL REFERENCES withdrawal_risk_assessments(id),
    rule_code VARCHAR(64) NOT NULL, score_delta INTEGER NOT NULL, decision_effect VARCHAR(16) NOT NULL,
    evidence_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT ck_withdrawal_risk_trigger_effect CHECK (decision_effect IN ('ALLOW','MANUAL_REVIEW','DENY','HOLD'))
);
COMMENT ON TABLE withdrawal_risk_assessments IS '提款風控決策不可變證據；未知依賴或錯誤必須導向 HOLD，不得默許放行。';

CREATE TABLE withdrawal_reviews (
    id UUID PRIMARY KEY, withdrawal_id VARCHAR(64) NOT NULL REFERENCES withdrawals(withdrawal_id),
    reviewer_user_id VARCHAR(64) NOT NULL REFERENCES users(user_id), reviewer_role VARCHAR(64) NOT NULL,
    decision VARCHAR(16) NOT NULL, reason VARCHAR(512) NOT NULL, approval_payload_hash CHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_withdrawal_review_decision CHECK (decision IN ('APPROVE','REJECT','ESCALATE')),
    CONSTRAINT uq_withdrawal_review_reviewer UNIQUE(withdrawal_id, reviewer_user_id)
);
CREATE TABLE withdrawal_signing_requests (
    id UUID PRIMARY KEY, withdrawal_id VARCHAR(64) NOT NULL REFERENCES withdrawals(withdrawal_id),
    intent_hash CHAR(64) NOT NULL, payload_json JSONB NOT NULL, status VARCHAR(16) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL, signed_raw_transaction TEXT, signed_transaction_hash CHAR(66),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_withdrawal_signing_request_status CHECK (status IN ('PENDING','SIGNED','REJECTED')),
    CONSTRAINT uq_withdrawal_signing_intent UNIQUE(withdrawal_id, intent_hash),
    CONSTRAINT uq_withdrawal_signing_withdrawal UNIQUE(withdrawal_id)
);
CREATE TABLE withdrawal_chain_transactions (
    id UUID PRIMARY KEY, withdrawal_id VARCHAR(64) NOT NULL UNIQUE REFERENCES withdrawals(withdrawal_id),
    transaction_hash CHAR(66) NOT NULL UNIQUE, raw_transaction_hash CHAR(64) NOT NULL UNIQUE,
    broadcast_attempts INTEGER NOT NULL DEFAULT 0, broadcasted_at TIMESTAMP WITH TIME ZONE,
    block_number BIGINT, receipt_status VARCHAR(16), finalized_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_withdrawal_chain_receipt_status CHECK (receipt_status IS NULL OR receipt_status IN ('PENDING','SUCCESS','FAILED'))
);
COMMENT ON TABLE withdrawal_signing_requests IS '固定提款 intent 與簽署結果。主應用不保存私鑰，raw transaction 僅作廣播不確定性對帳。';
COMMENT ON TABLE withdrawal_chain_transactions IS '鏈上廣播與收據證據；receipt 成功且符合 finality policy 前不得完成帳本結算。';

-- 每個 hot wallet/chain 的 nonce allocator 以資料列鎖串行化，避免多 pod 同時以 pending nonce 簽出相同交易。
CREATE TABLE withdrawal_nonce_states (
    chain_id BIGINT NOT NULL, source_address VARCHAR(64) NOT NULL, next_nonce NUMERIC(38,0) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(chain_id, source_address), CONSTRAINT ck_withdrawal_nonce_non_negative CHECK(next_nonce >= 0)
);
CREATE TABLE withdrawal_nonce_allocations (
    withdrawal_id VARCHAR(64) PRIMARY KEY REFERENCES withdrawals(withdrawal_id), chain_id BIGINT NOT NULL,
    source_address VARCHAR(64) NOT NULL, nonce NUMERIC(38,0) NOT NULL, allocated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_withdrawal_nonce_per_wallet UNIQUE(chain_id, source_address, nonce), CONSTRAINT ck_withdrawal_allocation_nonce_non_negative CHECK(nonce >= 0)
);
COMMENT ON TABLE withdrawal_nonce_states IS '受控 hot wallet nonce 游標；只在取得 provider pending nonce 後初始化，不能以 latest nonce 取代。';
COMMENT ON TABLE withdrawal_nonce_allocations IS '提款與已配置 nonce 的一對一不可變關聯，供重送與廣播不確定性對帳使用。';

-- 提款 custody 對應帳戶只作 double-entry 的交易所端對應，不投影為使用者可用餘額。
ALTER TABLE accounts DROP CONSTRAINT ck_accounts_account_purpose;
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_account_purpose CHECK (account_purpose IS NULL OR account_purpose IN ('AIRDROP_FUNDING', 'ASSET_ADJUSTMENT_COUNTERPARTY', 'WITHDRAWAL_CUSTODY'));
INSERT INTO users (user_id,email,display_name,status) VALUES ('system:withdrawal-custody','system-withdrawal-custody@lumix.internal','系統提款 custody 對應帳戶','SUSPENDED') ON CONFLICT(user_id) DO NOTHING;
INSERT INTO accounts(account_id,user_id,account_type,status,account_category,account_purpose) VALUES ('system:withdrawal-custody:spot','system:withdrawal-custody','SPOT','ACTIVE','EXCHANGE','WITHDRAWAL_CUSTODY') ON CONFLICT(account_id) DO NOTHING;
