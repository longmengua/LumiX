-- 現貨／永續受控執行的商品、不可變政策與訂單證據；不建立商品、資金或預設費率。
-- HUMAN_REVIEW_REQUIRED：撮合、扣款與結算使用此 schema；啟用仍要求完整商品／風險政策與真實資產。
-- 順序：既有 V001–V003 後套用。回滾先停新單並保留查詢／撤單；已有資料不得 DROP 或刪除帳本。
-- 保留現有 markets/orders/trades 的所有物件；新執行資料藉獨立 instrument identity 隔離現貨與永續。

-- 可執行商品識別及明確啟用狀態，不以交易對字尾猜商品種類。
CREATE TABLE trading_instruments (
    -- 穩定商品識別，現貨與永續不同。
    instrument_id VARCHAR(64) PRIMARY KEY,
    -- 商品類別。
    instrument_kind VARCHAR(16) NOT NULL CHECK (instrument_kind IN ('SPOT','PERPETUAL')),
    -- 既有 underlying 資產組合。
    market_symbol VARCHAR(32) NOT NULL REFERENCES markets(market_symbol),
    -- 只有具完整政策的 ACTIVE 商品可接新單。
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE','HALTED','CLOSED')),
    -- 目前政策版本；未設定時不能啟用。
    policy_version BIGINT,
    -- 建立時間。
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 最後治理變更時間。
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (instrument_kind,market_symbol),
    CHECK (status <> 'ACTIVE' OR policy_version IS NOT NULL)
);
COMMENT ON TABLE trading_instruments IS '可執行商品識別及明確啟用狀態，不以交易對字尾猜商品種類。';
COMMENT ON COLUMN trading_instruments.instrument_id IS '穩定商品識別，現貨與永續不同。';
COMMENT ON COLUMN trading_instruments.instrument_kind IS '商品類別。';
COMMENT ON COLUMN trading_instruments.market_symbol IS '既有 underlying 資產組合。';
COMMENT ON COLUMN trading_instruments.status IS '只有具完整政策的 ACTIVE 商品可接新單。';
COMMENT ON COLUMN trading_instruments.policy_version IS '目前政策版本；未設定時不能啟用。';
COMMENT ON COLUMN trading_instruments.created_at IS '建立時間。';
COMMENT ON COLUMN trading_instruments.updated_at IS '最後治理變更時間。';

-- 具名完整政策的不可變版本，不存在 runtime 預設費率或風險數字。
CREATE TABLE trading_execution_policies (
    -- 政策所屬商品。
    instrument_id VARCHAR(64) NOT NULL REFERENCES trading_instruments(instrument_id),
    -- 遞增版本識別。
    policy_version BIGINT NOT NULL CHECK (policy_version > 0),
    -- 經 application 驗證的完整規格與費率／風險政策。
    policy JSONB NOT NULL CHECK (jsonb_typeof(policy) = 'object'),
    -- 受驗證的政策制定者。
    created_by VARCHAR(64) NOT NULL,
    -- 政策建立時間。
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (instrument_id,policy_version)
);
COMMENT ON TABLE trading_execution_policies IS '具名完整政策的不可變版本，不存在 runtime 預設費率或風險數字。';
COMMENT ON COLUMN trading_execution_policies.instrument_id IS '政策所屬商品。';
COMMENT ON COLUMN trading_execution_policies.policy_version IS '遞增版本識別。';
COMMENT ON COLUMN trading_execution_policies.policy IS '經 application 驗證的完整規格與費率／風險政策。';
COMMENT ON COLUMN trading_execution_policies.created_by IS '受驗證的政策制定者。';
COMMENT ON COLUMN trading_execution_policies.created_at IS '政策建立時間。';

