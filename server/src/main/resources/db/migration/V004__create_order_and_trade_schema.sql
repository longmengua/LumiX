-- P12-T05: Order and trade execution schema.
-- 這份 migration 只建立訂單生命週期與成交 execution record 的資料表。
-- 不建立 reservation、settlement 或 matching runtime，也不在資料庫層執行資金異動。
--
-- 重要限制：
-- 1. orders / trades 只保存交易鏈路需要的資料結構。
-- 2. 不實作下單狀態轉換 service。
-- 3. 不實作撮合引擎。
-- 4. 不實作 order hold / release。
-- 5. 不建立 reservation records。
-- 6. 不建立 settlement records。
-- 7. 不把 trades 直接綁到 ledger_journal_id；ledger posting 留給後續 settlement / posting 流程。
--
-- Rollback 注意事項：
-- 若要回滾，必須先確認沒有後續 phase 已經依賴這些資料。
-- 回滾時應先刪除 trades，再刪除 orders。
--
-- 維護注意：
-- 本 migration 會在 accounts 上新增 UNIQUE (account_id, user_id)，用來讓 orders 可以用 composite foreign key
-- 保證 order.user_id 與 order.account_id 的歸屬一致。
-- account_id 本身已是 primary key，因此這個 unique constraint 是為資料一致性與 FK 表達能力服務。

ALTER TABLE accounts
    ADD CONSTRAINT uq_accounts_account_user
        UNIQUE (account_id, user_id);


-- 訂單主檔。
-- 只保存 order lifecycle 所需的狀態與數值。
-- 狀態轉換、資金預留、成交累加、撤單與結算流程都留給後續 runtime。
CREATE TABLE orders (
    -- 訂單唯一識別碼。由應用層或上游流程產生，供查詢、審計與回溯使用。
    order_id VARCHAR(64) PRIMARY KEY,

    -- 下單使用者。這是查詢與權限邊界的基本維度，不代表資金已被預留或扣款。
    user_id VARCHAR(64) NOT NULL,

    -- 下單帳戶。order 以帳戶作為資金與交易邊界，避免把使用者層級資料直接當成資金來源。
    account_id VARCHAR(64) NOT NULL,

    -- 交易對代號，例如 BTC-USDT。訂單只綁定單一 market，避免後續撮合時需要重新推導交易對。
    market_symbol VARCHAR(32) NOT NULL,

    -- 訂單方向。BUY / SELL 只描述下單意圖，不代表已成交或已結算。
    side VARCHAR(8) NOT NULL,

    -- 訂單類型。LIMIT 與 MARKET 的欄位約束不同，schema 只負責約束格式，不負責撮合策略。
    order_type VARCHAR(16) NOT NULL,

    -- 訂單有效期。由應用層決定是否要填值；schema 只保留合法 enum 值。
    time_in_force VARCHAR(8),

    -- 訂單狀態。schema 只限制合法值，不實作狀態轉換引擎。
    status VARCHAR(24) NOT NULL,

    -- 委託價格，以 quote asset 數值表示。LIMIT 訂單必填，MARKET 訂單可為 NULL。
    -- 價格階梯、價格保護與撮合價格選擇留給後續 matching runtime。
    price NUMERIC(38, 18),

    -- 委託數量，以 base asset 數值表示。這是 order 的名義數量，不是可用餘額。
    quantity NUMERIC(38, 18) NOT NULL,

    -- 已成交數量。後續撮合或結算可以累加，但必須和 remaining_quantity / quantity 保持一致。
    filled_quantity NUMERIC(38, 18) NOT NULL DEFAULT 0,

    -- 剩餘未成交數量。此欄位保留查詢便利性，但仍要和 filled_quantity / quantity 一致。
    remaining_quantity NUMERIC(38, 18) NOT NULL,

    -- 使用者自訂訂單代號。
    -- PostgreSQL UNIQUE 允許多筆 NULL client_order_id。
    -- 只有使用者提供 client_order_id 時，才在同一 account 下要求唯一。
    client_order_id VARCHAR(128),

    -- 請求識別碼。用於追蹤下單請求與降低重送造成重複下單的風險。
    -- 完整 idempotency policy 仍需由後續 idempotency_keys 與應用層流程保證。
    request_id VARCHAR(64) NOT NULL,

    -- 資料建立時間。由資料庫預設產生，供審計與歷史查詢使用。
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 資料最後更新時間。狀態變更時由應用層或後續 migration trigger 維護。
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_orders_user_id
        FOREIGN KEY (user_id)
        REFERENCES users (user_id),

    CONSTRAINT fk_orders_account_id
        FOREIGN KEY (account_id)
        REFERENCES accounts (account_id),

    -- 保證 order.user_id 與 order.account_id 屬於同一個 accounts row。
    -- 若只分別 FK 到 users 與 accounts，會允許 user_id 與 account_id 彼此不一致。
    CONSTRAINT fk_orders_account_user
        FOREIGN KEY (account_id, user_id)
        REFERENCES accounts (account_id, user_id),

    CONSTRAINT fk_orders_market_symbol
        FOREIGN KEY (market_symbol)
        REFERENCES markets (market_symbol),

    CONSTRAINT uq_orders_request_id
        UNIQUE (request_id),

    -- PostgreSQL UNIQUE 允許多筆 NULL client_order_id。
    -- 因此只有 client_order_id 有值時，才會限制同一 account 不可重複。
    CONSTRAINT uq_orders_account_client_order_id
        UNIQUE (account_id, client_order_id),

    -- 供 trades 建立 composite foreign key，保證 trade 的 order / account / market 不會漂移。
    -- order_id 本身已是 primary key，這個 unique constraint 是為跨欄位一致性服務。
    CONSTRAINT uq_orders_order_account_market
        UNIQUE (order_id, account_id, market_symbol),

    CONSTRAINT ck_orders_side
        CHECK (side IN ('BUY', 'SELL')),

    CONSTRAINT ck_orders_order_type
        CHECK (order_type IN ('LIMIT', 'MARKET')),

    CONSTRAINT ck_orders_time_in_force
        CHECK (time_in_force IS NULL OR time_in_force IN ('GTC', 'IOC', 'FOK')),

    CONSTRAINT ck_orders_status
        CHECK (
            status IN (
                'NEW',
                'VALIDATED',
                'ACCEPTED',
                'OPEN',
                'PARTIALLY_FILLED',
                'FILLED',
                'REJECTED',
                'CANCELED',
                'EXPIRED'
            )
        ),

    -- LIMIT 訂單必須有正數價格。
    -- MARKET 訂單可以沒有價格；若上游保留估算價格，也必須為正數。
    CONSTRAINT ck_orders_price_by_type
        CHECK (
            (order_type = 'LIMIT' AND price IS NOT NULL AND price > 0)
            OR (order_type = 'MARKET' AND (price IS NULL OR price > 0))
        ),

    CONSTRAINT ck_orders_quantity_positive
        CHECK (quantity > 0),

    CONSTRAINT ck_orders_filled_quantity_non_negative
        CHECK (filled_quantity >= 0),

    CONSTRAINT ck_orders_remaining_quantity_non_negative
        CHECK (remaining_quantity >= 0),

    -- 此約束只保證欄位數值一致，不代表已實作撮合、結算或撤單狀態轉換。
    CONSTRAINT ck_orders_quantity_consistency
        CHECK (filled_quantity + remaining_quantity = quantity)
);

