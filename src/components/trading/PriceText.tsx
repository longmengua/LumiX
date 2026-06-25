import { formatPrice } from '../../utils/format';

export function PriceText({ value }: { value: number }) {
  return <span>{formatPrice(value)}</span>;
}

