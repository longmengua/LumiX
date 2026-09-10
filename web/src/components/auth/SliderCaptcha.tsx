import { useCallback, useEffect, useState } from 'react';

import {
  createSliderCaptcha,
  verifySliderCaptcha,
  type SliderCaptchaChallenge,
  type SliderCaptchaPurpose,
} from '../../features/auth/authApi';

type SliderCaptchaProps = {
  purpose: SliderCaptchaPurpose;
  disabled?: boolean;
  onVerified(captchaToken: string): void;
};

/**
 * 只負責呈現 server-side 拼圖與提交使用者位移。
 *
 * 通過後的 token 僅留在父層表單 state；不可寫入 URL、localStorage 或 sessionStorage，因為它雖然短時效，
 * 仍是可消耗的防濫用憑證。
 */
export function SliderCaptcha({ purpose, disabled = false, onVerified }: SliderCaptchaProps) {
  const [challenge, setChallenge] = useState<SliderCaptchaChallenge | null>(null);
  const [offsetX, setOffsetX] = useState(0);
  const [loading, setLoading] = useState(true);
  const [verifying, setVerifying] = useState(false);
  const [verified, setVerified] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setVerified(false);
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
    void reload();
  }, [reload]);

  async function handleVerify() {
    if (!challenge || verified) return;
    setVerifying(true);
    setError(null);
    try {
      const captchaToken = await verifySliderCaptcha({ captchaId: challenge.captchaId, offsetX, purpose });
      setVerified(true);
      onVerified(captchaToken);
    } catch {
      setError('驗證未通過，請重新滑動拼圖。');
      await reload();
    } finally {
      setVerifying(false);
    }
  }

  const maximumOffset = challenge ? challenge.width - challenge.pieceWidth : 0;

  return (
    <section className="slider-captcha" aria-label="滑動驗證">
      <div className="slider-captcha__header">
        <span className="field__label">安全驗證</span>
        <button className="slider-captcha__refresh" type="button" onClick={() => void reload()} disabled={loading || verifying || disabled}>
          換一張
        </button>
      </div>
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
      <div className="slider-captcha__controls">
        <input
          className="slider-captcha__range"
          aria-label="拖動拼圖至缺口"
          type="range"
          min="0"
          max={maximumOffset}
          value={offsetX}
          disabled={!challenge || loading || verifying || disabled || verified}
          onChange={(event) => setOffsetX(Number(event.target.value))}
        />
        <button className="secondary-button slider-captcha__verify" type="button" disabled={!challenge || loading || verifying || disabled || verified} onClick={() => void handleVerify()}>
          {verified ? '驗證完成' : verifying ? '驗證中…' : '完成滑動驗證'}
        </button>
      </div>
      {error ? <p className="slider-captcha__message form-message form-message--error">{error}</p> : null}
      {verified ? <p className="slider-captcha__message form-message form-message--success">安全驗證已完成。</p> : null}
    </section>
  );
}
