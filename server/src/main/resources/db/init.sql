-- LumiX PostgreSQL schema bootstrap entry point.
--
-- This file is intended for an empty database initialized directly with psql.
-- The application normally uses Flyway, whose versioned files remain the
-- authoritative schema history. Data and configurable defaults belong in
-- seed.sql and must not be added here.

\set ON_ERROR_STOP on

\ir migration/V001__baseline.sql
