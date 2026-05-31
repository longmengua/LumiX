ALTER TABLE hedge_decision_audits
    ADD COLUMN internal_trade_ref_id VARCHAR(128);

CREATE INDEX idx_hedge_decision_trade_ref
    ON hedge_decision_audits (internal_trade_ref_id);

ALTER TABLE hedge_fills
    ADD COLUMN ledger_ref_id VARCHAR(128);

CREATE INDEX idx_hedge_fills_ledger_ref
    ON hedge_fills (ledger_ref_id);
