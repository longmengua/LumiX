-- LumiX fresh-database baseline.
-- 此候選檔直接建立 V001–V026 的最終 PostgreSQL schema；不包含歷史 ALTER/backfill 步驟。

-- P12-T02: Identity, account, asset, market foundation schema.
-- 本 migration 只建立使用者、帳戶、資產、交易對與帳戶資產關聯的基礎資料表。
-- 本 migration 不建立 ledger、reservation、orders、trades、deposits、withdrawals。
-- 本 migration 不實作任何 runtime money movement。

-- 使用者身分主檔。
-- 保存登入與帳戶歸屬所需的基本識別資料；不保存資金、帳本、訂單或交易狀態。
CREATE TABLE users (
    -- 使用者唯一識別碼。由應用層產生。
    user_id VARCHAR(64) PRIMARY KEY,

    -- 使用者電子郵件。必須唯一。
    email VARCHAR(320) NOT NULL,

    -- 使用者顯示名稱。僅供介面與管理後台顯示，不應作為安全或交易判斷依據。
    display_name VARCHAR(128) NOT NULL,

    -- 使用者狀態。ACTIVE 表示可正常使用；SUSPENDED 表示暫停；CLOSED 表示關閉。
    status VARCHAR(16) NOT NULL,
    new_device_login_email_notification_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    fund_transfer_restricted_until TIMESTAMP WITH TIME ZONE,
    withdrawal_frozen_at TIMESTAMP WITH TIME ZONE,

    -- 資料建立時間。由資料庫預設 CURRENT_TIMESTAMP 產生。
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 資料最後更新時間。此欄位不會自動更新，需由應用層或後續 migration trigger 維護。
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED'))
);

-- 使用者帳戶容器。
-- 用來區分 SPOT、FUTURES、MARGIN 等帳戶類型；本表不保存餘額，不處理資金異動。
CREATE TABLE accounts (
    -- 帳戶唯一識別碼。由應用層產生。
    account_id VARCHAR(64) PRIMARY KEY,

    -- 帳戶所屬使用者。關聯 users.user_id。
    user_id VARCHAR(64) NOT NULL,

    -- 帳戶類型。SPOT 為現貨帳戶；FUTURES 為合約帳戶；MARGIN 為槓桿帳戶。
    account_type VARCHAR(16) NOT NULL,

    -- 帳戶狀態。ACTIVE 表示可用；FROZEN 表示凍結；CLOSED 表示關閉。
    status VARCHAR(16) NOT NULL,
    account_category VARCHAR(16) NOT NULL DEFAULT 'USER',
    account_purpose VARCHAR(48),

    -- 資料建立時間。由資料庫預設 CURRENT_TIMESTAMP 產生。
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 資料最後更新時間。此欄位不會自動更新，需由應用層或後續 migration trigger 維護。
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_accounts_user_id
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT uq_accounts_user_type UNIQUE (user_id, account_type),
    CONSTRAINT uq_accounts_account_user UNIQUE (account_id, user_id),
    CONSTRAINT ck_accounts_type CHECK (account_type IN ('SPOT', 'FUTURES')),
    CONSTRAINT ck_accounts_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    CONSTRAINT ck_accounts_category CHECK (account_category IN ('USER', 'EXCHANGE')),
    CONSTRAINT ck_accounts_account_purpose CHECK (account_purpose IS NULL OR account_purpose IN ('AIRDROP_FUNDING', 'ASSET_ADJUSTMENT_COUNTERPARTY'))
);

-- 依 user_id 查詢使用者帳戶。
CREATE INDEX idx_accounts_user_id ON accounts (user_id);
CREATE INDEX idx_accounts_category ON accounts (account_category);
CREATE UNIQUE INDEX uq_accounts_account_purpose ON accounts (account_purpose) WHERE account_purpose IS NOT NULL;

-- 資產主檔。
-- 定義系統支援的資產，例如 BTC、ETH、USDT；不保存使用者餘額或鏈上交易資料。
CREATE TABLE assets (
    -- 資產代號，例如 BTC、ETH、USDT。作為資產主檔的主鍵。
    asset_symbol VARCHAR(32) PRIMARY KEY,

    -- 資產顯示名稱，例如 Bitcoin、Ethereum、Tether USD。
    display_name VARCHAR(128) NOT NULL,

    -- 資產數量精度。代表該資產可支援的小數位數，範圍為 0 到 18。
    precision_scale SMALLINT NOT NULL,

    -- 資產狀態。ACTIVE 表示可用；HALTED 表示暫停；DELISTED 表示下架。
    status VARCHAR(16) NOT NULL,

    -- 資料建立時間。由資料庫預設 CURRENT_TIMESTAMP 產生。
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 資料最後更新時間。此欄位不會自動更新，需由應用層或後續 migration trigger 維護。
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_assets_precision_scale CHECK (precision_scale BETWEEN 0 AND 18),
    CONSTRAINT ck_assets_status CHECK (status IN ('ACTIVE', 'HALTED', 'DELISTED'))
);

-- 交易對主檔。
-- 定義可交易的 base / quote 資產組合與價格、數量精度；不保存行情快照、成交量或撮合狀態。
CREATE TABLE markets (
    -- 交易對代號，例如 BTC-USDT。作為交易對主檔的主鍵。
    market_symbol VARCHAR(32) PRIMARY KEY,

    -- 交易對的 base asset，例如 BTC-USDT 中的 BTC。關聯 assets.asset_symbol。
    base_asset_symbol VARCHAR(32) NOT NULL,

    -- 交易對的 quote asset，例如 BTC-USDT 中的 USDT。關聯 assets.asset_symbol。
    quote_asset_symbol VARCHAR(32) NOT NULL,

    -- 價格精度。代表該交易對價格允許的小數位數，範圍為 0 到 18。
    price_scale SMALLINT NOT NULL,

    -- 數量精度。代表該交易對下單數量允許的小數位數，範圍為 0 到 18。
    quantity_scale SMALLINT NOT NULL,

    -- 交易對狀態。ACTIVE 表示可交易；HALTED 表示暫停；CLOSED 表示關閉。
    status VARCHAR(16) NOT NULL,

    -- 資料建立時間。由資料庫預設 CURRENT_TIMESTAMP 產生。
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 資料最後更新時間。此欄位不會自動更新，需由應用層或後續 migration trigger 維護。
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_markets_base_asset_symbol
        FOREIGN KEY (base_asset_symbol) REFERENCES assets (asset_symbol),
    CONSTRAINT fk_markets_quote_asset_symbol
        FOREIGN KEY (quote_asset_symbol) REFERENCES assets (asset_symbol),
    CONSTRAINT uq_markets_pair UNIQUE (base_asset_symbol, quote_asset_symbol),
    CONSTRAINT ck_markets_asset_pair CHECK (base_asset_symbol <> quote_asset_symbol),
    CONSTRAINT ck_markets_price_scale CHECK (price_scale BETWEEN 0 AND 18),
    CONSTRAINT ck_markets_quantity_scale CHECK (quantity_scale BETWEEN 0 AND 18),
    CONSTRAINT ck_markets_status CHECK (status IN ('ACTIVE', 'HALTED', 'CLOSED'))
);

-- 依 base asset 查詢交易對。
CREATE INDEX idx_markets_base_asset_symbol ON markets (base_asset_symbol);

-- 依 quote asset 查詢交易對。
CREATE INDEX idx_markets_quote_asset_symbol ON markets (quote_asset_symbol);