CREATE INDEX idx_orders_user_id
    ON orders (user_id);

CREATE INDEX idx_orders_account_id
    ON orders (account_id);

CREATE INDEX idx_orders_market_status_created_at
    ON orders (market_symbol, status, created_at);

CREATE INDEX idx_orders_status_created_at
    ON orders (status, created_at);

CREATE INDEX idx_orders_account_created_at
    ON orders (account_id, created_at);

COMMENT ON TABLE orders IS
'訂單主檔。只保存 order lifecycle 所需的狀態與數值；狀態轉換、預留、成交與撤單流程都留給後續 runtime。';

COMMENT ON COLUMN orders.order_id IS
'訂單唯一識別碼。由應用層或上游流程產生，供查詢、審計與回溯使用。';

COMMENT ON COLUMN orders.user_id IS
'下單使用者。這是查詢與權限邊界的基本維度，不代表資金已被預留或扣款。';

COMMENT ON COLUMN orders.account_id IS
'下單帳戶。order 以帳戶作為資金與交易邊界；資料庫透過 composite foreign key 保證 account_id 與 user_id 歸屬一致。';

COMMENT ON COLUMN orders.market_symbol IS
'交易對代號，例如 BTC-USDT。訂單只綁定單一 market，避免後續撮合時需要重新推導交易對。';

COMMENT ON COLUMN orders.side IS
'訂單方向。BUY / SELL 只描述下單意圖，不代表已成交或已結算。';

COMMENT ON COLUMN orders.order_type IS
'訂單類型。LIMIT 與 MARKET 的欄位約束不同，schema 只負責約束格式，不負責撮合策略。';

COMMENT ON COLUMN orders.time_in_force IS
'訂單有效期。由應用層決定是否要填值；schema 只保留合法 enum 值。';

COMMENT ON COLUMN orders.status IS
'訂單狀態。狀態值由應用層維護，schema 只限制合法值，不實作狀態轉換引擎。';

