-- 無流動性或保險基金不足時，保留結算與減倉命令，但停止開倉。
ALTER TABLE trading_instruments DROP CONSTRAINT trading_instruments_status_check;
ALTER TABLE trading_instruments ADD CONSTRAINT trading_instruments_status_check
    CHECK (status IN ('ACTIVE','REDUCE_ONLY','HALTED','CLOSED'));
COMMENT ON COLUMN trading_instruments.status IS 'REDUCE_ONLY 保留既有持倉處置與 funding，但拒絕新增風險。';
