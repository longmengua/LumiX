-- Generated from uploaded Polymarket markets JSON
-- Table: prediction_market_sync_key

INSERT INTO prediction_market_sync_key
(event_slug, event_title, team_a, team_b, event_date, source, sync_enabled)
VALUES
('fifwc-mex-rsa-2026-06-11', 'Mexico vs. South Africa', 'Mexico', 'South Africa', '2026-06-11', 'POLYMARKET', 1),
('fifwc-kr-cze-2026-06-11', 'Korea Republic vs. Czechia', 'South Korea', 'Czechia', '2026-06-11', 'POLYMARKET', 1),
('fifwc-can-bih-2026-06-12', 'Canada vs. Bosnia and Herzegovina', 'Canada', 'Bosnia and Herzegovina', '2026-06-12', 'POLYMARKET', 1),
('fifwc-usa-par-2026-06-12', 'United States vs. Paraguay', 'USA', 'Paraguay', '2026-06-12', 'POLYMARKET', 1),
('fifwc-qat-che-2026-06-13', 'Qatar vs. Switzerland', 'Qatar', 'Switzerland', '2026-06-12', 'POLYMARKET', 1),
('fifwc-aus-tur-2026-06-14', 'Australia vs. Türkiye', 'Australia', 'Türkiye', '2026-06-12', 'POLYMARKET', 1),
('fifwc-bra-mar-2026-06-13', 'Brazil vs. Morocco', 'Brazil', 'Morocco', '2026-06-13', 'POLYMARKET', 1),
('fifwc-hai-sco-2026-06-13', 'Haiti vs. Scotland', 'Haiti', 'Scotland', '2026-06-13', 'POLYMARKET', 1),
('fifwc-ger-kor-2026-06-14', 'Germany vs. Curaçao', 'Germany', 'Curacao', '2026-06-13', 'POLYMARKET', 1),
('fifwc-nld-jpn-2026-06-14', 'Netherlands vs. Japan', 'Netherlands', 'Japan', '2026-06-14', 'POLYMARKET', 1),
('fifwc-swe-tun-2026-06-14', 'Sweden vs. Tunisia', 'Sweden', 'Tunisia', '2026-06-14', 'POLYMARKET', 1),
('fifwc-ksa-ury-2026-06-15', 'Saudi Arabia vs. Uruguay', 'Saudi Arabia', 'Uruguay', '2026-06-14', 'POLYMARKET', 1),
('fifwc-bel-egy-2026-06-15', 'Belgium vs. Egypt', 'Belgium', 'Egypt', '2026-06-15', 'POLYMARKET', 1),
('fifwc-irn-nzl-2026-06-15', 'IR Iran vs. New Zealand', 'Iran', 'New Zealand', '2026-06-15', 'POLYMARKET', 1),
('fifwc-fra-sen-2026-06-16', 'France vs. Senegal', 'France', 'Senegal', '2026-06-16', 'POLYMARKET', 1),
('fifwc-irq-nor-2026-06-16', 'Iraq vs. Norway', 'Norway', 'Iraq', '2026-06-16', 'POLYMARKET', 1),
('fifwc-arg-alg-2026-06-16', 'Argentina vs. Algeria', 'Argentina', 'Algeria', '2026-06-16', 'POLYMARKET', 1),
('fifwc-aut-jor-2026-06-17', 'Austria vs. Jordan', 'Austria', 'Jordan', '2026-06-16', 'POLYMARKET', 1),
('fifwc-uzb-col-2026-06-17', 'Uzbekistan vs. Colombia', 'Colombia', 'Uzbekistan', '2026-06-17', 'POLYMARKET', 1),
('fifwc-eng-hrv-2026-06-17', 'England vs. Croatia', 'England', 'Croatia', '2026-06-17', 'POLYMARKET', 1),
('fifwc-gha-pan-2026-06-17', 'Ghana vs. Panama', 'Ghana', 'Panama', '2026-06-17', 'POLYMARKET', 1)
ON DUPLICATE KEY UPDATE
    event_title = VALUES(event_title),
    team_a = VALUES(team_a),
    team_b = VALUES(team_b),
    event_date = VALUES(event_date),
    source = VALUES(source),
    sync_enabled = VALUES(sync_enabled),
    updated_at = CURRENT_TIMESTAMP;
