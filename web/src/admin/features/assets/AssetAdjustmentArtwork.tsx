import { useId } from 'react';

/**
 * Hero icon 以獨立 SVG 建立完整景深，避免通用線框 icon 被誤認為可點擊的功能按鈕。
 */
export function AssetAdjustmentHeroIcon() {
  const id = useId();
  return <div className="admin-airdrop-form__header-icon" aria-hidden="true">
    <svg viewBox="0 0 84 84" focusable="false">
      <defs>
        <linearGradient id={`${id}-asset-adjustment-hero-tile`} x1="8" y1="5" x2="77" y2="80" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="var(--asset-adjustment-icon-violet)" />
          <stop offset=".5" stopColor="var(--asset-adjustment-icon-indigo)" />
          <stop offset="1" stopColor="var(--asset-adjustment-icon-blue)" />
        </linearGradient>
        <linearGradient id={`${id}-asset-adjustment-hero-edge`} x1="42" y1="3" x2="42" y2="82" gradientUnits="userSpaceOnUse">
          <stop stopColor="var(--asset-adjustment-icon-edge-light)" />
          <stop offset=".58" stopColor="var(--asset-adjustment-icon-edge-mid)" />
          <stop offset="1" stopColor="var(--asset-adjustment-icon-edge-dark)" />
        </linearGradient>
        <linearGradient id={`${id}-asset-adjustment-hero-glyph`} x1="28" y1="26" x2="57" y2="59" gradientUnits="userSpaceOnUse">
          <stop stopColor="var(--asset-adjustment-icon-glyph-start)" />
          <stop offset="1" stopColor="var(--asset-adjustment-icon-glyph-end)" />
        </linearGradient>
        <radialGradient id={`${id}-asset-adjustment-hero-highlight`} cx="0" cy="0" r="1" gradientTransform="translate(23 15) rotate(48) scale(62)" gradientUnits="userSpaceOnUse">
          <stop stopColor="#fff" stopOpacity=".34" />
          <stop offset=".5" stopColor="#fff" stopOpacity=".07" />
          <stop offset="1" stopColor="#fff" stopOpacity="0" />
        </radialGradient>
        <filter id={`${id}-asset-adjustment-hero-shadow`} x="-35%" y="-30%" width="170%" height="185%">
          <feDropShadow dx="0" dy="8" stdDeviation="8" floodColor="var(--asset-adjustment-icon-shadow)" floodOpacity=".34" />
        </filter>
        <filter id={`${id}-asset-adjustment-hero-glyph-glow`} x="-40%" y="-40%" width="180%" height="180%">
          <feGaussianBlur in="SourceGraphic" stdDeviation="1.15" result="glyphBlur" />
          <feMerge><feMergeNode in="glyphBlur" /><feMergeNode in="SourceGraphic" /></feMerge>
        </filter>
      </defs>
      <g filter={`url(#${id}-asset-adjustment-hero-shadow)`}>
        <rect x="2" y="5" width="80" height="77" rx="19" fill="var(--asset-adjustment-icon-depth)" opacity=".84" />
        <rect x="2" y="2" width="80" height="78" rx="19" fill={`url(#${id}-asset-adjustment-hero-tile)`} />
        <rect x="2.5" y="2.5" width="79" height="77" rx="18.5" fill="none" stroke={`url(#${id}-asset-adjustment-hero-edge)`} />
        <rect x="3" y="3" width="78" height="76" rx="18" fill={`url(#${id}-asset-adjustment-hero-highlight)`} />
        <path d="M14 70c11 6 44 8 58-2" fill="none" stroke="rgba(6, 28, 112, .28)" strokeLinecap="round" strokeWidth="2" />
      </g>
      <g fill="none" stroke={`url(#${id}-asset-adjustment-hero-glyph)`} strokeLinecap="round" strokeLinejoin="round" strokeWidth="4" filter={`url(#${id}-asset-adjustment-hero-glyph-glow)`}>
        <path d="M27 33h29m-8-8 8 8-8 8" />
        <path d="M57 51H28m8 8-8-8 8-8" />
      </g>
    </svg>
  </div>;
}
/** 聚光與陰影只作用於插圖，保留 Hero 背景與其他內容的視覺層級。 */
export function AssetAdjustmentIllustration() {
  const id = useId();
  return <svg viewBox="38 0 270 170" aria-hidden="true">
    <defs>
      <linearGradient id={`${id}-asset-adjustment-illustration-coin`} x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stopColor="#7198ff" />
        <stop offset=".48" stopColor="#4969e8" />
        <stop offset="1" stopColor="#263795" />
      </linearGradient>
      <linearGradient id={`${id}-asset-adjustment-illustration-card`} x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stopColor="#9B8CFF" />
        <stop offset=".42" stopColor="#617dff" />
        <stop offset="1" stopColor="#294ec2" />
      </linearGradient>
      <radialGradient id={`${id}-asset-adjustment-illustration-sphere`} cx="30%" cy="25%" r="75%">
        <stop offset="0" stopColor="#93C5FD" />
        <stop offset=".45" stopColor="#5f86ff" />
        <stop offset="1" stopColor="#303cae" />
      </radialGradient>
      <linearGradient id={`${id}-asset-adjustment-illustration-coin-top`} x2="1" y2=".65">
        <stop stopColor="#d1e7ff" />
        <stop offset=".38" stopColor="#82b2ff" />
        <stop offset="1" stopColor="#4162dd" />
      </linearGradient>
      <radialGradient id={`${id}-asset-adjustment-illustration-spotlight`}>
        <stop offset="0" stopColor="#8ba9ff" stopOpacity=".36" />
        <stop offset=".45" stopColor="#5965F2" stopOpacity=".16" />
        <stop offset="1" stopColor="#3B4CCA" stopOpacity="0" />
      </radialGradient>
      <linearGradient id={`${id}-asset-adjustment-illustration-highlight`} x2="0" y2="1">
        <stop stopColor="#fff" stopOpacity=".15" />
        <stop offset="1" stopColor="#fff" stopOpacity="0" />
      </linearGradient>
      <filter id={`${id}-asset-adjustment-illustration-shadow`} x="-50%" y="-50%" width="200%" height="200%">
        <feDropShadow dx="0" dy="10" stdDeviation="8" floodColor="#315BEF" floodOpacity=".42" />
      </filter>
    </defs>
    <ellipse cx="172" cy="91" rx="132" ry="77" fill={`url(#${id}-asset-adjustment-illustration-spotlight)`} />
    <path d="M62 120c28-41 154-73 214-22" fill="none" stroke="#78a8ff" strokeOpacity=".32" strokeWidth="1.25" />
    <ellipse cx="154" cy="132" rx="99" ry="20" fill="none" stroke="#73a7ff" strokeOpacity=".7" strokeWidth="1.8" />
    <ellipse cx="154" cy="132" rx="73" ry="13" fill="#315BCB" fillOpacity=".16" />
    <ellipse cx="154" cy="135" rx="56" ry="8" fill="#4f7cff" fillOpacity=".13" />
    <g transform="translate(2 16)" filter={`url(#${id}-asset-adjustment-illustration-shadow)`}>
      <rect x="88" y="86" width="66" height="20" rx="10" fill={`url(#${id}-asset-adjustment-illustration-coin)`} />
      <ellipse cx="121" cy="86" rx="33" ry="9.5" fill={`url(#${id}-asset-adjustment-illustration-coin-top)`} stroke="#c8e2ff" strokeOpacity=".4" />
      <rect x="92" y="74" width="60" height="19" rx="9.5" fill={`url(#${id}-asset-adjustment-illustration-coin)`} />
      <ellipse cx="122" cy="74" rx="30" ry="9" fill={`url(#${id}-asset-adjustment-illustration-coin-top)`} stroke="#b8d8ff" strokeOpacity=".43" />
      <rect x="96" y="62" width="54" height="18" rx="9" fill={`url(#${id}-asset-adjustment-illustration-coin)`} />
      <ellipse cx="123" cy="62" rx="27" ry="8.3" fill={`url(#${id}-asset-adjustment-illustration-coin-top)`} stroke="#c7e1ff" strokeOpacity=".48" />
      <rect x="101" y="51" width="45" height="16" rx="8" fill={`url(#${id}-asset-adjustment-illustration-coin)`} />
      <ellipse cx="123.5" cy="51" rx="22.5" ry="7.5" fill={`url(#${id}-asset-adjustment-illustration-coin-top)`} stroke="#e0efff" strokeOpacity=".58" />
    </g>
    <g transform="rotate(13 204 76)" filter={`url(#${id}-asset-adjustment-illustration-shadow)`}>
      <rect x="159" y="35" width="98" height="91" rx="18" fill="#1d2b82" opacity=".86" />
      <rect x="154" y="30" width="98" height="91" rx="18" fill={`url(#${id}-asset-adjustment-illustration-card)`} stroke="#c6d7ff" strokeOpacity=".62" strokeWidth="1.3" />
      <rect x="155.5" y="31.5" width="95" height="88" rx="16.5" fill={`url(#${id}-asset-adjustment-illustration-highlight)`} />
      <g stroke="#f3f8ff" strokeWidth="4.1" strokeLinecap="round" strokeLinejoin="round" fill="none">
        <path d="M179 60h37" />
        <path d="m208 52 8 8-8 8" />
        <path d="M228 88h-37" />
        <path d="m199 80-8 8 8 8" />
      </g>
    </g>
    <circle cx="70" cy="63" r="4.5" fill={`url(#${id}-asset-adjustment-illustration-sphere)`} opacity=".55" />
    <circle cx="149" cy="25" r="8.5" fill={`url(#${id}-asset-adjustment-illustration-sphere)`} opacity=".86" />
    <circle cx="275" cy="105" r="11" fill={`url(#${id}-asset-adjustment-illustration-sphere)`} filter={`url(#${id}-asset-adjustment-illustration-shadow)`} />
  </svg>;
}
