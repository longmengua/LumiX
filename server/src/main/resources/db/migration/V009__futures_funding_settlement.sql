-- 逐倉資金費用週期與逐持倉分配；實際 USDT 入帳與事件須在同一交易完成。
CREATE TABLE futures_funding_cycles (
    cycle_id VARCHAR(64) PRIMARY KEY,
    instrument_id VARCHAR(64) NOT NULL,
    policy_version BIGINT NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    funding_rate NUMERIC(20,18) NOT NULL,
    rate_source VARCHAR(32) NOT NULL,
    mark_price_usdt NUMERIC(38,18) NOT NULL CHECK (mark_price_usdt > 0),
    mark_source VARCHAR(128) NOT NULL,
    mark_source_at TIMESTAMPTZ NOT NULL,
    ledger_journal_id BIGINT REFERENCES ledger_journals(ledger_journal_id),
    settled_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (instrument_id,policy_version)
        REFERENCES futures_risk_policies(instrument_id,policy_version),
    UNIQUE (instrument_id,period_end)
);
COMMENT ON TABLE futures_funding_cycles IS '不可變 funding 週期、核定費率、標記價來源及同交易帳本證據。';

CREATE TABLE futures_funding_allocations (
    cycle_id VARCHAR(64) NOT NULL REFERENCES futures_funding_cycles(cycle_id),
    account_id VARCHAR(64) NOT NULL,
    instrument_id VARCHAR(64) NOT NULL,
    position_side VARCHAR(5) NOT NULL,
    funding_usdt NUMERIC(36,18) NOT NULL,
    previous_collateral NUMERIC(36,18) NOT NULL CHECK (previous_collateral >= 0),
    resulting_collateral NUMERIC(36,18) NOT NULL CHECK (resulting_collateral >= 0),
    previous_version BIGINT NOT NULL CHECK (previous_version >= 0),
    next_version BIGINT NOT NULL CHECK (next_version = previous_version + 1),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (cycle_id,account_id,instrument_id,position_side),
    FOREIGN KEY (account_id,instrument_id,position_side)
        REFERENCES futures_hedge_positions(account_id,instrument_id,position_side),
    CHECK ((funding_usdt < 0 AND resulting_collateral < previous_collateral)
        OR (funding_usdt >= 0 AND resulting_collateral = previous_collateral))
);
COMMENT ON TABLE futures_funding_allocations IS '每方向 funding 收付及隔離抵押品前後狀態，付款方扣抵押品、收款方收自由 USDT。';

CREATE TRIGGER immutable_futures_funding_cycles BEFORE UPDATE OR DELETE ON futures_funding_cycles
    FOR EACH ROW EXECUTE FUNCTION prevent_trading_evidence_mutation();
CREATE TRIGGER immutable_futures_funding_allocations BEFORE UPDATE OR DELETE ON futures_funding_allocations
    FOR EACH ROW EXECUTE FUNCTION prevent_trading_evidence_mutation();
