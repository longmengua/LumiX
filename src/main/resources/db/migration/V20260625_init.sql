-- ============================================================================
-- LumiX single Flyway baseline
-- File: V20260625_init.sql
-- Purpose: one clean init SQL for local/dev baseline, no step-by-step ALTER migration.
-- Note: this file merges the old V1..V31 structure into final CREATE TABLE statements.
-- ============================================================================

SET NAMES utf8mb4;

-- ============================================================================
-- 1. Outbox / DLQ / Idempotency
-- ============================================================================

CREATE TABLE IF NOT EXISTS outbox_events (
    id VARCHAR(36) NOT NULL COMMENT '事件ID，通常是UUID。',
    topic VARCHAR(128) NOT NULL COMMENT '要送出的Kafka topic或內部事件通道。',
    event_key VARCHAR(256) NOT NULL COMMENT '事件key，用來確保同一類資料可以進到同一個分區。',
    event_type VARCHAR(256) NOT NULL COMMENT '事件類型，例如ORDER_CREATED、TRADE_FILLED。',
    payload JSON NOT NULL COMMENT '事件內容，存完整業務資料。',
    headers JSON NULL COMMENT '事件header，放traceId、requestId等額外資訊。',
    status VARCHAR(32) NOT NULL COMMENT '發送狀態，例如PENDING、SENT、FAILED。',
    attempts INT NOT NULL DEFAULT 0 COMMENT '已重試次數。',
    last_error TEXT NULL COMMENT '最後一次失敗原因。',
    created_at TIMESTAMP(6) NOT NULL COMMENT '事件建立時間。',
    next_attempt_at TIMESTAMP(6) NULL COMMENT '下次可以重試的時間。',
    published_at TIMESTAMP(6) NULL COMMENT '成功發送時間。',
    PRIMARY KEY (id),
    KEY idx_outbox_due (status, next_attempt_at),
    KEY idx_outbox_topic_key (topic, event_key),
    KEY idx_outbox_events_type_created (event_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Outbox事件表，先落DB再非同步送出，避免交易完成但事件丟失。';

CREATE TABLE IF NOT EXISTS dlq_events (
    id VARCHAR(36) NOT NULL COMMENT '死信事件ID，通常是UUID。',
    outbox_id VARCHAR(36) NULL COMMENT '來源outbox_events.id，可能為空。',
    topic VARCHAR(128) NOT NULL COMMENT '原本要送出的topic。',
    event_key VARCHAR(256) NOT NULL COMMENT '原本事件key。',
    event_type VARCHAR(256) NOT NULL COMMENT '原本事件類型。',
    payload JSON NOT NULL COMMENT '原本事件內容。',
    attempts INT NOT NULL DEFAULT 0 COMMENT '進入死信前已重試次數。',
    error TEXT NULL COMMENT '最後錯誤內容。',
    created_at TIMESTAMP(6) NOT NULL COMMENT '進入死信時間。',
    PRIMARY KEY (id),
    KEY idx_dlq_created_at (created_at),
    KEY idx_dlq_topic_key (topic, event_key),
    KEY idx_dlq_events_type_created (event_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='死信事件表，放重試多次仍處理失敗的事件。';

CREATE TABLE IF NOT EXISTS idempotency_keys (
    idem_key VARCHAR(512) NOT NULL COMMENT '冪等key，同一個key只允許處理一次。',
    expires_at TIMESTAMP(6) NULL COMMENT '冪等key過期時間，過期後可以清理。',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '建立時間。',
    PRIMARY KEY (idem_key),
    KEY idx_idempotency_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='冪等控制表，避免同一請求被重複執行。';

CREATE TABLE IF NOT EXISTS snapshots (
    uid BIGINT NOT NULL COMMENT '使用者ID。',
    last_event_seq BIGINT NOT NULL COMMENT '快照已包含到哪一筆事件序號。',
    aggregates JSON NOT NULL COMMENT '聚合後的帳戶或狀態資料。',
    created_at TIMESTAMP(6) NOT NULL COMMENT '快照建立時間。',
    PRIMARY KEY (uid, last_event_seq),
    KEY idx_snapshots_uid_created (uid, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件快照表，用來加速事件重放。';

-- ============================================================================
-- 2. Order lifecycle
-- ============================================================================

CREATE TABLE IF NOT EXISTS order_lifecycle_events (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID。',
    schema_version INT NOT NULL COMMENT '資料格式版本。',
    order_id VARCHAR(36) NOT NULL COMMENT '訂單ID。',
    uid BIGINT NOT NULL COMMENT '使用者ID。',
    symbol VARCHAR(64) NOT NULL COMMENT '交易對，例如BTCUSDT-SPOT或BTCUSDT-PERP。',
    client_order_id VARCHAR(128) NULL COMMENT '客戶端自訂訂單ID。',
    stage VARCHAR(32) NOT NULL COMMENT '訂單目前階段，例如ACCEPTED、MATCHED、CANCELED。',
    status VARCHAR(32) NOT NULL COMMENT '訂單狀態，例如NEW、PARTIAL_FILLED、FILLED。',
    reason_code VARCHAR(128) NULL COMMENT '狀態原因，例如餘額不足、價格不合法。',
    price DECIMAL(38, 18) NULL COMMENT '委託價格，市價單可為空。',
    orig_qty DECIMAL(38, 18) NULL COMMENT '原始下單數量。',
    remaining_qty DECIMAL(38, 18) NULL COMMENT '剩餘未成交數量。',
    executed_qty DECIMAL(38, 18) NULL COMMENT '已成交數量。',
    avg_price DECIMAL(38, 18) NULL COMMENT '成交均價。',
    strategy_id VARCHAR(128) NULL COMMENT '策略ID，做市或策略單可用。',
    market_maker_id VARCHAR(128) NULL COMMENT '做市商ID。',
    event_ts DATETIME(6) NOT NULL COMMENT '業務事件時間。',
    recorded_at DATETIME(6) NOT NULL COMMENT '系統寫入時間。',
    PRIMARY KEY (id),
    KEY idx_order_lifecycle_order (order_id, event_ts),
    KEY idx_order_lifecycle_uid_symbol (uid, symbol, event_ts),
    KEY idx_order_lifecycle_stage_ts (stage, event_ts),
    KEY idx_order_lifecycle_client_order (client_order_id),
    KEY idx_order_lifecycle_event_status_time (status, event_ts),
    KEY idx_order_lifecycle_event_uid_client_time (uid, client_order_id, event_ts),
    KEY idx_order_lifecycle_strategy (uid, strategy_id, event_ts),
    KEY idx_order_lifecycle_market_maker (market_maker_id, event_ts)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='訂單生命週期事件表，記錄每次訂單狀態變化。';

CREATE TABLE IF NOT EXISTS order_lifecycle_projection (
    order_id VARCHAR(36) NOT NULL COMMENT '訂單ID。',
    schema_version INT NOT NULL COMMENT '資料格式版本。',
    uid BIGINT NOT NULL COMMENT '使用者ID。',
    symbol VARCHAR(64) NOT NULL COMMENT '交易對。',
    client_order_id VARCHAR(128) NULL COMMENT '客戶端自訂訂單ID。',
    latest_stage VARCHAR(32) NOT NULL COMMENT '最新訂單階段。',
    status VARCHAR(32) NOT NULL COMMENT '最新訂單狀態。',
    reason_code VARCHAR(128) NULL COMMENT '最新狀態原因。',
    price DECIMAL(38, 18) NULL COMMENT '委託價格。',
    orig_qty DECIMAL(38, 18) NULL COMMENT '原始下單數量。',
    remaining_qty DECIMAL(38, 18) NULL COMMENT '剩餘未成交數量。',
    executed_qty DECIMAL(38, 18) NULL COMMENT '已成交數量。',
    avg_price DECIMAL(38, 18) NULL COMMENT '成交均價。',
    strategy_id VARCHAR(128) NULL COMMENT '策略ID。',
    market_maker_id VARCHAR(128) NULL COMMENT '做市商ID。',
    first_event_at DATETIME(6) NOT NULL COMMENT '第一筆事件時間。',
    last_event_at DATETIME(6) NOT NULL COMMENT '最後一筆事件時間。',
    updated_at DATETIME(6) NOT NULL COMMENT '投影更新時間。',
    PRIMARY KEY (order_id),
    KEY idx_order_lifecycle_projection_uid_symbol (uid, symbol, last_event_at),
    KEY idx_order_lifecycle_projection_status (status, last_event_at),
    KEY idx_order_lifecycle_projection_client_order (client_order_id),
    KEY idx_order_lifecycle_projection_uid_status_updated (uid, status, updated_at),
    KEY idx_order_lifecycle_projection_symbol_status_time (symbol, status, last_event_at),
    KEY idx_order_lifecycle_projection_strategy (uid, strategy_id, last_event_at),
    KEY idx_order_lifecycle_projection_market_maker (market_maker_id, last_event_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='訂單最新狀態表，查訂單列表時不用掃事件表。';

-- ============================================================================
-- 3. Wallet ledger / reconciliation / finance audit
-- ============================================================================

CREATE TABLE IF NOT EXISTS wallet_ledger_entries (
    id VARCHAR(36) NOT NULL COMMENT '帳務分錄ID。',
    schema_version INT NOT NULL COMMENT '資料格式版本，必須大於0。',
    uid BIGINT NOT NULL COMMENT '使用者ID。',
    asset VARCHAR(32) NOT NULL COMMENT '幣種，例如USDT、BTC。',
    reason VARCHAR(64) NOT NULL COMMENT '帳務原因，例如DEPOSIT、TRADE、FEE。',
    ref_id VARCHAR(128) NULL COMMENT '關聯業務ID，例如訂單ID、成交ID、充值ID。',
    amount DECIMAL(38, 18) NOT NULL COMMENT '本次帳務金額，必須大於等於0。',
    balance_after DECIMAL(38, 18) NULL COMMENT '這筆之後的餘額快照。',
    previous_hash VARCHAR(128) NULL COMMENT '上一筆帳務hash，防止帳被偷偷改掉。',
    entry_hash VARCHAR(128) NULL COMMENT '本筆帳務hash。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    PRIMARY KEY (id),
    KEY idx_wallet_ledger_uid_asset_created (uid, asset, created_at),
    KEY idx_wallet_ledger_ref (ref_id),
    KEY idx_wallet_ledger_reason_created (reason, created_at),
    KEY idx_wallet_ledger_entries_created (created_at),
    KEY idx_wallet_ledger_entries_uid_reason_created (uid, reason, created_at),
    KEY idx_wallet_ledger_entries_asset_created (asset, created_at),
    KEY idx_wallet_ledger_entry_hash (entry_hash),
    CONSTRAINT chk_wallet_ledger_entry_amount_non_negative CHECK (amount >= 0),
    CONSTRAINT chk_wallet_ledger_entry_schema_version_positive CHECK (schema_version > 0),
    CONSTRAINT chk_wallet_ledger_entry_asset_not_blank CHECK (CHAR_LENGTH(TRIM(asset)) > 0),
    CONSTRAINT chk_wallet_ledger_entry_reason_not_blank CHECK (CHAR_LENGTH(TRIM(reason)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='錢包帳務主表，記錄每次資產變動。';

CREATE TABLE IF NOT EXISTS wallet_ledger_postings (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID。',
    entry_id VARCHAR(36) NOT NULL COMMENT '對應wallet_ledger_entries.id。',
    line_no INT NOT NULL COMMENT '同一筆分錄底下的第幾行。',
    account_code VARCHAR(64) NOT NULL COMMENT '會計科目代碼。',
    asset VARCHAR(32) NOT NULL COMMENT '幣種。',
    debit DECIMAL(38, 18) NOT NULL DEFAULT 0 COMMENT '借方金額。',
    credit DECIMAL(38, 18) NOT NULL DEFAULT 0 COMMENT '貸方金額。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallet_ledger_posting_line (entry_id, line_no),
    KEY idx_wallet_ledger_posting_entry (entry_id),
    KEY idx_wallet_ledger_posting_account (asset, account_code, created_at),
    KEY idx_wallet_ledger_postings_created (created_at),
    KEY idx_wallet_ledger_postings_account_created (account_code, created_at),
    CONSTRAINT fk_wallet_ledger_postings_entry FOREIGN KEY (entry_id) REFERENCES wallet_ledger_entries (id),
    CONSTRAINT chk_wallet_ledger_posting_non_negative CHECK (debit >= 0 AND credit >= 0),
    CONSTRAINT chk_wallet_ledger_posting_one_side CHECK ((debit > 0 AND credit = 0) OR (debit = 0 AND credit > 0)),
    CONSTRAINT chk_wallet_ledger_posting_line_positive CHECK (line_no > 0),
    CONSTRAINT chk_wallet_ledger_posting_account_not_blank CHECK (CHAR_LENGTH(TRIM(account_code)) > 0),
    CONSTRAINT chk_wallet_ledger_posting_asset_not_blank CHECK (CHAR_LENGTH(TRIM(asset)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='錢包借貸分錄明細，用來做雙分錄與試算平衡。';

CREATE TABLE IF NOT EXISTS reconciliation_reports (
    id VARCHAR(36) NOT NULL COMMENT '對帳報告ID。',
    schema_version INT NOT NULL COMMENT '資料格式版本。',
    report_type VARCHAR(64) NOT NULL COMMENT '對帳類型，例如WALLET、LEDGER、POSITION。',
    status VARCHAR(32) NOT NULL COMMENT '報告狀態，例如SUCCESS、FAILED、WARNING。',
    triggered_by VARCHAR(32) NOT NULL COMMENT '觸發來源，例如SCHEDULER、ADMIN。',
    scanned_accounts INT NOT NULL COMMENT '掃描帳戶數。',
    issue_count INT NOT NULL COMMENT '問題總數。',
    error_count INT NOT NULL COMMENT '錯誤數。',
    warn_count INT NOT NULL COMMENT '警告數。',
    alert_route VARCHAR(128) NULL COMMENT '告警路由。',
    started_at DATETIME(6) NOT NULL COMMENT '開始時間。',
    completed_at DATETIME(6) NOT NULL COMMENT '完成時間。',
    PRIMARY KEY (id),
    KEY idx_reconciliation_reports_status (status, completed_at),
    KEY idx_reconciliation_reports_trigger (triggered_by, started_at),
    CONSTRAINT chk_reconciliation_report_counts CHECK (scanned_accounts >= 0 AND issue_count >= 0 AND error_count >= 0 AND warn_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='對帳報告主表，記錄每次對帳結果。';

CREATE TABLE IF NOT EXISTS reconciliation_report_issues (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID。',
    report_id VARCHAR(36) NOT NULL COMMENT '對應reconciliation_reports.id。',
    line_no INT NOT NULL COMMENT '報告內問題序號。',
    severity VARCHAR(16) NOT NULL COMMENT '嚴重度，例如ERROR、WARN。',
    code VARCHAR(128) NOT NULL COMMENT '問題代碼。',
    message TEXT NOT NULL COMMENT '問題說明。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN' COMMENT '處理狀態，預設OPEN。',
    owner VARCHAR(128) NULL COMMENT '處理人。',
    resolved_at DATETIME(6) NULL COMMENT '解決時間。',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reconciliation_report_issue_line (report_id, line_no),
    KEY idx_reconciliation_report_issues_report (report_id),
    KEY idx_reconciliation_report_issues_code (code),
    KEY idx_reconciliation_report_issues_severity (severity),
    KEY idx_reconciliation_report_issues_status (status, created_at),
    KEY idx_reconciliation_report_issues_owner (owner, status),
    CONSTRAINT fk_reconciliation_report_issues_report FOREIGN KEY (report_id) REFERENCES reconciliation_reports (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='對帳問題明細表，一筆報告可以有多個問題。';

CREATE TABLE IF NOT EXISTS trial_balance_snapshots (
    id VARCHAR(36) NOT NULL COMMENT '試算平衡快照ID。',
    report_date DATE NOT NULL COMMENT '報告日期。',
    uid BIGINT NOT NULL COMMENT '使用者ID。',
    asset VARCHAR(32) NOT NULL COMMENT '幣種。',
    total_debit DECIMAL(38, 18) NOT NULL COMMENT '總借方金額。',
    total_credit DECIMAL(38, 18) NOT NULL COMMENT '總貸方金額。',
    balanced BOOLEAN NOT NULL COMMENT '借貸是否平衡。',
    generated_at DATETIME(6) NOT NULL COMMENT '產生時間。',
    lines_payload JSON NOT NULL COMMENT '明細內容快照。',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trial_balance_snapshot_scope (report_date, uid, asset),
    KEY idx_trial_balance_snapshot_date (report_date, uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日試算平衡快照，用於財務關帳與審計。';

CREATE TABLE IF NOT EXISTS insurance_fund_movements (
    movement_id VARCHAR(128) NOT NULL COMMENT '保險基金流水ID。',
    asset VARCHAR(32) NOT NULL COMMENT '幣種。',
    reason VARCHAR(128) NOT NULL COMMENT '異動原因，例如LIQUIDATION、ADL。',
    ref_id VARCHAR(128) NOT NULL COMMENT '關聯業務ID。',
    amount DECIMAL(38, 18) NOT NULL COMMENT '異動金額。',
    balance_after DECIMAL(38, 18) NOT NULL COMMENT '異動後保險基金餘額。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    PRIMARY KEY (movement_id),
    KEY idx_insurance_fund_asset_time (asset, created_at),
    KEY idx_insurance_fund_reason_time (reason, created_at),
    KEY idx_insurance_fund_ref (ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='保險基金異動表，記錄強平與ADL相關資金變化。';

-- ============================================================================
-- 4. Risk / position / ADL
-- ============================================================================

CREATE TABLE IF NOT EXISTS account_risk_snapshots (
    id VARCHAR(36) NOT NULL COMMENT '風險快照ID。',
    schema_version INT NOT NULL COMMENT '資料格式版本。',
    uid BIGINT NOT NULL COMMENT '使用者ID。',
    snapshot_date DATE NOT NULL COMMENT '快照日期。',
    cross_balance DECIMAL(38, 18) NOT NULL COMMENT '全倉餘額。',
    available_balance DECIMAL(38, 18) NOT NULL COMMENT '可用餘額。',
    order_hold DECIMAL(38, 18) NOT NULL COMMENT '委託凍結金額。',
    position_margin DECIMAL(38, 18) NOT NULL COMMENT '持倉佔用保證金。',
    frozen_funds DECIMAL(38, 18) NOT NULL COMMENT '其他凍結資金。',
    unrealized_pnl DECIMAL(38, 18) NOT NULL COMMENT '未實現盈虧。',
    total_equity DECIMAL(38, 18) NOT NULL COMMENT '總權益。',
    maintenance_margin DECIMAL(38, 18) NOT NULL COMMENT '維持保證金需求。',
    risk_ratio DECIMAL(38, 18) NOT NULL COMMENT '風險率。',
    open_position_count INT NOT NULL COMMENT '目前持倉數。',
    calculated_at DATETIME(6) NOT NULL COMMENT '計算時間。',
    created_at DATETIME(6) NOT NULL COMMENT '寫入時間。',
    PRIMARY KEY (id),
    KEY idx_account_risk_snapshots_uid_day (uid, snapshot_date),
    KEY idx_account_risk_snapshots_uid_time (uid, calculated_at),
    CONSTRAINT chk_account_risk_snapshot_open_positions CHECK (open_position_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帳戶風險快照，記錄合約帳戶保證金與風險率。';

CREATE TABLE IF NOT EXISTS position_lifecycle_projection (
    uid BIGINT NOT NULL COMMENT '使用者ID。',
    symbol VARCHAR(64) NOT NULL COMMENT '合約交易對。',
    schema_version INT NOT NULL DEFAULT 1 COMMENT '資料格式版本。',
    mode VARCHAR(32) NOT NULL COMMENT '倉位模式，例如CROSS或ISOLATED。',
    leverage DECIMAL(38, 18) NOT NULL DEFAULT 1.000000000000000000 COMMENT '目前使用槓桿。',
    qty DECIMAL(38, 18) NOT NULL DEFAULT 0.000000000000000000 COMMENT '持倉數量，正數/負數可代表多空。',
    entry_price DECIMAL(38, 18) NOT NULL DEFAULT 0.000000000000000000 COMMENT '開倉均價。',
    margin DECIMAL(38, 18) NOT NULL DEFAULT 0.000000000000000000 COMMENT '佔用保證金。',
    realized_pnl DECIMAL(38, 18) NOT NULL DEFAULT 0.000000000000000000 COMMENT '已實現盈虧。',
    fee_paid DECIMAL(38, 18) NOT NULL DEFAULT 0.000000000000000000 COMMENT '已付手續費。',
    rebate_earned DECIMAL(38, 18) NOT NULL DEFAULT 0.000000000000000000 COMMENT '已賺返佣。',
    funding_paid DECIMAL(38, 18) NOT NULL DEFAULT 0.000000000000000000 COMMENT '已付資金費。',
    funding_received DECIMAL(38, 18) NOT NULL DEFAULT 0.000000000000000000 COMMENT '已收資金費。',
    insurance_fund_covered DECIMAL(38, 18) NOT NULL DEFAULT 0.000000000000000000 COMMENT '保險基金覆蓋金額。',
    adl_covered DECIMAL(38, 18) NOT NULL DEFAULT 0.000000000000000000 COMMENT 'ADL覆蓋金額。',
    last_trade_ref VARCHAR(128) NULL COMMENT '最後一筆成交或業務參考ID。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '建立時間。',
    PRIMARY KEY (uid, symbol),
    KEY idx_position_projection_symbol_qty_updated (symbol, qty, updated_at),
    KEY idx_position_projection_uid_updated (uid, updated_at),
    KEY idx_position_projection_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='持倉最新狀態表，給前台持倉與風控查詢用。';

CREATE TABLE IF NOT EXISTS adl_execution_records (
    command_id VARCHAR(128) NOT NULL COMMENT 'ADL執行命令ID。',
    schema_version INT NOT NULL COMMENT '資料格式版本。',
    status VARCHAR(32) NOT NULL COMMENT '執行狀態。',
    reason VARCHAR(128) NOT NULL COMMENT 'ADL原因。',
    requested_notional DECIMAL(38, 18) NOT NULL COMMENT '要求處理的名義價值。',
    planned_notional DECIMAL(38, 18) NOT NULL COMMENT '計畫處理的名義價值。',
    executed_notional DECIMAL(38, 18) NOT NULL COMMENT '已執行名義價值。',
    remaining_notional DECIMAL(38, 18) NOT NULL COMMENT '剩餘名義價值。',
    socialized_loss_charged DECIMAL(38, 18) NOT NULL COMMENT '已分攤損失金額。',
    executed_at DATETIME(6) NULL COMMENT '執行時間。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    PRIMARY KEY (command_id),
    KEY idx_adl_execution_status_time (status, updated_at),
    KEY idx_adl_execution_reason_time (reason, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ADL強制減倉執行紀錄。';

CREATE TABLE IF NOT EXISTS adl_queue_entries (
    liquidation_id VARCHAR(128) NOT NULL COMMENT '強平事件ID。',
    uid BIGINT NOT NULL COMMENT '使用者ID。',
    symbol VARCHAR(64) NOT NULL COMMENT '交易對。',
    liquidated_side VARCHAR(16) NOT NULL COMMENT '被強平方向。',
    amount DECIMAL(38, 18) NOT NULL COMMENT '需要處理的數量或金額。',
    status VARCHAR(32) NOT NULL COMMENT '佇列狀態。',
    owner VARCHAR(128) NOT NULL COMMENT '目前處理 worker。',
    claimed_at DATETIME(6) NULL COMMENT '被worker認領時間。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    PRIMARY KEY (liquidation_id),
    KEY idx_adl_queue_status_time (status, created_at),
    KEY idx_adl_queue_owner_status (owner, status),
    KEY idx_adl_queue_symbol_status (symbol, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ADL佇列表，放強平後還需要自動減倉處理的任務。';

-- ============================================================================
-- 5. Matching engine durability
-- ============================================================================

CREATE TABLE IF NOT EXISTS matching_command_logs (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID。',
    symbol_code VARCHAR(32) NOT NULL COMMENT '撮合交易對。',
    offset_value BIGINT NOT NULL COMMENT '命令序號。',
    command_type VARCHAR(32) NOT NULL COMMENT '命令類型，例如NEW_ORDER、CANCEL_ORDER。',
    order_payload JSON NOT NULL COMMENT '訂單命令內容。',
    replacement_order_payload JSON NULL COMMENT '改单後的新訂單內容。',
    new_price DECIMAL(38, 18) NULL COMMENT '改单後價格。',
    new_qty DECIMAL(38, 18) NULL COMMENT '改单後數量。',
    owner_id VARCHAR(128) NULL COMMENT '處理這個symbol的撮合worker。',
    owner_epoch BIGINT NOT NULL DEFAULT 0 COMMENT 'worker任期，避免舊worker寫回。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    PRIMARY KEY (id),
    UNIQUE KEY uk_matching_command_symbol_offset (symbol_code, offset_value),
    KEY idx_matching_command_symbol_created (symbol_code, created_at),
    KEY idx_matching_command_owner_epoch (symbol_code, owner_id, owner_epoch),
    KEY idx_matching_command_logs_type_created (command_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='撮合命令日誌，用來恢復與重放撮合狀態。';

CREATE TABLE IF NOT EXISTS matching_event_logs (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID。',
    symbol_code VARCHAR(32) NOT NULL COMMENT '撮合交易對。',
    offset_value BIGINT NOT NULL COMMENT '事件序號。',
    command_offset BIGINT NOT NULL COMMENT '來源命令序號。',
    trade_payload JSON NOT NULL COMMENT '撮合成交事件內容。',
    owner_id VARCHAR(128) NULL COMMENT '處理這個symbol的撮合worker。',
    owner_epoch BIGINT NOT NULL DEFAULT 0 COMMENT 'worker任期。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    PRIMARY KEY (id),
    UNIQUE KEY uk_matching_event_symbol_offset (symbol_code, offset_value),
    KEY idx_matching_event_command_offset (symbol_code, command_offset),
    KEY idx_matching_event_symbol_created (symbol_code, created_at),
    KEY idx_matching_event_owner_epoch (symbol_code, owner_id, owner_epoch),
    KEY idx_matching_event_logs_command_created (symbol_code, command_offset, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='撮合事件日誌，記錄成交等撮合結果。';

CREATE TABLE IF NOT EXISTS matching_offset_checkpoints (
    symbol_code VARCHAR(32) NOT NULL COMMENT '撮合交易對。',
    command_offset BIGINT NOT NULL DEFAULT 0 COMMENT '已處理到的命令序號。',
    event_offset BIGINT NOT NULL DEFAULT 0 COMMENT '已產生到的事件序號。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    PRIMARY KEY (symbol_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='撮合offset檢查點，重啟時知道從哪裡恢復。';

CREATE TABLE IF NOT EXISTS matching_engine_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID。',
    symbol_code VARCHAR(32) NOT NULL COMMENT '撮合交易對。',
    match_sequence BIGINT NOT NULL COMMENT '撮合序號。',
    command_offset BIGINT NOT NULL COMMENT '快照包含到的命令序號。',
    event_offset BIGINT NOT NULL COMMENT '快照包含到的事件序號。',
    snapshot_payload JSON NOT NULL COMMENT '訂單簿與撮合狀態快照。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    PRIMARY KEY (id),
    KEY idx_matching_snapshot_latest (symbol_code, command_offset, event_offset, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='撮合引擎快照表，降低重放成本。';

CREATE TABLE IF NOT EXISTS matching_replay_validation_reports (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID。',
    symbol_code VARCHAR(32) NOT NULL COMMENT '撮合交易對。',
    valid BOOLEAN NOT NULL COMMENT '重放驗證是否通過。',
    start_command_offset BIGINT NOT NULL COMMENT '重放起始命令序號。',
    expected_command_offset BIGINT NOT NULL COMMENT '預期命令序號。',
    actual_command_offset BIGINT NOT NULL COMMENT '實際命令序號。',
    expected_event_offset BIGINT NOT NULL COMMENT '預期事件序號。',
    actual_event_offset BIGINT NOT NULL COMMENT '實際事件序號。',
    expected_match_sequence BIGINT NOT NULL COMMENT '預期撮合序號。',
    actual_match_sequence BIGINT NOT NULL COMMENT '實際撮合序號。',
    issues_payload JSON NOT NULL COMMENT '驗證問題列表。',
    validated_at DATETIME(6) NOT NULL COMMENT '驗證時間。',
    PRIMARY KEY (id),
    KEY idx_matching_replay_report_symbol_time (symbol_code, validated_at),
    KEY idx_matching_replay_report_valid (valid, validated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='撮合重放驗證報告。';

CREATE TABLE IF NOT EXISTS matching_sequencer_leases (
    symbol_code VARCHAR(32) NOT NULL COMMENT '撮合交易對。',
    owner_id VARCHAR(128) NOT NULL COMMENT '目前持有租約的worker。',
    epoch BIGINT NOT NULL COMMENT '租約任期。',
    expires_at DATETIME(6) NOT NULL COMMENT '租約過期時間。',
    command_offset BIGINT NOT NULL DEFAULT 0 COMMENT 'worker已處理命令序號。',
    event_offset BIGINT NOT NULL DEFAULT 0 COMMENT 'worker已產生事件序號。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    PRIMARY KEY (symbol_code),
    KEY idx_matching_sequencer_owner (owner_id, expires_at),
    KEY idx_matching_sequencer_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='撮合worker租約表，確保同一交易對只有一個worker在處理。';

-- ============================================================================
-- 6. Market data
-- ============================================================================

CREATE TABLE IF NOT EXISTS market_data_sequence_checkpoints (
    symbol VARCHAR(32) NOT NULL COMMENT '交易對。',
    stream VARCHAR(64) NOT NULL COMMENT '行情流名稱，例如DEPTH、TRADE。',
    sequence_value BIGINT NOT NULL COMMENT '最後處理到的序號。',
    checksum BIGINT NOT NULL COMMENT '資料校驗值。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    PRIMARY KEY (symbol, stream),
    KEY idx_md_seq_symbol_stream (symbol, stream)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行情序號檢查點，重連後知道要從哪裡補資料。';

CREATE TABLE IF NOT EXISTS market_data_depth_deltas (
    symbol VARCHAR(32) NOT NULL COMMENT '交易對。',
    version_value BIGINT NOT NULL COMMENT '深度版本號。',
    checksum BIGINT NOT NULL COMMENT '深度校驗值。',
    bids_json LONGTEXT NOT NULL COMMENT '買盤變動JSON。',
    asks_json LONGTEXT NOT NULL COMMENT '賣盤變動JSON。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    PRIMARY KEY (symbol, version_value),
    KEY idx_md_depth_symbol_version (symbol, version_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='訂單簿深度增量，給行情重連補資料使用。';

CREATE TABLE IF NOT EXISTS market_data_trade_tape (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID。',
    symbol VARCHAR(32) NOT NULL COMMENT '交易對。',
    match_id VARCHAR(128) NOT NULL COMMENT '撮合成交ID。',
    order_id VARCHAR(36) NULL COMMENT '關聯訂單ID。',
    side VARCHAR(16) NOT NULL COMMENT '成交方向。',
    price DECIMAL(38, 18) NOT NULL COMMENT '成交價格。',
    qty DECIMAL(38, 18) NOT NULL COMMENT '成交數量。',
    maker BOOLEAN NOT NULL COMMENT '是否為maker。',
    trade_ts DATETIME(6) NOT NULL COMMENT '成交時間。',
    created_at DATETIME(6) NOT NULL COMMENT '寫入時間。',
    PRIMARY KEY (id),
    KEY idx_md_trade_symbol_time (symbol, trade_ts, id),
    KEY idx_md_trade_match (match_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='近期成交行情表。';

CREATE TABLE IF NOT EXISTS market_data_tickers (
    symbol VARCHAR(32) NOT NULL COMMENT '交易對。',
    last_price DECIMAL(38, 18) NULL COMMENT '最新成交價。',
    best_bid DECIMAL(38, 18) NULL COMMENT '最佳買價。',
    best_ask DECIMAL(38, 18) NULL COMMENT '最佳賣價。',
    volume_24h DECIMAL(38, 18) NOT NULL COMMENT '24小時成交量。',
    high_24h DECIMAL(38, 18) NULL COMMENT '24小時最高價。',
    low_24h DECIMAL(38, 18) NULL COMMENT '24小時最低價。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    PRIMARY KEY (symbol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易對最新行情摘要。';

CREATE TABLE IF NOT EXISTS market_data_klines (
    symbol VARCHAR(32) NOT NULL COMMENT '交易對。',
    interval_value VARCHAR(16) NOT NULL COMMENT 'K線週期，例如1m、5m、1h。',
    open_time DATETIME(6) NOT NULL COMMENT 'K線開始時間。',
    open_price DECIMAL(38, 18) NOT NULL COMMENT '開盤價。',
    high_price DECIMAL(38, 18) NOT NULL COMMENT '最高價。',
    low_price DECIMAL(38, 18) NOT NULL COMMENT '最低價。',
    close_price DECIMAL(38, 18) NOT NULL COMMENT '收盤價。',
    volume DECIMAL(38, 18) NOT NULL COMMENT '成交量。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    PRIMARY KEY (symbol, interval_value, open_time),
    KEY idx_md_kline_symbol_interval_time (symbol, interval_value, open_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='K線資料表。';

-- ============================================================================
-- 7. Symbol / product config
-- ============================================================================

CREATE TABLE IF NOT EXISTS trading_symbol (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID。',
    symbol VARCHAR(64) NOT NULL COMMENT '交易對代碼，例如BTCUSDT-SPOT或BTCUSDT-PERP。',
    product_type VARCHAR(32) NOT NULL COMMENT '商品類型：SPOT現貨、PERPETUAL永續合約。',
    base_asset VARCHAR(32) NOT NULL COMMENT '前面的幣，例如BTC。',
    quote_asset VARCHAR(32) NOT NULL COMMENT '後面的幣，例如USDT。',
    margin_asset VARCHAR(32) NULL COMMENT '保證金幣種，現貨通常為空，USDT合約通常是USDT。',
    price_tick DECIMAL(38, 18) NOT NULL COMMENT '價格每次最小跳動單位，例如0.01。',
    lot_size DECIMAL(38, 18) NOT NULL COMMENT '數量每次最小跳動單位，例如0.001。',
    min_qty DECIMAL(38, 18) NOT NULL COMMENT '最小下單數量。',
    min_notional DECIMAL(38, 18) NOT NULL COMMENT '最小下單金額，通常用USDT計。',
    max_order_notional DECIMAL(38, 18) NOT NULL COMMENT '單筆最多可以下多少錢。',
    max_position_notional DECIMAL(38, 18) NOT NULL DEFAULT 0 COMMENT '合約最大持倉金額，現貨通常為0。',
    max_open_orders INT NOT NULL DEFAULT 200 COMMENT '單一使用者最多可掛幾筆未成交訂單。',
    max_leverage INT NOT NULL DEFAULT 1 COMMENT '最大槓桿，現貨固定1。',
    initial_margin_rate DECIMAL(38, 18) NOT NULL COMMENT '初始保證金率，現貨可填1。',
    maintenance_margin_rate DECIMAL(38, 18) NOT NULL COMMENT '維持保證金率，現貨可填1。',
    maker_fee_rate DECIMAL(38, 18) NOT NULL COMMENT '掛單手續費率。',
    taker_fee_rate DECIMAL(38, 18) NOT NULL COMMENT '吃單手續費率。',
    maker_rebate_rate DECIMAL(38, 18) NOT NULL DEFAULT 0 COMMENT '掛單返佣率，沒有返佣就0。',
    referral_rebate_rate DECIMAL(38, 18) NOT NULL DEFAULT 0 COMMENT '推薦返佣率，沒有返佣就0。',
    price_band_rate DECIMAL(38, 18) NOT NULL DEFAULT 0.10 COMMENT '價格保護範圍，例如0.10代表10%。',
    trading_enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否允許交易。',
    visible BOOLEAN NOT NULL DEFAULT TRUE COMMENT '前台是否顯示。',
    reduce_only BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否只允許平倉或減倉。',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '建立時間。',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間。',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trading_symbol_symbol (symbol),
    KEY idx_trading_symbol_product_visible (product_type, visible, trading_enabled),
    KEY idx_trading_symbol_assets (base_asset, quote_asset)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易對設定表，後台用來配置現貨與合約交易規則。';

CREATE TABLE IF NOT EXISTS trading_symbol_risk_tier (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID。',
    symbol VARCHAR(64) NOT NULL COMMENT '交易對代碼，對應trading_symbol.symbol。',
    tier INT NOT NULL COMMENT '第幾層風控。',
    max_position_notional DECIMAL(38, 18) NOT NULL COMMENT '這層最多可以持有多少倉位金額。',
    max_leverage INT NOT NULL COMMENT '這層最多可以開幾倍槓桿。',
    initial_margin_rate DECIMAL(38, 18) NOT NULL COMMENT '這層初始保證金率。',
    maintenance_margin_rate DECIMAL(38, 18) NOT NULL COMMENT '這層維持保證金率。',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '建立時間。',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間。',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trading_symbol_risk_tier (symbol, tier),
    KEY idx_trading_symbol_risk_tier_symbol_notional (symbol, max_position_notional),
    CONSTRAINT fk_trading_symbol_risk_tier_symbol FOREIGN KEY (symbol) REFERENCES trading_symbol (symbol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合約風控分層表，倉位越大通常槓桿越低。';

-- Seed only the first three assets requested for the exchange MVP.
INSERT INTO trading_symbol (
    symbol, product_type, base_asset, quote_asset, margin_asset,
    price_tick, lot_size, min_qty, min_notional,
    max_order_notional, max_position_notional, max_open_orders,
    max_leverage, initial_margin_rate, maintenance_margin_rate,
    maker_fee_rate, taker_fee_rate, maker_rebate_rate, referral_rebate_rate, price_band_rate,
    trading_enabled, visible, reduce_only
) VALUES
('BTCUSDT-SPOT', 'SPOT', 'BTC', 'USDT', NULL, 0.10, 0.00001, 0.00010, 5, 1000000, 0, 200, 1, 1, 1, 0.0010, 0.0015, 0, 0, 0.10, TRUE, TRUE, FALSE),
('ETHUSDT-SPOT', 'SPOT', 'ETH', 'USDT', NULL, 0.01, 0.00010, 0.00100, 5, 500000, 0, 200, 1, 1, 1, 0.0010, 0.0015, 0, 0, 0.10, TRUE, TRUE, FALSE),
('BNBUSDT-SPOT', 'SPOT', 'BNB', 'USDT', NULL, 0.01, 0.00100, 0.01000, 5, 300000, 0, 200, 1, 1, 1, 0.0010, 0.0015, 0, 0, 0.10, TRUE, TRUE, FALSE),
('BTCUSDT-PERP', 'PERPETUAL', 'BTC', 'USDT', 'USDT', 0.10, 0.00100, 0.00100, 5, 1000000, 5000000, 200, 100, 0.010000000000000000, 0.005000000000000000, 0.0002, 0.0005, 0, 0, 0.10, TRUE, TRUE, FALSE),
('ETHUSDT-PERP', 'PERPETUAL', 'ETH', 'USDT', 'USDT', 0.01, 0.01000, 0.01000, 5, 500000, 3000000, 200, 75, 0.013333333333333333, 0.006500000000000000, 0.0002, 0.0005, 0, 0, 0.10, TRUE, TRUE, FALSE),
('BNBUSDT-PERP', 'PERPETUAL', 'BNB', 'USDT', 'USDT', 0.01, 0.01000, 0.01000, 5, 300000, 1000000, 200, 50, 0.020000000000000000, 0.010000000000000000, 0.0002, 0.0005, 0, 0, 0.10, TRUE, TRUE, FALSE)
ON DUPLICATE KEY UPDATE
    product_type = VALUES(product_type),
    base_asset = VALUES(base_asset),
    quote_asset = VALUES(quote_asset),
    margin_asset = VALUES(margin_asset),
    price_tick = VALUES(price_tick),
    lot_size = VALUES(lot_size),
    min_qty = VALUES(min_qty),
    min_notional = VALUES(min_notional),
    max_order_notional = VALUES(max_order_notional),
    max_position_notional = VALUES(max_position_notional),
    max_open_orders = VALUES(max_open_orders),
    max_leverage = VALUES(max_leverage),
    initial_margin_rate = VALUES(initial_margin_rate),
    maintenance_margin_rate = VALUES(maintenance_margin_rate),
    maker_fee_rate = VALUES(maker_fee_rate),
    taker_fee_rate = VALUES(taker_fee_rate),
    maker_rebate_rate = VALUES(maker_rebate_rate),
    referral_rebate_rate = VALUES(referral_rebate_rate),
    price_band_rate = VALUES(price_band_rate),
    trading_enabled = VALUES(trading_enabled),
    visible = VALUES(visible),
    reduce_only = VALUES(reduce_only);

INSERT INTO trading_symbol_risk_tier (
    symbol, tier, max_position_notional, max_leverage, initial_margin_rate, maintenance_margin_rate
) VALUES
('BTCUSDT-PERP', 1, 100000, 100, 0.010000000000000000, 0.005000000000000000),
('BTCUSDT-PERP', 2, 500000, 50, 0.020000000000000000, 0.010000000000000000),
('BTCUSDT-PERP', 3, 5000000, 20, 0.050000000000000000, 0.025000000000000000),
('ETHUSDT-PERP', 1, 100000, 75, 0.013333333333333333, 0.006500000000000000),
('ETHUSDT-PERP', 2, 500000, 50, 0.020000000000000000, 0.010000000000000000),
('ETHUSDT-PERP', 3, 3000000, 20, 0.050000000000000000, 0.025000000000000000),
('BNBUSDT-PERP', 1, 50000, 50, 0.020000000000000000, 0.010000000000000000),
('BNBUSDT-PERP', 2, 300000, 25, 0.040000000000000000, 0.020000000000000000),
('BNBUSDT-PERP', 3, 1000000, 10, 0.100000000000000000, 0.050000000000000000)
ON DUPLICATE KEY UPDATE
    max_position_notional = VALUES(max_position_notional),
    max_leverage = VALUES(max_leverage),
    initial_margin_rate = VALUES(initial_margin_rate),
    maintenance_margin_rate = VALUES(maintenance_margin_rate);

-- ============================================================================
-- 8. Turnover / bonus / market maker / hedge
-- ============================================================================

CREATE TABLE IF NOT EXISTS turnover_records (
    id VARCHAR(36) NOT NULL COMMENT '成交量紀錄ID。',
    schema_version INT NOT NULL COMMENT '資料格式版本。',
    uid BIGINT NOT NULL COMMENT '使用者ID。',
    account_id VARCHAR(64) NULL COMMENT '帳戶ID。',
    symbol VARCHAR(32) NOT NULL COMMENT '交易對。',
    strategy_id VARCHAR(128) NULL COMMENT '策略ID。',
    market_maker_id VARCHAR(128) NULL COMMENT '做市商ID。',
    order_id VARCHAR(36) NOT NULL COMMENT '訂單ID。',
    match_id VARCHAR(128) NULL COMMENT '撮合成交ID。',
    trade_seq BIGINT NOT NULL COMMENT '成交序號。',
    quantity DECIMAL(38, 18) NOT NULL COMMENT '成交數量。',
    price DECIMAL(38, 18) NOT NULL COMMENT '成交價格。',
    notional DECIMAL(38, 18) NOT NULL COMMENT '成交金額。',
    traded_at DATETIME(6) NOT NULL COMMENT '成交時間。',
    created_at DATETIME(6) NOT NULL COMMENT '寫入時間。',
    PRIMARY KEY (id),
    UNIQUE KEY uk_turnover_trade_order (trade_seq, order_id),
    KEY idx_turnover_uid_created (uid, created_at),
    KEY idx_turnover_symbol_created (symbol, created_at),
    KEY idx_turnover_strategy (uid, strategy_id, created_at),
    KEY idx_turnover_market_maker (market_maker_id, created_at),
    KEY idx_turnover_match (match_id),
    CONSTRAINT chk_turnover_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT chk_turnover_price_non_negative CHECK (price >= 0),
    CONSTRAINT chk_turnover_notional_non_negative CHECK (notional >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成交量紀錄表，用於活動、返佣、報表與做市統計。';

CREATE TABLE IF NOT EXISTS bonus_credit_grants (
    id VARCHAR(36) NOT NULL COMMENT '贈金ID。',
    schema_version INT NOT NULL COMMENT '資料格式版本。',
    uid BIGINT NOT NULL COMMENT '使用者ID。',
    asset VARCHAR(32) NOT NULL COMMENT '贈金幣種。',
    original_amount DECIMAL(38, 18) NOT NULL COMMENT '原始贈金金額。',
    remaining_amount DECIMAL(38, 18) NOT NULL COMMENT '剩餘可用贈金。',
    campaign_id VARCHAR(128) NULL COMMENT '活動ID。',
    status VARCHAR(32) NOT NULL COMMENT '贈金狀態。',
    granted_at DATETIME(6) NOT NULL COMMENT '發放時間。',
    expires_at DATETIME(6) NULL COMMENT '過期時間。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    PRIMARY KEY (id),
    KEY idx_bonus_credit_uid_asset_status (uid, asset, status, expires_at),
    KEY idx_bonus_credit_expiry (status, expires_at),
    KEY idx_bonus_credit_campaign (campaign_id),
    CONSTRAINT chk_bonus_credit_original_non_negative CHECK (original_amount >= 0),
    CONSTRAINT chk_bonus_credit_remaining_non_negative CHECK (remaining_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='贈金發放紀錄表。';

CREATE TABLE IF NOT EXISTS market_maker_profiles (
    market_maker_id VARCHAR(128) NOT NULL COMMENT '做市商ID。',
    schema_version INT NOT NULL COMMENT '資料格式版本。',
    uid BIGINT NOT NULL COMMENT '對應使用者ID。',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否啟用。',
    PRIMARY KEY (market_maker_id),
    UNIQUE KEY uk_market_maker_profiles_uid (uid),
    KEY idx_market_maker_profiles_uid (uid),
    KEY idx_market_maker_profiles_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='做市商基本資料。';

CREATE TABLE IF NOT EXISTS market_maker_risk_limits (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID。',
    market_maker_id VARCHAR(128) NOT NULL COMMENT '做市商ID。',
    symbol VARCHAR(32) NOT NULL COMMENT '交易對。',
    max_long_notional DECIMAL(38, 18) NOT NULL DEFAULT 0 COMMENT '最大多頭金額。',
    max_short_notional DECIMAL(38, 18) NOT NULL DEFAULT 0 COMMENT '最大空頭金額。',
    max_order_notional DECIMAL(38, 18) NOT NULL DEFAULT 0 COMMENT '單筆最大下單金額。',
    max_slippage_rate DECIMAL(38, 18) NOT NULL DEFAULT 0 COMMENT '最大允許滑點。',
    kill_switch BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否啟用熔斷，true時禁止做市。',
    PRIMARY KEY (id),
    UNIQUE KEY uk_market_maker_risk_limit_symbol (market_maker_id, symbol),
    KEY idx_market_maker_risk_limits_mm (market_maker_id),
    CONSTRAINT fk_market_maker_risk_limits_profile FOREIGN KEY (market_maker_id) REFERENCES market_maker_profiles (market_maker_id),
    CONSTRAINT chk_market_maker_risk_limit_non_negative CHECK (max_long_notional >= 0 AND max_short_notional >= 0 AND max_order_notional >= 0 AND max_slippage_rate >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='做市商風控限制表。';

CREATE TABLE IF NOT EXISTS hedge_decision_audits (
    id VARCHAR(36) NOT NULL COMMENT '對沖決策ID。',
    schema_version INT NOT NULL COMMENT '資料格式版本。',
    market_maker_id VARCHAR(128) NOT NULL COMMENT '做市商ID。',
    symbol VARCHAR(32) NOT NULL COMMENT '交易對。',
    accepted BOOLEAN NOT NULL COMMENT '是否接受對沖。',
    reason VARCHAR(128) NULL COMMENT '決策原因。',
    order_notional DECIMAL(38, 18) NOT NULL COMMENT '訂單金額。',
    venue_order_id VARCHAR(128) NULL COMMENT '外部交易所訂單ID。',
    ref_id VARCHAR(128) NULL COMMENT '關聯業務ID。',
    internal_trade_ref_id VARCHAR(128) NULL COMMENT '內部成交參考ID。',
    decided_at DATETIME(6) NOT NULL COMMENT '決策時間。',
    created_at DATETIME(6) NOT NULL COMMENT '寫入時間。',
    PRIMARY KEY (id),
    KEY idx_hedge_decision_mm_time (market_maker_id, decided_at),
    KEY idx_hedge_decision_symbol_time (symbol, decided_at),
    KEY idx_hedge_decision_ref (ref_id),
    KEY idx_hedge_decision_accepted (accepted, decided_at),
    KEY idx_hedge_decision_trade_ref (internal_trade_ref_id),
    CONSTRAINT chk_hedge_decision_notional_non_negative CHECK (order_notional >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='對沖決策審計表。';

CREATE TABLE IF NOT EXISTS hedge_fills (
    id VARCHAR(36) NOT NULL COMMENT '外部對沖成交ID。',
    schema_version INT NOT NULL COMMENT '資料格式版本。',
    market_maker_id VARCHAR(128) NOT NULL COMMENT '做市商ID。',
    symbol VARCHAR(32) NOT NULL COMMENT '交易對。',
    venue_order_id VARCHAR(128) NOT NULL COMMENT '外部交易所訂單ID。',
    venue_fill_id VARCHAR(128) NOT NULL COMMENT '外部交易所成交ID。',
    side VARCHAR(16) NOT NULL COMMENT '買賣方向。',
    quantity DECIMAL(38, 18) NOT NULL COMMENT '成交數量。',
    price DECIMAL(38, 18) NOT NULL COMMENT '成交價格。',
    fee DECIMAL(38, 18) NOT NULL DEFAULT 0 COMMENT '手續費。',
    fee_asset VARCHAR(32) NULL COMMENT '手續費幣種。',
    ref_id VARCHAR(128) NULL COMMENT '關聯業務ID。',
    ledger_ref_id VARCHAR(128) NULL COMMENT '帳務參考ID。',
    filled_at DATETIME(6) NOT NULL COMMENT '成交時間。',
    created_at DATETIME(6) NOT NULL COMMENT '寫入時間。',
    PRIMARY KEY (id),
    UNIQUE KEY uk_hedge_fills_venue_fill (venue_order_id, venue_fill_id),
    KEY idx_hedge_fills_mm_time (market_maker_id, filled_at),
    KEY idx_hedge_fills_venue_order (venue_order_id),
    KEY idx_hedge_fills_ref (ref_id),
    KEY idx_hedge_fills_symbol_time (symbol, filled_at),
    KEY idx_hedge_fills_ledger_ref (ledger_ref_id),
    CONSTRAINT chk_hedge_fills_non_negative CHECK (quantity > 0 AND price > 0 AND fee >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外部對沖成交表。';

CREATE TABLE IF NOT EXISTS hedge_venue_idempotency_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID。',
    ref_id VARCHAR(128) NOT NULL COMMENT '對沖請求參考ID。',
    fingerprint VARCHAR(512) NOT NULL COMMENT '請求指紋，用來判斷是否同一個請求。',
    completed BOOLEAN NOT NULL COMMENT '是否已完成。',
    accepted BOOLEAN NULL COMMENT '外部交易所是否接受。',
    venue_order_id VARCHAR(128) NULL COMMENT '外部交易所訂單ID。',
    reason VARCHAR(256) NULL COMMENT '結果原因。',
    retryable BOOLEAN NULL COMMENT '失敗後是否可重試。',
    submitted_at DATETIME(6) NULL COMMENT '送出時間。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    PRIMARY KEY (id),
    UNIQUE KEY uk_hedge_venue_idem_ref_id (ref_id),
    KEY idx_hedge_venue_idem_completed (completed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外部對沖下單冪等表，避免重複送單。';

CREATE TABLE IF NOT EXISTS hedge_execution_locks (
    lock_name VARCHAR(128) NOT NULL COMMENT '鎖名稱。',
    owner_id VARCHAR(128) NOT NULL COMMENT '目前持有鎖的worker。',
    expires_at DATETIME(6) NOT NULL COMMENT '鎖過期時間。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    PRIMARY KEY (lock_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='對沖排程worker鎖。';

CREATE TABLE IF NOT EXISTS market_maker_quote_states (
    id VARCHAR(192) NOT NULL COMMENT '報價狀態ID。',
    market_maker_id VARCHAR(128) NOT NULL COMMENT '做市商ID。',
    uid BIGINT NOT NULL COMMENT '使用者ID。',
    symbol VARCHAR(32) NOT NULL COMMENT '交易對。',
    ref_id VARCHAR(128) NULL COMMENT '關聯業務ID。',
    active BOOLEAN NOT NULL COMMENT '報價是否仍有效。',
    accepted BOOLEAN NOT NULL COMMENT '報價是否被接受。',
    reason VARCHAR(256) NULL COMMENT '狀態原因。',
    canceled_count INT NOT NULL COMMENT '已取消次數。',
    bid_order_id VARCHAR(36) NULL COMMENT '買一側訂單ID。',
    ask_order_id VARCHAR(36) NULL COMMENT '賣一側訂單ID。',
    bid_version BIGINT NOT NULL DEFAULT 0 COMMENT '買一側報價版本。',
    ask_version BIGINT NOT NULL DEFAULT 0 COMMENT '賣一側報價版本。',
    replaced_bid_order_id VARCHAR(36) NULL COMMENT '被替換的買單ID。',
    replaced_ask_order_id VARCHAR(36) NULL COMMENT '被替換的賣單ID。',
    bid_price DECIMAL(38, 18) NULL COMMENT '最新買價。',
    bid_quantity DECIMAL(38, 18) NULL COMMENT '最新買量。',
    ask_price DECIMAL(38, 18) NULL COMMENT '最新賣價。',
    ask_quantity DECIMAL(38, 18) NULL COMMENT '最新賣量。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    PRIMARY KEY (id),
    UNIQUE KEY uk_mm_quote_states_mm_symbol (market_maker_id, symbol),
    KEY idx_mm_quote_states_mm_updated (market_maker_id, updated_at),
    KEY idx_mm_quote_states_active_updated (active, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='做市商目前報價狀態表。';

-- ============================================================================
-- 9. Polymarket integration tables
-- ============================================================================

CREATE TABLE IF NOT EXISTS polymarket_session (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID。',
    session_id VARCHAR(64) NOT NULL COMMENT 'Polymarket session ID。',
    user_address VARCHAR(64) NOT NULL COMMENT '使用者錢包地址。',
    session_signer_address VARCHAR(64) NOT NULL COMMENT 'session signer地址。',
    session_private_key VARCHAR(512) NOT NULL COMMENT 'session私鑰，正式環境應改用KMS或加密保存。',
    typed_data LONGTEXT NOT NULL COMMENT '簽名用typed data。',
    signature LONGTEXT NULL COMMENT '使用者簽名。',
    status VARCHAR(32) NOT NULL COMMENT 'session狀態。',
    issued_at BIGINT NULL COMMENT '發行時間戳。',
    expires_at BIGINT NULL COMMENT '過期時間戳。',
    created_at VARCHAR(64) NOT NULL COMMENT '建立時間字串。',
    confirmed_at VARCHAR(64) NULL COMMENT '確認時間字串。',
    revoked_at VARCHAR(64) NULL COMMENT '撤銷時間字串。',
    last_used_at VARCHAR(64) NULL COMMENT '最後使用時間字串。',
    max_order_usdt DECIMAL(38, 18) NULL COMMENT '單筆最大USDT額度。',
    daily_limit_usdt DECIMAL(38, 18) NULL COMMENT '每日USDT額度。',
    daily_used_usdt DECIMAL(38, 18) NULL COMMENT '今日已用USDT額度。',
    daily_reset_date VARCHAR(16) NULL COMMENT '每日額度重置日期。',
    revoked_reason VARCHAR(256) NULL COMMENT '撤銷原因。',
    PRIMARY KEY (id),
    UNIQUE KEY idx_session_id (session_id),
    KEY idx_user_address (user_address),
    KEY idx_status (status),
    KEY idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Polymarket session資料表。';

CREATE TABLE IF NOT EXISTS prediction_market_sync_key (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID。',
    event_slug VARCHAR(128) NULL COMMENT '事件slug。',
    event_title VARCHAR(256) NULL COMMENT '事件標題。',
    team_a VARCHAR(128) NOT NULL COMMENT '隊伍A或選項A。',
    team_b VARCHAR(128) NOT NULL COMMENT '隊伍B或選項B。',
    event_date DATE NOT NULL COMMENT '事件日期。',
    source VARCHAR(64) NULL COMMENT '資料來源。',
    sync_enabled BOOLEAN NOT NULL COMMENT '是否啟用同步。',
    sync_status VARCHAR(32) NULL COMMENT '同步狀態。',
    retry_count INT NULL COMMENT '重試次數。',
    last_error TEXT NULL COMMENT '最後錯誤。',
    last_synced_at DATETIME(6) NULL COMMENT '最後同步時間。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pm_event_slug (event_slug),
    KEY idx_pm_sync_enabled (sync_enabled),
    KEY idx_pm_sync_status (sync_status),
    KEY idx_pm_event_date (event_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Polymarket事件同步key。';

CREATE TABLE IF NOT EXISTS prediction_market_info (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID。',
    market_id VARCHAR(128) NOT NULL COMMENT 'Polymarket market ID。',
    event_slug VARCHAR(128) NOT NULL COMMENT '事件slug。',
    event_title VARCHAR(256) NULL COMMENT '事件標題。',
    team_a VARCHAR(128) NULL COMMENT '隊伍A或選項A。',
    team_b VARCHAR(128) NULL COMMENT '隊伍B或選項B。',
    event_date DATE NULL COMMENT '事件日期。',
    condition_id VARCHAR(128) NULL COMMENT 'Polymarket condition ID。',
    question VARCHAR(512) NULL COMMENT '市場問題。',
    market_slug VARCHAR(256) NOT NULL COMMENT '市場slug。',
    outcome_key VARCHAR(32) NOT NULL COMMENT '結果key。',
    outcome_label VARCHAR(128) NULL COMMENT '結果顯示名稱。',
    yes_token_id VARCHAR(256) NULL COMMENT 'YES token ID。',
    no_token_id VARCHAR(256) NULL COMMENT 'NO token ID。',
    active BOOLEAN NULL COMMENT '是否啟用。',
    closed BOOLEAN NULL COMMENT '是否已關閉。',
    accepting_orders BOOLEAN NULL COMMENT '是否接受訂單。',
    enable_order_book BOOLEAN NULL COMMENT '是否啟用訂單簿。',
    neg_risk BOOLEAN NULL COMMENT '是否為neg risk市場。',
    best_bid DOUBLE NULL COMMENT '最佳買價。',
    best_ask DOUBLE NULL COMMENT '最佳賣價。',
    last_trade_price DOUBLE NULL COMMENT '最新成交價。',
    static_yes_price DOUBLE NULL COMMENT '靜態YES價格。',
    static_no_price DOUBLE NULL COMMENT '靜態NO價格。',
    no_buy_price DOUBLE NULL COMMENT 'NO買入價格。',
    no_sell_price DOUBLE NULL COMMENT 'NO賣出價格。',
    liquidity DOUBLE NULL COMMENT '流動性。',
    volume DOUBLE NULL COMMENT '成交量。',
    volume_24hr DOUBLE NULL COMMENT '24小時成交量。',
    outcome_prices TEXT NULL COMMENT '結果價格原始資料。',
    clob_token_ids TEXT NULL COMMENT 'CLOB token ID原始資料。',
    last_price_updated_at DATETIME(6) NULL COMMENT '價格最後更新時間。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pm_market_slug (market_slug),
    KEY idx_pm_info_event_slug (event_slug),
    KEY idx_pm_info_outcome (outcome_key),
    KEY idx_pm_info_price_updated (last_price_updated_at),
    KEY idx_pm_info_active_closed (active, closed),
    CONSTRAINT fk_pm_info_event_slug FOREIGN KEY (event_slug) REFERENCES prediction_market_sync_key (event_slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Polymarket市場資訊快照。';

CREATE TABLE IF NOT EXISTS prediction_market_sync_progress (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID。',
    job_name VARCHAR(64) NOT NULL COMMENT '同步任務名稱。',
    last_sync_key_id BIGINT NULL COMMENT '最後同步到的key ID。',
    status VARCHAR(32) NOT NULL COMMENT '同步狀態。',
    total_count INT NULL COMMENT '總數。',
    success_count INT NULL COMMENT '成功數。',
    failed_count INT NULL COMMENT '失敗數。',
    last_error TEXT NULL COMMENT '最後錯誤。',
    started_at DATETIME(6) NULL COMMENT '開始時間。',
    finished_at DATETIME(6) NULL COMMENT '完成時間。',
    updated_at DATETIME(6) NULL COMMENT '更新時間。',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pm_sync_progress_job (job_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Polymarket同步進度表。';

CREATE TABLE IF NOT EXISTS prediction_polymarket_order (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID。',
    internal_order_id VARCHAR(64) NOT NULL COMMENT '內部訂單ID。',
    clob_order_id VARCHAR(128) NULL COMMENT 'Polymarket CLOB訂單ID。',
    user_id VARCHAR(64) NOT NULL COMMENT '使用者ID。',
    session_id VARCHAR(64) NOT NULL COMMENT 'session ID。',
    event_slug VARCHAR(128) NULL COMMENT '事件slug。',
    market_slug VARCHAR(256) NOT NULL COMMENT '市場slug。',
    condition_id VARCHAR(128) NULL COMMENT 'condition ID。',
    outcome_key VARCHAR(32) NULL COMMENT '結果key。',
    token_id VARCHAR(256) NULL COMMENT 'token ID。',
    direction VARCHAR(32) NULL COMMENT '方向。',
    side VARCHAR(16) NULL COMMENT '買賣方向。',
    order_type VARCHAR(16) NULL COMMENT '訂單類型。',
    price DECIMAL(38, 18) NULL COMMENT '價格。',
    size DECIMAL(38, 18) NULL COMMENT '數量。',
    usdt_amount DECIMAL(38, 18) NULL COMMENT 'USDT金額。',
    status VARCHAR(64) NOT NULL COMMENT '內部狀態。',
    trade_status VARCHAR(64) NULL COMMENT '交易狀態。',
    size_matched DECIMAL(38, 18) NULL COMMENT '已成交數量。',
    last_trade_id VARCHAR(128) NULL COMMENT '最後成交ID。',
    last_error TEXT NULL COMMENT '最後錯誤。',
    last_clob_payload LONGTEXT NULL COMMENT '最後CLOB回傳內容。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    last_synced_at DATETIME(6) NULL COMMENT '最後同步時間。',
    PRIMARY KEY (id),
    UNIQUE KEY uk_poly_internal_order_id (internal_order_id),
    KEY idx_poly_order_internal (internal_order_id),
    KEY idx_poly_order_clob (clob_order_id),
    KEY idx_poly_order_status (status),
    KEY idx_poly_order_market (market_slug),
    KEY idx_poly_order_session (session_id),
    KEY idx_prediction_order_user_status_updated (user_id, status, updated_at),
    KEY idx_prediction_order_market_status_updated (market_slug, status, updated_at),
    KEY idx_prediction_order_event_status_updated (event_slug, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Polymarket下單紀錄表。';

CREATE TABLE IF NOT EXISTS prediction_polymarket_ws_event (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID。',
    event_key VARCHAR(256) NOT NULL COMMENT 'WS事件唯一key。',
    event_type VARCHAR(64) NULL COMMENT '事件類型。',
    status VARCHAR(64) NULL COMMENT '事件狀態。',
    wallet_address VARCHAR(64) NULL COMMENT '錢包地址。',
    market VARCHAR(128) NULL COMMENT '市場ID。',
    asset_id VARCHAR(256) NULL COMMENT '資產ID。',
    order_id VARCHAR(128) NULL COMMENT '訂單ID。',
    trade_id VARCHAR(128) NULL COMMENT '成交ID。',
    payload LONGTEXT NULL COMMENT '原始WS內容。',
    received_at DATETIME(6) NULL COMMENT '收到時間。',
    created_at DATETIME(6) NOT NULL COMMENT '寫入時間。',
    PRIMARY KEY (id),
    UNIQUE KEY uk_poly_ws_event_key (event_key),
    KEY idx_poly_ws_event_key (event_key),
    KEY idx_poly_ws_order (order_id),
    KEY idx_poly_ws_trade (trade_id),
    KEY idx_poly_ws_type (event_type),
    KEY idx_prediction_ws_wallet_type_received (wallet_address, event_type, received_at),
    KEY idx_prediction_ws_market_type_received (market, event_type, received_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Polymarket WebSocket事件表。';

CREATE TABLE IF NOT EXISTS polymarket_clob_command_record (
    command_id VARCHAR(128) NOT NULL COMMENT 'CLOB命令ID。',
    command_type VARCHAR(32) NOT NULL COMMENT '命令類型。',
    internal_order_id VARCHAR(64) NOT NULL COMMENT '內部訂單ID。',
    fingerprint VARCHAR(512) NOT NULL COMMENT '命令指紋，用來判斷是否重複。',
    completed BOOLEAN NOT NULL COMMENT '是否完成。',
    result_status VARCHAR(64) NULL COMMENT '結果狀態。',
    last_error TEXT NULL COMMENT '最後錯誤。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    PRIMARY KEY (command_id),
    KEY idx_poly_clob_command_order (internal_order_id),
    KEY idx_poly_clob_command_type_completed (command_type, completed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Polymarket CLOB命令冪等紀錄表。';

CREATE TABLE IF NOT EXISTS rpc_transaction_record (
    command_id VARCHAR(128) NOT NULL COMMENT 'RPC命令ID。',
    chain_id VARCHAR(32) NOT NULL COMMENT '鏈ID。',
    transaction_type VARCHAR(64) NOT NULL COMMENT '交易類型。',
    wallet_address VARCHAR(64) NOT NULL COMMENT '錢包地址。',
    fingerprint VARCHAR(512) NOT NULL COMMENT '請求指紋。',
    tx_hash VARCHAR(128) NOT NULL COMMENT '鏈上交易hash。',
    status VARCHAR(32) NOT NULL COMMENT '交易狀態。',
    last_error TEXT NULL COMMENT '最後錯誤。',
    completed BOOLEAN NOT NULL COMMENT '是否完成。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    PRIMARY KEY (command_id),
    KEY idx_rpc_tx_hash (tx_hash),
    KEY idx_rpc_tx_wallet (wallet_address),
    KEY idx_rpc_tx_type_completed (transaction_type, completed),
    KEY idx_rpc_tx_status_updated (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鏈上RPC交易紀錄表。';

CREATE TABLE IF NOT EXISTS prediction_polymarket_user_ws_checkpoint (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID。',
    stream_key VARCHAR(160) NOT NULL COMMENT '使用者WS流key。',
    wallet_address VARCHAR(64) NULL COMMENT '錢包地址。',
    last_event_key VARCHAR(256) NULL COMMENT '最後事件key。',
    last_event_type VARCHAR(64) NULL COMMENT '最後事件類型。',
    last_received_at DATETIME(6) NULL COMMENT '最後收到時間。',
    last_payload LONGTEXT NULL COMMENT '最後事件內容。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    PRIMARY KEY (id),
    UNIQUE KEY uk_poly_user_ws_checkpoint_stream (stream_key),
    KEY idx_poly_user_ws_checkpoint_wallet (wallet_address),
    KEY idx_poly_user_ws_checkpoint_received (last_received_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Polymarket使用者WS檢查點。';

-- ============================================================================
-- 10. Auth / customer registration
-- ============================================================================

CREATE TABLE IF NOT EXISTS app_users (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '使用者ID，也是交易所帳戶uid。',
    email VARCHAR(320) NOT NULL COMMENT '登入email，建議統一轉小寫。',
    password_hash VARCHAR(255) NOT NULL COMMENT '密碼hash，不存明文密碼。',
    status VARCHAR(32) NOT NULL COMMENT '使用者狀態，例如ACTIVE、DISABLED。',
    roles VARCHAR(255) NOT NULL COMMENT '角色清單，會放進JWT。',
    scopes VARCHAR(255) NOT NULL COMMENT '權限範圍清單，會放進JWT。',
    email_verified_at DATETIME(6) NULL COMMENT 'email驗證完成時間，null代表尚未驗證。',
    email_verification_token_hash VARCHAR(64) NULL COMMENT 'email驗證token hash，不存原文token。',
    email_verification_expires_at DATETIME(6) NULL COMMENT 'email驗證token過期時間。',
    preferred_language VARCHAR(16) NOT NULL DEFAULT 'en' COMMENT '使用者偏好語言，例如en、zh-TW、ms、ko。',
    created_at DATETIME(6) NOT NULL COMMENT '註冊完成時間。',
    updated_at DATETIME(6) NOT NULL COMMENT '最後更新時間。',
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_users_email (email),
    KEY idx_app_users_status_created (status, created_at),
    KEY idx_app_users_verification_token (email_verification_token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本地登入使用者表。';

CREATE TABLE IF NOT EXISTS auth_refresh_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'refresh session流水ID。',
    user_id BIGINT NOT NULL COMMENT '使用者ID，對應app_users.id。',
    refresh_token_hash VARCHAR(64) NOT NULL COMMENT 'refresh token的SHA-256 hash，不存原文token。',
    session_id VARCHAR(64) NOT NULL COMMENT '穩定session ID，用於裝置/session管理。',
    expires_at DATETIME(6) NOT NULL COMMENT 'refresh token過期時間。',
    revoked_at DATETIME(6) NULL COMMENT '登出或撤銷時間，null代表未撤銷。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_refresh_sessions_token_hash (refresh_token_hash),
    KEY idx_auth_refresh_sessions_user_created (user_id, created_at),
    KEY idx_auth_refresh_sessions_expires (expires_at),
    CONSTRAINT fk_auth_refresh_sessions_user FOREIGN KEY (user_id) REFERENCES app_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='refresh token session表，用於登出與撤銷。';

CREATE TABLE IF NOT EXISTS customer_registration_requests (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '註冊申請ID，還不是正式uid。',
    email VARCHAR(320) NOT NULL COMMENT '正在註冊的email。',
    password_hash VARCHAR(255) NOT NULL COMMENT '註冊時提交的密碼hash，驗證成功後才轉進app_users。',
    verification_token_hash VARCHAR(64) NOT NULL COMMENT 'email連結token hash，不存原文token。',
    status VARCHAR(32) NOT NULL COMMENT '申請狀態，例如PENDING、VERIFIED、EXPIRED。',
    expires_at DATETIME(6) NOT NULL COMMENT '驗證期限。',
    verified_at DATETIME(6) NULL COMMENT '驗證完成時間。',
    preferred_language VARCHAR(16) NOT NULL DEFAULT 'en' COMMENT '註冊時瀏覽器或使用者選擇的語言。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_registration_token (verification_token_hash),
    KEY idx_customer_registration_email_status_created (email, status, created_at),
    KEY idx_customer_registration_status_expires (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='註冊申請表，email驗證完成後才建立正式使用者。';

CREATE TABLE IF NOT EXISTS customer_verification_codes (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '驗證碼ID。',
    email VARCHAR(320) NOT NULL COMMENT '驗證碼對應email。',
    app_user_id BIGINT NULL COMMENT '正式使用者ID，帳戶已存在時可使用。',
    registration_request_id BIGINT NULL COMMENT '註冊申請ID。',
    code_hash VARCHAR(64) NOT NULL COMMENT '六位數驗證碼hash，不存原文code。',
    status VARCHAR(32) NOT NULL COMMENT '驗證碼狀態，例如PENDING、VERIFIED、EXPIRED。',
    expires_at DATETIME(6) NOT NULL COMMENT '驗證碼過期時間。',
    verified_at DATETIME(6) NULL COMMENT '驗證完成時間。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    PRIMARY KEY (id),
    KEY idx_customer_verification_email_status_created (email, status, created_at),
    KEY idx_customer_verification_registration_status (registration_request_id, status),
    KEY idx_customer_verification_user_status (app_user_id, status),
    KEY idx_customer_verification_status_expires (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='email驗證碼表，註冊與帳戶驗證都可使用。';

-- ============================================================================
-- 11. Fee config audit
-- ============================================================================

CREATE TABLE IF NOT EXISTS fee_config_change_log (
    id VARCHAR(36) NOT NULL COMMENT '手續費異動紀錄ID。',
    symbol VARCHAR(32) NOT NULL COMMENT '交易對。',
    old_maker_fee_rate DECIMAL(38, 18) NOT NULL COMMENT '調整前maker費率。',
    old_taker_fee_rate DECIMAL(38, 18) NOT NULL COMMENT '調整前taker費率。',
    new_maker_fee_rate DECIMAL(38, 18) NOT NULL COMMENT '調整後maker費率。',
    new_taker_fee_rate DECIMAL(38, 18) NOT NULL COMMENT '調整後taker費率。',
    operator_id VARCHAR(128) NOT NULL COMMENT '操作人ID。',
    reason VARCHAR(512) NOT NULL COMMENT '調整原因。',
    request_id VARCHAR(128) NULL COMMENT '請求ID或trace ID。',
    effective_at DATETIME(6) NOT NULL COMMENT '新費率生效時間。',
    changed_at DATETIME(6) NOT NULL COMMENT '操作時間。',
    PRIMARY KEY (id),
    KEY idx_fee_config_symbol_effective (symbol, effective_at),
    KEY idx_fee_config_operator_changed (operator_id, changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='手續費設定異動紀錄，後台每次調整都要留痕。';

-- ============================================================================
-- 12. Message center
-- ============================================================================

CREATE TABLE IF NOT EXISTS message_center_messages (
    id VARCHAR(36) NOT NULL COMMENT '訊息ID。',
    template_code VARCHAR(128) NOT NULL DEFAULT '' COMMENT '模板代碼，沒有模板時可為空字串。',
    title VARCHAR(255) NOT NULL COMMENT '訊息標題。',
    summary LONGTEXT NOT NULL COMMENT '訊息摘要，列表或推播預覽使用。',
    body LONGTEXT NOT NULL COMMENT '訊息正文。',
    category VARCHAR(32) NOT NULL COMMENT '訊息分類。',
    severity VARCHAR(32) NOT NULL COMMENT '嚴重度。',
    action_url VARCHAR(512) NULL COMMENT '點擊後導向連結。',
    action_label VARCHAR(128) NULL COMMENT '按鈕文字。',
    metadata_json LONGTEXT NOT NULL COMMENT '額外資料JSON，空值請由應用寫{}。',
    template_vars_json LONGTEXT NOT NULL COMMENT '模板變數JSON，空值請由應用寫{}。',
    source_user_id BIGINT NULL COMMENT '來源使用者ID。',
    source_event_type VARCHAR(128) NULL COMMENT '來源事件類型。',
    source_event_id VARCHAR(128) NULL COMMENT '來源事件ID。',
    source_event_hash VARCHAR(128) NULL COMMENT '來源事件hash，用於去重。',
    dedupe_key VARCHAR(255) NULL COMMENT '去重key。',
    created_by_subject VARCHAR(255) NOT NULL COMMENT '建立者身份。',
    created_by_type VARCHAR(32) NOT NULL COMMENT '建立者類型，例如SYSTEM、ADMIN。',
    effective_at DATETIME(6) NOT NULL COMMENT '訊息生效時間。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    expire_at DATETIME(6) NULL COMMENT '過期時間。',
    is_scheduled BOOLEAN NOT NULL COMMENT '是否為排程訊息。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    PRIMARY KEY (id),
    KEY idx_msg_center_msg_created_id (created_at, id),
    KEY idx_msg_center_msg_category_created (category, created_at),
    KEY idx_msg_center_msg_expire (expire_at),
    KEY idx_msg_center_msg_dedupe (dedupe_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站內信主表，存訊息內容與來源資訊。';

CREATE TABLE IF NOT EXISTS message_center_message_states (
    uid BIGINT NOT NULL COMMENT '使用者ID。',
    message_id VARCHAR(36) NOT NULL COMMENT '訊息ID。',
    is_read BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已讀。',
    read_at DATETIME(6) NULL COMMENT '已讀時間。',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否刪除，軟刪除。',
    is_archived BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否封存。',
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否置頂。',
    pin_at DATETIME(6) NULL COMMENT '置頂時間。',
    last_notified_at DATETIME(6) NULL COMMENT '最後通知時間。',
    dedupe_key VARCHAR(255) NULL COMMENT '去重key。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    PRIMARY KEY (uid, message_id),
    KEY idx_msg_state_uid_category (uid, is_deleted, is_archived, is_read, message_id),
    KEY idx_msg_state_uid_archived (uid, is_archived, is_deleted, is_read, message_id),
    KEY idx_msg_state_uid_dedupe (uid, dedupe_key),
    KEY idx_msg_state_dedupe (dedupe_key),
    CONSTRAINT fk_msg_state_message FOREIGN KEY (message_id) REFERENCES message_center_messages (id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='使用者訊息狀態表，記錄已讀、刪除、封存、置頂。';

CREATE TABLE IF NOT EXISTS message_center_notification_preferences (
    uid BIGINT NOT NULL COMMENT '使用者ID。',
    category VARCHAR(32) NOT NULL COMMENT '訊息分類。',
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否接收站內信。',
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否接收email。',
    sms_enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否接收簡訊。',
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否接收App推播。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    updated_by VARCHAR(255) NULL COMMENT '更新人。',
    PRIMARY KEY (uid, category),
    KEY idx_msg_pref_uid_category (uid, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='使用者通知偏好設定。';

CREATE TABLE IF NOT EXISTS message_center_announcements (
    id VARCHAR(36) NOT NULL COMMENT '公告ID。',
    title VARCHAR(255) NOT NULL COMMENT '公告標題。',
    summary LONGTEXT NOT NULL COMMENT '公告摘要。',
    category VARCHAR(32) NOT NULL COMMENT '公告分類。',
    severity VARCHAR(32) NOT NULL COMMENT '嚴重度。',
    template_code VARCHAR(128) NULL COMMENT '模板代碼。',
    template_vars_json LONGTEXT NOT NULL COMMENT '模板變數JSON，空值請由應用寫{}。',
    action_url VARCHAR(512) NULL COMMENT '點擊導向連結。',
    action_label VARCHAR(128) NULL COMMENT '按鈕文字。',
    audience_type VARCHAR(32) NOT NULL COMMENT '受眾類型，例如ALL、UID_LIST、SEGMENT。',
    audience_data LONGTEXT NOT NULL COMMENT '受眾條件JSON，空值請由應用寫{}。',
    send_at DATETIME(6) NOT NULL COMMENT '預計發送時間。',
    expire_at DATETIME(6) NULL COMMENT '過期時間。',
    status VARCHAR(32) NOT NULL COMMENT '公告狀態。',
    delivery_mode VARCHAR(32) NOT NULL COMMENT '發送模式。',
    dedupe_key VARCHAR(255) NULL COMMENT '去重key。',
    estimated_recipients BIGINT NOT NULL COMMENT '預估收件人數。',
    sent_count BIGINT NOT NULL COMMENT '已發送數。',
    failed_count BIGINT NOT NULL COMMENT '失敗數。',
    skipped_count BIGINT NOT NULL COMMENT '略過數。',
    created_by_subject VARCHAR(255) NOT NULL COMMENT '建立者身份。',
    created_by_type VARCHAR(32) NOT NULL COMMENT '建立者類型。',
    created_at DATETIME(6) NOT NULL COMMENT '建立時間。',
    updated_at DATETIME(6) NOT NULL COMMENT '更新時間。',
    PRIMARY KEY (id),
    KEY idx_msg_announce_status_send_at (status, send_at),
    KEY idx_msg_announce_category_status (category, status),
    KEY idx_msg_announce_created_at (created_at),
    KEY idx_msg_announce_dedupe (dedupe_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告設定表，發送前的公告草稿與排程資料。';
