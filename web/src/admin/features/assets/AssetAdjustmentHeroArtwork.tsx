import desktopArtwork from '../../../assets/hero/asset-adjustment-hero-illustration.webp';
import mediumArtwork from '../../../assets/hero/asset-adjustment-hero-illustration-md.webp';
import smallArtwork from '../../../assets/hero/asset-adjustment-hero-illustration-sm.webp';

type AssetAdjustmentArtworkProps = {
  className?: string;
};

/**
 * 資產調整主視覺是純裝飾性素材；以 picture 保留交接檔的透明背景與清晰度，
 * 而 Hero 的 container query 仍負責實際可用空間的顯示與隱藏策略。
 */
export function AssetAdjustmentArtwork({ className }: AssetAdjustmentArtworkProps) {
  return (
    <picture className={className} aria-hidden="true">
      <source media="(max-width: 760px)" srcSet={smallArtwork} type="image/webp" />
      <source media="(max-width: 1440px)" srcSet={mediumArtwork} type="image/webp" />
      <img src={desktopArtwork} alt="" decoding="async" draggable={false} />
    </picture>
  );
}
