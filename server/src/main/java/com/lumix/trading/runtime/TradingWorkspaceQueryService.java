package com.lumix.trading.runtime;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 客戶交易畫面只從 primary 讀取真實資金、成交及 LumiX 自有委託簿；不借用外部深度。 */
@Service
public class TradingWorkspaceQueryService {
    private final JdbcTemplate jdbc;
    public TradingWorkspaceQueryService(JdbcTemplate jdbc){this.jdbc=jdbc;}

    /** 單一 REPEATABLE_READ 快照讓可用、鎖定及自有簿不混合兩次成交前後的狀態。 */
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Workspace snapshot(String owner,String kind,String market) {
        String type=TradingPolicyRepository.kind(kind),accountType="spot".equals(kind)?"SPOT":"FUTURES";
        if(market==null||!market.matches("[A-Z0-9][A-Z0-9_-]{0,31}"))throw new IllegalArgumentException("invalid market");
        List<String> accounts=jdbc.query("SELECT account_id FROM accounts WHERE user_id=? AND account_type=? AND account_category='USER'",(r,n)->r.getString(1),owner,accountType);
        if(accounts.size()!=1)throw new NoSuchElementException("trading account not found");
        String account=accounts.getFirst();
        List<Balance> balances=jdbc.query("SELECT aa.asset_symbol,a.status AS account_status,aa.status AS asset_status,p.total_amount,p.available_amount,p.locked_amount,p.projected_at AS updated_at "
                +"FROM account_assets aa JOIN accounts a ON a.account_id=aa.account_id LEFT JOIN balance_projections p ON p.account_id=aa.account_id AND p.asset_symbol=aa.asset_symbol "
                +"WHERE aa.account_id=? ORDER BY aa.asset_symbol",(r,n)->new Balance(r.getString("asset_symbol"),r.getString("account_status"),r.getString("asset_status"),
                decimal(r.getBigDecimal("total_amount")),decimal(r.getBigDecimal("available_amount")),decimal(r.getBigDecimal("locked_amount")),r.getTimestamp("updated_at")==null?null:r.getTimestamp("updated_at").toInstant()),account);
        List<Fill> fills=jdbc.query("SELECT t.trade_id,t.price,t.quantity,t.quote_quantity,t.fee_asset_symbol,t.created_at,"
                +"CASE WHEN t.maker_account_id=? THEN t.maker_fee_amount ELSE t.taker_fee_amount END AS fee,"
                +"CASE WHEN t.maker_account_id=? THEN m.side ELSE k.side END AS side "
                +"FROM trades t JOIN trading_trade_executions e ON e.trade_id=t.trade_id JOIN trading_instruments i ON i.instrument_id=e.instrument_id "
                +"JOIN orders m ON m.order_id=t.maker_order_id JOIN orders k ON k.order_id=t.taker_order_id "
                +"WHERE (t.maker_account_id=? OR t.taker_account_id=?) AND i.instrument_kind=? AND t.market_symbol=? ORDER BY t.created_at DESC,t.trade_id DESC LIMIT 100",
                (r,n)->new Fill(r.getString("trade_id"),r.getString("side"),decimal(r.getBigDecimal("price")),decimal(r.getBigDecimal("quantity")),decimal(r.getBigDecimal("quote_quantity")),decimal(r.getBigDecimal("fee")),r.getString("fee_asset_symbol"),r.getTimestamp("created_at").toInstant()),account,account,account,account,type,market);
        List<Position> positions="futures".equals(kind) ? jdbc.query(
                "SELECT p.position_side,p.quantity,p.entry_notional_usdt,p.collateral_asset_symbol,"
                + "p.isolated_collateral FROM futures_hedge_positions p "
                + "JOIN trading_instruments i ON i.instrument_id=p.instrument_id "
                + "WHERE p.account_id=? AND i.market_symbol=? AND p.quantity>0 "
                + "ORDER BY p.position_side",(r,n)->new Position(r.getString("position_side"),
                decimal(r.getBigDecimal("quantity")),decimal(r.getBigDecimal("entry_notional_usdt")),
                r.getString("collateral_asset_symbol"),decimal(r.getBigDecimal("isolated_collateral"))),
                account,market) : List.of();
        return new Workspace(Instant.now(),balances,depth(type,market,"BUY"),depth(type,market,"SELL"),fills,positions);
    }

    private List<Level> depth(String kind,String market,String side) {
        return jdbc.query("SELECT o.price,SUM(o.remaining_quantity) AS quantity FROM orders o JOIN trading_order_executions e ON e.order_id=o.order_id "
                +"JOIN trading_instruments i ON i.instrument_id=e.instrument_id JOIN accounts a ON a.account_id=o.account_id JOIN users u ON u.user_id=o.user_id "
                +"WHERE i.instrument_kind=? AND i.market_symbol=? AND o.side=? AND o.status IN ('OPEN','PARTIALLY_FILLED') "
                +"AND a.status='ACTIVE' AND u.status='ACTIVE' GROUP BY o.price ORDER BY o.price "+("BUY".equals(side)?"DESC":"ASC")+" LIMIT 20",
                (r,n)->new Level(decimal(r.getBigDecimal("price")),decimal(r.getBigDecimal("quantity"))),kind,market,side);
    }
    private static String decimal(BigDecimal value){return value==null?null:value.stripTrailingZeros().toPlainString();}
    /** 無 projection 時明確回 null，不能把缺少資金證據包裝成固定零餘額。 */
    public record Balance(String asset,String accountStatus,String assetStatus,String total,String available,String locked,Instant updatedAt){}
    /** 自有委託只揭露彙總價格與數量，不揭露其他客戶委託識別。 */
    public record Level(String price,String quantity){}
    /** 只回本人成交方向與本人費用，不揭露對手方帳戶。 */
    public record Fill(String tradeId,String side,String price,String quantity,String notional,String fee,String feeAsset,Instant createdAt){}
    /** 所有金額使用 decimal string；前端必須以 receivedAt 管理過期及斷線呈現。 */
    public record Position(String positionSide,String quantity,String entryNotionalUsdt,
                           String collateralAsset,String isolatedCollateral){}
    public record Workspace(Instant receivedAt,List<Balance> balances,List<Level> bids,List<Level> asks,
                            List<Fill> fills,List<Position> positions){}
}
