import { useEffect, useState, useMemo } from 'react';
import {
  BookOpen,
  Calendar,
  Clock,
  Plus,
  Trash2,
  FileText,
  CheckCircle2,
  AlertCircle,
  HelpCircle,
  Settings,
  Info,
  ChevronRight,
  ListOrdered,
  Layers,
  Users,
  Award,
} from 'lucide-react';
import { formatClassroomDate } from '../../utils/classroomHelpers';
import classroomApi from '../../api/classroomApi';
import { Panel } from '../../components/content-manager/ContentManagerUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import {
  ClassroomEmptyState,
  ClassroomErrorState,
  ClassroomLoadingState,
} from '../../components/classroom/ClassroomUi';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';

export default function ContentManagerClassroomsPage() {
  const [classrooms, setClassrooms] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [syllabus, setSyllabus] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionMessage, setActionMessage] = useState('');
  const [form, setForm] = useState({ weekNumber: '', title: '', description: '' });

  const loadClassrooms = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await classroomApi.getContentManagerClassrooms();
      setClassrooms(data);
      if (data.length > 0) {
        setSelectedId(String(data[0].id));
      }
    } catch (err) {
      setClassrooms([]);
      setError(getClassroomErrorMessage(err, 'Không thể tải danh sách lớp.'));
    } finally {
      setLoading(false);
    }
  };

  const loadSyllabus = async (classroomId) => {
    if (!classroomId) {
      setSyllabus([]);
      return;
    }
    try {
      const data = await classroomApi.getContentManagerSyllabus(classroomId);
      setSyllabus(data);
    } catch (err) {
      setSyllabus([]);
      setActionMessage(getClassroomErrorMessage(err, 'Không thể tải đề cương.'));
    }
  };

  useEffect(() => {
    loadClassrooms();
  }, []);

  useEffect(() => {
    if (selectedId) loadSyllabus(selectedId);
  }, [selectedId]);

  const handleCreate = async () => {
    setActionMessage('');
    try {
      await classroomApi.createContentManagerSyllabusItem(selectedId, {
        weekNumber: form.weekNumber ? Number(form.weekNumber) : null,
        title: form.title,
        description: form.description,
      });
      setForm({ weekNumber: '', title: '', description: '' });
      setActionMessage('Đã thêm mục đề cương thành công.');
      await loadSyllabus(selectedId);
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể thêm mục đề cương.'));
    }
  };

  const handleDelete = async (itemId) => {
    setActionMessage('');
    try {
      await classroomApi.deleteContentManagerSyllabusItem(itemId);
      setActionMessage('Đã xóa mục đề cương thành công.');
      await loadSyllabus(selectedId);
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể xóa mục đề cương.'));
    }
  };

  const selectedClassroom = useMemo(() => {
    return classrooms.find((c) => String(c.id) === selectedId) || null;
  }, [classrooms, selectedId]);

  if (loading) {
    return <ClassroomLoadingState message="Đang tải dữ liệu lớp học..." />;
  }

  if (error) {
    return <ClassroomErrorState message={error} onRetry={loadClassrooms} />;
  }

  return (
    <div className="space-y-8">
      {/* Page Header */}
      <div className="rounded-[32px] border border-[#dfbfbd]/20 bg-gradient-to-br from-white via-[#fffafb] to-[#fff3f4] p-8 shadow-sm relative overflow-hidden">
        <div className="absolute right-0 top-0 -mr-16 -mt-16 h-64 w-64 rounded-full bg-[#730014]/5 blur-3xl"></div>
        <div className="max-w-3xl">
          <p className="text-[12px] font-extrabold uppercase tracking-[0.18em] text-[#730014]">Content Studio</p>
          <h1 className="mt-3 font-['Manrope'] text-4xl font-extrabold text-[#2b2828]">Quản lý Đề cương Lớp học</h1>
          <p className="mt-3 text-sm leading-7 text-[#584140]">
            Thiết kế lộ trình học tập, phân chia bài giảng theo tuần và xây dựng đề cương chi tiết cho từng lớp học offline hoặc trực tuyến.
          </p>
        </div>
      </div>

      {/* Action Notification */}
      {actionMessage ? (
        <div className={`rounded-2xl border p-4 text-xs flex items-start gap-2 ${
          actionMessage.includes('thành công')
            ? 'bg-emerald-50 border-emerald-100 text-emerald-800'
            : 'bg-rose-50 border-rose-100 text-rose-800'
        }`}>
          {actionMessage.includes('thành công') ? (
            <CheckCircle2 className="h-4 w-4 flex-shrink-0 mt-0.5 text-emerald-700" />
          ) : (
            <AlertCircle className="h-4 w-4 flex-shrink-0 mt-0.5 text-rose-700" />
          )}
          <p className="leading-5">{actionMessage}</p>
        </div>
      ) : null}

      {/* Class Selector Panel */}
      <Panel className="p-6">
        <div className="max-w-md space-y-2">
          <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider flex items-center gap-1">
            <BookOpen className="h-4 w-4 text-[#730014]" />
            Lớp học cần thiết kế đề cương
          </label>
          <BrandedSelect
            onChange={(event) => setSelectedId(event.target.value)}
            options={classrooms.map((item) => ({ label: item.title, value: String(item.id) }))}
            placeholder="Chọn lớp học..."
            value={selectedId}
          />
        </div>
      </Panel>

      {!classrooms.length ? (
        <ClassroomEmptyState
          description="Hiện tại chưa có lớp học nào được khởi tạo để thiết kế đề cương."
          title="Chưa có lớp học"
          icon={HelpCircle}
        />
      ) : (
        <div className="grid gap-6 lg:grid-cols-[1fr_380px]">
          {/* Left Column: Builder Workspace */}
          <div className="space-y-6">
            {/* Syllabus Creator Form */}
            <Panel className="p-6 space-y-6">
              <div className="flex items-start gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-50 text-[#730014] flex-shrink-0">
                  <Plus className="h-6 w-6" />
                </div>
                <div>
                  <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Thêm mục đề cương mới</h3>
                  <p className="mt-1 text-xs text-[#8b706e] leading-5">Thiết lập tuần học mới, tiêu đề bài giảng và mô tả chi tiết nội dung học tập.</p>
                </div>
              </div>

              <div className="grid gap-4 md:grid-cols-3 pt-2">
                <div className="space-y-2">
                  <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Tuần số</label>
                  <input
                    className="w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-4 py-3 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white"
                    onChange={(event) => setForm((current) => ({ ...current, weekNumber: event.target.value }))}
                    placeholder="Ví dụ: 1"
                    value={form.weekNumber}
                  />
                </div>
                <div className="space-y-2 md:col-span-2">
                  <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Tiêu đề tuần học / Bài giảng</label>
                  <input
                    className="w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-4 py-3 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white"
                    onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
                    placeholder="Ví dụ: Giới thiệu tổng quan & IELTS Listening Section 1"
                    value={form.title}
                  />
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Mô tả nội dung học tập chi tiết</label>
                <textarea
                  className="min-h-[120px] w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-4 py-3 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white"
                  onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
                  placeholder="Mô tả các chủ điểm ngữ pháp, từ vựng, dạng bài tập sẽ được giảng dạy trong tuần..."
                  value={form.description}
                />
              </div>

              <div className="pt-2 flex justify-end">
                <button
                  className="inline-flex items-center gap-1.5 rounded-2xl bg-[#4b0009] px-6 py-3.5 text-sm font-extrabold text-white shadow-md transition hover:bg-[#730014] hover:shadow active:scale-95"
                  onClick={handleCreate}
                  type="button"
                >
                  <Plus className="h-4 w-4" />
                  Thêm mục đề cương
                </button>
              </div>
            </Panel>

            {/* Syllabus List Panel */}
            <Panel className="overflow-hidden">
              <div className="border-b border-[#f0e3e4] px-6 py-5 bg-gray-50/30 flex items-center gap-2">
                <ListOrdered className="h-5 w-5 text-[#730014]" />
                <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Đề cương lớp học hiện tại</h3>
              </div>

              {!syllabus.length ? (
                <div className="p-6">
                  <ClassroomEmptyState
                    description="Lớp học này chưa có bất kỳ mục đề cương nào. Hãy bắt đầu thiết kế bằng form ở trên."
                    title="Chưa có đề cương học tập"
                    icon={Layers}
                  />
                </div>
              ) : (
                <div className="divide-y divide-gray-100">
                  {syllabus.map((item) => (
                    <article
                      key={item.id}
                      className="flex flex-col gap-4 p-6 md:flex-row md:items-center md:justify-between hover:bg-[#fffafb]/10 transition"
                    >
                      <div className="space-y-1.5">
                        <span className="inline-flex rounded-full bg-[#fff1f3] px-2.5 py-0.5 text-[10px] font-extrabold uppercase text-[#730014]">
                          {item.weekNumber ? `Tuần ${item.weekNumber}` : 'Chủ đề bổ sung'}
                        </span>
                        <h4 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">
                          {item.title}
                        </h4>
                        {item.description ? (
                          <p className="text-xs text-[#584140] leading-5 max-w-xl">{item.description}</p>
                        ) : null}
                      </div>

                      <button
                        className="inline-flex items-center gap-1.5 rounded-xl border border-rose-100 bg-rose-50/20 px-4 py-2 text-xs font-extrabold text-rose-700 hover:bg-rose-50 hover:text-rose-800 transition active:scale-95 flex-shrink-0"
                        onClick={() => handleDelete(item.id)}
                        type="button"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                        Xóa mục
                      </button>
                    </article>
                  ))}
                </div>
              )}
            </Panel>
          </div>

          {/* Right Column: Studio Preview / Metadata Panel */}
          <div className="space-y-6">
            {selectedClassroom && (
              <Panel className="p-6 space-y-5">
                <h3 className="font-['Manrope'] text-base font-extrabold text-[#2b2828] border-b border-gray-50 pb-3 flex items-center gap-2">
                  <Info className="h-4.5 w-4.5 text-[#730014]" />
                  Thông tin lớp học
                </h3>

                <div className="space-y-3 text-xs text-[#584140]">
                  <p className="flex items-center gap-2">
                    <BookOpen className="h-4 w-4 text-[#730014]" />
                    <span>Lớp: <strong className="text-[#2b2828]">{selectedClassroom.title}</strong></span>
                  </p>
                  <p className="flex items-center gap-2">
                    <Calendar className="h-4 w-4 text-[#730014]" />
                    <span>Khai giảng: <strong className="text-[#2b2828]">{formatClassroomDate(selectedClassroom.startDate)}</strong></span>
                  </p>
                  <p className="flex items-center gap-2">
                    <Users className="h-4 w-4 text-[#730014]" />
                    <span>Sĩ số: <strong className="text-[#2b2828]">{selectedClassroom.enrolledCount ?? 0} / {selectedClassroom.maxCapacity ?? '—'} học viên</strong></span>
                  </p>
                </div>

                <div className="rounded-2xl bg-rose-50/20 border border-rose-100/40 p-4 text-xs text-[#584140] space-y-1">
                  <p className="font-extrabold text-[#730014]">Lưu ý thiết kế đề cương:</p>
                  <p className="leading-5">Đề cương được thiết kế tại đây sẽ hiển thị trực quan cho học viên và giảng viên tại trang chi tiết lớp học. Hãy đảm bảo nội dung chính xác và phân chia tuần học hợp lý.</p>
                </div>
              </Panel>
            )}

            {/* Test Bank Quick Tips */}
            <Panel className="p-6 space-y-4">
              <h3 className="font-['Manrope'] text-base font-extrabold text-[#2b2828] border-b border-gray-50 pb-3 flex items-center gap-2">
                <Award className="h-4.5 w-4.5 text-[#730014]" />
                Thư viện học liệu & Đề thi
              </h3>
              <p className="text-xs text-[#584140] leading-5">
                Bạn có thể tái sử dụng các bộ câu hỏi, bài tập thực hành hoặc đề thi thử từ <strong>Test Bank</strong> để đính kèm trực tiếp vào đề cương hoặc giao bài tập tự động cho lớp học này.
              </p>
              <div className="space-y-2 pt-1">
                <div className="rounded-xl bg-gray-50 border border-gray-100 p-3 text-xs font-bold text-[#584140] flex items-center justify-between">
                  <span>Bài tập trắc nghiệm (Quiz)</span>
                  <ChevronRight className="h-4 w-4 text-gray-400" />
                </div>
                <div className="rounded-xl bg-gray-50 border border-gray-100 p-3 text-xs font-bold text-[#584140] flex items-center justify-between">
                  <span>Đề thi thử IELTS / TOEIC</span>
                  <ChevronRight className="h-4 w-4 text-gray-400" />
                </div>
              </div>
            </Panel>
          </div>
        </div>
      )}
    </div>
  );
}