-- 帳戶與資產的基礎關聯表。
-- 表示某帳戶可關聯某資產；不保存 available、locked、reserved 或 total balance。
CREATE TABLE account_assets (
    -- 帳戶資產關聯唯一識別碼。由資料庫 identity 產生。
    account_asset_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,

    -- 帳戶識別碼。關聯 accounts.account_id。
    account_id VARCHAR(64) NOT NULL,

    -- 資產代號。關聯 assets.asset_symbol。
    asset_symbol VARCHAR(32) NOT NULL,

    -- 帳戶資產關聯狀態。ACTIVE 表示可用；INACTIVE 表示停用。
    status VARCHAR(16) NOT NULL,

    -- 資料建立時間。由資料庫預設 CURRENT_TIMESTAMP 產生。
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 資料最後更新時間。此欄位不會自動更新，需由應用層或後續 migration trigger 維護。
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_account_assets_account_id
        FOREIGN KEY (account_id) REFERENCES accounts (account_id),
    CONSTRAINT fk_account_assets_asset_symbol
        FOREIGN KEY (asset_symbol) REFERENCES assets (asset_symbol),
    CONSTRAINT uq_account_assets_account_asset UNIQUE (account_id, asset_symbol),
    CONSTRAINT ck_account_assets_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- 依 account_id 查詢帳戶資產關聯。
CREATE INDEX idx_account_assets_account_id ON account_assets (account_id);

-- 依 asset_symbol 查詢帳戶資產關聯。
CREATE INDEX idx_account_assets_asset_symbol ON account_assets (asset_symbol);


-- PostgreSQL database comments.
-- 這些註解會寫入 PostgreSQL metadata，可由 psql、DBeaver、DataGrip 等工具讀取。

COMMENT ON TABLE users IS
'使用者身分主檔。保存登入與帳戶歸屬所需的基本識別資料；不保存資金、帳本、訂單或交易狀態。';

COMMENT ON COLUMN users.user_id IS
'使用者唯一識別碼。由應用層產生，作為其他帳戶與身分相關資料的外鍵目標。';

COMMENT ON COLUMN users.email IS
'使用者電子郵件。必須唯一；可作為登入識別或通知地址，但不應直接承載權限或 KYC 狀態。';

COMMENT ON COLUMN users.display_name IS
'使用者顯示名稱。僅供介面與管理後台顯示，不應作為安全或交易判斷依據。';

COMMENT ON COLUMN users.status IS
'使用者狀態。ACTIVE 表示可正常使用；SUSPENDED 表示暫停；CLOSED 表示關閉。';

COMMENT ON COLUMN users.created_at IS
'資料建立時間。由資料庫預設 CURRENT_TIMESTAMP 產生。';

COMMENT ON COLUMN users.updated_at IS
'資料最後更新時間。此欄位不會自動更新，需由應用層或後續 migration trigger 維護。';


COMMENT ON TABLE accounts IS
'使用者帳戶容器。用來區分 SPOT、FUTURES、MARGIN 等帳戶類型；本表不保存餘額，不處理資金異動。';

COMMENT ON COLUMN accounts.account_id IS
'帳戶唯一識別碼。由應用層產生。';

COMMENT ON COLUMN accounts.user_id IS
'帳戶所屬使用者。關聯 users.user_id。';

COMMENT ON COLUMN accounts.account_type IS
'帳戶類型。SPOT 為現貨帳戶；FUTURES 為合約帳戶；MARGIN 為槓桿帳戶。';

COMMENT ON COLUMN accounts.status IS
'帳戶狀態。ACTIVE 表示可用；FROZEN 表示凍結；CLOSED 表示關閉。';

COMMENT ON COLUMN accounts.created_at IS
'資料建立時間。由資料庫預設 CURRENT_TIMESTAMP 產生。';

COMMENT ON COLUMN accounts.updated_at IS
'資料最後更新時間。此欄位不會自動更新，需由應用層或後續 migration trigger 維護。';


COMMENT ON TABLE assets IS
'資產主檔。定義系統支援的資產，例如 BTC、ETH、USDT；不保存使用者餘額或鏈上交易資料。';

COMMENT ON COLUMN assets.asset_symbol IS
'資產代號，例如 BTC、ETH、USDT。作為資產主檔的主鍵。';

COMMENT ON COLUMN assets.display_name IS
'資產顯示名稱，例如 Bitcoin、Ethereum、Tether USD。';

COMMENT ON COLUMN assets.precision_scale IS
'資產數量精度。代表該資產可支援的小數位數，範圍為 0 到 18。';

COMMENT ON COLUMN assets.status IS
'資產狀態。ACTIVE 表示可用；HALTED 表示暫停；DELISTED 表示下架。';

COMMENT ON COLUMN assets.created_at IS
'資料建立時間。由資料庫預設 CURRENT_TIMESTAMP 產生。';

COMMENT ON COLUMN assets.updated_at IS
'資料最後更新時間。此欄位不會自動更新，需由應用層或後續 migration trigger 維護。';


COMMENT ON TABLE markets IS
'交易對主檔。定義可交易的 base / quote 資產組合與價格、數量精度；不保存行情快照、成交量或撮合狀態。';

COMMENT ON COLUMN markets.market_symbol IS
'交易對代號，例如 BTC-USDT。作為交易對主檔的主鍵。';

COMMENT ON COLUMN markets.base_asset_symbol IS
'交易對的 base asset，例如 BTC-USDT 中的 BTC。關聯 assets.asset_symbol。';

COMMENT ON COLUMN markets.quote_asset_symbol IS
'交易對的 quote asset，例如 BTC-USDT 中的 USDT。關聯 assets.asset_symbol。';

COMMENT ON COLUMN markets.price_scale IS
'價格精度。代表該交易對價格允許的小數位數，範圍為 0 到 18。';

COMMENT ON COLUMN markets.quantity_scale IS
'數量精度。代表該交易對下單數量允許的小數位數，範圍為 0 到 18。';

COMMENT ON COLUMN markets.status IS
'交易對狀態。ACTIVE 表示可交易；HALTED 表示暫停；CLOSED 表示關閉。';

COMMENT ON COLUMN markets.created_at IS
'資料建立時間。由資料庫預設 CURRENT_TIMESTAMP 產生。';

COMMENT ON COLUMN markets.updated_at IS
'資料最後更新時間。此欄位不會自動更新，需由應用層或後續 migration trigger 維護。';


COMMENT ON TABLE account_assets IS
'帳戶與資產的基礎關聯表。表示某帳戶可關聯某資產；不保存 available、locked、reserved 或 total balance。';

COMMENT ON COLUMN account_assets.account_asset_id IS
'帳戶資產關聯唯一識別碼。由資料庫 identity 產生。';

COMMENT ON COLUMN account_assets.account_id IS
'帳戶識別碼。關聯 accounts.account_id。';

COMMENT ON COLUMN account_assets.asset_symbol IS
'資產代號。關聯 assets.asset_symbol。';

COMMENT ON COLUMN account_assets.status IS
'帳戶資產關聯狀態。ACTIVE 表示可用；INACTIVE 表示停用。';

COMMENT ON COLUMN account_assets.created_at IS
'資料建立時間。由資料庫預設 CURRENT_TIMESTAMP 產生。';

COMMENT ON COLUMN account_assets.updated_at IS
'資料最後更新時間。此欄位不會自動更新，需由應用層或後續 migration trigger 維護。';
-- Balance projections are the query-facing snapshot of account balances.
-- They must remain rebuildable from ledger data once ledger tables exist.
CREATE TABLE balance_projections (
    balance_projection_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    account_id VARCHAR(64) NOT NULL,
    asset_symbol VARCHAR(32) NOT NULL,
    total_amount NUMERIC(36, 18) NOT NULL DEFAULT 0,
    available_amount NUMERIC(36, 18) NOT NULL DEFAULT 0,
    locked_amount NUMERIC(36, 18) NOT NULL DEFAULT 0,
    projection_version BIGINT NOT NULL DEFAULT 0,
    projected_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reconciled_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_balance_projections_account_id
        FOREIGN KEY (account_id) REFERENCES accounts (account_id),
    CONSTRAINT fk_balance_projections_asset_symbol
        FOREIGN KEY (asset_symbol) REFERENCES assets (asset_symbol),
    CONSTRAINT uq_balance_projections_account_asset UNIQUE (account_id, asset_symbol),
    CONSTRAINT ck_balance_projections_amounts_non_negative
        CHECK (total_amount >= 0 AND available_amount >= 0 AND locked_amount >= 0),
    CONSTRAINT ck_balance_projections_total_matches_components
        CHECK (total_amount = available_amount + locked_amount),
    CONSTRAINT ck_balance_projections_projection_version CHECK (projection_version >= 0)
);

COMMENT ON TABLE balance_projections IS 'Account balance read model for query-side balance projection and reconciliation.';
COMMENT ON COLUMN balance_projections.balance_projection_id IS 'Surrogate identifier for the balance projection row.';
COMMENT ON COLUMN balance_projections.account_id IS 'Reference to the owning account.';
COMMENT ON COLUMN balance_projections.asset_symbol IS 'Reference to the asset whose balance is projected.';
COMMENT ON COLUMN balance_projections.total_amount IS 'Projected total balance for the account and asset.';
COMMENT ON COLUMN balance_projections.available_amount IS 'Projected available balance that is not locked.';
COMMENT ON COLUMN balance_projections.locked_amount IS 'Projected locked balance held for orders or withdrawals.';
COMMENT ON COLUMN balance_projections.projection_version IS 'Monotonic version of the projection row for rebuild and replay checks.';
COMMENT ON COLUMN balance_projections.projected_at IS 'Timestamp when the balance projection was last refreshed.';
COMMENT ON COLUMN balance_projections.reconciled_at IS 'Timestamp when the row was last reconciled against source data.';

CREATE INDEX idx_balance_projections_account_id ON balance_projections (account_id);
CREATE INDEX idx_balance_projections_asset_symbol ON balance_projections (asset_symbol);
CREATE INDEX idx_balance_projections_reconciled_at ON balance_projections (reconciled_at);

-- Ledger journals are immutable business-event headers.
-- Ledger entries are append-only double-entry lines tied to a journal.
CREATE TABLE ledger_journals (
    ledger_journal_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    business_reference_type VARCHAR(32) NOT NULL,
    business_reference_id VARCHAR(128) NOT NULL,
    request_id VARCHAR(64),
    journal_note VARCHAR(256),
    posted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_ledger_journals_reference_type
        CHECK (business_reference_type IN ('DEPOSIT', 'WITHDRAWAL', 'ORDER', 'TRADE', 'SETTLEMENT', 'FEE', 'ADJUSTMENT'))
);

COMMENT ON TABLE ledger_journals IS 'Immutable journal header for ledger business events.';
COMMENT ON COLUMN ledger_journals.ledger_journal_id IS 'Surrogate identifier for the journal header.';
COMMENT ON COLUMN ledger_journals.business_reference_type IS 'Business event category such as deposit, withdrawal, order, trade, settlement, fee, or adjustment.';
COMMENT ON COLUMN ledger_journals.business_reference_id IS 'Business-side reference identifier for the source event or aggregate.';
COMMENT ON COLUMN ledger_journals.request_id IS 'Optional request identifier for idempotency or audit correlation.';
COMMENT ON COLUMN ledger_journals.journal_note IS 'Optional human-readable note for audit and operations.';
COMMENT ON COLUMN ledger_journals.posted_at IS 'Business timestamp when the journal was posted.';
COMMENT ON COLUMN ledger_journals.created_at IS 'Database timestamp when the journal row was created.';

CREATE INDEX idx_ledger_journals_posted_at ON ledger_journals (posted_at);
CREATE INDEX idx_ledger_journals_reference ON ledger_journals (business_reference_type, business_reference_id);
CREATE INDEX idx_ledger_journals_request_id ON ledger_journals (request_id);

CREATE TABLE ledger_entries (
    ledger_entry_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    ledger_journal_id BIGINT NOT NULL,
    entry_sequence INTEGER NOT NULL,
    account_id VARCHAR(64) NOT NULL,
    asset_symbol VARCHAR(32) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    amount NUMERIC(36, 18) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ledger_entries_ledger_journal_id
        FOREIGN KEY (ledger_journal_id) REFERENCES ledger_journals (ledger_journal_id),
    CONSTRAINT fk_ledger_entries_account_id
        FOREIGN KEY (account_id) REFERENCES accounts (account_id),
    CONSTRAINT fk_ledger_entries_asset_symbol
        FOREIGN KEY (asset_symbol) REFERENCES assets (asset_symbol),
    CONSTRAINT uq_ledger_entries_journal_sequence UNIQUE (ledger_journal_id, entry_sequence),
    CONSTRAINT ck_ledger_entries_sequence_positive CHECK (entry_sequence > 0),
    CONSTRAINT ck_ledger_entries_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_ledger_entries_amount_positive CHECK (amount > 0)
);

COMMENT ON TABLE ledger_entries IS 'Append-only double-entry lines for each immutable ledger journal.';
COMMENT ON COLUMN ledger_entries.ledger_entry_id IS 'Surrogate identifier for the ledger line item.';
COMMENT ON COLUMN ledger_entries.ledger_journal_id IS 'Reference to the immutable journal header that groups this entry.';
COMMENT ON COLUMN ledger_entries.entry_sequence IS 'Journal-local sequence number for deterministic entry ordering.';
COMMENT ON COLUMN ledger_entries.account_id IS 'Reference to the account affected by this entry.';
COMMENT ON COLUMN ledger_entries.asset_symbol IS 'Reference to the asset affected by this entry.';
COMMENT ON COLUMN ledger_entries.direction IS 'Debit or credit direction for double-entry accounting.';
COMMENT ON COLUMN ledger_entries.amount IS 'Positive monetary amount for the entry.';
COMMENT ON COLUMN ledger_entries.created_at IS 'Database timestamp when the entry row was created.';

CREATE INDEX idx_ledger_entries_journal_id ON ledger_entries (ledger_journal_id);
CREATE INDEX idx_ledger_entries_account_id ON ledger_entries (account_id);
CREATE INDEX idx_ledger_entries_asset_symbol ON ledger_entries (asset_symbol);
CREATE INDEX idx_ledger_entries_direction ON ledger_entries (direction);

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
-- P12-T06: Wallet lifecycle schema for deposit, withdrawal, address, and chain transaction records.
-- 這份 migration 只建立錢包生命週期資料結構，供後續對帳與查詢使用。
-- 不實作鏈上監聽、入金入帳、提款簽章、廣播或任何 runtime money movement。
-- Rollback 注意事項：若要回滾，必須先確認沒有後續資料依賴這些表；刪除順序應為 withdrawals、deposits、chain_transactions、deposit_addresses。

-- 使用者入金地址主檔。
-- 這只是一份可查詢的 address registry，不代表地址已被入帳，也不代表地址私鑰由本系統持有。
CREATE TABLE deposit_addresses (
    -- 入金地址唯一識別碼。由資料庫產生，避免地址輪替時需要改動外部引用。
    deposit_address_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,

    -- 所屬帳戶。使用 account 作為邊界，避免把地址直接綁死在使用者層級。
    account_id VARCHAR(64) NOT NULL,

    -- 所屬資產。與 account_id 共同構成可用的資產範圍，必須與 account_assets 保持一致。
    asset_symbol VARCHAR(32) NOT NULL,

    -- 鏈別，例如 ERC20、TRC20、BTC、SOL。這只代表地址所在網路，不代表任何鏈上事件已確認。
    chain_type VARCHAR(16) NOT NULL,

    -- 入金地址字串。只保存可供查詢的地址內容，不保存密鑰或簽章材料。
    address VARCHAR(256) NOT NULL,

    -- 地址標籤或備註。部分鏈或業務場景需要額外標記，但這不應承載安全判斷。
    address_label VARCHAR(128),

    -- 地址狀態。ACTIVE 表示可用；DISABLED 表示停用；ARCHIVED 表示歷史保留。
    status VARCHAR(16) NOT NULL,

    -- 資料建立時間。由資料庫預設產生，供審計與對帳使用。
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 資料最後更新時間。地址輪替或狀態變更時由應用層維護。
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_deposit_addresses_account_asset
        FOREIGN KEY (account_id, asset_symbol) REFERENCES account_assets (account_id, asset_symbol),
    CONSTRAINT ck_deposit_addresses_chain_type CHECK (chain_type IN ('TRC20', 'ERC20', 'BTC', 'SOL')),
    CONSTRAINT ck_deposit_addresses_status CHECK (status IN ('ACTIVE', 'DISABLED', 'ARCHIVED')),
    CONSTRAINT uq_deposit_addresses_chain_address UNIQUE (chain_type, address),
    CONSTRAINT uq_deposit_addresses_account_asset_chain UNIQUE (account_id, asset_symbol, chain_type)
);

CREATE INDEX idx_deposit_addresses_account_id ON deposit_addresses (account_id);
CREATE INDEX idx_deposit_addresses_asset_symbol ON deposit_addresses (asset_symbol);
CREATE INDEX idx_deposit_addresses_status ON deposit_addresses (status);

COMMENT ON TABLE deposit_addresses IS
'使用者入金地址主檔。這只是一份可查詢的 address registry，不代表地址已被入帳，也不代表地址私鑰由本系統持有。';

COMMENT ON COLUMN deposit_addresses.deposit_address_id IS
'入金地址唯一識別碼。由資料庫產生，避免地址輪替時需要改動外部引用。';

COMMENT ON COLUMN deposit_addresses.account_id IS
'所屬帳戶。使用 account 作為邊界，避免把地址直接綁死在使用者層級。';

COMMENT ON COLUMN deposit_addresses.asset_symbol IS
'所屬資產。與 account_id 共同構成可用的資產範圍，必須與 account_assets 保持一致。';

COMMENT ON COLUMN deposit_addresses.chain_type IS
'鏈別，例如 ERC20、TRC20、BTC、SOL。這只代表地址所在網路，不代表任何鏈上事件已確認。';

COMMENT ON COLUMN deposit_addresses.address IS
'入金地址字串。只保存可供查詢的地址內容，不保存密鑰或簽章材料。';

COMMENT ON COLUMN deposit_addresses.address_label IS
'地址標籤或備註。部分鏈或業務場景需要額外標記，但這不應承載安全判斷。';

COMMENT ON COLUMN deposit_addresses.status IS
'地址狀態。ACTIVE 表示可用；DISABLED 表示停用；ARCHIVED 表示歷史保留。';

COMMENT ON COLUMN deposit_addresses.created_at IS
'資料建立時間。由資料庫預設產生，供審計與對帳使用。';

COMMENT ON COLUMN deposit_addresses.updated_at IS
'資料最後更新時間。地址輪替或狀態變更時由應用層維護。';

-- 鏈上交易主檔。
-- 這是 chain observer 的可查詢結果，不是 ledger source of truth，也不是入帳完成證明。
CREATE TABLE chain_transactions (
    -- 鏈上交易唯一識別碼。由資料庫產生，供本地關聯與對帳。
    chain_transaction_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,

    -- 鏈別。這決定 tx hash 的命名空間，不同鏈不能混用相同 hash 做唯一性判斷。
    chain_type VARCHAR(16) NOT NULL,

    -- 交易類型。DEPOSIT / WITHDRAWAL 只描述這筆鏈上交易對應的業務方向。
    transaction_type VARCHAR(16) NOT NULL,

    -- 鏈上交易哈希。這是鏈層唯一識別碼，必須保留以便查詢與對帳。
    tx_hash VARCHAR(128) NOT NULL,

    -- 區塊哈希。僅供追溯；交易仍可能因 reorg 或確認政策而改變業務狀態。
    block_hash VARCHAR(128),

    -- 區塊高度。僅供觀測與查詢，不代表本地資金狀態已完成更新。
    block_height BIGINT,

    -- 發送地址。這是觀測欄位，不是授權來源，也不代表風控已通過。
    from_address VARCHAR(256),

    -- 收款地址。對入金而言通常對應 deposit address；對提款而言則代表外部收款方。
    to_address VARCHAR(256),

    -- 鏈上金額。使用 NUMERIC(38, 18) 避免 binary floating point 誤差。
    amount NUMERIC(38, 18) NOT NULL,

    -- 鏈上手續費。這是觀測欄位，不能直接拿來當作正式扣費規則。
    fee_amount NUMERIC(38, 18) NOT NULL DEFAULT 0,

    -- 鏈上交易狀態。只描述觀測與確認層級，不代表本地帳務已完成。
    status VARCHAR(16) NOT NULL,

    -- 最早觀測時間。用來區分 observer 看到交易與業務完成入帳之間的時間差。
    observed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 確認時間。只有在確認政策滿足時才會填值；這不等於入帳完成。
    confirmed_at TIMESTAMP WITH TIME ZONE,

    -- 資料建立時間。由資料庫預設產生，供審計與對帳使用。
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 資料最後更新時間。狀態修正或重新對帳時由應用層維護。
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_chain_transactions_chain_type CHECK (chain_type IN ('TRC20', 'ERC20', 'BTC', 'SOL')),
    CONSTRAINT ck_chain_transactions_transaction_type CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL')),
    CONSTRAINT ck_chain_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_chain_transactions_fee_non_negative CHECK (fee_amount >= 0),
    CONSTRAINT ck_chain_transactions_status CHECK (status IN ('OBSERVED', 'CONFIRMED', 'FAILED', 'REORGED', 'IGNORED')),
    CONSTRAINT uq_chain_transactions_chain_tx_hash UNIQUE (chain_type, tx_hash)
);

CREATE INDEX idx_chain_transactions_chain_type_status ON chain_transactions (chain_type, status);
CREATE INDEX idx_chain_transactions_observed_at ON chain_transactions (observed_at);
CREATE INDEX idx_chain_transactions_confirmed_at ON chain_transactions (confirmed_at);

COMMENT ON TABLE chain_transactions IS
'鏈上交易主檔。這是 chain observer 的可查詢結果，不是 ledger source of truth，也不是入帳完成證明。';

COMMENT ON COLUMN chain_transactions.chain_transaction_id IS
'鏈上交易唯一識別碼。由資料庫產生，供本地關聯與對帳。';

COMMENT ON COLUMN chain_transactions.chain_type IS
'鏈別。這決定 tx hash 的命名空間，不同鏈不能混用相同 hash 做唯一性判斷。';

COMMENT ON COLUMN chain_transactions.transaction_type IS
'交易類型。DEPOSIT / WITHDRAWAL 只描述這筆鏈上交易對應的業務方向。';

COMMENT ON COLUMN chain_transactions.tx_hash IS
'鏈上交易哈希。這是鏈層唯一識別碼，必須保留以便查詢與對帳。';

COMMENT ON COLUMN chain_transactions.block_hash IS
'區塊哈希。僅供追溯；交易仍可能因 reorg 或確認政策而改變業務狀態。';

COMMENT ON COLUMN chain_transactions.block_height IS
'區塊高度。僅供觀測與查詢，不代表本地資金狀態已完成更新。';

COMMENT ON COLUMN chain_transactions.from_address IS
'發送地址。這是觀測欄位，不是授權來源，也不代表風控已通過。';

COMMENT ON COLUMN chain_transactions.to_address IS
'收款地址。對入金而言通常對應 deposit address；對提款而言則代表外部收款方。';

COMMENT ON COLUMN chain_transactions.amount IS
'鏈上金額。使用 NUMERIC(38, 18) 避免 binary floating point 誤差。';

COMMENT ON COLUMN chain_transactions.fee_amount IS
'鏈上手續費。這是觀測欄位，不能直接拿來當作正式扣費規則。';

COMMENT ON COLUMN chain_transactions.status IS
'鏈上交易狀態。只描述觀測與確認層級，不代表本地帳務已完成。';

COMMENT ON COLUMN chain_transactions.observed_at IS
'最早觀測時間。用來區分 observer 看到交易與業務完成入帳之間的時間差。';

COMMENT ON COLUMN chain_transactions.confirmed_at IS
'確認時間。只有在確認政策滿足時才會填值；這不等於入帳完成。';

COMMENT ON COLUMN chain_transactions.created_at IS
'資料建立時間。由資料庫預設產生，供審計與對帳使用。';

COMMENT ON COLUMN chain_transactions.updated_at IS
'資料最後更新時間。狀態修正或重新對帳時由應用層維護。';

-- 充值紀錄。
-- 這只保存入金流程的查詢結果，不代表鏈上觀測一出現就已經完成 credit。
CREATE TABLE deposits (
    -- 充值唯一識別碼。由應用層或上游流程產生，便於操作與對帳。
    deposit_id VARCHAR(64) PRIMARY KEY,

    -- 所屬帳戶。入金最終是落到哪個 account，必須能直接查詢。
    account_id VARCHAR(64) NOT NULL,

    -- 所屬資產。必須與 account_assets 保持一致，不能讓 deposit 自己決定資產歸屬。
    asset_symbol VARCHAR(32) NOT NULL,

    -- 對應的入金地址。這裡保留可追查的 address reference，方便後續對帳。
    deposit_address_id BIGINT NOT NULL,

    -- 對應的鏈上交易。鏈上交易先被觀測，deposits 只是業務層記錄，不是入帳證明。
    chain_transaction_id BIGINT NOT NULL,

    -- 鏈別。保留查詢便利性，避免每次都 join chain_transactions。
    chain_type VARCHAR(16) NOT NULL,

    -- 充值地址。為了對帳與稽核，保留當時使用的地址字串快照。
    address VARCHAR(256) NOT NULL,

    -- 充值金額。使用 NUMERIC(38, 18) 避免浮點誤差。
    amount NUMERIC(38, 18) NOT NULL,

    -- 已確認數。只做對帳與顯示，不等於已 credit。
    confirmations INTEGER NOT NULL DEFAULT 0,

    -- 充值狀態。只描述入金流程狀態，不代表 ledger 已寫入。
    status VARCHAR(16) NOT NULL,

    -- 入帳時間。只有在後續 credit 完成時才應由應用層填值。
    credited_at TIMESTAMP WITH TIME ZONE,

    -- 資料建立時間。由資料庫預設產生，供審計與對帳使用。
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 資料最後更新時間。狀態進展或對帳修正時由應用層維護。
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_deposits_account_asset
        FOREIGN KEY (account_id, asset_symbol) REFERENCES account_assets (account_id, asset_symbol),
    CONSTRAINT fk_deposits_deposit_address_id
        FOREIGN KEY (deposit_address_id) REFERENCES deposit_addresses (deposit_address_id),
    CONSTRAINT fk_deposits_chain_transaction_id
        FOREIGN KEY (chain_transaction_id) REFERENCES chain_transactions (chain_transaction_id),
    CONSTRAINT ck_deposits_chain_type CHECK (chain_type IN ('TRC20', 'ERC20', 'BTC', 'SOL')),
    CONSTRAINT ck_deposits_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_deposits_confirmations_non_negative CHECK (confirmations >= 0),
    CONSTRAINT ck_deposits_status CHECK (status IN ('PENDING', 'CONFIRMING', 'SUCCESS', 'FAILED', 'IGNORED')),
    CONSTRAINT uq_deposits_chain_transaction UNIQUE (chain_transaction_id)
);

CREATE INDEX idx_deposits_account_id ON deposits (account_id);
CREATE INDEX idx_deposits_asset_symbol ON deposits (asset_symbol);
CREATE INDEX idx_deposits_status ON deposits (status);
CREATE INDEX idx_deposits_chain_transaction_id ON deposits (chain_transaction_id);

COMMENT ON TABLE deposits IS
'充值紀錄。這只保存入金流程的查詢結果，不代表鏈上觀測一出現就已經完成 credit。';

COMMENT ON COLUMN deposits.deposit_id IS
'充值唯一識別碼。由應用層或上游流程產生，便於操作與對帳。';

COMMENT ON COLUMN deposits.account_id IS
'所屬帳戶。入金最終是落到哪個 account，必須能直接查詢。';

COMMENT ON COLUMN deposits.asset_symbol IS
'所屬資產。必須與 account_assets 保持一致，不能讓 deposit 自己決定資產歸屬。';

COMMENT ON COLUMN deposits.deposit_address_id IS
'對應的入金地址。這裡保留可追查的 address reference，方便後續對帳。';

COMMENT ON COLUMN deposits.chain_transaction_id IS
'對應的鏈上交易。鏈上交易先被觀測，deposits 只是業務層記錄，不是入帳證明。';

COMMENT ON COLUMN deposits.chain_type IS
'鏈別。保留查詢便利性，避免每次都 join chain_transactions。';

COMMENT ON COLUMN deposits.address IS
'充值地址。為了對帳與稽核，保留當時使用的地址字串快照。';

COMMENT ON COLUMN deposits.amount IS
'充值金額。使用 NUMERIC(38, 18) 避免浮點誤差。';

COMMENT ON COLUMN deposits.confirmations IS
'已確認數。只做對帳與顯示，不等於已 credit。';

COMMENT ON COLUMN deposits.status IS
'充值狀態。只描述入金流程狀態，不代表 ledger 已寫入。';

COMMENT ON COLUMN deposits.credited_at IS
'入帳時間。只有在後續 credit 完成時才應由應用層填值。';

COMMENT ON COLUMN deposits.created_at IS
'資料建立時間。由資料庫預設產生，供審計與對帳使用。';

COMMENT ON COLUMN deposits.updated_at IS
'資料最後更新時間。狀態進展或對帳修正時由應用層維護。';

-- 提現紀錄。
-- 這只保存提款流程的查詢結果，不代表已核准、已簽章或已廣播。
CREATE TABLE withdrawals (
    -- 提現唯一識別碼。由應用層產生，作為操作與稽核主鍵。
    withdrawal_id VARCHAR(64) PRIMARY KEY,

    -- 請求識別碼。這裡只做查詢與重送辨識的輔助欄位，不把它當成完整 idempotency 保證。
    request_id VARCHAR(64) NOT NULL,

    -- 所屬帳戶。提款從哪個 account 出去，必須能直接查詢。
    account_id VARCHAR(64) NOT NULL,

    -- 所屬資產。必須與 account_assets 保持一致。
    asset_symbol VARCHAR(32) NOT NULL,

    -- 鏈別。提款對應哪個網路，會影響地址格式與鏈上費用觀測。
    chain_type VARCHAR(16) NOT NULL,

    -- 收款地址。只保存外部地址字串，不保存任何密鑰或簽章材料。
    address VARCHAR(256) NOT NULL,

    -- 收款地址備註。部分業務或鏈別會需要標記，但不應承載安全判斷。
    address_label VARCHAR(128),

    -- 提現金額。使用 NUMERIC(38, 18) 避免浮點誤差。
    amount NUMERIC(38, 18) NOT NULL,

    -- 手續費。這是展示與對帳欄位，不決定實際扣費策略。
    fee_amount NUMERIC(38, 18) NOT NULL DEFAULT 0,

    -- 對應的鏈上交易。尚未廣播或尚未確認時可為 NULL。
    chain_transaction_id BIGINT,

    -- 提現狀態。這些值只代表流程階段，不代表資金已移出。
    status VARCHAR(24) NOT NULL,

    -- 資料建立時間。由資料庫預設產生，供審計與對帳使用。
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 資料最後更新時間。狀態進展、風控或對帳修正時由應用層維護。
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_withdrawals_account_asset
        FOREIGN KEY (account_id, asset_symbol) REFERENCES account_assets (account_id, asset_symbol),
    CONSTRAINT fk_withdrawals_chain_transaction_id
        FOREIGN KEY (chain_transaction_id) REFERENCES chain_transactions (chain_transaction_id),
    CONSTRAINT ck_withdrawals_chain_type CHECK (chain_type IN ('TRC20', 'ERC20', 'BTC', 'SOL')),
    CONSTRAINT ck_withdrawals_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_withdrawals_fee_non_negative CHECK (fee_amount >= 0),
    CONSTRAINT ck_withdrawals_status CHECK (status IN ('SUBMITTED', 'RISK_REVIEW', 'ADMIN_REVIEW', 'APPROVED', 'REJECTED', 'BROADCASTING', 'CHAIN_PENDING', 'SUCCESS', 'FAILED', 'CANCELED')),
    CONSTRAINT uq_withdrawals_request_id UNIQUE (request_id),
    CONSTRAINT uq_withdrawals_chain_transaction UNIQUE (chain_transaction_id)
);

CREATE INDEX idx_withdrawals_account_id ON withdrawals (account_id);
CREATE INDEX idx_withdrawals_asset_symbol ON withdrawals (asset_symbol);
CREATE INDEX idx_withdrawals_status ON withdrawals (status);
CREATE INDEX idx_withdrawals_chain_transaction_id ON withdrawals (chain_transaction_id);

COMMENT ON TABLE withdrawals IS
'提現紀錄。這只保存提款流程的查詢結果，不代表已核准、已簽章或已廣播。';

COMMENT ON COLUMN withdrawals.withdrawal_id IS
'提現唯一識別碼。由應用層產生，作為操作與稽核主鍵。';

COMMENT ON COLUMN withdrawals.request_id IS
'請求識別碼。這裡只做查詢與重送辨識的輔助欄位，不把它當成完整 idempotency 保證。';

COMMENT ON COLUMN withdrawals.account_id IS
'所屬帳戶。提款從哪個 account 出去，必須能直接查詢。';

COMMENT ON COLUMN withdrawals.asset_symbol IS
'所屬資產。必須與 account_assets 保持一致。';

COMMENT ON COLUMN withdrawals.chain_type IS
'鏈別。提款對應哪個網路，會影響地址格式與鏈上費用觀測。';

COMMENT ON COLUMN withdrawals.address IS
'收款地址。只保存外部地址字串，不保存任何密鑰或簽章材料。';

COMMENT ON COLUMN withdrawals.address_label IS
'收款地址備註。部分業務或鏈別會需要標記，但不應承載安全判斷。';

COMMENT ON COLUMN withdrawals.amount IS
'提現金額。使用 NUMERIC(38, 18) 避免浮點誤差。';

COMMENT ON COLUMN withdrawals.fee_amount IS
'手續費。這是展示與對帳欄位，不決定實際扣費策略。';

COMMENT ON COLUMN withdrawals.chain_transaction_id IS
'對應的鏈上交易。尚未廣播或尚未確認時可為 NULL。';

COMMENT ON COLUMN withdrawals.status IS
'提現狀態。這些值只代表流程階段，不代表資金已移出。';

COMMENT ON COLUMN withdrawals.created_at IS
'資料建立時間。由資料庫預設產生，供審計與對帳使用。';

COMMENT ON COLUMN withdrawals.updated_at IS
'資料最後更新時間。狀態進展、風控或對帳修正時由應用層維護。';

-- P12-T06: Reservation, hold, and release schema.
-- 這份 migration 只建立 reservation 資料結構，用來表達資金或資產被預留、部分消耗、完全釋放或到期失效的狀態。
-- 不實作 order hold / release service、withdrawal hold / release service、matching runtime 或 settlement runtime。
-- reservation 不是 ledger entry，也不是 balance projection；真正的資金異動與對帳仍由後續 runtime 與 ledger / reconciliation 完成。
-- Rollback 注意事項：若要回滾，必須先確認沒有後續流程依賴 reservation；刪除順序為 reservations，且不得影響 V005 wallet lifecycle schema。

-- 預留主檔。
-- 這只是一份狀態資料結構，記錄某筆資產在特定業務目的下被保留、消耗或釋放的結果，不直接代表資金已扣款。
CREATE TABLE reservations (
    -- 預留唯一識別碼。由應用層或上游流程產生，供查詢、對帳與回放使用。
    reservation_id VARCHAR(64) PRIMARY KEY,

    -- 所屬帳戶。reservation 必須綁定 account，因為預留是以帳戶為邊界的資金控制資料。
    account_id VARCHAR(64) NOT NULL,

    -- 所屬資產。與 account_id 一起約束 reservation 只可對既有 account_assets 建立。
    asset_symbol VARCHAR(32) NOT NULL,

    -- 業務參照類型。用來標識 reservation 來源，例如 ORDER、WITHDRAWAL、SETTLEMENT 或 ADJUSTMENT。
    business_reference_type VARCHAR(16) NOT NULL,

    -- 業務參照識別碼。指向對應業務物件，但不代表該物件已完成最終資金動作。
    business_reference_id VARCHAR(128) NOT NULL,

    -- 預留類型。描述這筆 reservation 的用途，例如下單、提款、結算或人工處理。
    reservation_type VARCHAR(24) NOT NULL,

    -- 預留狀態。這只描述資料上的生命週期，不等於 ledger 已經寫入或餘額已經結算。
    status VARCHAR(24) NOT NULL,

    -- 原始預留金額。使用 NUMERIC(38, 18) 保存，避免 binary floating point 誤差。
    original_amount NUMERIC(38, 18) NOT NULL,

    -- 剩餘可釋放或可再消耗的預留金額。這是查詢欄位，必須和其他金額欄位保持一致。
    remaining_amount NUMERIC(38, 18) NOT NULL,

    -- 已消耗金額。這只表示 reservation 已被部分或完全用掉，不代表 ledger 已做最終 posting。
    consumed_amount NUMERIC(38, 18) NOT NULL DEFAULT 0,

    -- 已釋放金額。這只表示 reservation 已釋放回可用狀態，不代表任何 runtime 已完成扣款或退款。
    released_amount NUMERIC(38, 18) NOT NULL DEFAULT 0,

    -- 請求識別碼。只用於追蹤與降低重送風險，不可視為完整 idempotency 保證；完整 policy 留待後續 idempotency_keys。
    request_id VARCHAR(64),

    -- 到期時間。reservation 可能因業務規則到期釋放，但是否真正釋放仍由後續流程判斷。
    expires_at TIMESTAMP WITH TIME ZONE,

    -- 資料建立時間。由資料庫預設產生，供審計與對帳使用。
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 資料最後更新時間。狀態變更或對帳修正時由應用層維護。
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reservations_account_asset
        FOREIGN KEY (account_id, asset_symbol) REFERENCES account_assets (account_id, asset_symbol),
    CONSTRAINT ck_reservations_business_reference_type CHECK (business_reference_type IN ('ORDER', 'WITHDRAWAL', 'SETTLEMENT', 'ADJUSTMENT')),
    CONSTRAINT ck_reservations_reservation_type CHECK (reservation_type IN ('ORDER_HOLD', 'WITHDRAWAL_HOLD', 'SETTLEMENT_HOLD', 'ADMIN_HOLD')),
    CONSTRAINT ck_reservations_status CHECK (status IN ('ACTIVE', 'PARTIALLY_CONSUMED', 'CONSUMED', 'RELEASED', 'CANCELED', 'EXPIRED')),
    CONSTRAINT ck_reservations_original_amount_positive CHECK (original_amount > 0),
    CONSTRAINT ck_reservations_amounts_non_negative CHECK (
        remaining_amount >= 0 AND consumed_amount >= 0 AND released_amount >= 0
    ),
    CONSTRAINT ck_reservations_amounts_consistent CHECK (
        original_amount = remaining_amount + consumed_amount + released_amount
    )
);

CREATE INDEX idx_reservations_account_id ON reservations (account_id);
CREATE INDEX idx_reservations_asset_symbol ON reservations (asset_symbol);
CREATE INDEX idx_reservations_account_asset_status ON reservations (account_id, asset_symbol, status);
CREATE INDEX idx_reservations_business_reference ON reservations (business_reference_type, business_reference_id);
CREATE INDEX idx_reservations_request_id ON reservations (request_id);
CREATE INDEX idx_reservations_expires_at ON reservations (expires_at);

COMMENT ON TABLE reservations IS
'預留主檔。這只是一份狀態資料結構，記錄某筆資產在特定業務目的下被保留、消耗或釋放的結果，不直接代表資金已扣款。';

COMMENT ON COLUMN reservations.reservation_id IS
'預留唯一識別碼。由應用層或上游流程產生，供查詢、對帳與回放使用。';

COMMENT ON COLUMN reservations.account_id IS
'所屬帳戶。reservation 必須綁定 account，因為預留是以帳戶為邊界的資金控制資料。';

COMMENT ON COLUMN reservations.asset_symbol IS
'所屬資產。與 account_id 一起約束 reservation 只可對既有 account_assets 建立。';

COMMENT ON COLUMN reservations.business_reference_type IS
'業務參照類型。用來標識 reservation 來源，例如 ORDER、WITHDRAWAL、SETTLEMENT 或 ADJUSTMENT。';

COMMENT ON COLUMN reservations.business_reference_id IS
'業務參照識別碼。指向對應業務物件，但不代表該物件已完成最終資金動作。';

COMMENT ON COLUMN reservations.reservation_type IS
'預留類型。描述這筆 reservation 的用途，例如下單、提款、結算或人工處理。';

COMMENT ON COLUMN reservations.status IS
'預留狀態。這只描述資料上的生命週期，不等於 ledger 已經寫入或餘額已經結算。';

COMMENT ON COLUMN reservations.original_amount IS
'原始預留金額。使用 NUMERIC(38, 18) 保存，避免 binary floating point 誤差。';

COMMENT ON COLUMN reservations.remaining_amount IS
'剩餘可釋放或可再消耗的預留金額。這是查詢欄位，必須和其他金額欄位保持一致。';

COMMENT ON COLUMN reservations.consumed_amount IS
'已消耗金額。這只表示 reservation 已被部分或完全用掉，不代表 ledger 已做最終 posting。';

COMMENT ON COLUMN reservations.released_amount IS
'已釋放金額。這只表示 reservation 已釋放回可用狀態，不代表任何 runtime 已完成扣款或退款。';

COMMENT ON COLUMN reservations.request_id IS
'請求識別碼。只用於追蹤與降低重送風險，不可視為完整 idempotency 保證；完整 policy 留待後續 idempotency_keys。';

COMMENT ON COLUMN reservations.expires_at IS
'到期時間。reservation 可能因業務規則到期釋放，但是否真正釋放仍由後續流程判斷。';

COMMENT ON COLUMN reservations.created_at IS
'資料建立時間。由資料庫預設產生，供審計與對帳使用。';

COMMENT ON COLUMN reservations.updated_at IS
'資料最後更新時間。狀態變更或對帳修正時由應用層維護。';

-- P12-T08: Outbox, audit log, and idempotency schema.
-- 這份 migration 只建立高風險操作的去重、外部副作用與稽核資料結構。
-- 不實作 idempotency interceptor、event publisher、audit writer、ledger posting、deposit crediting 或 withdrawal runtime。
-- 這些表只提供 durable storage，實際重試、發佈與審計流程要由後續應用層與 runtime 實作。
-- Rollback 注意事項：若要回滾，先確認沒有後續流程依賴這些表；僅能刪除本 migration 建立的表，不得回寫既有資料。

-- Idempotency key 主檔。
-- 這不是完整的 runtime idempotency 保證，而是讓高風險操作能以 scope + key 做去重與對帳。
CREATE TABLE idempotency_keys (
    -- Idempotency key 唯一識別碼。由資料庫產生，作為查詢與稽核主鍵。
    idempotency_key_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,

    -- Idempotency 範圍。用來區分 ORDER_CREATE、WITHDRAWAL_REQUEST、LEDGER_POSTING、DEPOSIT_CONFIRMATION 等高風險操作。
    scope VARCHAR(32) NOT NULL,

    -- Idempotency key 值。與 scope 組合後才具有唯一意義。
    idempotency_key VARCHAR(128) NOT NULL,

    -- 請求識別碼。只用於追蹤與關聯，不可被誤解成單欄位完成完整 idempotency 保證。
    request_id VARCHAR(64),

    -- Idempotency 狀態。只描述處理流程狀態，不代表 runtime 已完成副作用。
    status VARCHAR(16) NOT NULL,

    -- 關聯的資源類型。用來記錄這筆去重鍵最後綁定到哪一類業務資源。
    resource_type VARCHAR(32),

    -- 關聯的資源識別碼。用來回查這筆去重鍵最終對應的業務物件。
    resource_id VARCHAR(128),

    -- 可選的 response code。只做查詢輔助，避免重送時需要重算整個流程。
    response_code INTEGER,

    -- 回應摘要。以文字形式保存，不在這裡定義 API schema，也不承擔 runtime response contract。
    response_summary TEXT,

    -- 到期時間。過期的 key 可重新清理，但清理策略仍由後續維運流程決定。
    expires_at TIMESTAMP WITH TIME ZONE,

    -- 資料建立時間。由資料庫預設產生，供審計與去重使用。
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 資料最後更新時間。狀態改變或 resource 綁定完成時由應用層維護。
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_idempotency_keys_scope_key UNIQUE (scope, idempotency_key),
    CONSTRAINT ck_idempotency_keys_scope CHECK (scope IN ('ORDER_CREATE', 'ORDER_CANCEL', 'WITHDRAWAL_REQUEST', 'WITHDRAWAL_CANCEL', 'LEDGER_POSTING', 'DEPOSIT_CONFIRMATION', 'RESERVATION_HOLD', 'RESERVATION_RELEASE', 'RESERVATION_CAPTURE', 'INTERNAL_TRANSFER', 'PLATFORM_INTERNAL_TRANSFER', 'ADMIN_ACTION')),
    CONSTRAINT ck_idempotency_keys_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'EXPIRED')),
    CONSTRAINT ck_idempotency_keys_response_code CHECK (response_code IS NULL OR response_code BETWEEN 100 AND 599)
);

