-- 報價預覽版本屬不可變政策證據；更正只能新增下一版，不能覆寫舊版及既有稽核。
CREATE TRIGGER immutable_external_quote_preview_policies
    BEFORE UPDATE OR DELETE ON external_quote_preview_policies
    FOR EACH ROW EXECUTE FUNCTION prevent_trading_evidence_mutation();

COMMENT ON TABLE external_quote_preview_policies IS '最高管理員逐版追加的外部報價預覽設定；舊版不可修改，且沒有交易啟用效力。';
