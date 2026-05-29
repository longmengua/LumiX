-- Production query indexes for operational order, ledger, event, and prediction-order projections.
CREATE INDEX idx_order_lifecycle_event_status_time
    ON order_lifecycle_events (status, event_ts);

CREATE INDEX idx_order_lifecycle_event_uid_client_time
    ON order_lifecycle_events (uid, client_order_id, event_ts);

CREATE INDEX idx_order_lifecycle_projection_uid_status_updated
    ON order_lifecycle_projection (uid, status, updated_at);

CREATE INDEX idx_order_lifecycle_projection_symbol_status_time
    ON order_lifecycle_projection (symbol, status, last_event_at);

CREATE INDEX idx_wallet_ledger_entries_created
    ON wallet_ledger_entries (created_at);

CREATE INDEX idx_wallet_ledger_entries_uid_reason_created
    ON wallet_ledger_entries (uid, reason, created_at);

CREATE INDEX idx_wallet_ledger_entries_asset_created
    ON wallet_ledger_entries (asset, created_at);

CREATE INDEX idx_wallet_ledger_postings_created
    ON wallet_ledger_postings (created_at);

CREATE INDEX idx_wallet_ledger_postings_account_created
    ON wallet_ledger_postings (account_code, created_at);

CREATE INDEX idx_outbox_events_type_created
    ON outbox_events (event_type, created_at);

CREATE INDEX idx_dlq_events_type_created
    ON dlq_events (event_type, created_at);

CREATE INDEX idx_matching_command_logs_type_created
    ON matching_command_logs (command_type, created_at);

CREATE INDEX idx_matching_event_logs_command_created
    ON matching_event_logs (symbol_code, command_offset, created_at);

CREATE INDEX idx_prediction_order_user_status_updated
    ON prediction_polymarket_order (user_id, status, updated_at);

CREATE INDEX idx_prediction_order_market_status_updated
    ON prediction_polymarket_order (market_slug, status, updated_at);

CREATE INDEX idx_prediction_order_event_status_updated
    ON prediction_polymarket_order (event_slug, status, updated_at);

CREATE INDEX idx_prediction_ws_wallet_type_received
    ON prediction_polymarket_ws_event (wallet_address, event_type, received_at);

CREATE INDEX idx_prediction_ws_market_type_received
    ON prediction_polymarket_ws_event (market, event_type, received_at);