CREATE INDEX idx_idempotency_keys_scope_status ON idempotency_keys (scope, status);
CREATE INDEX idx_idempotency_keys_request_id ON idempotency_keys (request_id);
CREATE INDEX idx_idempotency_keys_expires_at ON idempotency_keys (expires_at);
CREATE INDEX idx_idempotency_keys_resource_reference ON idempotency_keys (resource_type, resource_id);

COMMENT ON TABLE idempotency_keys IS
'Idempotency key 主檔。這不是完整的 runtime idempotency 保證，而是讓高風險操作能以 scope + key 做去重與對帳。';

COMMENT ON COLUMN idempotency_keys.idempotency_key_id IS
'Idempotency key 唯一識別碼。由資料庫產生，作為查詢與稽核主鍵。';

COMMENT ON COLUMN idempotency_keys.scope IS
'Idempotency 範圍。用來區分 ORDER_CREATE、WITHDRAWAL_REQUEST、LEDGER_POSTING、DEPOSIT_CONFIRMATION 等高風險操作。';

COMMENT ON COLUMN idempotency_keys.idempotency_key IS
'Idempotency key 值。與 scope 組合後才具有唯一意義。';

COMMENT ON COLUMN idempotency_keys.request_id IS
'請求識別碼。只用於追蹤與關聯，不可被誤解成單欄位完成完整 idempotency 保證。';

