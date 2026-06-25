import { formatAmount } from '../../utils/format';

export function AmountText({ value }: { value: number }) {
  return <span>{formatAmount(value)}</span>;
}

