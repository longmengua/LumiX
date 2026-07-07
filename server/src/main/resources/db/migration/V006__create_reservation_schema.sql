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
