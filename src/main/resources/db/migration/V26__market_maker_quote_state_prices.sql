-- File purpose: preserve the visible price/quantity for the latest market-maker quote legs so client/admin screens can identify maker liquidity without reverse-engineering order ids.
ALTER TABLE market_maker_quote_states
    ADD COLUMN bid_price DECIMAL(38, 18) COMMENT 'Latest accepted bid quote price for this market-maker/symbol state.',
    ADD COLUMN bid_quantity DECIMAL(38, 18) COMMENT 'Latest accepted bid quote quantity shown in the client order book.',
    ADD COLUMN ask_price DECIMAL(38, 18) COMMENT 'Latest accepted ask quote price for this market-maker/symbol state.',
    ADD COLUMN ask_quantity DECIMAL(38, 18) COMMENT 'Latest accepted ask quote quantity shown in the client order book.';
