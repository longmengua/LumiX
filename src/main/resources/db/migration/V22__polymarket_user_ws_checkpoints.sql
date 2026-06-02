-- Durable checkpoint for the Polymarket authenticated user WebSocket gateway.
CREATE TABLE IF NOT EXISTS prediction_polymarket_user_ws_checkpoint (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stream_key VARCHAR(160) NOT NULL,
    wallet_address VARCHAR(64) NULL,
    last_event_key VARCHAR(256) NULL,
    last_event_type VARCHAR(64) NULL,
    last_received_at DATETIME(6) NULL,
    last_payload LONGTEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_poly_user_ws_checkpoint_stream (stream_key),
    KEY idx_poly_user_ws_checkpoint_wallet (wallet_address),
    KEY idx_poly_user_ws_checkpoint_received (last_received_at)
);
