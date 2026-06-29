import { createContext, createElement, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';

import { enUS } from './dictionaries/en-US';
import { zhTW } from './dictionaries/zh-TW';
import type { I18nContextValue, Locale, TranslationDictionary } from './types';
import { supportedLocales } from './types';

const STORAGE_KEY = 'lumix.locale';
const DEFAULT_LOCALE: Locale = 'zh-TW';

const dictionaries: Record<Locale, TranslationDictionary> = {
  'zh-TW': zhTW,
  'en-US': enUS,
};

const I18nContext = createContext<I18nContextValue | null>(null);

export function I18nProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>(() => readInitialLocale());

  useEffect(() => {
    try {
      window.localStorage.setItem(STORAGE_KEY, locale);
    } catch {
      // Ignore persistence failures and keep the in-memory locale.
    }
  }, [locale]);

  const value = useMemo<I18nContextValue>(
    () => ({
      locale,
      setLocale: (nextLocale: Locale) => {
        if (!supportedLocales.includes(nextLocale)) {
          return;
        }
        setLocaleState(nextLocale);
      },
      t: (key: string, fallback?: string, values?: Record<string, string | number>) => translate(locale, key, fallback, values),
    }),
    [locale],
  );

  return createElement(I18nContext.Provider, { value }, children);
}

export function useI18n() {
  const context = useContext(I18nContext);
  if (!context) {
    throw new Error('useI18n must be used within I18nProvider');
  }
  return context;
}

function readInitialLocale(): Locale {
  if (typeof window === 'undefined') {
    return DEFAULT_LOCALE;
  }

  try {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    if (stored && supportedLocales.includes(stored as Locale)) {
      return stored as Locale;
    }
  } catch {
    // Ignore storage read errors and fall back to default locale.
  }

  return DEFAULT_LOCALE;
}

function translate(locale: Locale, key: string, fallback?: string, values?: Record<string, string | number>) {
  const template = dictionaries[locale][key] ?? dictionaries[DEFAULT_LOCALE][key] ?? fallback ?? key;
  if (!values) {
    return template;
  }

  return template.replace(/\{(\w+)\}/g, (match, token: string) => {
    const value = values[token];
    return value === undefined ? match : String(value);
  });
}
