import { useId } from 'react';

/**
 * Hero icon 保留在資產 feature，避免共用 Hero 承擔資產調整的圖像語意。
 */
export function AssetAdjustmentHeroIcon() {
  const id = useId();
  return <div className="admin-airdrop-form__header-icon" aria-hidden="true">
    <svg viewBox="0 0 84 84" focusable="false">
      <defs>
        <linearGradient id={`${id}-asset-adjustment-hero-tile`} x1="8" y1="5" x2="77" y2="80" gradientUnits="userSpaceOnUse"><stop offset="0" stopColor="var(--asset-adjustment-icon-violet)" /><stop offset=".5" stopColor="var(--asset-adjustment-icon-indigo)" /><stop offset="1" stopColor="var(--asset-adjustment-icon-blue)" /></linearGradient>
        <linearGradient id={`${id}-asset-adjustment-hero-edge`} x1="42" y1="3" x2="42" y2="82" gradientUnits="userSpaceOnUse"><stop stopColor="var(--asset-adjustment-icon-edge-light)" /><stop offset=".58" stopColor="var(--asset-adjustment-icon-edge-mid)" /><stop offset="1" stopColor="var(--asset-adjustment-icon-edge-dark)" /></linearGradient>
        <linearGradient id={`${id}-asset-adjustment-hero-glyph`} x1="28" y1="26" x2="57" y2="59" gradientUnits="userSpaceOnUse"><stop stopColor="var(--asset-adjustment-icon-glyph-start)" /><stop offset="1" stopColor="var(--asset-adjustment-icon-glyph-end)" /></linearGradient>
        <radialGradient id={`${id}-asset-adjustment-hero-highlight`} cx="0" cy="0" r="1" gradientTransform="translate(23 15) rotate(48) scale(62)" gradientUnits="userSpaceOnUse"><stop stopColor="#fff" stopOpacity=".34" /><stop offset=".5" stopColor="#fff" stopOpacity=".07" /><stop offset="1" stopColor="#fff" stopOpacity="0" /></radialGradient>
        <filter id={`${id}-asset-adjustment-hero-shadow`} x="-35%" y="-30%" width="170%" height="185%"><feDropShadow dx="0" dy="8" stdDeviation="8" floodColor="var(--asset-adjustment-icon-shadow)" floodOpacity=".34" /></filter>
        <filter id={`${id}-asset-adjustment-hero-glyph-glow`} x="-40%" y="-40%" width="180%" height="180%"><feGaussianBlur in="SourceGraphic" stdDeviation="1.15" result="glyphBlur" /><feMerge><feMergeNode in="glyphBlur" /><feMergeNode in="SourceGraphic" /></feMerge></filter>
      </defs>
      <g filter={`url(#${id}-asset-adjustment-hero-shadow)`}><rect x="2" y="5" width="80" height="77" rx="19" fill="var(--asset-adjustment-icon-depth)" opacity=".84" /><rect x="2" y="2" width="80" height="78" rx="19" fill={`url(#${id}-asset-adjustment-hero-tile)`} /><rect x="2.5" y="2.5" width="79" height="77" rx="18.5" fill="none" stroke={`url(#${id}-asset-adjustment-hero-edge)`} /><rect x="3" y="3" width="78" height="76" rx="18" fill={`url(#${id}-asset-adjustment-hero-highlight)`} /><path d="M14 70c11 6 44 8 58-2" fill="none" stroke="rgba(6, 28, 112, .28)" strokeLinecap="round" strokeWidth="2" /></g>
      <g fill="none" stroke={`url(#${id}-asset-adjustment-hero-glyph)`} strokeLinecap="round" strokeLinejoin="round" strokeWidth="4" filter={`url(#${id}-asset-adjustment-hero-glyph-glow)`}><path d="M27 33h29m-8-8 8 8-8 8" /><path d="M57 51H28m8 8-8-8 8-8" /></g>
    </svg>
  </div>;
}