COMMENT ON COLUMN idempotency_keys.status IS
'Idempotency 狀態。只描述處理流程狀態，不代表 runtime 已完成副作用。';

COMMENT ON COLUMN idempotency_keys.resource_type IS
'關聯的資源類型。用來記錄這筆去重鍵最後綁定到哪一類業務資源。';

COMMENT ON COLUMN idempotency_keys.resource_id IS
'關聯的資源識別碼。用來回查這筆去重鍵最終對應的業務物件。';

COMMENT ON COLUMN idempotency_keys.response_code IS
'可選的 response code。只做查詢輔助，避免重送時需要重算整個流程。';

COMMENT ON COLUMN idempotency_keys.response_summary IS
'回應摘要。以文字形式保存，不在這裡定義 API schema，也不承擔 runtime response contract。';

COMMENT ON COLUMN idempotency_keys.expires_at IS
'到期時間。過期的 key 可重新清理，但清理策略仍由後續維運流程決定。';

COMMENT ON COLUMN idempotency_keys.created_at IS
'資料建立時間。由資料庫預設產生，供審計與去重使用。';

COMMENT ON COLUMN idempotency_keys.updated_at IS
'資料最後更新時間。狀態改變或 resource 綁定完成時由應用層維護。';

-- Outbox 事件主檔。
-- 這只保存 transactional outbox 所需的持久化事件，不會在 migration 內發送事件。
CREATE TABLE outbox_events (
    -- Outbox 事件識別碼。由資料庫產生，供排程與重試查詢使用。
    outbox_event_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,

    -- 聚合類型。用來指向事件來源，例如 ORDER、WITHDRAWAL、DEPOSIT、RESERVATION、LEDGER_JOURNAL。
    aggregate_type VARCHAR(32) NOT NULL,

    -- 聚合識別碼。用來定位產生事件的業務物件。
    aggregate_id VARCHAR(128) NOT NULL,

    -- 事件類型。描述此次 outbox 事件的語意，例如 ORDER_CREATED 或 WITHDRAWAL_APPROVED。
    event_type VARCHAR(64) NOT NULL,

    -- 事件 payload。以文字保存序列化內容，避免 JSON 型別在 H2 / PostgreSQL 之間產生相容性問題。
    payload TEXT NOT NULL,

    -- 事件狀態。只描述 outbox 的配送流程，不代表已被外部系統消費。
    status VARCHAR(16) NOT NULL,

    -- 重試次數。用來支援失敗重送與 dead-letter 條件。
    retry_count INTEGER NOT NULL DEFAULT 0,

    -- 下次可用時間。讓 publisher 可以延後重送，而不是立刻 busy loop。
    available_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 請求識別碼。只做來源關聯，不承諾完整去重。
    request_id VARCHAR(64),

    -- 成功發佈時間。只有在外部發佈完成後才應填值。
    published_at TIMESTAMP WITH TIME ZONE,

    -- 最近一次錯誤摘要。僅供排障，不能拿來當成外部契約。
    last_error TEXT,

    -- 資料建立時間。由資料庫預設產生，供重試與審計使用。
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 資料最後更新時間。狀態變更、重試或發佈完成時由應用層維護。
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_outbox_events_status CHECK (status IN ('READY', 'PROCESSING', 'PUBLISHED', 'FAILED', 'DEAD_LETTER')),
    CONSTRAINT ck_outbox_events_retry_count CHECK (retry_count >= 0)
);

