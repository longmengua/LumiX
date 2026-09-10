import { useCallback, useEffect, useState, type KeyboardEvent, type PointerEvent } from 'react';

import {
  createSliderCaptcha,
  verifySliderCaptcha,
  type SliderCaptchaChallenge,
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
  const [challenge, setChallenge] = useState<SliderCaptchaChallenge | null>(null);
  const [offsetX, setOffsetX] = useState(0);
  const [loading, setLoading] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    setOffsetX(0);
    try {
      setChallenge(await createSliderCaptcha());
    } catch {
      setChallenge(null);
      setError('無法載入滑動驗證，請重新整理後再試。');
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
      const captchaToken = await verifySliderCaptcha({ captchaId: challenge.captchaId, offsetX: currentOffset, purpose });
      onVerified(captchaToken);
    } catch {
      setError('驗證未通過，請重新滑動拼圖。');
      await reload();
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

  if (!open) return null;

  const maximumOffset = challenge ? challenge.width - challenge.pieceWidth : 0;
  const interactionDisabled = !challenge || loading || verifying;

  return (
    <div className="slider-captcha__backdrop" role="presentation">
      <section className="slider-captcha" role="dialog" aria-modal="true" aria-labelledby="slider-captcha-title">
        <div className="slider-captcha__header">
          <h2 id="slider-captcha-title" className="slider-captcha__title">安全驗證</h2>
          <div className="slider-captcha__actions">
            <button className="slider-captcha__refresh" type="button" onClick={() => void reload()} disabled={loading || verifying}>
              換一張
            </button>
            <button className="slider-captcha__close" type="button" onClick={onCancel} disabled={verifying} aria-label="關閉安全驗證">
              ×
            </button>
          </div>
        </div>

        <p className="slider-captcha__instruction">請將拼圖拖到缺口；放開後會自動判定。</p>

        {challenge ? (
          <div className="slider-captcha__canvas" style={{ width: challenge.width, height: challenge.height }}>
            <img className="slider-captcha__background" src={challenge.backgroundImage} alt="請將拼圖滑到缺口處" />
            <img
              className="slider-captcha__piece"
              src={challenge.pieceImage}
              alt=""
              aria-hidden="true"
              style={{ top: challenge.pieceY, left: offsetX, width: challenge.pieceWidth, height: challenge.pieceHeight }}
            />
          </div>
        ) : null}

        <input
          className="slider-captcha__range"
          aria-label="拖動拼圖至缺口"
          type="range"
          min="0"
          max={maximumOffset}
          value={offsetX}
          disabled={interactionDisabled}
          onChange={(event) => setOffsetX(Number(event.target.value))}
          onPointerUp={handlePointerUp}
          onKeyUp={handleKeyUp}
        />

        {loading ? <p className="slider-captcha__message">正在建立驗證題目…</p> : null}
        {verifying ? <p className="slider-captcha__message">正在驗證…</p> : null}
        {error ? <p className="slider-captcha__message form-message form-message--error">{error}</p> : null}
      </section>
    </div>
  );
}
