import { useEffect, useMemo, useState } from 'react';
import { CheckCircle2, Megaphone } from 'lucide-react';
import { Link, Navigate } from 'react-router-dom';
import classroomApi from '../api/classroomApi';
import LearnerPageShell from '../components/learner/LearnerPageShell';
import NotificationsEmptyState from '../components/notifications/NotificationsEmptyState';
import Pagination, { usePagination } from '../components/ui/Pagination';
import { ClassroomLoadingState } from '../components/classroom/ClassroomUi';
import { useLearnerExperience } from '../context/LearnerExperienceContext';
import { useAuth } from '../context/AuthContext';
import { getClassroomErrorMessage } from '../utils/classroomErrorMessages';
import { hasAccessToken } from '../utils/auth';
import { EMPTY_PAGE, normalizePage, pageParams } from '../utils/pagination';

export const ROLE_NOTIFICATION_DESCRIPTIONS = {
  TEACHER: 'Thông báo hệ thống, cập nhật lớp học và lịch giảng dạy sẽ hiển thị tại đây.',
  STAFF: 'Thông báo từ quản trị viên, cập nhật đăng ký và vận hành lớp học sẽ hiển thị tại đây.',
  MANAGER: 'Thông báo điều hành, phê duyệt đề xuất lớp và thông tin ghi danh sẽ hiển thị tại đây.',
  CONTENT_MANAGER: 'Thông báo về nội dung, khóa học và lộ trình học sẽ hiển thị tại đây.',
  ADMIN: 'Bản nháp, lịch gửi và lịch sử thông báo hệ thống do bạn tạo sẽ hiển thị tại đây.',
  LEARNER: 'Các cập nhật học tập, lớp học, khóa học và nhắc nhở gần đây của bạn trên EnglishLab.',
};

export const ROLE_EMPTY_HINTS = {
  TEACHER: 'Khi có thông báo mới dành cho giáo viên (lịch dạy, yêu cầu thay đổi, thông báo hệ thống), EnglishLab sẽ hiển thị tại đây.',
  STAFF: 'Khi có thông báo mới dành cho nhân viên đào tạo (yêu cầu đăng ký, lịch gửi, cập nhật hệ thống), EnglishLab sẽ hiển thị tại đây.',
  MANAGER: 'Khi có thông báo mới dành cho quản lý đào tạo (đề xuất lớp, ghi danh online, thông báo hệ thống), EnglishLab sẽ hiển thị tại đây.',
  CONTENT_MANAGER: 'Khi có thông báo mới dành cho quản lý nội dung, EnglishLab sẽ hiển thị tại đây.',
  ADMIN: 'Khi có thông báo hệ thống mới hoặc khi bạn tạo chiến dịch broadcast, EnglishLab sẽ hiển thị tại đây.',
  LEARNER: 'Khi có cập nhật về khóa học, lớp học hoặc tiến độ học tập, EnglishLab sẽ hiển thị tại đây.',
};

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

export function useNotificationsData({ pageSize = 8, storageKey = 'notifications' } = {}) {
  const { markAllNotificationsRead, notifications: contextNotifications } = useLearnerExperience();
  const [apiNotifications, setApiNotifications] = useState([]);
  const [pageResult, setPageResult] = useState(EMPTY_PAGE);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const isAuthenticated = hasAccessToken();
  const { page, setPage, totalPages, pageItems: paginatedNotifications, totalItems } = usePagination(
    isAuthenticated ? apiNotifications : contextNotifications,
    pageSize,
    storageKey,
    isAuthenticated ? pageResult : null,
  );

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
        const result = normalizePage(await classroomApi.getStudentNotificationsPage(pageParams(page, pageSize)));
        if (!active) return;
        setPageResult(result);
        setApiNotifications(result.content.map(mapApiNotification));
        if (result.content.some((item) => !item.read)) {
          await classroomApi.markAllNotificationsRead();
        }
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
  }, [isAuthenticated, markAllNotificationsRead, page, pageSize]);

  const notifications = useMemo(() => {
    if (isAuthenticated && apiNotifications.length) return apiNotifications;
    if (isAuthenticated && !loading && !error) return apiNotifications;
    return contextNotifications;
  }, [apiNotifications, contextNotifications, error, isAuthenticated, loading]);

  return {
    notifications,
    paginatedNotifications,
    loading,
    error,
    page,
    setPage,
    totalPages,
    totalItems,
  };
}

