import { useCallback, useEffect, useState, type KeyboardEvent, type PointerEvent } from 'react';

import {
  createCaptchaChallenge,
  verifySliderCaptcha,
  type CaptchaChallenge,
  type SliderCaptchaPurpose,
} from '../../features/auth/authApi';

type SliderCaptchaProps = {
  open: boolean;
  purpose: SliderCaptchaPurpose;
  onCancel(): void;
  onVerified(captchaToken: string): void;
};

/**
 * 只在使用者送出帳號表單後開啟的驗證彈窗。
 *
 * 拼圖答案與通行 token 都由後端維護；前端僅在放開滑塊時送出位移，避免把可繞過的 UI 當成安全邊界。
 */
export function SliderCaptcha({ open, purpose, onCancel, onVerified }: SliderCaptchaProps) {
  const [challenge, setChallenge] = useState<CaptchaChallenge | null>(null);
  const [offsetX, setOffsetX] = useState(0);
  const [loading, setLoading] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedIndexes, setSelectedIndexes] = useState<number[]>([]);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    setOffsetX(0);
    setSelectedIndexes([]);
    // 畫布尺寸固定保留，載入時改為 skeleton，避免題目圖片插入 DOM 時造成彈窗重新置中。
    setChallenge(null);
    try {
      setChallenge(await createCaptchaChallenge());
    } catch {
      setChallenge(null);
      setError('載入失敗，請重試。');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // 僅在彈窗開啟後建立短時效題目，避免使用者停留表單時 challenge 已過期。
    if (open) void reload();
  }, [open, reload]);

  async function handleVerify(currentOffset: number) {
    if (!challenge || verifying) return;
    setVerifying(true);
    setError(null);
    try {
      const captchaToken = await verifySliderCaptcha({ captchaId: challenge.captchaId, type: 'SLIDER', offsetX: currentOffset, purpose });
      onVerified(captchaToken);
    } catch {
      // reload 會清除舊狀態；必須在取得新題目後再顯示錯誤，否則使用者看不到失敗原因。
      await reload();
      setError('驗證失敗，請重試。');
    } finally {
      setVerifying(false);
    }
  }

  function handlePointerUp(event: PointerEvent<HTMLInputElement>) {
    const currentOffset = Number(event.currentTarget.value);
    setOffsetX(currentOffset);
    // 滑鼠與觸控放開即送後端判定，不再要求使用者額外點擊確認按鈕。
    void handleVerify(currentOffset);
  }

  function handleKeyUp(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter') void handleVerify(Number(event.currentTarget.value));
  }

  async function handleVisualVerify(indexes: number[]) {
    if (!challenge || challenge.type === 'SLIDER' || verifying) return;
    setVerifying(true);
    setError(null);
    try {
      const captchaToken = await verifySliderCaptcha({
        captchaId: challenge.captchaId,
        type: challenge.type,
        selectedIndexes: indexes,
        purpose,
      });
      onVerified(captchaToken);
    } catch {
      await reload();
      setError('驗證失敗，請重試。');
    } finally {
      setVerifying(false);
    }
  }

  function toggleGridSelection(index: number) {
    setSelectedIndexes((current) => current.includes(index) ? current.filter((value) => value !== index) : [...current, index]);
  }

  if (!open) return null;

  const sliderChallenge = challenge?.type === 'SLIDER' ? challenge : null;
  const maximumOffset = sliderChallenge ? sliderChallenge.width - sliderChallenge.pieceWidth : 0;
  const interactionDisabled = !sliderChallenge || loading || verifying;
  // 狀態區只呈現最終錯誤；驗證中的短暫文字會在失敗時閃動，反而干擾使用者判讀。
  const statusMessage = error ?? '\u00a0';

  return (
    <div className="slider-captcha__backdrop" role="presentation">
      <section className="slider-captcha" role="dialog" aria-modal="true" aria-labelledby="slider-captcha-title">
        <div className="slider-captcha__header">
          <h2 id="slider-captcha-title" className="slider-captcha__title">安全驗證</h2>
          <div className="slider-captcha__actions">
            <button
              className="slider-captcha__refresh"
              type="button"
              onClick={() => void reload()}
              disabled={loading || verifying}
              aria-label="換一張驗證題目"
              title="換一張驗證題目"
            >
              <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                <path d="M21 12a9 9 0 0 0-15.5-6.2L3 8" />
                <path d="M3 3v5h5" />
                <path d="M3 12a9 9 0 0 0 15.5 6.2L21 16" />
                <path d="M16 16h5v5" />
              </svg>
            </button>
            <button className="slider-captcha__close" type="button" onClick={onCancel} disabled={verifying} aria-label="關閉安全驗證">
              ×
            </button>
          </div>
        </div>

        {sliderChallenge ? (
          <>
            <div className="slider-captcha__canvas" style={{ width: sliderChallenge.width, height: sliderChallenge.height }}>
            <>
            <img className="slider-captcha__background" src={sliderChallenge.backgroundImage} alt="請將拼圖滑到缺口處" />
            <img
              className="slider-captcha__piece"
              src={sliderChallenge.pieceImage}
              alt=""
              aria-hidden="true"
              style={{ top: sliderChallenge.pieceY, left: offsetX, width: sliderChallenge.pieceWidth, height: sliderChallenge.pieceHeight }}
            />
            </>
            </div>
            <input className="slider-captcha__range" aria-label="拖動拼圖至缺口" type="range" min="0" max={maximumOffset}
              value={offsetX} disabled={interactionDisabled} onChange={(event) => setOffsetX(Number(event.target.value))}
              onPointerUp={handlePointerUp} onKeyUp={handleKeyUp} />
          </>
        ) : challenge?.type === 'ICON_MATCH' ? (
          <div className="visual-captcha" aria-busy={verifying}>
            <p>{challenge.prompt}</p>
            <img className="visual-captcha__reference" src={challenge.referenceImage} alt="比對圖示" />
            <div className="visual-captcha__icons">
              {challenge.candidateImages.map((image, index) => <button key={image} type="button" disabled={verifying} onClick={() => void handleVisualVerify([index])} aria-label={`選擇第 ${index + 1} 個圖示`}><img src={image} alt="" /></button>)}
            </div>
          </div>
        ) : challenge?.type === 'IMAGE_GRID' ? (
          <div className="visual-captcha" aria-busy={verifying}>
            <p>{challenge.prompt}</p>
            <div className="visual-captcha__grid">
              {challenge.gridImages.map((image, index) => <button key={image} type="button" disabled={verifying} className={selectedIndexes.includes(index) ? 'is-selected' : ''} onClick={() => toggleGridSelection(index)} aria-pressed={selectedIndexes.includes(index)}><img src={image} alt={`圖片 ${index + 1}`} /></button>)}
            </div>
            <button className="visual-captcha__submit" type="button" disabled={verifying || selectedIndexes.length === 0} onClick={() => void handleVisualVerify(selectedIndexes)}>確認</button>
          </div>
        ) : (
          <div className="slider-captcha__canvas slider-captcha__canvas--loading" style={{ width: 280, height: 144 }} aria-busy="true"><span className="slider-captcha__skeleton" aria-hidden="true" /></div>
        )}

        <p className={`slider-captcha__message form-message${error ? ' form-message--error' : ''}`} aria-live="polite">
          {statusMessage}
        </p>
      </section>
    </div>
  );
}
