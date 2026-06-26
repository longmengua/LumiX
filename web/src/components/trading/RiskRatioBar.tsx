export function RiskRatioBar({ ratio }: { ratio: number }) {
  const width = Math.max(0, Math.min(100, ratio));
  return (
    <div className="risk-bar" aria-label={`Risk ratio ${ratio}%`}>
      <span className="risk-bar__fill" style={{ width: `${width}%` }} />
    </div>
  );
}

