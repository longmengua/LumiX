const SCALE = 10n ** 18n;

/** 只接受來源支援的非負十進位；拒絕指數、無限值與超出受控精度的資料。 */
export function isMarketDecimal(value: unknown): value is string {
  return typeof value === 'string' && /^\d{1,38}(?:\.\d{1,18})?$/.test(value);
}

/** 金額與數量先轉為固定 18 位精度整數，累計與圖表運算均不經 binary floating point。 */
export function marketAtomic(value: string): bigint {
  if (!isMarketDecimal(value)) throw new Error('invalid market decimal');
  const [whole, fraction = ''] = value.split('.');
  return BigInt(whole) * SCALE + BigInt(fraction.padEnd(18, '0'));
}

/** 僅移除無意義的尾零及加入千分位，保留來源所有有效小數，不重新四捨五入。 */
export function formatMarketDecimal(value: string): string {
  const [whole, fraction = ''] = value.split('.');
  const trimmed = fraction.replace(/0+$/, '');
  return whole.replace(/^0+(?=\d)/, '').replace(/\B(?=(\d{3})+(?!\d))/g, ',') + (trimmed ? `.${trimmed}` : '');
}

/** 固定精度累計值只在呈現時還原字串，避免訂單簿累計量產生浮點誤差。 */
export function formatMarketAtomic(value: bigint): string {
  return formatMarketDecimal(`${value / SCALE}.${(value % SCALE).toString().padStart(18, '0')}`);
}
