-- 雙向逐倉持倉的持久化投影與不可變變動證據。
-- HUMAN_REVIEW_REQUIRED：此結構承載合約保證金及損益，只有完整政策與結算 runtime 驗證後才可開倉。
-- 順序：V004 商品與交易政策之後。回滾須先停新單並確認沒有持倉、事件或資金；已有資料不得 DROP。

-- 複合外鍵將持倉限制在明確的永續商品，不靠應用層字尾推測。
ALTER TABLE trading_instruments ADD CONSTRAINT uq_trading_instrument_id_kind
    UNIQUE (instrument_id,instrument_kind);

-- 同一帳戶與商品分別保存 LONG、SHORT，禁止以淨數量合併兩個方向。
CREATE TABLE futures_hedge_positions (
    -- 使用者的 FUTURES 帳戶，runtime 仍須檢查 owner 與帳戶狀態。
    account_id VARCHAR(64) NOT NULL REFERENCES accounts(account_id),
    -- 永續商品，runtime 仍須檢查商品種類及狀態。
    instrument_id VARCHAR(64) NOT NULL,
    -- 固定商品種類，與商品表的複合外鍵共同拒絕現貨商品。
    instrument_kind VARCHAR(16) NOT NULL DEFAULT 'PERPETUAL' CHECK (instrument_kind = 'PERPETUAL'),
    -- 雙向持倉識別，LONG 與 SHORT 可同時存在。
    position_side VARCHAR(5) NOT NULL CHECK (position_side IN ('LONG','SHORT')),
    -- 正數持倉數量，零代表該方向已平倉。
    quantity NUMERIC(38,18) NOT NULL CHECK (quantity >= 0),
    -- 未平部位的 USDT 成本基礎；零數量必須清零。
    entry_notional_usdt NUMERIC(76,36) NOT NULL CHECK (entry_notional_usdt >= 0),
    -- 此方向專用的抵押幣別；不能從另一方向借用保證金。
    collateral_asset_symbol VARCHAR(32) NOT NULL REFERENCES assets(asset_symbol),
    -- 已隔離的抵押品數量，變動須與 reservation／ledger 對帳。
    isolated_collateral NUMERIC(36,18) NOT NULL CHECK (isolated_collateral >= 0),
    -- 持倉事件序號，更新須檢查舊版本以防競態覆蓋。
    position_version BIGINT NOT NULL CHECK (position_version >= 0),
    -- 最後一次受控更新時間。
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (account_id,instrument_id,position_side),
    FOREIGN KEY (instrument_id,instrument_kind)
        REFERENCES trading_instruments(instrument_id,instrument_kind),
    CHECK ((quantity = 0 AND entry_notional_usdt = 0 AND isolated_collateral = 0)
        OR (quantity > 0 AND entry_notional_usdt > 0 AND isolated_collateral > 0))
);
COMMENT ON TABLE futures_hedge_positions IS '雙向逐倉持倉投影，LONG／SHORT 不相互抵銷；權威變動須有不可變事件與帳本證據。';
COMMENT ON COLUMN futures_hedge_positions.account_id IS '持倉所屬期貨帳戶。';
COMMENT ON COLUMN futures_hedge_positions.instrument_id IS '獨立永續商品識別。';
COMMENT ON COLUMN futures_hedge_positions.instrument_kind IS '固定 PERPETUAL，防止現貨商品寫入合約持倉。';
COMMENT ON COLUMN futures_hedge_positions.position_side IS '明確 LONG 或 SHORT 方向。';
COMMENT ON COLUMN futures_hedge_positions.quantity IS '目前未平數量。';
COMMENT ON COLUMN futures_hedge_positions.entry_notional_usdt IS '未平數量對應的精確 USDT 成本基礎。';
COMMENT ON COLUMN futures_hedge_positions.collateral_asset_symbol IS '此方向保證金幣別。';
COMMENT ON COLUMN futures_hedge_positions.isolated_collateral IS '此方向隔離抵押品數量。';
COMMENT ON COLUMN futures_hedge_positions.position_version IS '防止併發覆蓋的持倉版本。';
COMMENT ON COLUMN futures_hedge_positions.updated_at IS '最後一次受控更新時間。';

