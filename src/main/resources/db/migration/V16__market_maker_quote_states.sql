-- File purpose: durable active quote state for market-maker quote ownership restore.
CREATE TABLE market_maker_quote_states
(
    id              VARCHAR(192) NOT NULL,
    market_maker_id VARCHAR(128) NOT NULL,
    uid             BIGINT       NOT NULL,
    symbol          VARCHAR(32)  NOT NULL,
    ref_id          VARCHAR(128),
    active          BOOLEAN      NOT NULL,
    accepted        BOOLEAN      NOT NULL,
    reason          VARCHAR(256),
    canceled_count  INT          NOT NULL,
    bid_order_id    VARCHAR(36),
    ask_order_id    VARCHAR(36),
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_mm_quote_states_mm_symbol UNIQUE (market_maker_id, symbol)
);

CREATE INDEX idx_mm_quote_states_mm_updated ON market_maker_quote_states (market_maker_id, updated_at);
CREATE INDEX idx_mm_quote_states_active_updated ON market_maker_quote_states (active, updated_at);