CREATE INDEX idx_outbox_events_status_available_at ON outbox_events (status, available_at);
CREATE INDEX idx_outbox_events_aggregate_reference ON outbox_events (aggregate_type, aggregate_id);
CREATE INDEX idx_outbox_events_event_type ON outbox_events (event_type);
CREATE INDEX idx_outbox_events_request_id ON outbox_events (request_id);
CREATE INDEX idx_outbox_events_created_at ON outbox_events (created_at);

COMMENT ON TABLE outbox_events IS
'Outbox 事件主檔。這只保存 transactional outbox 所需的持久化事件，不會在 migration 內發送事件。';

COMMENT ON COLUMN outbox_events.outbox_event_id IS
'Outbox 事件識別碼。由資料庫產生，供排程與重試查詢使用。';

COMMENT ON COLUMN outbox_events.aggregate_type IS
'聚合類型。用來指向事件來源，例如 ORDER、WITHDRAWAL、DEPOSIT、RESERVATION、LEDGER_JOURNAL。';

COMMENT ON COLUMN outbox_events.aggregate_id IS
'聚合識別碼。用來定位產生事件的業務物件。';

COMMENT ON COLUMN outbox_events.event_type IS
'事件類型。描述此次 outbox 事件的語意，例如 ORDER_CREATED 或 WITHDRAWAL_APPROVED。';