COMMENT ON COLUMN orders.price IS
'委託價格，以 quote asset 數值表示。LIMIT 訂單必填，MARKET 訂單可為 NULL；價格階梯與撮合保護規則留到後續流程。';

COMMENT ON COLUMN orders.quantity IS
'委託數量，以 base asset 數值表示。這是 order 的名義數量，不是可用餘額。';

COMMENT ON COLUMN orders.filled_quantity IS
'已成交數量。後續撮合或結算可以累加，但必須和 remaining_quantity / quantity 保持一致。';

COMMENT ON COLUMN orders.remaining_quantity IS
'剩餘未成交數量。此欄位保留查詢便利性，但仍要和 filled_quantity / quantity 一致。';

COMMENT ON COLUMN orders.client_order_id IS
'使用者自訂訂單代號。PostgreSQL UNIQUE 允許多筆 NULL；只有使用者提供 client_order_id 時，才要求同一 account 下唯一。';

COMMENT ON COLUMN orders.request_id IS
'請求識別碼。用於追蹤下單請求與降低重送造成重複下單的風險；完整 idempotency policy 仍需由後續 idempotency_keys 與應用層流程保證。';

COMMENT ON COLUMN orders.created_at IS
'資料建立時間。由資料庫預設產生，供審計與歷史查詢使用。';

COMMENT ON COLUMN orders.updated_at IS
'資料最後更新時間。狀態變更時由應用層或後續 migration trigger 維護。';


-- 成交 / execution record。
-- trades 只記錄撮合後的結果與費用基礎欄位，不在這一階段綁定 settlement 或 ledger posting。
-- maker / taker order、account 與 market 的一致性由 composite foreign key 保證。
CREATE TABLE trades (
    -- 成交唯一識別碼。由撮合或 execution pipeline 產生，供後續稽核與查詢追蹤。
    trade_id VARCHAR(64) PRIMARY KEY,

    -- 交易對代號。成交紀錄綁定單一 market，避免後續查詢需要再回推市場資訊。
    market_symbol VARCHAR(32) NOT NULL,

    -- Maker 訂單。保留 maker / taker 關係，供撮合回放與費用追蹤使用。
    maker_order_id VARCHAR(64) NOT NULL,

    -- Taker 訂單。保留 maker / taker 關係，供撮合回放與費用追蹤使用。
    taker_order_id VARCHAR(64) NOT NULL,

    -- Maker 所屬帳戶。透過 composite foreign key 保證此帳戶與 maker_order_id / market_symbol 一致。
    maker_account_id VARCHAR(64) NOT NULL,

    -- Taker 所屬帳戶。透過 composite foreign key 保證此帳戶與 taker_order_id / market_symbol 一致。
    taker_account_id VARCHAR(64) NOT NULL,

    -- 成交價格。使用 quote asset 單位記錄，必須保持 NUMERIC 精度。
    price NUMERIC(38, 18) NOT NULL,

    -- 成交數量。使用 base asset 單位記錄，必須保持 NUMERIC 精度。
    quantity NUMERIC(38, 18) NOT NULL,

    -- 成交金額。由 price × quantity 與精度規則推導後寫入，作為查詢與對帳便利欄位。
    -- 這裡不強制定義四捨五入政策，因為 fee / rounding 規則會影響後續 phase 的一致性。
    quote_quantity NUMERIC(38, 18) NOT NULL,

    -- Maker 手續費。這是高風險欄位，費用精度與四捨五入政策必須在後續 review 後固定。
    maker_fee_amount NUMERIC(38, 18) NOT NULL DEFAULT 0,

    -- Taker 手續費。這是高風險欄位，費用精度與四捨五入政策必須在後續 review 後固定。
    taker_fee_amount NUMERIC(38, 18) NOT NULL DEFAULT 0,

    -- 手續費幣別。費用可能與成交 base / quote 不同，這個欄位只記錄結果，不決定扣費路徑。
    fee_asset_symbol VARCHAR(32) NOT NULL,

    -- 實際成交時間。用來表示 market execution 發生時間，與資料列寫入時間分開。
    traded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 資料建立時間。與 traded_at 分開可避免查詢時把寫入延遲誤認為成交延遲。
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_trades_market_symbol
        FOREIGN KEY (market_symbol)
        REFERENCES markets (market_symbol),

    CONSTRAINT fk_trades_maker_order_id
        FOREIGN KEY (maker_order_id)
        REFERENCES orders (order_id),

    CONSTRAINT fk_trades_taker_order_id
        FOREIGN KEY (taker_order_id)
        REFERENCES orders (order_id),

    CONSTRAINT fk_trades_maker_account_id
        FOREIGN KEY (maker_account_id)
        REFERENCES accounts (account_id),

    CONSTRAINT fk_trades_taker_account_id
        FOREIGN KEY (taker_account_id)
        REFERENCES accounts (account_id),

    -- 保證 maker_order_id、maker_account_id、market_symbol 指向同一張 orders row 的一致組合。
    CONSTRAINT fk_trades_maker_order_account_market
        FOREIGN KEY (maker_order_id, maker_account_id, market_symbol)
        REFERENCES orders (order_id, account_id, market_symbol),

    -- 保證 taker_order_id、taker_account_id、market_symbol 指向同一張 orders row 的一致組合。
    CONSTRAINT fk_trades_taker_order_account_market
        FOREIGN KEY (taker_order_id, taker_account_id, market_symbol)
        REFERENCES orders (order_id, account_id, market_symbol),

    CONSTRAINT fk_trades_fee_asset_symbol
        FOREIGN KEY (fee_asset_symbol)
        REFERENCES assets (asset_symbol),

    CONSTRAINT ck_trades_distinct_orders
        CHECK (maker_order_id <> taker_order_id),

    CONSTRAINT ck_trades_price_positive
        CHECK (price > 0),

    CONSTRAINT ck_trades_quantity_positive
        CHECK (quantity > 0),

    CONSTRAINT ck_trades_quote_quantity_positive
        CHECK (quote_quantity > 0),

    CONSTRAINT ck_trades_fee_amounts_non_negative
        CHECK (maker_fee_amount >= 0 AND taker_fee_amount >= 0)
);

