-- 合約委託明確保存 LONG／SHORT；BUY／SELL 不得暗示淨倉方向。
ALTER TABLE trading_order_executions ADD COLUMN position_side VARCHAR(5)
    CHECK (position_side IS NULL OR position_side IN ('LONG','SHORT'));
COMMENT ON COLUMN trading_order_executions.position_side IS '永續委託指定的雙向持倉方向；現貨為 NULL。';

-- 重送持倉成交時須讀取原始抵押品結果，不能採用重送請求所帶的數字。
ALTER TABLE futures_hedge_position_events ADD COLUMN resulting_isolated_collateral NUMERIC(36,18)
    NOT NULL DEFAULT 0 CHECK (resulting_isolated_collateral >= 0);
COMMENT ON COLUMN futures_hedge_position_events.resulting_isolated_collateral IS '成交後該方向隔離抵押品數量。';

-- 商品身分與持倉方向在 DB 同時約束，防止直接寫入繞過下單 service。
CREATE FUNCTION validate_trading_order_position() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE kind VARCHAR(16);
BEGIN
    SELECT instrument_kind INTO kind FROM trading_instruments WHERE instrument_id = NEW.instrument_id;
    IF kind = 'PERPETUAL' AND (NEW.position_side IS NULL OR NEW.leverage IS NULL OR NEW.leverage < 1) THEN
        RAISE EXCEPTION 'futures order requires position side and leverage';
    END IF;
    IF kind = 'SPOT' AND (NEW.position_side IS NOT NULL OR NEW.reduce_only OR NEW.leverage IS NOT NULL) THEN
        RAISE EXCEPTION 'spot order cannot carry futures position fields';
    END IF;
    IF TG_OP = 'UPDATE' THEN
        IF NEW.position_side IS DISTINCT FROM OLD.position_side
                OR NEW.reduce_only IS DISTINCT FROM OLD.reduce_only
                OR NEW.leverage IS DISTINCT FROM OLD.leverage
                OR NEW.instrument_id IS DISTINCT FROM OLD.instrument_id THEN
            RAISE EXCEPTION 'order position identity is immutable';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trading_order_position_boundary BEFORE INSERT OR UPDATE OF
    position_side,reduce_only,leverage,instrument_id ON trading_order_executions
    FOR EACH ROW EXECUTE FUNCTION validate_trading_order_position();
