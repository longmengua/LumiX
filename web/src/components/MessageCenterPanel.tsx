import { useMemo } from 'react';
import { useI18n } from '../lib/i18n';

export type MessageLevel = 'info' | 'warn' | 'error';

/**
 * Message center row contract, for system and stream notices on the right-side panel.
 */
export interface MessageItem {
  id: string;
  ts: number;
  level: MessageLevel;
  text: string;
  detail?: string;
}

/**
 * Message center keeps websocket/system events visible as a timeline,
 * so operators can trace why page state changed.
 */
interface Props {
  messages: readonly MessageItem[];
}

export function MessageCenterPanel({ messages }: Props) {
  const { t } = useI18n();
  // 顯示最新事件在最上面，便於快速確認當前狀態。
  const timeline = useMemo(
    () => [...messages].sort((a, b) => b.ts - a.ts).slice(0, 120),
    [messages]
  );

  return (
    <section className="panel message-center-panel">
      <div className="panel-head">
        <h2>{t('message.title')}</h2>
      </div>
      <div className="message-list">
        {timeline.length === 0 ? (
          <div className="muted no-data">{t('message.empty')}</div>
        ) : (
          timeline.map((item) => (
            <div className={`message-item ${item.level}`} key={item.id}>
              <span className="message-dot" aria-hidden />
              <div className="message-body">
                <div className="message-text">
                  {item.text}
                  {item.detail ? <span className="message-detail">：{item.detail}</span> : null}
                </div>
                <div className="message-time">
                  {new Date(item.ts).toLocaleTimeString('en-US', {
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit'
                  })}
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </section>
  );
}
