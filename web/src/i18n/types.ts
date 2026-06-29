export const supportedLocales = ['zh-TW', 'en-US'] as const;

export type Locale = (typeof supportedLocales)[number];

export type TranslationDictionary = Record<string, string>;

export type I18nContextValue = {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: (key: string, fallback?: string, values?: Record<string, string | number>) => string;
};
