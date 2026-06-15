/**
 * Exchange screen data contracts and constants.
 * Keep types here so app/container and components share stable shapes
 * and we can update API contracts in one place.
 */

export type DecimalLike = string | number | null | undefined;

export type Side = 'BUY' | 'SELL';
export type KlineInterval = '1m' | '5m' | '15m' | '1h';

export type WsEvent =
  | 'subscribed.market'
  | 'unsubscribed.market'
  | 'order.lifecycle'
  | 'trade'
  | 'ticker'
  | 'gateway.heartbeat'
  | 'error'
  | (string & {});

export type ConnStatus = 'idle' | 'connecting' | 'open' | 'reconnecting' | 'closed' | 'error';

export interface ApiResponse<T> {
  ok: boolean;
  data: T;
  error?: string | null;
}

export interface RawTrade {
  symbol: string;
  matchId: string;
  orderId: string;
  side: Side;
  price: DecimalLike;
  qty: DecimalLike;
  maker: boolean;
  ts: string;
}

export interface RawTicker {
  symbol: string;
  lastPrice?: DecimalLike;
  bestBid?: DecimalLike;
  bestAsk?: DecimalLike;
  volume24h?: DecimalLike;
  high24h?: DecimalLike;
  low24h?: DecimalLike;
  updatedAt?: string;
}

export interface RawKline {
  symbol: string;
  interval: string;
  openTime: string;
  open: DecimalLike;
  high: DecimalLike;
  low: DecimalLike;
  close: DecimalLike;
  volume: DecimalLike;
}

export interface WsEnvelope {
  event: WsEvent;
  data: unknown;
}

export interface MarketTickerState {
  symbol: string;
  lastPrice: number | null;
  bestBid: number | null;
  bestAsk: number | null;
  volume24h: number | null;
  high24h: number | null;
  low24h: number | null;
  updatedAt: string | null;
}

export interface RawPerpetualContract {
  symbol: string;
  contractType: string;
  baseAsset: string;
  quoteAsset: string;
  contractSize: DecimalLike;
  indexPrice: DecimalLike;
  markPrice: DecimalLike;
  fundingRate: DecimalLike;
  nextFundingTime: string;
  maxLeverage: number;
  defaultLeverage: number;
  marginMode: string;
  initialMarginRate: DecimalLike;
  maintenanceMarginRate: DecimalLike;
  estimatedLiquidationPrice?: DecimalLike;
  status: string;
  updatedAt: string;
}

export interface PerpetualContractState {
  symbol: string;
  contractType: string;
  baseAsset: string;
  quoteAsset: string;
  contractSize: number;
  indexPrice: number | null;
  markPrice: number | null;
  fundingRate: number | null;
  nextFundingTime: string | null;
  maxLeverage: number;
  defaultLeverage: number;
  marginMode: string;
  initialMarginRate: number | null;
  maintenanceMarginRate: number | null;
  estimatedLiquidationPrice: number | null;
  status: string;
  updatedAt: string | null;
}

export interface TradeRow {
  matchId: string;
  time: string;
  side: Side;
  price: string;
  qty: string;
  notional: string;
  tsKey: number;
}

export interface KlineRow {
  openTime: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export interface TradeFormState {
  side: Side;
  type: 'LIMIT' | 'MARKET';
  price: string;
  qty: string;
}

export interface PositionRow {
  market: string;
  side: 'LONG' | 'SHORT';
  qty: string;
  entry: string;
  margin: string;
  pnl: string;
}

export const DEFAULT_SYMBOL = 'BTCUSDT';
export const KLINE_INTERVALS: KlineInterval[] = ['1m', '5m', '15m', '1h'];
export const MAX_CANDLE_HISTORY = 120;
export const MAX_CHART_CANDLES = 84;
export const MAX_TRADE_ROWS = 200;
export const TRADE_PAGE_SIZE = 20;

export const SAMPLE_POSITIONS: PositionRow[] = [
  { market: 'BTCUSDT', side: 'LONG', qty: '0.45', entry: '99,840.12', margin: '2,100.00', pnl: '+3.22%' },
  { market: 'ETHUSDT', side: 'SHORT', qty: '1.7', entry: '2,740.40', margin: '1,200.00', pnl: '-0.74%' }
];

export const DEFAULT_ORDER_FORM: TradeFormState = {
  side: 'BUY',
  type: 'LIMIT',
  price: '100000.00',
  qty: '0.01'
};
