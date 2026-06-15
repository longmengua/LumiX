import { ConnStatus } from '../types/exchange';
import { useMemo, useState } from 'react';
import { useI18n } from '../lib/i18n';

/**
 * Connection header block, keeping current symbol and stream status visible
 * at the top-left/right of the page.
 */
interface Props {
  symbol: string;
  connText: string;
  status: ConnStatus;
}

export function ExchangeTopBar({ symbol, connText, status }: Props) {
  // 控制兩個上方操作下拉選單的開關，不同來源按鈕互斥顯示。
  const [isProfileOpen, setIsProfileOpen] = useState<boolean>(false);
  const [isLanguageOpen, setIsLanguageOpen] = useState<boolean>(false);
  const { languageCode, setLanguageCode, t } = useI18n();

  const languageLabel = useMemo(
    () => t(languageCode === 'en' ? 'topbar.languageLabel.en' : 'topbar.languageLabel.zh'),
    [languageCode, t]
  );

  return (
    <header className="exchange-topbar">
      <div className="exchange-topbar-left">
        <h1>{t('app.title')}</h1>
        <p>
          {t('app.symbolLabel')}: {symbol}
        </p>
      </div>
      <div className="exchange-topbar-right">
        <div className={`conn-pill ${status}`}>
          <span className="conn-dot" />
          {connText}
        </div>

        <div className="topbar-actions">
          <div className="topbar-dropdown">
            <button
              type="button"
              className="topbar-icon-btn"
              aria-label={t('topbar.languageSelectorTitle')}
              title={t('topbar.languageSelectorTitle')}
              onClick={() => {
                setIsLanguageOpen((current) => !current);
                setIsProfileOpen(false);
              }}
            >
              <span className="topbar-icon" aria-hidden="true">
                🌐
              </span>
              <span>{languageLabel}</span>
            </button>
            {isLanguageOpen ? (
              <div className="topbar-dropdown-menu">
                <button
                  type="button"
                  className={`menu-item ${languageCode === 'zh-Hant' ? 'active' : ''}`}
                  onClick={() => {
                    // 切換到中文並關閉語言選單。
                  setLanguageCode('zh-Hant');
                    setIsLanguageOpen(false);
                  }}
                >
                  {t('topbar.languageOption.zh')}
                </button>
                <button
                  type="button"
                  className={`menu-item ${languageCode === 'en' ? 'active' : ''}`}
                  onClick={() => {
                    // 切換到英文並關閉語言選單。
                  setLanguageCode('en');
                    setIsLanguageOpen(false);
                  }}
                >
                  {t('topbar.languageOption.en')}
                </button>
              </div>
            ) : null}
          </div>

          <div className="topbar-dropdown">
            <button
              type="button"
              className="topbar-icon-btn"
              aria-label={t('topbar.profileTitle')}
              title={t('topbar.profileTitle')}
              onClick={() => {
                setIsProfileOpen((current) => !current);
                setIsLanguageOpen(false);
              }}
            >
              <span className="topbar-icon avatar-dot" aria-hidden="true">
                👤
              </span>
              <span>{t('topbar.profileLabel')}</span>
            </button>
            {isProfileOpen ? (
              <div className="topbar-dropdown-menu">
                <div className="menu-label">{t('topbar.profileNotLoggedIn')}</div>
                <button type="button" className="menu-item" onClick={() => setIsProfileOpen(false)}>
                  {t('topbar.profileLogin')}
                </button>
                <button type="button" className="menu-item" onClick={() => setIsProfileOpen(false)}>
                  {t('topbar.profileRegister')}
                </button>
                <button type="button" className="menu-item" onClick={() => setIsProfileOpen(false)}>
                  {t('topbar.profileTheme')}
                </button>
              </div>
            ) : null}
          </div>
        </div>
      </div>
    </header>
  );
}
