import { Link } from 'react-router-dom';
import { Megaphone } from 'lucide-react';
import { AdminNotificationsList } from '../NotificationsPage';

export default function AdminNotificationsPage() {
  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.18em] text-[#8a0018]">Quản trị</p>
          <h1 className="mt-2 font-['Manrope'] text-3xl font-extrabold tracking-tight text-slate-900">
            Thông báo hệ thống của tôi
          </h1>
          <p className="mt-2 text-sm text-slate-500">
            Bản nháp, lịch gửi và lịch sử thông báo hệ thống do bạn tạo sẽ hiển thị tại đây.
          </p>
        </div>
        <Link
          className="inline-flex items-center justify-center gap-2 rounded-lg bg-[#4b0009] px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-[#730014]"
          to="/admin/broadcasts"
        >
          <Megaphone className="h-4 w-4" />
          Quản lý broadcasts
        </Link>
      </div>
      <AdminNotificationsList />
    </div>
  );
}