COMMENT ON COLUMN outbox_events.payload IS
'事件 payload。以文字保存序列化內容，避免 JSON 型別在 H2 / PostgreSQL 之間產生相容性問題。';

COMMENT ON COLUMN outbox_events.status IS
'事件狀態。只描述 outbox 的配送流程，不代表已被外部系統消費。';

COMMENT ON COLUMN outbox_events.retry_count IS
'重試次數。用來支援失敗重送與 dead-letter 條件。';

COMMENT ON COLUMN outbox_events.available_at IS
'下次可用時間。讓 publisher 可以延後重送，而不是立刻 busy loop。';

COMMENT ON COLUMN outbox_events.request_id IS
'請求識別碼。只做來源關聯，不承諾完整去重。';

COMMENT ON COLUMN outbox_events.published_at IS
'成功發佈時間。只有在外部發佈完成後才應填值。';

COMMENT ON COLUMN outbox_events.last_error IS
'最近一次錯誤摘要。僅供排障，不能拿來當成外部契約。';

COMMENT ON COLUMN outbox_events.created_at IS
'資料建立時間。由資料庫預設產生，供重試與審計使用。';

COMMENT ON COLUMN outbox_events.updated_at IS
'資料最後更新時間。狀態變更、重試或發佈完成時由應用層維護。';

-- 稽核紀錄主檔。
-- 這只保存高風險操作的可追溯紀錄，不包含任何 runtime writer 或 admin workflow。
CREATE TABLE audit_logs (
    -- 稽核紀錄識別碼。由資料庫產生，供查詢與對帳使用。
    audit_log_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,

    -- 行為者類型。描述誰觸發了這次高風險操作，例如 USER、ADMIN、SYSTEM、SERVICE。
    actor_type VARCHAR(16) NOT NULL,

    -- 行為者識別碼。用來定位實際操作者或服務主體。
    actor_id VARCHAR(64) NOT NULL,

    -- 動作類型。描述被審計的行為，例如 ORDER_CREATE、WITHDRAWAL_REQUEST、LEDGER_POSTING、ADMIN_ACTION。
    action_type VARCHAR(64) NOT NULL,

    -- 目標類型。描述被影響的資源類別，例如 ORDER、WITHDRAWAL、DEPOSIT、RESERVATION、LEDGER_JOURNAL。
    target_type VARCHAR(32) NOT NULL,

    -- 目標識別碼。描述被影響的具體資源。
    target_id VARCHAR(128) NOT NULL,

    -- 請求識別碼。用來串起原始請求與稽核紀錄，不代表完整 idempotency 保證。
    request_id VARCHAR(64) NOT NULL,

    -- 關聯識別碼。可用於跨 service 追蹤同一條鏈路。
    correlation_id VARCHAR(64),

    -- 操作結果。記錄這次高風險操作是成功、失敗還是被拒絕。
    outcome VARCHAR(16) NOT NULL,

    -- 原因說明。只有在需要人工回顧或失敗排障時才會填入。
    reason TEXT,

    -- 變更前狀態快照。以文字保存，供後續審計與事故分析。
    before_state TEXT,

    -- 變更後狀態快照。以文字保存，供後續審計與事故分析。
    after_state TEXT,

    -- 資料建立時間。這是稽核紀錄的時間錨點，之後不應更新。
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_audit_logs_actor_type CHECK (actor_type IN ('USER', 'ADMIN', 'SYSTEM', 'SERVICE')),
    CONSTRAINT ck_audit_logs_outcome CHECK (outcome IN ('SUCCESS', 'FAILURE', 'REJECTED'))
);

CREATE INDEX idx_audit_logs_actor ON audit_logs (actor_type, actor_id);
CREATE INDEX idx_audit_logs_action_type ON audit_logs (action_type);
CREATE INDEX idx_audit_logs_target ON audit_logs (target_type, target_id);
CREATE INDEX idx_audit_logs_request_id ON audit_logs (request_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);

