export type DateRangePreset = 'today' | 'last-7-days' | 'last-30-days' | 'this-month' | 'last-month';

export type InputDateRange = {
  startDate: string;
  endDate: string;
};

/**
 * 日期篩選以使用者選擇的日曆日為單位；API 端仍由呼叫方統一轉換成 UTC 半開區間，避免各頁自行處理時區邊界。
 */
export function getInputDateRangePreset(preset: DateRangePreset, now = new Date()): InputDateRange {
  const today = startOfLocalDay(now);
  switch (preset) {
    case 'today':
      return range(today, today);
    case 'last-7-days':
      return range(addDays(today, -6), today);
    case 'last-30-days':
      return range(addDays(today, -29), today);
    case 'this-month':
      return range(new Date(today.getFullYear(), today.getMonth(), 1), today);
    case 'last-month': {
      const start = new Date(today.getFullYear(), today.getMonth() - 1, 1);
      return range(start, new Date(today.getFullYear(), today.getMonth(), 0));
    }
  }
}

function range(start: Date, end: Date): InputDateRange {
  return { startDate: toInputDate(start), endDate: toInputDate(end) };
}

function startOfLocalDay(value: Date) {
  return new Date(value.getFullYear(), value.getMonth(), value.getDate());
}

function addDays(value: Date, days: number) {
  const result = new Date(value);
  result.setDate(result.getDate() + days);
  return result;
}

function toInputDate(value: Date) {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, '0');
  const day = String(value.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}