export function NotificationsList({
  emptyState = null,
  loadingState = null,
  containerClassName = '',
  itemClassName = '',
  variant = 'rose',
  pageSize = 8,
  storageKey = 'notifications',
}) {
  const {
    notifications,
    paginatedNotifications,
    loading,
    error,
    page,
    setPage,
    totalPages,
    totalItems,
  } = useNotificationsData({ pageSize, storageKey });

  const theme = variant === 'neutral'
    ? {
        containerBorder: 'border-slate-200',
        readBorder: 'border-slate-200',
        unreadBorder: 'border-rose-200',
        readBg: 'bg-white',
        unreadBg: 'bg-rose-50/40',
        hoverBg: 'hover:bg-slate-50',
        iconBg: 'bg-rose-50',
        iconText: 'text-rose-700',
        titleText: 'text-slate-900',
        messageText: 'text-slate-600',
        timeText: 'text-slate-400',
        unreadDot: 'bg-rose-600',
        errorBorder: 'border-rose-200',
        errorText: 'text-rose-700',
      }
    : {
        containerBorder: 'border-[#dfbfbd]/30',
        readBorder: 'border-[#f0e4e2]',
        unreadBorder: 'border-[#f0d4d7]',
        readBg: 'bg-white',
        unreadBg: 'bg-[#fff6f7]',
        hoverBg: 'hover:bg-[#fff8f7]',
        iconBg: 'bg-[#fff3f4]',
        iconText: 'text-[#8a0018]',
        titleText: 'text-[#2b2828]',
        messageText: 'text-[#584140]',
        timeText: 'text-[#8b706e]',
        unreadDot: 'bg-[#c5162e]',
        errorBorder: 'border-[#f0d4d7]',
        errorText: 'text-[#93000a]',
      };

  const renderLoading = loadingState ?? <ClassroomLoadingState message="Đang tải thông báo..." />;

  return (
    <>
      {loading ? renderLoading : null}
      {!loading && error ? (
        <section className={`flex min-h-[360px] flex-1 flex-col items-center justify-center rounded-[32px] border ${theme.errorBorder} bg-white px-6 py-16 text-center ${theme.errorText}`}>
          {error}
        </section>
      ) : null}
      {!loading && !error && notifications.length === 0 ? (
        emptyState
      ) : null}
      {!loading && !error && notifications.length > 0 ? (
        <section className={`flex flex-1 flex-col justify-between min-h-[420px] rounded-[32px] border ${theme.containerBorder} bg-white p-4 shadow-sm md:p-6 space-y-6 ${containerClassName}`}>
          <div className="flex flex-col gap-3">
            {paginatedNotifications.map((notification) => {
              const content = (
                <article
                  className={`flex gap-4 rounded-[24px] border px-4 py-4 transition ${theme.hoverBg} ${
                    notification.read
                      ? `${theme.readBorder} ${theme.readBg}`
                      : `${theme.unreadBorder} ${theme.unreadBg}`
                  } ${itemClassName}`}
                >
                  <div className={`mt-1 flex h-10 w-10 shrink-0 items-center justify-center rounded-full ${theme.iconBg} ${theme.iconText}`}>
                    <CheckCircle2 className="h-5 w-5" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <h2 className={`truncate text-sm font-extrabold ${theme.titleText}`}>{notification.title}</h2>
                      {!notification.read ? (
                        <span className={`h-2 w-2 rounded-full ${theme.unreadDot}`} />
                      ) : null}
                    </div>
                    <p className={`mt-1 text-sm leading-6 ${theme.messageText}`}>{notification.message}</p>
                    <p className={`mt-2 text-xs font-semibold ${theme.timeText}`}>
                      {formatNotificationTime(notification.createdAt)}
                    </p>
                  </div>
                </article>
              );

              return notification.actionPath ? (
                <Link key={notification.id} className="block" to={notification.actionPath}>
                  {content}
                </Link>
              ) : (
                <div key={notification.id}>{content}</div>
              );
            })}
          </div>

          {totalItems > pageSize && (
            <div className="flex justify-end">
              <Pagination
                page={page}
                totalPages={totalPages}
                onChange={setPage}
                totalItems={totalItems}
                pageSize={pageSize}
              />
            </div>
          )}
        </section>
      ) : null}
    </>
  );
}

export function AdminNotificationsList(props) {
  const role = String(useAuth().user?.role || '').toUpperCase();
  const hint = ROLE_EMPTY_HINTS[role] || ROLE_EMPTY_HINTS.LEARNER;
  return (
    <NotificationsList
      variant="neutral"
      loadingState={
        <div className="flex min-h-[360px] items-center justify-center rounded-[32px] border border-slate-200 bg-white px-6 py-16 text-center shadow-sm">
          <p className="text-sm font-semibold text-slate-500">Đang tải thông báo...</p>
        </div>
      }
      emptyState={<NotificationsEmptyState variant="neutral" description={hint} />}
      {...props}
    />
  );
}

export default function NotificationsPage() {
  const { user } = useAuth();
  const role = String(user?.role || 'LEARNER').toUpperCase();
  const isAdmin = role === 'ADMIN';
  const description = ROLE_NOTIFICATION_DESCRIPTIONS[role] || ROLE_NOTIFICATION_DESCRIPTIONS.LEARNER;
  const emptyHint = ROLE_EMPTY_HINTS[role] || ROLE_EMPTY_HINTS.LEARNER;

  return (
    <RoleAwareNotificationsRedirect>
      <LearnerPageShell
        title={isAdmin ? 'Thông báo hệ thống của tôi' : 'Thông báo'}
        description={description}
        actions={isAdmin ? (
          <Link
            className="inline-flex items-center gap-2 rounded-xl bg-[#730014] px-4 py-2 text-sm font-bold text-white shadow-sm transition hover:bg-[#56000f]"
            to="/admin/notifications"
          >
            <Megaphone className="h-4 w-4" /> Quản lý broadcasts
          </Link>
        ) : null}
      >
        <NotificationsList
          emptyState={
            <NotificationsEmptyState variant="rose" description={emptyHint} />
          }
        />
      </LearnerPageShell>
    </RoleAwareNotificationsRedirect>
  );
}

function RoleAwareNotificationsRedirect({ children }) {
  const { user } = useAuth();
  const role = String(user?.role || '').toUpperCase();
  if (role === 'ADMIN') return <Navigate replace to="/admin/notifications" />;
  if (role === 'MANAGER') return <Navigate replace to="/manager/notifications" />;
  if (role === 'STAFF') return <Navigate replace to="/staff/notifications" />;
  if (role === 'CONTENT_MANAGER') return <Navigate replace to="/content-manager/notifications" />;
  return children;
}