COMMENT ON TABLE audit_logs IS
'稽核紀錄主檔。這只保存高風險操作的可追溯紀錄，不包含任何 runtime writer 或 admin workflow。';

COMMENT ON COLUMN audit_logs.audit_log_id IS
'稽核紀錄識別碼。由資料庫產生，供查詢與對帳使用。';

COMMENT ON COLUMN audit_logs.actor_type IS
'行為者類型。描述誰觸發了這次高風險操作，例如 USER、ADMIN、SYSTEM、SERVICE。';

COMMENT ON COLUMN audit_logs.actor_id IS
'行為者識別碼。用來定位實際操作者或服務主體。';

COMMENT ON COLUMN audit_logs.action_type IS
'動作類型。描述被審計的行為，例如 ORDER_CREATE、WITHDRAWAL_REQUEST、LEDGER_POSTING、ADMIN_ACTION。';

COMMENT ON COLUMN audit_logs.target_type IS
'目標類型。描述被影響的資源類別，例如 ORDER、WITHDRAWAL、DEPOSIT、RESERVATION、LEDGER_JOURNAL。';

COMMENT ON COLUMN audit_logs.target_id IS
'目標識別碼。描述被影響的具體資源。';

COMMENT ON COLUMN audit_logs.request_id IS
'請求識別碼。用來串起原始請求與稽核紀錄，不代表完整 idempotency 保證。';

COMMENT ON COLUMN audit_logs.correlation_id IS
'關聯識別碼。可用於跨 service 追蹤同一條鏈路。';

COMMENT ON COLUMN audit_logs.outcome IS
'操作結果。記錄這次高風險操作是成功、失敗還是被拒絕。';

COMMENT ON COLUMN audit_logs.reason IS
'原因說明。只有在需要人工回顧或失敗排障時才會填入。';

COMMENT ON COLUMN audit_logs.before_state IS
'變更前狀態快照。以文字保存，供後續審計與事故分析。';

COMMENT ON COLUMN audit_logs.after_state IS
'變更後狀態快照。以文字保存，供後續審計與事故分析。';

COMMENT ON COLUMN audit_logs.created_at IS
'資料建立時間。這是稽核紀錄的時間錨點，之後不應更新。';

-- P29-R02: 使用者帳密、伺服器端 session 與密碼重設憑證。
-- 本 migration 僅保存認證資料；不接觸帳本、餘額、交易或任何資金移動。
-- 密碼與重設 token 只可保存不可逆雜湊，明文不得寫入資料庫、log 或 audit payload。

CREATE TABLE user_credentials (
    user_id VARCHAR(64) PRIMARY KEY,
    password_hash VARCHAR(100) NOT NULL,
    password_algorithm VARCHAR(32) NOT NULL,
    password_changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_credentials_user_id
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_user_credentials_password_algorithm
        CHECK (password_algorithm = 'BCRYPT')
);

