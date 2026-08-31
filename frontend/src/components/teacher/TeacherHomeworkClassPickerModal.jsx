import { useEffect, useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import { ArrowRight, Calendar, CheckCircle2, Search, Users, X } from 'lucide-react';
import { ClassroomTypeBadge, StatusBadge } from '../classroom/ClassroomUi';
import { formatClassroomDate } from '../../utils/classroomHelpers';

const SCROLL_LIST_THRESHOLD = 4;

export default function TeacherHomeworkClassPickerModal({
  open,
  classrooms,
  onClose,
  onConfirm,
}) {
  const [selectedId, setSelectedId] = useState('');
  const [search, setSearch] = useState('');

  useEffect(() => {
    if (!open || !classrooms.length) return;
    setSelectedId(String(classrooms[0].id));
    setSearch('');
  }, [open, classrooms]);

  const filteredClassrooms = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return classrooms;
    return classrooms.filter((classroom) => {
      const title = String(classroom.title || '').toLowerCase();
      const id = String(classroom.id || '');
      return title.includes(query) || id.includes(query);
    });
  }, [classrooms, search]);

  useEffect(() => {
    if (!open || !filteredClassrooms.length) return;
    const stillVisible = filteredClassrooms.some((item) => String(item.id) === selectedId);
    if (!stillVisible) {
      setSelectedId(String(filteredClassrooms[0].id));
    }
  }, [filteredClassrooms, open, selectedId]);

  if (!open || !classrooms.length) return null;

  const showSearch = classrooms.length >= 5;
  const enableListScroll = filteredClassrooms.length > SCROLL_LIST_THRESHOLD;

  return createPortal(
    <div className="fixed inset-0 z-[100] flex min-h-[100dvh] items-center justify-center p-4">
      <div
        aria-hidden="true"
        className="absolute inset-0 min-h-[100dvh] bg-black/40"
        onClick={onClose}
      />
      <div
        aria-labelledby="homework-class-picker-title"
        aria-modal="true"
        className="relative z-10 flex max-h-[min(88dvh,720px)] w-full max-w-2xl flex-col overflow-hidden rounded-2xl border border-[#dfbfbd]/30 bg-white shadow-xl"
        role="dialog"
      >
        <div className="border-b border-gray-100 px-6 py-5">
          <div className="flex items-start justify-between gap-3">
            <div>
              <h3 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828]" id="homework-class-picker-title">
                Chọn lớp để giao bài
              </h3>
              <p className="mt-1 text-xs leading-5 text-[#8b706e]">
                Chọn một lớp trong danh sách bên dưới. Hệ thống sẽ mở tab <strong>Bài tập</strong> để tạo bài.
              </p>
            </div>
            <button
              aria-label="Đóng"
              className="rounded-xl p-2 text-[#8b706e] transition hover:bg-gray-100"
              onClick={onClose}
              type="button"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {showSearch ? (
            <label className="relative mt-4 block">
              <Search className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-[#8b706e]" />
              <input
                className="w-full rounded-xl border border-[#dfbfbd]/60 bg-[#fffafb] py-3 pl-11 pr-4 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white"
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Tìm theo tên lớp hoặc mã lớp..."
                value={search}
              />
            </label>
          ) : null}
        </div>

        <div
          className={`relative min-h-0 flex-1 px-6 py-4 ${
            enableListScroll ? 'max-h-[min(52dvh,420px)] overflow-y-auto' : 'overflow-visible'
          }`}
        >
          {filteredClassrooms.length ? (
            <div className="space-y-3">
              {filteredClassrooms.map((classroom) => {
                const isSelected = String(classroom.id) === selectedId;
                return (
                  <button
                    key={classroom.id}
                    className={`w-full rounded-2xl border p-4 text-left transition ${
                      isSelected
                        ? 'border-[#730014] bg-[#fffafb] shadow-sm ring-1 ring-[#730014]/15'
                        : 'border-[#e5e7eb] bg-white hover:border-[#dfbfbd] hover:bg-[#fffafb]/60'
                    }`}
                    onClick={() => setSelectedId(String(classroom.id))}
                    type="button"
                  >
                    <div className="flex items-start gap-3">
                      <div className={`mt-0.5 flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-full border ${
                        isSelected ? 'border-[#730014] bg-[#730014] text-white' : 'border-[#dfbfbd] bg-white'
                      }`}
                      >
                        {isSelected ? <CheckCircle2 className="h-3.5 w-3.5" /> : null}
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-center gap-2">
                          <ClassroomTypeBadge mode={classroom.deliveryMode} />
                          <StatusBadge status={classroom.classroomStatus} />
                        </div>
                        <h4 className="mt-2 font-['Manrope'] text-base font-extrabold text-[#2b2828] line-clamp-2">
                          {classroom.title || `Lớp #${classroom.id}`}
                        </h4>
                        <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-xs text-[#8b706e]">
                          <span className="inline-flex items-center gap-1">
                            <Users className="h-3.5 w-3.5" />
                            {classroom.enrolledCount ?? 0} học viên
                          </span>
                          <span className="inline-flex items-center gap-1">
                            <Calendar className="h-3.5 w-3.5" />
                            Khai giảng {formatClassroomDate(classroom.startDate)}
                          </span>
                          <span>Mã lớp #{classroom.id}</span>
                        </div>
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>
          ) : (
            <div className="rounded-xl border border-dashed border-[#dfbfbd] px-4 py-8 text-center text-sm text-[#8b706e]">
              Không tìm thấy lớp phù hợp với từ khóa &quot;{search.trim()}&quot;.
            </div>
          )}
          {enableListScroll ? (
            <p className="pointer-events-none sticky bottom-0 mt-3 bg-gradient-to-t from-white via-white/95 to-transparent pt-4 text-center text-[11px] font-semibold text-[#8b706e]">
              Cuộn để xem thêm {filteredClassrooms.length} lớp
            </p>
          ) : null}
        </div>

        <div className="flex flex-wrap justify-end gap-3 border-t border-gray-100 px-6 py-4">
          <button
            className="rounded-xl border border-gray-200 bg-white px-5 py-3 text-xs font-extrabold text-[#584140]"
            onClick={onClose}
            type="button"
          >
            Hủy
          </button>
          <button
            className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-5 py-3 text-xs font-extrabold text-white shadow-sm transition hover:bg-[#730014] disabled:cursor-not-allowed disabled:opacity-50"
            disabled={!selectedId}
            onClick={() => onConfirm?.(selectedId)}
            type="button"
          >
            Tiếp tục giao bài
            <ArrowRight className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>,
    document.body,
  );
}