-- 每次倉位變動都保存不可變的前後狀態及已提交帳本，供重放和對帳。
CREATE TABLE futures_hedge_position_events (
    -- 事件身分由應用層建立，不能因重試另生相同業務事件。
    event_id VARCHAR(64) PRIMARY KEY,
    -- 對應持倉帳戶。
    account_id VARCHAR(64) NOT NULL,
    -- 對應永續商品。
    instrument_id VARCHAR(64) NOT NULL,
    -- 對應持倉方向。
    position_side VARCHAR(5) NOT NULL,
    -- 明確開、加、減、平或強平，禁止含糊的反向淨額更新。
    event_type VARCHAR(16) NOT NULL CHECK (event_type IN ('OPEN','INCREASE','DECREASE','CLOSE','LIQUIDATE')),
    -- 成交證據，不能用行情價格憑空產生持倉。
    trade_id VARCHAR(64) NOT NULL REFERENCES trades(trade_id),
    -- 事件前版本。
    previous_version BIGINT NOT NULL CHECK (previous_version >= 0),
    -- 事件後版本。
    next_version BIGINT NOT NULL CHECK (next_version > 0),
    -- 成交數量，方向由 event_type 與 position_side 決定。
    fill_quantity NUMERIC(38,18) NOT NULL CHECK (fill_quantity > 0),
    -- 實際成交價格。
    fill_price_usdt NUMERIC(38,18) NOT NULL CHECK (fill_price_usdt > 0),
    -- 事件後持倉數量。
    resulting_quantity NUMERIC(38,18) NOT NULL CHECK (resulting_quantity >= 0),
    -- 事件後未平 USDT 成本基礎。
    resulting_entry_notional_usdt NUMERIC(76,36) NOT NULL CHECK (resulting_entry_notional_usdt >= 0),
    -- 此次實現的 USDT 損益，可正可負。
    realized_pnl_usdt NUMERIC(36,18) NOT NULL,
    -- 成交與保證金、手續費及損益必須同交易入帳。
    ledger_journal_id BIGINT NOT NULL REFERENCES ledger_journals(ledger_journal_id),
    -- 重放時固定商品／風險政策版本。
    policy_version BIGINT NOT NULL,
    -- 受控建立時間。
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id,instrument_id,position_side)
        REFERENCES futures_hedge_positions(account_id,instrument_id,position_side),
    FOREIGN KEY (instrument_id,policy_version)
        REFERENCES trading_execution_policies(instrument_id,policy_version),
    UNIQUE (account_id,instrument_id,position_side,next_version),
    UNIQUE (account_id,instrument_id,position_side,trade_id),
    CHECK (next_version = previous_version + 1),
    CHECK ((resulting_quantity = 0 AND resulting_entry_notional_usdt = 0)
        OR (resulting_quantity > 0 AND resulting_entry_notional_usdt > 0))
);
COMMENT ON TABLE futures_hedge_position_events IS '逐倉成交的不可變前後狀態、政策版本與帳本證據。';
COMMENT ON COLUMN futures_hedge_position_events.event_id IS '不可變事件身分。';
COMMENT ON COLUMN futures_hedge_position_events.account_id IS '持倉帳戶。';
COMMENT ON COLUMN futures_hedge_position_events.instrument_id IS '永續商品。';
COMMENT ON COLUMN futures_hedge_position_events.position_side IS 'LONG 或 SHORT。';
COMMENT ON COLUMN futures_hedge_position_events.event_type IS '開倉、加倉、減倉、平倉或強平。';
COMMENT ON COLUMN futures_hedge_position_events.trade_id IS '實際成交證據。';
COMMENT ON COLUMN futures_hedge_position_events.previous_version IS '事件前版本。';
COMMENT ON COLUMN futures_hedge_position_events.next_version IS '事件後版本。';
COMMENT ON COLUMN futures_hedge_position_events.fill_quantity IS '本次實際成交數量。';
COMMENT ON COLUMN futures_hedge_position_events.fill_price_usdt IS '本次 USDT 成交價格。';
COMMENT ON COLUMN futures_hedge_position_events.resulting_quantity IS '事件後未平數量。';
COMMENT ON COLUMN futures_hedge_position_events.resulting_entry_notional_usdt IS '事件後未平 USDT 成本基礎。';
COMMENT ON COLUMN futures_hedge_position_events.realized_pnl_usdt IS '本次 USDT 已實現損益。';
COMMENT ON COLUMN futures_hedge_position_events.ledger_journal_id IS '同交易提交的帳本 journal。';
COMMENT ON COLUMN futures_hedge_position_events.policy_version IS '成交時固定的政策版本。';
COMMENT ON COLUMN futures_hedge_position_events.created_at IS '事件建立時間。';

CREATE TRIGGER immutable_futures_position_events BEFORE UPDATE OR DELETE ON futures_hedge_position_events
    FOR EACH ROW EXECUTE FUNCTION prevent_trading_evidence_mutation();
