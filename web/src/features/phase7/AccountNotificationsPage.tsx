import { useEffect, useMemo, useState } from 'react';

import { Badge } from '../../components/base/Badge';
import { Card } from '../../components/base/Card';
import { ErrorState, LoadingState } from '../../components/base/State';
import { useI18n } from '../../i18n';
import { fetchNotificationCenterMock, type NotificationRecord } from './mockPhase7Service';
import { NotificationList } from './Phase7Tables';

type NotificationFilter = 'All' | 'Unread' | 'Security' | 'Orders' | 'Positions' | 'Funding' | 'System';

const filters: NotificationFilter[] = ['All', 'Unread', 'Security', 'Orders', 'Positions', 'Funding', 'System'];

export function AccountNotificationsPage() {
  const { t } = useI18n();
  const [notifications, setNotifications] = useState<NotificationRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<NotificationFilter>('All');
  const [summary, setSummary] = useState<Array<{ label: string; value: string; hint: string }>>([]);
  const [adapterNotice, setAdapterNotice] = useState('Development adapter only.');

  useEffect(() => {
    let alive = true;

    async function loadNotifications() {
      setLoading(true);
      setError(null);

      try {
        const snapshot = await fetchNotificationCenterMock();
        if (alive) {
          setNotifications(snapshot.notifications);
          setSummary(snapshot.summary);
          setAdapterNotice(snapshot.adapterNotice);
        }
      } catch (loadError) {
        if (alive) {
          setError(loadError instanceof Error ? loadError.message : t('state.noNotificationsDescription'));
        }
      } finally {
        if (alive) {
          setLoading(false);
        }
      }
    }

    void loadNotifications();

    return () => {
      alive = false;
    };
  }, []);

  const filteredNotifications = useMemo(() => {
    if (filter === 'All') {
      return notifications;
    }

    if (filter === 'Unread') {
      return notifications.filter((notification) => !notification.isRead);
    }

    return notifications.filter((notification) => notification.category === filter);
  }, [filter, notifications]);

  const unreadCount = notifications.filter((notification) => !notification.isRead).length;

  function toggleRead(id: string) {
    setNotifications((current) =>
      current.map((notification) =>
        notification.id === id ? { ...notification, isRead: !notification.isRead } : notification,
      ),
    );
  }

  return (
    <div className="stack">
      {loading ? <LoadingState title={t('account.loadingTitle')} description={t('account.loadingDescription')} /> : null}
      {error ? <ErrorState title={t('state.noNotificationsTitle')} description={error} /> : null}

      {!loading && !error ? (
        <>
          <Card title={t('nav.account.notifications')}>
            <div className="stack">
              <div className="notice-row">
                <Badge tone="warning">{t('common.developmentAdapterOnly')}</Badge>
                <span>{adapterNotice}</span>
              </div>

              <div className="dashboard-grid dashboard-grid--three">
                {summary.map((item) => (
                  <div className="stat-card" key={item.label}>
                    <span className="stat-card__label">{item.label}</span>
                    <strong>{item.value}</strong>
                    <p className="assets-metric__hint">{item.hint}</p>
                  </div>
                ))}
              </div>

              <div className="notice-row">
                <Badge tone="neutral">Unread {unreadCount}</Badge>
                <Badge tone="neutral">Visible {filteredNotifications.length}</Badge>
                <Badge tone="neutral">Filter {filter}</Badge>
              </div>

              <div className="tab-list">
                {filters.map((item) => (
                  <button
                    key={item}
                    className={`tab-button${filter === item ? ' tab-button--active' : ''}`}
                    type="button"
                    onClick={() => setFilter(item)}
                  >
                    {item}
                  </button>
                ))}
              </div>

              <NotificationList notifications={filteredNotifications} onToggleRead={toggleRead} />
            </div>
          </Card>
        </>
      ) : null}
    </div>
  );
}
