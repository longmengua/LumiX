-- 強平委託由後端在維持保證金不足時建立，客戶命令不得自行標記強平。
ALTER TABLE trading_order_executions ADD COLUMN liquidation BOOLEAN NOT NULL DEFAULT FALSE;
COMMENT ON COLUMN trading_order_executions.liquidation IS '受控強平來源；成交後持倉事件標為 LIQUIDATE。';

CREATE FUNCTION validate_futures_liquidation_order() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.liquidation AND (NEW.position_side IS NULL OR NOT NEW.reduce_only) THEN
        RAISE EXCEPTION 'liquidation order must reduce an explicit futures position';
    END IF;
    IF TG_OP = 'UPDATE' THEN
        IF NEW.liquidation IS DISTINCT FROM OLD.liquidation THEN
            RAISE EXCEPTION 'liquidation identity is immutable';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER futures_liquidation_order_boundary BEFORE INSERT OR UPDATE OF liquidation
    ON trading_order_executions FOR EACH ROW EXECUTE FUNCTION validate_futures_liquidation_order();

CREATE TABLE futures_liquidation_attempts (
    liquidation_id VARCHAR(64) PRIMARY KEY,
    account_id VARCHAR(64) NOT NULL,
    instrument_id VARCHAR(64) NOT NULL,
    position_side VARCHAR(5) NOT NULL,
    mark_price_usdt NUMERIC(38,18) NOT NULL CHECK (mark_price_usdt > 0),
    mark_source VARCHAR(128) NOT NULL,
    mark_source_at TIMESTAMPTZ NOT NULL,
    equity_usdt NUMERIC(36,18) NOT NULL,
    maintenance_usdt NUMERIC(36,18) NOT NULL CHECK (maintenance_usdt > 0),
    order_id VARCHAR(64) NOT NULL UNIQUE REFERENCES orders(order_id),
    policy_version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id,instrument_id,position_side)
        REFERENCES futures_hedge_positions(account_id,instrument_id,position_side),
    FOREIGN KEY (instrument_id,policy_version)
        REFERENCES futures_risk_policies(instrument_id,policy_version)
);
COMMENT ON TABLE futures_liquidation_attempts IS '具名標記價觸發的真實減倉委託；可為部分成交或無成交 IOC。';
CREATE TRIGGER immutable_futures_liquidation_attempts BEFORE UPDATE OR DELETE ON futures_liquidation_attempts
    FOR EACH ROW EXECUTE FUNCTION prevent_trading_evidence_mutation();

ALTER TABLE idempotency_keys DROP CONSTRAINT ck_idempotency_keys_scope;
ALTER TABLE idempotency_keys ADD CONSTRAINT ck_idempotency_keys_scope CHECK (scope IN
    ('ORDER_CREATE','ORDER_CANCEL','FUTURES_ORDER_CREATE','FUTURES_ORDER_CANCEL','FUTURES_LIQUIDATION',
     'WITHDRAWAL_REQUEST','WITHDRAWAL_CANCEL','LEDGER_POSTING','DEPOSIT_CONFIRMATION',
     'RESERVATION_HOLD','RESERVATION_RELEASE','RESERVATION_CAPTURE',
     'INTERNAL_TRANSFER','PLATFORM_INTERNAL_TRANSFER','ADMIN_ACTION'));
