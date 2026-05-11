-- =========================================================
-- Seed Data
-- =========================================================

-- ---------------------------------------------------------
-- account
-- ---------------------------------------------------------
INSERT INTO account (
    uid,
    kyc_level,
    status
)
VALUES
(
    1002001,
    2,
    1
);

-- ---------------------------------------------------------
-- symbol_config
-- ---------------------------------------------------------
INSERT INTO symbol_config
(
    symbol,
    base_asset,
    quote_asset,
    contract_size,
    price_tick,
    qty_step,
    min_qty,
    max_leverage,
    maker_fee,
    taker_fee,
    risk_tiers_json,
    status
)
VALUES
(
    'BTCUSDT-PERP',
    'BTC',
    'USDT',
    1.00000000,
    0.10000000,
    0.00100000,
    0.00100000,
    125,
    0.0002000000,
    0.0005000000,
    '[{"notional_upper":50000,"mm_base":0.004,"mm_add":5}]',
    1
);

-- ---------------------------------------------------------
-- order
-- ---------------------------------------------------------
INSERT INTO `order`
(
    uid,
    symbol,
    side,
    type,
    time_in_force,
    price,
    qty,
    client_order_id,
    status,
    source
)
VALUES
(
    1002001,
    'BTCUSDT-PERP',
    1,
    1,
    1,
    65000.0,
    0.01,
    'stratA-20250929-0001',
    0,
    'api'
);

-- ---------------------------------------------------------
-- trade
-- ---------------------------------------------------------
INSERT INTO trade
(
    trade_id,
    match_id,
    order_id,
    uid,
    symbol,
    side,
    price,
    qty,
    maker,
    fee,
    fee_asset,
    ctime
)
VALUES
(
    900000001234,
    700000000321,
    1,
    1002001,
    'BTCUSDT-PERP',
    1,
    64990.0,
    0.005,
    0,
    0.162475,
    'USDT',
    '2025-09-29 00:00:10'
);

-- ---------------------------------------------------------
-- position
-- ---------------------------------------------------------
INSERT INTO position
(
    uid,
    symbol,
    side,
    qty,
    entry_price,
    leverage,
    margin_mode,
    isolated_margin,
    upnl,
    rpnl,
    maint_margin_req
)
VALUES
(
    1002001,
    'BTCUSDT-PERP',
    1,
    0.01000000,
    65010.0,
    20,
    1,
    0,
    -1.23,
    2.34,
    12.34
);

-- ---------------------------------------------------------
-- wallet_account
-- ---------------------------------------------------------
INSERT INTO wallet_account
(
    uid,
    asset,
    available,
    hold
)
VALUES
(
    1002001,
    'USDT',
    988.337525,
    50.0
);

-- ---------------------------------------------------------
-- wallet_ledger
-- ---------------------------------------------------------
INSERT INTO wallet_ledger
(
    uid,
    asset,
    change_amount,
    balance_after,
    reason,
    ref_id,
    extra
)
VALUES
(
    1002001,
    'USDT',
    -50.0,
    950.0,
    'order_reserve',
    '1',
    '{"symbol":"BTCUSDT-PERP"}'
);

INSERT INTO wallet_ledger
(
    uid,
    asset,
    change_amount,
    balance_after,
    reason,
    ref_id
)
VALUES
(
    1002001,
    'USDT',
    38.5,
    988.5,
    'reserve_release',
    '1'
);

INSERT INTO wallet_ledger
(
    uid,
    asset,
    change_amount,
    balance_after,
    reason,
    ref_id,
    extra
)
VALUES
(
    1002001,
    'USDT',
    -0.162475,
    988.337525,
    'trade_fee',
    '900000001234',
    '{"maker":0}'
);

-- ---------------------------------------------------------
-- funding_history
-- ---------------------------------------------------------
INSERT INTO funding_history
(
    symbol,
    period_start,
    period_end,
    rate
)
VALUES
(
    'BTCUSDT-PERP',
    '2025-09-29 00:00:00',
    '2025-09-29 08:00:00',
    0.0001
);

-- ---------------------------------------------------------
-- funding_settlement
-- ---------------------------------------------------------
INSERT INTO funding_settlement
(
    uid,
    symbol,
    period_end,
    rate,
    position_qty,
    mark_price,
    amount
)
VALUES
(
    1002001,
    'BTCUSDT-PERP',
    '2025-09-29 08:00:00',
    0.0001,
    0.01,
    64800.0,
    -0.0648
);