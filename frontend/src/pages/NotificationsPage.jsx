import { useEffect } from 'react';
import { Bell, CheckCircle2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import LearnerPageShell from '../components/learner/LearnerPageShell';
import { useLearnerExperience } from '../context/LearnerExperienceContext';

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

const NotificationsPage = () => {
  const { markAllNotificationsRead, notifications } = useLearnerExperience();

  useEffect(() => {
    markAllNotificationsRead();
  }, [markAllNotificationsRead]);

  return (
    <LearnerPageShell
      title="Thông báo"
      description="Các cập nhật học tập, giỏ hàng, khóa học và nhắc nhở gần đây của bạn trên EnglishLab."
    >
      {notifications.length === 0 ? (
        <section className="flex min-h-[420px] flex-col items-center justify-center rounded-[32px] border border-dashed border-[#dfbfbd] bg-white px-6 py-16 text-center shadow-[0_18px_45px_rgba(75,0,9,0.04)]">
          <div className="flex h-14 w-14 items-center justify-center rounded-full bg-[#fff3f4] text-[#8a0018]">
            <Bell className="h-6 w-6" />
          </div>
          <h2 className="mt-5 font-['Manrope'] text-3xl font-extrabold text-[#2b2828]">Chưa có thông báo mới</h2>
          <p className="mx-auto mt-3 max-w-xl text-sm leading-7 text-[#584140]">
            Khi có cập nhật về khóa học, giỏ hàng hoặc tiến độ học tập, EnglishLab sẽ hiển thị tại đây.
          </p>
        </section>
      ) : (
        <section className="rounded-[32px] border border-[#dfbfbd]/30 bg-white p-4 shadow-sm md:p-6">
          <div className="space-y-3">
            {notifications.map((notification) => {
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
        </section>
      )}
    </LearnerPageShell>
  );
};

export default NotificationsPage;