CREATE TABLE user_sessions (
    session_id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    token_digest CHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    device_id UUID,
    ip_address VARCHAR(64),
    device_label VARCHAR(256),

    CONSTRAINT fk_user_sessions_user_id
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT uq_user_sessions_token_digest UNIQUE (token_digest),
    CONSTRAINT ck_user_sessions_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_user_sessions_active_lookup
    ON user_sessions (session_id, token_digest, expires_at)
    WHERE revoked_at IS NULL;

CREATE INDEX idx_user_sessions_user_id ON user_sessions (user_id);

CREATE TABLE password_reset_requests (
    reset_request_id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    token_digest CHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_password_reset_requests_user_id
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT uq_password_reset_requests_token_digest UNIQUE (token_digest),
    CONSTRAINT ck_password_reset_requests_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_password_reset_requests_active_lookup
    ON password_reset_requests (token_digest, expires_at)
    WHERE consumed_at IS NULL;

COMMENT ON TABLE user_credentials IS
'使用者認證資料。只保存 BCrypt 密碼雜湊與演算法資訊，禁止保存明文密碼。';
COMMENT ON COLUMN user_credentials.user_id IS
'所屬使用者，與 users.user_id 一對一。';
COMMENT ON COLUMN user_credentials.password_hash IS
'BCrypt 不可逆密碼雜湊；不得寫入明文、可逆加密結果或 log。';
COMMENT ON COLUMN user_credentials.password_algorithm IS
'雜湊演算法版本標示；目前僅允許 BCRYPT。';
COMMENT ON COLUMN user_credentials.password_changed_at IS
'最後一次成功變更密碼的時間，用於安全稽核與 session 失效判斷。';

COMMENT ON TABLE user_sessions IS
'伺服器端登入 session。Cookie 僅保存 session id 與高熵秘密值；資料庫只保存秘密值摘要。';
COMMENT ON COLUMN user_sessions.session_id IS
'非秘密的 session 識別碼，用於定位伺服器端 session。';
COMMENT ON COLUMN user_sessions.token_digest IS
'Cookie 高熵秘密值的 SHA-256 摘要；原始秘密值不落庫。';
COMMENT ON COLUMN user_sessions.expires_at IS
'session 絕對到期時間，逾期後不得再被認證。';
COMMENT ON COLUMN user_sessions.revoked_at IS
'撤銷時間；非空代表該 session 已永久失效。';

COMMENT ON TABLE password_reset_requests IS
'密碼重設一次性憑證。原始 token 只會出現在受控寄信內容，不得保存或回傳 API。';
COMMENT ON COLUMN password_reset_requests.token_digest IS
'一次性重設 token 的 SHA-256 摘要；原始 token 不得落庫。';
COMMENT ON COLUMN password_reset_requests.consumed_at IS
'成功重設密碼後的消耗時間；非空的 token 不得再次使用。';

-- P29-R08: 受信任裝置、新裝置登入確認與成功登入的裝置快照。
-- 此 migration 只強化認證邊界；不接觸帳本、資產、交易或任何資金移動。
-- 所有 cookie／email token 均只保存 SHA-256 摘要，原始秘密不可落庫、寫入 log 或回傳 API。

CREATE TABLE user_login_devices (
    device_id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    token_digest CHAR(64) NOT NULL,
    user_agent_digest CHAR(64) NOT NULL,
    device_label VARCHAR(256) NOT NULL,
    device_platform VARCHAR(16) NOT NULL,
    last_ip_address VARCHAR(64) NOT NULL,
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_user_login_devices_user_id
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT uq_user_login_devices_token_digest UNIQUE (token_digest),
    CONSTRAINT ck_user_login_devices_platform CHECK (device_platform IN ('DESKTOP', 'TABLET', 'MOBILE'))
);

CREATE INDEX idx_user_login_devices_active_lookup
    ON user_login_devices (user_id, device_id, token_digest)
    WHERE revoked_at IS NULL;

CREATE TABLE login_verification_requests (
    verification_request_id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    pending_token_digest CHAR(64) NOT NULL,
    approval_token_digest CHAR(64) NOT NULL,
    candidate_device_id UUID NOT NULL,
    candidate_device_token_digest CHAR(64) NOT NULL,
    user_agent_digest CHAR(64) NOT NULL,
    device_label VARCHAR(256) NOT NULL,
    ip_address VARCHAR(64) NOT NULL,
    state VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    decided_at TIMESTAMP WITH TIME ZONE,
    consumed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    device_platform VARCHAR(16) NOT NULL,

    CONSTRAINT fk_login_verification_requests_user_id
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT uq_login_verification_requests_pending_digest UNIQUE (pending_token_digest),
    CONSTRAINT uq_login_verification_requests_approval_digest UNIQUE (approval_token_digest),
    CONSTRAINT ck_login_verification_requests_state
        CHECK (state IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_login_verification_requests_expiry CHECK (expires_at > created_at),
    CONSTRAINT ck_login_verification_requests_platform
        CHECK (device_platform IN ('DESKTOP', 'TABLET', 'MOBILE'))
);

CREATE INDEX idx_login_verification_requests_pending_lookup
    ON login_verification_requests (verification_request_id, pending_token_digest, expires_at)
    WHERE consumed_at IS NULL;

CREATE INDEX idx_login_verification_requests_approval_lookup
    ON login_verification_requests (approval_token_digest, expires_at)
    WHERE consumed_at IS NULL;

COMMENT ON TABLE user_login_devices IS
'已由 email 明確確認的登入裝置。Cookie 原始值只在 HttpOnly browser cookie，資料庫只保存不可逆摘要。';
COMMENT ON COLUMN user_login_devices.token_digest IS
'受信任裝置 Cookie 的高熵秘密摘要；不得保存原始 cookie 值。';
COMMENT ON COLUMN user_login_devices.user_agent_digest IS
'登入時 User-Agent 的 SHA-256 摘要；變更時須重新進行 email 裝置確認。';
COMMENT ON COLUMN user_login_devices.last_ip_address IS
'最後一次以此受信任裝置登入時由可信反向代理轉交的用戶端 IP。';

COMMENT ON TABLE login_verification_requests IS
'新裝置登入的一次性 email 確認請求。核准只允許原始登入瀏覽器以 pending cookie 交換 session。';
COMMENT ON COLUMN login_verification_requests.pending_token_digest IS
'原始登入瀏覽器 HttpOnly pending cookie 的秘密摘要；只可完成同一次登入。';
COMMENT ON COLUMN login_verification_requests.approval_token_digest IS
'email 確認頁使用的一次性 token 摘要；原始 token 不可寫入 log、DB 或 API response。';
COMMENT ON COLUMN login_verification_requests.candidate_device_token_digest IS
'核准後建立受信任裝置時使用的 cookie 秘密摘要；核准前不可視為可信裝置。';
COMMENT ON COLUMN login_verification_requests.state IS
'PENDING 等待使用者決定；APPROVED 只允許原始登入瀏覽器完成一次；REJECTED 永不建立 session。';
COMMENT ON COLUMN login_verification_requests.consumed_at IS
'成功以核准請求建立 session 後的消耗時間；非空表示不得再完成登入。';

COMMENT ON COLUMN user_sessions.device_id IS
'建立此成功 session 的受信任裝置識別；歷史 session 可為空，不能藉 migration 偽造裝置資料。';
COMMENT ON COLUMN user_sessions.ip_address IS
'成功登入時由應用程式取得的用戶端 IP 快照，僅供本人登入紀錄與安全通知使用。';
COMMENT ON COLUMN user_sessions.device_label IS
'成功登入時的去敏裝置標籤快照，避免在登入歷程保存原始 User-Agent。';

-- P29-R11: 註冊必須完成 email 雙驗證碼後才建立 users / credential / session。
-- 原始數字碼與五碼英文字母碼絕不可落庫；只保存 SHA-256 摘要。密碼只以 BCrypt hash 暫存，驗證過期或
-- 錯誤次數用盡時不會建立帳號。本 migration 不涉及帳本、資產、交易或任何資金移動。

CREATE TABLE registration_verification_requests (
    registration_id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    numeric_code_digest CHAR(64) NOT NULL,
    letter_code_digest CHAR(64) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_registration_verification_requests_email UNIQUE (email),
    CONSTRAINT ck_registration_verification_requests_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT ck_registration_verification_requests_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_registration_verification_requests_active_lookup
    ON registration_verification_requests (registration_id, expires_at)
    WHERE consumed_at IS NULL;

COMMENT ON TABLE registration_verification_requests IS
'尚未完成 email 雙驗證碼的註冊申請。驗證成功前不得建立 users、user_credentials、受信任裝置或 session。';
COMMENT ON COLUMN registration_verification_requests.password_hash IS
'註冊時已計算的 BCrypt 密碼雜湊；原始密碼不得在此表、log 或 API response 出現。';
COMMENT ON COLUMN registration_verification_requests.numeric_code_digest IS
'六位數 email 驗證碼的 SHA-256 摘要；原始碼只存在於寄送中的信件。';
COMMENT ON COLUMN registration_verification_requests.letter_code_digest IS
'五位英文字母 email 驗證碼的 SHA-256 摘要；原始碼只存在於寄送中的信件。';
COMMENT ON COLUMN registration_verification_requests.attempt_count IS
'已驗證失敗次數；達到 application 設定上限時會標記 consumed，使用者必須重新發起註冊。';

-- P27 管理者啟用基礎：最高管理員是獨立 principal，不可用前端 localStorage 或環境信箱白名單取代。
-- 本 migration 只建立單一最高管理員的身分與啟用狀態；不提供角色指派、後台寫入命令、資金或帳本操作。

CREATE TABLE admin_principals (
    -- 主體沿用 users 的受驗證身分，避免另建一套密碼或 session 儲存機制。
    user_id VARCHAR(64) PRIMARY KEY,

    -- 目前只允許唯一且不可由 runtime 任意變更的最高管理員角色。
    role VARCHAR(32) NOT NULL,

    -- PENDING_ACTIVATION 在 email 設定密碼前沒有任何後台權限；ACTIVE 才能通過 server-side 授權。
    status VARCHAR(32) NOT NULL,

    -- 只記錄啟用流程時間，不保存 email token、明文密碼或可逆祕密。
    activation_requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_admin_principals_user_id
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT uq_admin_principals_role UNIQUE (role),
    CONSTRAINT ck_admin_principals_role CHECK (role = 'SUPER_ADMIN'),
    CONSTRAINT ck_admin_principals_status CHECK (status IN ('PENDING_ACTIVATION', 'ACTIVE')),
    CONSTRAINT ck_admin_principals_activation_time
        CHECK ((status = 'PENDING_ACTIVATION' AND activated_at IS NULL)
            OR (status = 'ACTIVE' AND activated_at IS NOT NULL))
);

CREATE INDEX idx_admin_principals_active_user
    ON admin_principals (user_id)
    WHERE status = 'ACTIVE';

COMMENT ON TABLE admin_principals IS
'管理後台 principal。僅保存最高管理員啟用狀態；不得用於一般角色授與或任何資金操作。';
COMMENT ON COLUMN admin_principals.user_id IS
'最高管理員對應的既有 users 身分；密碼仍只存於 user_credentials 的 BCrypt 雜湊。';
COMMENT ON COLUMN admin_principals.role IS
'角色固定為 SUPER_ADMIN，unique 約束阻止啟動設定靜默新增或取代第二位最高管理員。';
COMMENT ON COLUMN admin_principals.status IS
'PENDING_ACTIVATION 不具後台權限；只有成功使用 email 一次性連結設定密碼後才會成為 ACTIVE。';
COMMENT ON COLUMN admin_principals.activation_requested_at IS
'最近一次啟用信成功排程的時間；不保存信件內的一次性 token。';
COMMENT ON COLUMN admin_principals.activated_at IS
'首次成功設定最高管理員密碼並啟用 server-side 授權的時間。';

-- P12-T07: Normalize wallet lifecycle schema without changing the V005 table definitions.
-- 這份 migration 只補強查詢索引，讓 support、reconciliation 與營運查詢能更穩定地查到 wallet lifecycle 資料。
-- 不修改 V005 的欄位、約束或 comment，不新增 runtime service，也不改變任何入金／提款流程語意。
-- Rollback 注意事項：若要回滾，僅移除本 migration 新增的索引即可；不得回頭改寫 V005。

-- 入金地址與入金紀錄通常會被 support 以 address 直接查詢，因此補 address 層級索引。
CREATE INDEX idx_deposit_addresses_address ON deposit_addresses (address);
CREATE INDEX idx_deposits_chain_type_address ON deposits (chain_type, address);

-- 提現紀錄也需要以外部地址與鏈別做查詢，避免 support 與對帳只能依賴 request_id 或 account_id。
CREATE INDEX idx_withdrawals_chain_type_address ON withdrawals (chain_type, address);

-- 鏈上交易通常以 tx_hash 直接回查，因此補單欄位索引，降低 support / reconciliation 的查詢成本。
CREATE INDEX idx_chain_transactions_tx_hash ON chain_transactions (tx_hash);

-- P29-R04: 使用者登入紀錄只讀 API 的查詢索引。
-- user_sessions 已是成功登入 session 的權威來源；本 migration 不新增認證材料、不記錄 IP/user agent，
-- 也不修改或刪除既有 session，以避免把安全歷程與現有 session 驗證語意混在一起。

CREATE INDEX idx_user_sessions_login_history
    ON user_sessions (user_id, created_at DESC);

COMMENT ON INDEX idx_user_sessions_login_history IS
'依使用者倒序讀取成功登入 session 的 bounded login-history API 索引；不包含 token digest 或其他秘密欄位。';

-- P27 使用者唯讀檢視的索引。
-- 此 migration 僅改善最高管理員的 bounded read query；不建立帳戶狀態、角色、資產或帳本的任何寫入能力。
-- 名稱索引專供 lower(display_name) 的前綴 LIKE 使用，查詢端不得改成 '%keyword%'，否則索引無法縮小候選資料。

CREATE INDEX idx_users_admin_created_cursor
    ON users (created_at DESC, user_id DESC);

CREATE INDEX idx_users_admin_display_name_prefix
    ON users (lower(display_name) text_pattern_ops);

COMMENT ON INDEX idx_users_admin_created_cursor IS
'管理端使用者清單依註冊時間與 user_id 倒序翻頁的 keyset 索引；不包含任何認證秘密。';

COMMENT ON INDEX idx_users_admin_display_name_prefix IS
'管理端不分大小寫顯示名稱前綴搜尋索引，僅支援 keyword% 而非 %keyword%。';

CREATE UNIQUE INDEX uq_user_login_devices_active_platform ON user_login_devices (user_id, device_platform) WHERE revoked_at IS NULL;
CREATE INDEX idx_user_sessions_login_history_device ON user_sessions (user_id, created_at DESC);


-- 最終 deterministic system accounting principals；purpose 與 account identity 在首次建立時即一致。
INSERT INTO users (user_id, email, display_name, status) VALUES
 ('system:airdrop', 'system-airdrop@lumix.internal', '系統空投帳戶', 'SUSPENDED'),
 ('system:asset-adjustment', 'system-asset-adjustment@lumix.internal', '系統資產調整對應帳戶', 'SUSPENDED');
INSERT INTO accounts (account_id, user_id, account_type, status, account_category, account_purpose) VALUES
 ('system:airdrop:spot', 'system:airdrop', 'SPOT', 'ACTIVE', 'EXCHANGE', 'AIRDROP_FUNDING'),
 ('system:asset-adjustment:spot', 'system:asset-adjustment', 'SPOT', 'ACTIVE', 'EXCHANGE', 'ASSET_ADJUSTMENT_COUNTERPARTY');

CREATE TABLE admin_asset_adjustments (
    adjustment_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    adjustment_type VARCHAR(32) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    user_id VARCHAR(64) NOT NULL REFERENCES users(user_id),
    account_id VARCHAR(64) NOT NULL REFERENCES accounts(account_id),
    account_type VARCHAR(16) NOT NULL,
    asset_symbol VARCHAR(32) NOT NULL REFERENCES assets(asset_symbol),
    amount NUMERIC(36,18) NOT NULL,
    source_business_type VARCHAR(32), source_business_id VARCHAR(128),
    source_journal_id BIGINT REFERENCES ledger_journals(ledger_journal_id),
    source_ledger_entry_id BIGINT REFERENCES ledger_entries(ledger_entry_id),
    incident_reference VARCHAR(128),
    ledger_journal_id BIGINT NOT NULL UNIQUE REFERENCES ledger_journals(ledger_journal_id),
    actor_id VARCHAR(64) NOT NULL REFERENCES users(user_id),
    reason VARCHAR(256) NOT NULL, status VARCHAR(16) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_admin_asset_adjustments_type CHECK (adjustment_type IN ('MANUAL_CORRECTION','COMPENSATION','BUSINESS_REVERSAL')),
    CONSTRAINT ck_admin_asset_adjustments_direction CHECK (direction IN ('CREDIT','DEBIT')),
    CONSTRAINT ck_admin_asset_adjustments_amount CHECK (amount > 0),
    CONSTRAINT ck_admin_asset_adjustments_status CHECK (status = 'COMPLETED'),
    CONSTRAINT ck_admin_asset_adjustments_business_reference CHECK ((source_business_type IS NULL) = (source_business_id IS NULL)),
    CONSTRAINT ck_admin_asset_adjustments_reversal_source CHECK (adjustment_type <> 'BUSINESS_REVERSAL' OR source_ledger_entry_id IS NOT NULL)
);
CREATE INDEX idx_admin_asset_adjustments_user_created ON admin_asset_adjustments (user_id, created_at DESC);
CREATE INDEX idx_admin_asset_adjustments_source_entry ON admin_asset_adjustments (source_ledger_entry_id);
CREATE INDEX idx_admin_asset_adjustments_business ON admin_asset_adjustments (source_business_type, source_business_id);
CREATE INDEX idx_admin_asset_adjustments_journal ON admin_asset_adjustments (ledger_journal_id);
COMMENT ON TABLE admin_asset_adjustments IS '受權 admin 的 completed asset adjustment command；資金效果只能在 immutable ledger entries 中取得。';
