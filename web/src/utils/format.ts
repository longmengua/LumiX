export function formatPrice(value: number, fractionDigits = 2) {
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(value);
}

export function formatAmount(value: number, fractionDigits = 4) {
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(value);
}

/**
 * 對 API 回傳的十進位字串只加上千分位，不轉成 JavaScript number。
 *
 * <p>資產 amount 可達 NUMERIC(36,18)，以 number 格式化會讓尾數精度遺失；此 helper 不做四捨五入或截斷。</p>
 */
export function formatDecimalString(value: string) {
  const [integerPart, fractionPart] = value.split('.');
  if (!integerPart || !/^\d+$/.test(integerPart) || (fractionPart !== undefined && !/^\d+$/.test(fractionPart))) {
    return value;
  }
  const groupedInteger = integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return fractionPart === undefined ? groupedInteger : `${groupedInteger}.${fractionPart}`;
}

export function formatCurrency(value: number, currency = 'USD') {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    maximumFractionDigits: 2,
  }).format(value);
}

export function formatPercent(value: number, fractionDigits = 2) {
  return `${new Intl.NumberFormat('en-US', {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(value)}%`;
}

export function formatTime(value: string | Date) {
  return new Intl.DateTimeFormat('en-US', {
    dateStyle: 'medium',
    timeStyle: 'short',
    hourCycle: 'h23',
  }).format(typeof value === 'string' ? new Date(value) : value);
}

/**
 * 密集清單將日期與時間分行，讓欄位可維持窄且整齊；統一使用 24 小時制，不改變時間來源或時區換算。
 */
export function formatDateTimeParts(value: string | Date) {
  const date = typeof value === 'string' ? new Date(value) : value;
  return {
    date: new Intl.DateTimeFormat('en-US', { dateStyle: 'medium' }).format(date),
    time: new Intl.DateTimeFormat('en-US', { timeStyle: 'short', hourCycle: 'h23' }).format(date),
  };
}

export function maskEmail(email: string) {
  const [local, domain] = email.split('@');
  if (!local || !domain) return email;
  return `${local.slice(0, 2)}***@${domain}`;
}

export function maskPhone(phone: string) {
  if (phone.length <= 4) return phone;
  return `${phone.slice(0, 3)}****${phone.slice(-2)}`;
}

export function maskApiKey(key: string) {
  if (key.length <= 8) return key;
  return `${key.slice(0, 4)}...${key.slice(-4)}`;
}

export function maskAddress(address: string) {
  if (address.length <= 12) return address;
  return `${address.slice(0, 6)}...${address.slice(-4)}`;
}
