-- 將 README.md 內的 SQL 區塊抽離為獨立檔案
-- Java21-Exchange Demo schema draft

-- 0) 建議全庫參數
SET time_zone = '+00:00';
SET sql_mode='STRICT_ALL_TABLES,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO';

-- 1) 使用者 / 基礎
-- 使用者帳戶（極簡）
CREATE TABLE user_account (
  uid           BIGINT PRIMARY KEY COMMENT '使用者唯一ID；例：1002001',
  kyc_level     TINYINT NOT NULL DEFAULT 0 COMMENT 'KYC 等級；0=未認證, 1=Lv1, 2=Lv2；例：2',
  status        TINYINT NOT NULL DEFAULT 1 COMMENT '帳戶狀態；1=active, 0=locked；例：1',
  ctime         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間；例：2025-09-29 00:00:00',
  mtime         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最後更新時間；例：2025-09-29 01:23:45'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='使用者基本資料表';

-- 2) 合約/幣對配置（交易規則）
CREATE TABLE symbol_config (
  symbol             VARCHAR(30) PRIMARY KEY COMMENT '合約代碼；例：BTCUSDT-PERP',
  base_asset         VARCHAR(16) NOT NULL COMMENT '標的資產；例：BTC',
  quote_asset        VARCHAR(16) NOT NULL COMMENT '計價資產；例：USDT',
  contract_size      DECIMAL(22,8) NOT NULL COMMENT '每張合約名義；例：1.00000000 (1張=1 USDT 名義/或依品種)',
  price_tick         DECIMAL(22,8) NOT NULL COMMENT '最小價格跳動；例：0.10',
  qty_step           DECIMAL(22,8) NOT NULL COMMENT '下單數量步長；例：0.00100000',
  min_qty            DECIMAL(22,8) NOT NULL COMMENT '最小下單量；例：0.00100000',
  max_leverage       INT NOT NULL COMMENT '最大槓桿；例：125',
  maker_fee          DECIMAL(18,10) NOT NULL DEFAULT 0 COMMENT 'Maker 費率（正數=收費）；例：0.0002 表示 0.02%',
  taker_fee          DECIMAL(18,10) NOT NULL DEFAULT 0 COMMENT 'Taker 費率；例：0.0005 表示 0.05%',
  risk_tiers_json    JSON NOT NULL COMMENT '維持保證金分段；例：[{"notional_upper":50000,"mm_base":0.004,"mm_add":5}]',
  status             TINYINT NOT NULL DEFAULT 1 COMMENT '交易狀態；1=可交易, 0=下架；例：1',
  ctime              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
  mtime              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合約與風控參數';

-- 3) 訂單（冪等：uid+symbol+client_order_id 唯一）
CREATE TABLE `order` (
  order_id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '系統訂單ID；例：789654123',
  uid               BIGINT NOT NULL COMMENT '下單者UID；例：1002001',
  symbol            VARCHAR(30) NOT NULL COMMENT '合約代碼；例：BTCUSDT-PERP',
  side              TINYINT NOT NULL COMMENT '方向；1=BUY, -1=SELL；例：1',
  type              TINYINT NOT NULL COMMENT '訂單型別；1=LIMIT,2=MARKET,3=STOP,4=STOP_MARKET,5=TAKE_PROFIT,6=TP_MARKET,7=POST_ONLY；例：1',
  time_in_force     TINYINT NOT NULL DEFAULT 1 COMMENT '時效；1=GTC,2=IOC,3=FOK；例：1',
  price             DECIMAL(22,8) DEFAULT NULL COMMENT '限價；MARKET 可為 NULL；例：65000.00000000',
  qty               DECIMAL(22,8) NOT NULL COMMENT '委託數量（合約張數或標的數量）；例：0.01000000',
  reduce_only       TINYINT NOT NULL DEFAULT 0 COMMENT '僅減倉；1=是,0=否；例：0',
  post_only         TINYINT NOT NULL DEFAULT 0 COMMENT '僅做Maker；1=是,0=否；例：0',
  trigger_src       TINYINT DEFAULT NULL COMMENT '觸發來源；1=MARK,2=LAST,3=INDEX；止損/止盈單使用；例：1',
  stop_price        DECIMAL(22,8) DEFAULT NULL COMMENT '觸發價格；例：64000.00000000',
  client_order_id   VARCHAR(64) NOT NULL COMMENT '客戶端自定義ID（冪等鍵）；例：stratA-20250929-0001',
  status            TINYINT NOT NULL COMMENT '狀態；0=NEW,1=PARTIALLY_FILLED,2=FILLED,3=CANCELED,4=REJECTED,5=EXPIRED；例：0',
  executed_qty      DECIMAL(22,8) NOT NULL DEFAULT 0 COMMENT '已成交數量；例：0.00500000',
  avg_price         DECIMAL(22,8) DEFAULT NULL COMMENT '成交均價（有成交才更新）；例：64990.00000000',
  maker             TINYINT DEFAULT NULL COMMENT '最後一次成交是否Maker；1=Maker,0=Taker；例：0',
  reject_code       VARCHAR(32) DEFAULT NULL COMMENT '拒單原因碼；例：MARGIN_INSUFF',
  source            VARCHAR(16) DEFAULT NULL COMMENT '下單來源；api/web/app/mm；例：api',
  ctime             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建單時間（伺服器）',
  mtime             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最後更新時間',
  UNIQUE KEY uk_order_idempo (uid, symbol, client_order_id),
  KEY idx_uid_ctime (uid, ctime),
  KEY idx_symbol_ctime (symbol, ctime),
  KEY idx_status (status),
  CONSTRAINT fk_order_uid FOREIGN KEY (uid) REFERENCES user_account(uid),
  CONSTRAINT fk_order_symbol FOREIGN KEY (symbol) REFERENCES symbol_config(symbol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委託主表（冪等鍵：uid+symbol+client_order_id）';

INSERT INTO `order`
(uid, symbol, side, type, time_in_force, price, qty, client_order_id, status, source)
VALUES
(1002001, 'BTCUSDT-PERP', 1, 1, 1, 65000.0, 0.01, 'stratA-20250929-0001', 0, 'api');

-- 4) 成交（撮合事實來源）
CREATE TABLE trade (
  trade_id         BIGINT PRIMARY KEY COMMENT '成交ID（引擎唯一）；例：900000001234',
  match_id         BIGINT NOT NULL COMMENT '撮合事件ID（一次撮合分解多筆時相同）；例：700000000321',
  order_id         BIGINT NOT NULL COMMENT '所屬訂單ID；例：789654123',
  uid              BIGINT NOT NULL COMMENT '成交方UID；例：1002001',
  symbol           VARCHAR(30) NOT NULL COMMENT '合約代碼；例：BTCUSDT-PERP',
  side             TINYINT NOT NULL COMMENT '對該使用者的方向；1=BUY,-1=SELL；例：1',
  price            DECIMAL(22,8) NOT NULL COMMENT '成交價；例：64990.00000000',
  qty              DECIMAL(22,8) NOT NULL COMMENT '成交量；例：0.00500000',
  notional         DECIMAL(28,8) AS (price*qty) STORED COMMENT '名義金額（生成列）；例：324.95',
  maker            TINYINT NOT NULL COMMENT '流動性標記；1=Maker,0=Taker；例：0',
  fee              DECIMAL(22,8) NOT NULL DEFAULT 0 COMMENT '手續費；例：0.16247500',
  fee_asset        VARCHAR(16) NOT NULL DEFAULT 'USDT' COMMENT '手續費資產；例：USDT',
  ctime            TIMESTAMP NOT NULL COMMENT '成交時間（引擎時鐘）；例：2025-09-29 00:00:10',
  KEY idx_order (order_id),
  KEY idx_uid_time (uid, ctime),
  KEY idx_symbol_time (symbol, ctime),
  UNIQUE KEY uk_trade (trade_id),
  UNIQUE KEY uk_match_order (match_id, order_id),
  CONSTRAINT fk_trade_order FOREIGN KEY (order_id) REFERENCES `order`(order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成交明細（撮合唯一事實源）';

INSERT INTO trade
(trade_id, match_id, order_id, uid, symbol, side, price, qty, maker, fee, fee_asset, ctime)
VALUES
(900000001234, 700000000321, 789654123, 1002001, 'BTCUSDT-PERP', 1, 64990.0, 0.005, 0, 0.162475, 'USDT', '2025-09-29 00:00:10');

-- 5) 持倉（Cross/Isolated、單邊/雙邊可擴展）
CREATE TABLE position (
  uid               BIGINT NOT NULL COMMENT 'UID；例：1002001',
  symbol            VARCHAR(30) NOT NULL COMMENT '合約；例：BTCUSDT-PERP',
  side              TINYINT NOT NULL COMMENT '倉位方向；1=LONG,-1=SHORT；例：1',
  qty               DECIMAL(22,8) NOT NULL DEFAULT 0 COMMENT '當前倉位數量；例：0.01000000',
  entry_price       DECIMAL(22,8) DEFAULT NULL COMMENT '加權進場均價；例：65010.00000000',
  leverage          INT NOT NULL DEFAULT 1 COMMENT '槓桿；例：20',
  margin_mode       TINYINT NOT NULL DEFAULT 1 COMMENT '保證金模式；1=Cross(全倉),2=Isolated(逐倉)；例：1',
  isolated_margin   DECIMAL(22,8) DEFAULT 0 COMMENT '逐倉保證金；全倉為0；例：50.00000000',
  upnl              DECIMAL(22,8) DEFAULT 0 COMMENT '未實現盈虧（標記價計算）；例：-1.23450000',
  rpnl              DECIMAL(22,8) DEFAULT 0 COMMENT '已實現盈虧；例：2.34560000',
  maint_margin_req  DECIMAL(22,8) DEFAULT 0 COMMENT '維持保證金需求快取；例：12.34560000',
  mtime             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
  PRIMARY KEY (uid, symbol, side),
  KEY idx_symbol (symbol),
  CONSTRAINT fk_pos_uid FOREIGN KEY (uid) REFERENCES user_account(uid),
  CONSTRAINT fk_pos_symbol FOREIGN KEY (symbol) REFERENCES symbol_config(symbol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='持倉狀態（均價/盈虧/維持保證金）';

UPDATE position
SET qty=0.01000000, entry_price=65010.0, leverage=20, upnl=-1.23, rpnl=2.34, maint_margin_req=12.34
WHERE uid=1002001 AND symbol='BTCUSDT-PERP' AND side=1;

-- 6) 錢包賬戶 & 帳本（雙式簿記）
CREATE TABLE wallet_account (
  uid           BIGINT NOT NULL COMMENT 'UID；例：1002001',
  asset         VARCHAR(16) NOT NULL COMMENT '資產；例：USDT',
  available     DECIMAL(30,10) NOT NULL DEFAULT 0 COMMENT '可用餘額；例：1000.0000000000',
  hold          DECIMAL(30,10) NOT NULL DEFAULT 0 COMMENT '凍結/預扣；例：50.0000000000',
  mtime         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
  PRIMARY KEY (uid, asset),
  CONSTRAINT fk_wacct_uid FOREIGN KEY (uid) REFERENCES user_account(uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='資產餘額快取（來源：ledger 聚合）';

CREATE TABLE wallet_ledger (
  ledger_id      BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '帳本流水ID',
  uid            BIGINT NOT NULL COMMENT 'UID；例：1002001',
  asset          VARCHAR(16) NOT NULL COMMENT '資產；例：USDT',
  change_amount  DECIMAL(30,10) NOT NULL COMMENT '本次變動金額；可正負；例：-10.5000000000',
  balance_after  DECIMAL(30,10) NOT NULL COMMENT '變動後餘額；例：989.5000000000',
  reason         VARCHAR(32) NOT NULL COMMENT '原因碼；order_reserve,reserve_release,trade_fee,funding,liquidation,rebate 等',
  ref_id         VARCHAR(64) NOT NULL COMMENT '關聯ID；如 orderId/tradeId/fundingPeriod/liqId；例："789654123"',
  extra          JSON DEFAULT NULL COMMENT '補充資訊；例：{"symbol":"BTCUSDT-PERP","side":"BUY"}',
  ctime          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入帳時間（UTC）',
  KEY idx_uid_time (uid, ctime),
  KEY idx_reason_ref (reason, ref_id),
  CONSTRAINT fk_wledger_uid FOREIGN KEY (uid) REFERENCES user_account(uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='資金流水（不可修改，對賬用）';

INSERT INTO wallet_ledger(uid, asset, change_amount, balance_after, reason, ref_id, extra)
VALUES (1002001,'USDT', -50.0, 950.0, 'order_reserve', '789654123', '{"symbol":"BTCUSDT-PERP"}');

INSERT INTO wallet_ledger(uid, asset, change_amount, balance_after, reason, ref_id)
VALUES (1002001,'USDT', +38.5, 988.5, 'reserve_release', '789654123');

INSERT INTO wallet_ledger(uid, asset, change_amount, balance_after, reason, ref_id, extra)
VALUES (1002001,'USDT', -0.162475, 988.337525, 'trade_fee', '900000001234', '{"maker":0}');

-- 7) 資金費（歷史與結算）
CREATE TABLE funding_history (
  symbol         VARCHAR(30) NOT NULL COMMENT '合約；例：BTCUSDT-PERP',
  period_start   TIMESTAMP NOT NULL COMMENT '計費期起；例：2025-09-29 00:00:00',
  period_end     TIMESTAMP NOT NULL COMMENT '計費期止；例：2025-09-29 08:00:00',
  rate           DECIMAL(18,10) NOT NULL COMMENT '資金費率；例：0.0001000000 (0.01%)',
  status         TINYINT NOT NULL DEFAULT 1 COMMENT '狀態；1=有效,0=作廢/回滾',
  ctime          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成時間',
  PRIMARY KEY (symbol, period_end),
  KEY idx_period (period_start, period_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='資金費率歷史（每期一筆）';

CREATE TABLE funding_settlement (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '流水ID',
  uid            BIGINT NOT NULL COMMENT 'UID；例：1002001',
  symbol         VARCHAR(30) NOT NULL COMMENT '合約；例：BTCUSDT-PERP',
  period_end     TIMESTAMP NOT NULL COMMENT '對應的結算期止；例：2025-09-29 08:00:00',
  rate           DECIMAL(18,10) NOT NULL COMMENT '當期費率；例：0.0001',
  position_qty   DECIMAL(22,8) NOT NULL COMMENT '當期持倉量（取結算快照）；例：0.01000000',
  mark_price     DECIMAL(22,8) NOT NULL COMMENT '結算時標記價；例：64800.00000000',
  amount         DECIMAL(22,8) NOT NULL COMMENT '資金費金額；多付空收或反之；例：-0.06480000',
  ctime          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '結算時間',
  KEY idx_user_period (uid, period_end),
  KEY idx_symbol_period (symbol, period_end),
  CONSTRAINT fk_funding_uid FOREIGN KEY (uid) REFERENCES user_account(uid),
  CONSTRAINT fk_funding_symbol FOREIGN KEY (symbol) REFERENCES symbol_config(symbol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='使用者資金費結算流水';

INSERT INTO funding_history(symbol, period_start, period_end, rate)
VALUES ('BTCUSDT-PERP','2025-09-29 00:00:00','2025-09-29 08:00:00',0.0001);

INSERT INTO funding_settlement(uid, symbol, period_end, rate, position_qty, mark_price, amount)
VALUES (1002001,'BTCUSDT-PERP','2025-09-29 08:00:00',0.0001,0.01,64800.0,-0.0648);

INSERT INTO wallet_ledger(uid, asset, change_amount, balance_after, reason, ref_id, extra)
VALUES (1002001,'USDT', -0.0648, 988.272725, 'funding', 'BTCUSDT-PERP@2025-09-29 08:00', '{"rate":0.0001}');

-- 8) 強平 / ADL / 保險基金
CREATE TABLE liquidation_event (
  liq_id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '強平事件ID',
  uid             BIGINT NOT NULL COMMENT '被強平用戶；例：1002001',
  symbol          VARCHAR(30) NOT NULL COMMENT '合約；例：BTCUSDT-PERP',
  reason          VARCHAR(16) NOT NULL COMMENT '觸發原因；MM_BREACH/ADL',
  stage           VARCHAR(16) NOT NULL COMMENT '階段；PARTIAL/FULL/ADL',
  protected_price DECIMAL(22,8) DEFAULT NULL COMMENT '保護價；例：64500.00000000',
  filled_qty      DECIMAL(22,8) NOT NULL DEFAULT 0 COMMENT '已平倉量；例：0.01000000',
  insurance_used  DECIMAL(22,8) NOT NULL DEFAULT 0 COMMENT '動用保險基金金額；例：5.00000000',
  adl_counter_uid BIGINT DEFAULT NULL COMMENT '若ADL，被動減倉對手UID；例：1003002',
  ctime           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件時間',
  KEY idx_user_time (uid, ctime),
  KEY idx_symbol_time (symbol, ctime),
  CONSTRAINT fk_liq_uid FOREIGN KEY (uid) REFERENCES user_account(uid),
  CONSTRAINT fk_liq_symbol FOREIGN KEY (symbol) REFERENCES symbol_config(symbol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='強平/ADL 事件表';

CREATE TABLE insurance_fund_ledger (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '流水ID',
  symbol         VARCHAR(30) NOT NULL COMMENT '合約；例：BTCUSDT-PERP',
  change_amount  DECIMAL(22,8) NOT NULL COMMENT '變動；正=注入, 負=支出；例：+10.00000000',
  reason         VARCHAR(16) NOT NULL COMMENT '原因；FUNDING_DIFF/LIQ_SURPLUS/LIQ_DEFICIT/ADL',
  ref_id         VARCHAR(64) NOT NULL COMMENT '關聯ID；例：liq_id 或 funding period',
  ctime          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入帳時間',
  KEY idx_symbol_time (symbol, ctime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保險基金收支流水';

INSERT INTO liquidation_event(uid, symbol, reason, stage, protected_price, filled_qty, insurance_used)
VALUES (1002001,'BTCUSDT-PERP','MM_BREACH','FULL',64500.0,0.01,2.5);

INSERT INTO insurance_fund_ledger(symbol, change_amount, reason, ref_id)
VALUES ('BTCUSDT-PERP', -2.5, 'LIQ_DEFICIT', 'liq:12345');

-- 9) Snapshot / Outbox（可靠投遞 & 恢復）
CREATE TABLE outbox_event (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Outbox事件ID',
  aggregate_type VARCHAR(32) NOT NULL COMMENT '聚合種類；order/trade/position 等',
  aggregate_id   VARCHAR(64) NOT NULL COMMENT '聚合ID；例：orderId=789654123',
  event_type     VARCHAR(32) NOT NULL COMMENT '事件名；order.created 等',
  payload        JSON NOT NULL COMMENT '事件內容（序列化）',
  headers        JSON DEFAULT NULL COMMENT '追蹤/版本/冪等鍵等',
  published      TINYINT NOT NULL DEFAULT 0 COMMENT '是否已投遞；0=尚未,1=已投',
  ctime          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '寫入時間'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Outbox 事件暫存（確保不丟/不重）';

CREATE TABLE snapshot_meta (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '快照ID',
  snapshot_type  VARCHAR(32) NOT NULL COMMENT '種類；orderbook/positions/wallet/kafka_offset',
  scope_key      VARCHAR(64) NOT NULL COMMENT '範圍鍵；例：symbol=BTCUSDT-PERP 或 全域',
  min_event_ts   TIMESTAMP NOT NULL COMMENT '涵蓋事件最小時間',
  max_event_ts   TIMESTAMP NOT NULL COMMENT '涵蓋事件最大時間',
  location       VARCHAR(256) NOT NULL COMMENT '快照存放位置；例：s3://bucket/path/file.gz',
  checksum       VARCHAR(64) NOT NULL COMMENT '校驗碼；例：sha256...',
  ctime          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
  KEY idx_type_scope (snapshot_type, scope_key),
  KEY idx_time (max_event_ts)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='快照索引（資料本體放物件儲存）';
