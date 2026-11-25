-- LumiX PostgreSQL schema bootstrap entry point.
--
-- This file is intended for an empty database initialized directly with psql.
-- The application normally uses Flyway, whose versioned files remain the
-- authoritative schema history. Data and configurable defaults belong in
-- seed.sql and must not be added here.

\set ON_ERROR_STOP on

\ir migration/V001__baseline.sql
\ir migration/V002__withdrawal_platform_runtime.sql
\ir migration/V003__withdrawal_runtime_recovery_evidence.sql
\ir migration/V004__trading_execution.sql
\ir migration/V005__futures_hedge_positions.sql
\ir migration/V006__futures_risk_policies.sql
\ir migration/V007__futures_order_position_side.sql
\ir migration/V008__futures_command_idempotency.sql
\ir migration/V009__futures_funding_settlement.sql
\ir migration/V010__futures_liquidation_evidence.sql
\ir migration/V011__futures_reduce_only_risk_halt.sql
\ir migration/V012__external_quote_preview_policy.sql
\ir migration/V013__immutable_external_quote_preview_policy.sql
\ir migration/V014__external_order_proxy_settings.sql
\ir migration/V015__trading_runtime_settings.sql
