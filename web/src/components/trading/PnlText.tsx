import { formatCurrency } from '../../utils/format';

export function PnlText({ value }: { value: number }) {
  const tone = value > 0 ? 'positive' : value < 0 ? 'negative' : 'neutral';
  return <span className={`pnl pnl--${tone}`}>{formatCurrency(value)}</span>;
}

