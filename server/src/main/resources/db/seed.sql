-- LumiX idempotent initial configuration data.
-- Schema definitions, indexes and constraints must remain in init.sql/Flyway.

INSERT INTO assets (asset_symbol, display_name, precision_scale, status) VALUES
    ('USDT', 'Tether USD', 6, 'ACTIVE'),
    ('BTC', 'Bitcoin', 8, 'ACTIVE'),
    ('ETH', 'Ethereum', 8, 'ACTIVE')
ON CONFLICT (asset_symbol) DO NOTHING;

INSERT INTO markets (market_symbol, base_asset_symbol, quote_asset_symbol, price_scale, quantity_scale, status) VALUES
    ('BTC-USDT', 'BTC', 'USDT', 8, 8, 'ACTIVE'),
    ('ETH-USDT', 'ETH', 'USDT', 8, 8, 'ACTIVE')
ON CONFLICT (market_symbol) DO NOTHING;

INSERT INTO users (user_id, email, display_name, status) VALUES
    ('system:airdrop', 'system-airdrop@lumix.internal', '系統空投帳戶', 'SUSPENDED'),
    ('system:asset-adjustment', 'system-asset-adjustment@lumix.internal', '系統資產調整對應帳戶', 'SUSPENDED'),
    ('system:withdrawal-custody', 'system-withdrawal-custody@lumix.internal', '系統提款 custody 對應帳戶', 'SUSPENDED'),
    ('admin-local-default', 'waltor.huang@gmail.com', '最高管理員', 'ACTIVE')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO accounts (account_id, user_id, account_type, status, account_category, account_purpose) VALUES
    ('system:airdrop:spot', 'system:airdrop', 'SPOT', 'ACTIVE', 'EXCHANGE', 'AIRDROP_FUNDING'),
    ('system:asset-adjustment:spot', 'system:asset-adjustment', 'SPOT', 'ACTIVE', 'EXCHANGE', 'ASSET_ADJUSTMENT_COUNTERPARTY'),
    ('system:withdrawal-custody:spot', 'system:withdrawal-custody', 'SPOT', 'ACTIVE', 'EXCHANGE', 'WITHDRAWAL_CUSTODY')
ON CONFLICT (account_id) DO NOTHING;

-- Local seed credential only. The plaintext password is never stored in SQL;
-- changing it through the normal account flow replaces this BCrypt hash.
INSERT INTO user_credentials (user_id, password_hash, password_algorithm)
VALUES ('admin-local-default', '$2a$12$urfhFlWK7Q3WgcYBi4M4ueeTOAK..OuSTOJSRIXagyLE0cn8FMT.e', 'BCRYPT')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO admin_principals (user_id, role, status, activation_requested_at, activated_at)
VALUES ('admin-local-default', 'SUPER_ADMIN', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO external_order_proxy_settings (setting_key, enabled, version, updated_by)
VALUES ('LUMIX_TRADING_EXTERNAL_PROXY_DEMO_ENABLED', TRUE, 1, NULL)
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO trading_runtime_settings (trading_kind, enabled, version, updated_by) VALUES
    ('SPOT', FALSE, 1, NULL),
    ('FUTURES', FALSE, 1, NULL)
ON CONFLICT (trading_kind) DO NOTHING;