CREATE INDEX idx_trades_market_traded_at
    ON trades (market_symbol, traded_at);

CREATE INDEX idx_trades_maker_order_id
    ON trades (maker_order_id);

CREATE INDEX idx_trades_taker_order_id
    ON trades (taker_order_id);

CREATE INDEX idx_trades_maker_account_id
    ON trades (maker_account_id);

CREATE INDEX idx_trades_taker_account_id
    ON trades (taker_account_id);

CREATE INDEX idx_trades_fee_asset_symbol
    ON trades (fee_asset_symbol);

COMMENT ON TABLE trades IS
'成交 / execution record。只記錄撮合後的結果與費用基礎欄位，不在這一階段綁定 settlement 或 ledger posting。';

COMMENT ON COLUMN trades.trade_id IS
'成交唯一識別碼。由撮合或 execution pipeline 產生，供後續稽核與查詢追蹤。';

COMMENT ON COLUMN trades.market_symbol IS
'交易對代號。成交紀錄綁定單一 market，避免後續查詢需要再回推市場資訊。';

COMMENT ON COLUMN trades.maker_order_id IS
'Maker 訂單。保留 maker / taker 關係，供撮合回放與費用追蹤使用。';

COMMENT ON COLUMN trades.taker_order_id IS
'Taker 訂單。保留 maker / taker 關係，供撮合回放與費用追蹤使用。';

COMMENT ON COLUMN trades.maker_account_id IS
'Maker 所屬帳戶。透過 composite foreign key 保證此帳戶與 maker_order_id / market_symbol 一致；不代表此階段已完成任何結算寫入。';

COMMENT ON COLUMN trades.taker_account_id IS
'Taker 所屬帳戶。透過 composite foreign key 保證此帳戶與 taker_order_id / market_symbol 一致；不代表此階段已完成任何結算寫入。';

COMMENT ON COLUMN trades.price IS
'成交價格。使用 quote asset 單位記錄，必須保持 NUMERIC 精度。';

COMMENT ON COLUMN trades.quantity IS
'成交數量。使用 base asset 單位記錄，必須保持 NUMERIC 精度。';

COMMENT ON COLUMN trades.quote_quantity IS
'成交金額。由 price × quantity 與精度規則推導後寫入，作為查詢與對帳便利欄位；實際 rounding policy 需由後續費率與結算設計固定。';

COMMENT ON COLUMN trades.maker_fee_amount IS
'Maker 手續費。這是高風險欄位，費用精度與四捨五入政策必須在後續 review 後固定。';

COMMENT ON COLUMN trades.taker_fee_amount IS
'Taker 手續費。這是高風險欄位，費用精度與四捨五入政策必須在後續 review 後固定。';

COMMENT ON COLUMN trades.fee_asset_symbol IS
'手續費幣別。費用可能與成交 base / quote 不同，這個欄位只記錄結果，不決定扣費路徑。';

COMMENT ON COLUMN trades.traded_at IS
'實際成交時間。用來表示 market execution 發生時間，與資料列寫入時間分開。';

COMMENT ON COLUMN trades.created_at IS
'資料建立時間。與 traded_at 分開可避免查詢時把寫入延遲誤認為成交延遲。';