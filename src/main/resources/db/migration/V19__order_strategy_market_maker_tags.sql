ALTER TABLE order_lifecycle_events
    ADD COLUMN strategy_id VARCHAR(128),
    ADD COLUMN market_maker_id VARCHAR(128);

CREATE INDEX idx_order_lifecycle_strategy
    ON order_lifecycle_events (uid, strategy_id, event_ts);

CREATE INDEX idx_order_lifecycle_market_maker
    ON order_lifecycle_events (market_maker_id, event_ts);

ALTER TABLE order_lifecycle_projection
    ADD COLUMN strategy_id VARCHAR(128),
    ADD COLUMN market_maker_id VARCHAR(128);

CREATE INDEX idx_order_lifecycle_projection_strategy
    ON order_lifecycle_projection (uid, strategy_id, last_event_at);

CREATE INDEX idx_order_lifecycle_projection_market_maker
    ON order_lifecycle_projection (market_maker_id, last_event_at);