-- 新 runtime 訂單的商品、確定順序與資金證據；不是舊訂單的相容 adapter。
CREATE TABLE trading_order_executions (
    -- 既有訂單主檔關聯。
    order_id VARCHAR(64) PRIMARY KEY REFERENCES orders(order_id),
    -- 獨立撮合商品。
    instrument_id VARCHAR(64) NOT NULL REFERENCES trading_instruments(instrument_id),
    -- 接單時固定的不可變政策版本。
    policy_version BIGINT NOT NULL,
    -- 持久化時間優先順序，重啟不改序。
    order_sequence BIGINT GENERATED ALWAYS AS IDENTITY UNIQUE,
    -- 只屬於本訂單的資金保留。
    reservation_id VARCHAR(64) REFERENCES reservations(reservation_id),
    -- 累積已成交名義額。
    filled_notional NUMERIC(36,18) NOT NULL DEFAULT 0 CHECK (filled_notional >= 0),
    -- 未取整的累計費用，用於明確的累計 quote 計費政策。
    fee_accrued NUMERIC(76,36) NOT NULL DEFAULT 0 CHECK (fee_accrued >= 0),
    -- 已實際入帳的累計費用。
    fee_charged NUMERIC(36,18) NOT NULL DEFAULT 0 CHECK (fee_charged >= 0),
    -- 永續是否只允許減少持倉。
    reduce_only BOOLEAN NOT NULL DEFAULT FALSE,
    -- 永續委託的明確槓桿。
    leverage NUMERIC(18,8),
    -- canonical 業務內容摘要。
    request_hash VARCHAR(64) NOT NULL,
    FOREIGN KEY (instrument_id,policy_version) REFERENCES trading_execution_policies(instrument_id,policy_version)
);
COMMENT ON TABLE trading_order_executions IS '新 runtime 訂單的商品、確定順序與資金證據；不是舊訂單的相容 adapter。';
COMMENT ON COLUMN trading_order_executions.order_id IS '既有訂單主檔關聯。';
COMMENT ON COLUMN trading_order_executions.instrument_id IS '獨立撮合商品。';
COMMENT ON COLUMN trading_order_executions.policy_version IS '接單時固定的不可變政策版本。';
COMMENT ON COLUMN trading_order_executions.order_sequence IS '持久化時間優先順序，重啟不改序。';
COMMENT ON COLUMN trading_order_executions.reservation_id IS '只屬於本訂單的資金保留。';
COMMENT ON COLUMN trading_order_executions.filled_notional IS '累積已成交名義額。';
COMMENT ON COLUMN trading_order_executions.fee_accrued IS '未取整的累計費用，用於明確的累計 quote 計費政策。';
COMMENT ON COLUMN trading_order_executions.fee_charged IS '已實際入帳的累計費用。';
COMMENT ON COLUMN trading_order_executions.reduce_only IS '永續是否只允許減少持倉。';
COMMENT ON COLUMN trading_order_executions.leverage IS '永續委託的明確槓桿。';
COMMENT ON COLUMN trading_order_executions.request_hash IS 'canonical 業務內容摘要。';

-- 成交與不可變帳本的原子關聯，只有實際成交才寫入。
CREATE TABLE trading_trade_executions (
    -- 真實成交識別。
    trade_id VARCHAR(64) PRIMARY KEY REFERENCES trades(trade_id),
    -- 成交商品。
    instrument_id VARCHAR(64) NOT NULL REFERENCES trading_instruments(instrument_id),
    -- Maker 接單政策版本。
    maker_policy_version BIGINT NOT NULL,
    -- Taker 接單政策版本。
    taker_policy_version BIGINT NOT NULL,
    -- 與此次成交同交易提交的守恆 journal。
    ledger_journal_id BIGINT NOT NULL REFERENCES ledger_journals(ledger_journal_id),
    -- 成交證據建立時間。
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (instrument_id,maker_policy_version) REFERENCES trading_execution_policies(instrument_id,policy_version),
    FOREIGN KEY (instrument_id,taker_policy_version) REFERENCES trading_execution_policies(instrument_id,policy_version)
);
COMMENT ON TABLE trading_trade_executions IS '成交與不可變帳本的原子關聯，只有實際成交才寫入。';
COMMENT ON COLUMN trading_trade_executions.trade_id IS '真實成交識別。';
COMMENT ON COLUMN trading_trade_executions.instrument_id IS '成交商品。';
COMMENT ON COLUMN trading_trade_executions.maker_policy_version IS 'Maker 接單政策版本。';
COMMENT ON COLUMN trading_trade_executions.taker_policy_version IS 'Taker 接單政策版本。';
COMMENT ON COLUMN trading_trade_executions.ledger_journal_id IS '與此次成交同交易提交的守恆 journal。';
COMMENT ON COLUMN trading_trade_executions.created_at IS '成交證據建立時間。';

ALTER TABLE trading_instruments ADD CONSTRAINT fk_trading_current_policy
    FOREIGN KEY (instrument_id,policy_version) REFERENCES trading_execution_policies(instrument_id,policy_version) DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX idx_trading_order_priority ON trading_order_executions(instrument_id,order_sequence);
-- 系統帳戶 purpose 僅增加具名交易用途；保留既有用途，不建立有餘額的系統帳戶。
ALTER TABLE accounts DROP CONSTRAINT ck_accounts_account_purpose;
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_account_purpose CHECK (account_purpose IS NULL OR account_purpose IN
    ('AIRDROP_FUNDING','ASSET_ADJUSTMENT_COUNTERPARTY','WITHDRAWAL_CUSTODY','TRADING_FEE_REVENUE','PERPETUAL_MARGIN_POOL','PERPETUAL_INSURANCE'));

-- 政策與成交 evidence 只能追加；更正另增版本或新 journal，不覆蓋舊證據。
CREATE FUNCTION prevent_trading_evidence_mutation() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'trading evidence is append-only';
END;
$$;
CREATE TRIGGER immutable_trading_policies BEFORE UPDATE OR DELETE ON trading_execution_policies
    FOR EACH ROW EXECUTE FUNCTION prevent_trading_evidence_mutation();
CREATE TRIGGER immutable_trading_trade_evidence BEFORE UPDATE OR DELETE ON trading_trade_executions
    FOR EACH ROW EXECUTE FUNCTION prevent_trading_evidence_mutation();
