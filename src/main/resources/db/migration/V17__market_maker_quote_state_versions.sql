-- File purpose: per-side quote version and replacement metadata for market-maker quote state.
ALTER TABLE market_maker_quote_states
    ADD COLUMN bid_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN ask_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN replaced_bid_order_id VARCHAR(36),
    ADD COLUMN replaced_ask_order_id VARCHAR(36);
