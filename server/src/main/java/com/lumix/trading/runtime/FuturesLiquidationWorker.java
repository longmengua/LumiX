package com.lumix.trading.runtime;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 已啟用合約的逐倉風險巡檢；每次執行由結算服務重讀標記價與持倉。 */
@Component
@Profile("infrastructure")
public class FuturesLiquidationWorker {
    private static final Logger log=LoggerFactory.getLogger(FuturesLiquidationWorker.class);
    private final JdbcTemplate jdbc;
    private final FuturesTradingExecutionService trading;
    private final FuturesInstrumentStatusService statuses;
    private final TradingRuntimeSettingsService settings;

    public FuturesLiquidationWorker(JdbcTemplate jdbc,FuturesTradingExecutionService trading,
            FuturesInstrumentStatusService statuses,TradingRuntimeSettingsService settings){
        this.jdbc=jdbc;this.trading=trading;this.statuses=statuses;this.settings=settings;
    }

    private record Candidate(String owner,String market,String side,String position){}

    @Scheduled(fixedDelayString="${LUMIX_FUTURES_LIQUIDATION_POLL_DELAY:PT5S}")
    public void scan(){
        if(!settings.enabled("futures"))return;
        List<Candidate> positions=jdbc.query("SELECT a.user_id,i.market_symbol,p.position_side,"
                + "p.account_id FROM futures_hedge_positions p "
                + "JOIN accounts a ON a.account_id=p.account_id "
                + "JOIN trading_instruments i ON i.instrument_id=p.instrument_id "
                + "WHERE p.quantity>0 AND i.status IN ('ACTIVE','REDUCE_ONLY') "
                + "AND i.instrument_kind='PERPETUAL' "
                + "ORDER BY p.account_id,i.instrument_id,p.position_side",
                (r,n)->new Candidate(r.getString(1),r.getString(2),r.getString(3),r.getString(4)));
        for(var position:positions){
            String bucket=Long.toString(java.time.Instant.now().getEpochSecond()/5);
            String key="liquidation:"+TradingExecutionSupport.hash(position.position()+":"
                    +position.market()+":"+position.side()+":"+bucket);
            try {trading.liquidate(position.owner(),position.market(),position.side(),key);}
            catch(TradingOrderRejectedException expected){
                String reason=switch(java.util.Objects.toString(expected.getMessage(),"")){
                    case "market order needs real futures liquidity" -> "NO_LIQUIDITY";
                    case "insurance fund lacks settled USDT backing" -> "INSURANCE_SHORTFALL";
                    case "margin pool lacks settled USDT backing" -> "MARGIN_POOL_SHORTFALL";
                    default -> null;
                };
                if(reason!=null) {
                    try {statuses.riskHalt(position.market(),reason);}
                    catch(RuntimeException failure){log.error("Futures risk halt failed for {}",position.market(),failure);}
                }
                else if (!"position is above maintenance margin".equals(expected.getMessage()))
                    log.warn("Futures liquidation delayed for {} {}: {}",position.market(),
                            position.position(),expected.getMessage());
            }
            catch(RuntimeException failure){
                log.error("Futures liquidation failed for {} {}",position.market(),position.position(),failure);
            }
        }
    }
}
