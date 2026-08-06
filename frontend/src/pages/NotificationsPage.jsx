import { useEffect, useMemo, useState } from 'react';
import { Bell, CheckCircle2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import classroomApi from '../api/classroomApi';
import LearnerPageShell from '../components/learner/LearnerPageShell';
import Pagination, { usePagination } from '../components/ui/Pagination';
import { ClassroomLoadingState } from '../components/classroom/ClassroomUi';
import { useLearnerExperience } from '../context/LearnerExperienceContext';
import { getClassroomErrorMessage } from '../utils/classroomErrorMessages';
import { hasAccessToken } from '../utils/auth';

const formatNotificationTime = (value) => {
  if (!value) return '';
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
};

const mapApiNotification = (notification) => ({
  id: `api-${notification.id}`,
  title: notification.title,
  message: notification.body,
  read: notification.read,
  createdAt: notification.createdAt,
  actionPath: notification.actionPath || null,
});

export default function NotificationsPage() {
  const { markAllNotificationsRead, notifications: contextNotifications } = useLearnerExperience();
  const [apiNotifications, setApiNotifications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const isAuthenticated = hasAccessToken();

  useEffect(() => {
    if (!isAuthenticated) {
      markAllNotificationsRead();
      return undefined;
    }

    let active = true;
    setLoading(true);
    setError('');

    const loadNotifications = async () => {
      try {
        const items = await classroomApi.getStudentNotifications();
        if (!active) return;
        setApiNotifications(items.map(mapApiNotification));
        await classroomApi.markAllNotificationsRead();
      } catch (err) {
        if (!active) return;
        setApiNotifications([]);
        setError(getClassroomErrorMessage(err, 'Không thể tải thông báo từ máy chủ.'));
      } finally {
        if (active) setLoading(false);
      }
    };

    loadNotifications();

    return () => {
      active = false;
    };
  }, [isAuthenticated, markAllNotificationsRead]);

  const notifications = useMemo(() => {
    if (isAuthenticated && apiNotifications.length) return apiNotifications;
    if (isAuthenticated && !loading && !error) return apiNotifications;
    return contextNotifications;
  }, [apiNotifications, contextNotifications, error, isAuthenticated, loading]);

  const { page, setPage, totalPages, pageItems: paginatedNotifications, totalItems } = usePagination(
    notifications,
    8,
    'notifications'
  );

  return (
    <LearnerPageShell
      title="Thông báo"
      description="Các cập nhật học tập, lớp học, khóa học và nhắc nhở gần đây của bạn trên EnglishLab."
    >
      {loading ? <ClassroomLoadingState message="Đang tải thông báo..." /> : null}
      {!loading && error ? (
        <section className="flex min-h-[360px] flex-1 flex-col items-center justify-center rounded-[32px] border border-[#f0d4d7] bg-white px-6 py-16 text-center text-[#93000a]">
          {error}
        </section>
      ) : null}
      {!loading && !error && notifications.length === 0 ? (
        <section className="flex min-h-[420px] flex-1 flex-col items-center justify-center rounded-[32px] border border-dashed border-[#dfbfbd] bg-white px-6 py-16 text-center shadow-[0_18px_45px_rgba(75,0,9,0.04)]">
          <div className="flex h-14 w-14 items-center justify-center rounded-full bg-[#fff3f4] text-[#8a0018]">
            <Bell className="h-6 w-6" />
          </div>
          <h2 className="mt-5 font-['Manrope'] text-3xl font-extrabold text-[#2b2828]">Chưa có thông báo mới</h2>
          <p className="mx-auto mt-3 max-w-xl text-sm leading-7 text-[#584140]">
            Khi có cập nhật về khóa học, lớp học hoặc tiến độ học tập, EnglishLab sẽ hiển thị tại đây.
          </p>
        </section>
      ) : null}
      {!loading && !error && notifications.length > 0 ? (
        <section className="flex flex-1 flex-col justify-between min-h-[420px] rounded-[32px] border border-[#dfbfbd]/30 bg-white p-4 shadow-sm md:p-6 space-y-6">
          <div className="space-y-3">
            {paginatedNotifications.map((notification) => {
              const content = (
                <article
                  className={`flex gap-4 rounded-[24px] border px-4 py-4 transition hover:bg-[#fff8f7] ${
                    notification.read
                      ? 'border-[#f0e4e2] bg-white'
                      : 'border-[#f0d4d7] bg-[#fff6f7]'
                  }`}
                >
                  <div className="mt-1 flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[#fff3f4] text-[#8a0018]">
                    <CheckCircle2 className="h-5 w-5" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <h2 className="truncate text-sm font-extrabold text-[#2b2828]">{notification.title}</h2>
                      {!notification.read ? (
                        <span className="h-2 w-2 rounded-full bg-[#c5162e]" />
                      ) : null}
                    </div>
                    <p className="mt-1 text-sm leading-6 text-[#584140]">{notification.message}</p>
                    <p className="mt-2 text-xs font-semibold text-[#8b706e]">
                      {formatNotificationTime(notification.createdAt)}
                    </p>
                  </div>
                </article>
              );

              return notification.actionPath ? (
                <Link key={notification.id} to={notification.actionPath}>
                  {content}
                </Link>
              ) : (
                <div key={notification.id}>{content}</div>
              );
            })}
          </div>

          {notifications.length > 8 && (
            <div className="flex justify-end">
              <Pagination
                page={page}
                totalPages={totalPages}
                onChange={setPage}
                totalItems={totalItems}
                pageSize={8}
              />
            </div>
          )}
        </section>
      ) : null}
    </LearnerPageShell>
  );
}
