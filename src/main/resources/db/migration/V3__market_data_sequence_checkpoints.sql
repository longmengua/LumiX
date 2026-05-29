-- Durable market-data stream checkpoints for restart recovery and reconnect backfill.
CREATE TABLE IF NOT EXISTS market_data_sequence_checkpoints (
    symbol VARCHAR(32) NOT NULL,
    stream VARCHAR(64) NOT NULL,
    sequence_value BIGINT NOT NULL,
    checksum BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (symbol, stream),
    KEY idx_md_seq_symbol_stream (symbol, stream)
);
