import { useEffect, useMemo, useState } from 'react';
import { BookMarked, Plus, RefreshCw, Save, Send, Trash2 } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import {
  CARD_CLASS,
  DANGER_BUTTON_CLASS,
  EMPTY_STATE_CLASS,
  ERROR_NOTICE_CLASS,
  FIELD_CLASS,
  GHOST_BUTTON_CLASS,
  PANEL_CLASS,
  PRIMARY_BUTTON_CLASS,
  SECONDARY_BUTTON_CLASS,
  SUCCESS_NOTICE_CLASS,
  TEXTAREA_CLASS,
} from '../../utils/formStyles';

const emptyItem = {
  title: '',
  description: '',
  displayOrder: 0,
  sessionNumber: '',
  sessionPlan: '',
  homeworkNotes: '',
  quizNotes: '',
  teacherNotes: '',
  linkedSessionId: '',
  status: 'DRAFT',
};

export default function ContentManagerSyllabusBuilderPage() {
  const [classrooms, setClassrooms] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [items, setItems] = useState([]);
  const [form, setForm] = useState(emptyItem);
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadClassrooms = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await classroomApi.getContentManagerClassrooms();
      setClassrooms(data);
      if (!selectedId && data.length > 0) setSelectedId(String(data[0].id));
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được danh sách lớp học.');
    } finally {
      setLoading(false);
    }
  };

  const loadSyllabus = async (classroomId) => {
    if (!classroomId) return;
    try {
      setItems(await classroomApi.getContentManagerSyllabus(classroomId));
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được giáo trình.');
    }
  };

  useEffect(() => { loadClassrooms(); }, []);
  useEffect(() => { if (selectedId) loadSyllabus(selectedId); }, [selectedId]);

  const classroomOptions = classrooms.map((item) => ({ label: item.title, value: String(item.id) }));

  const resetForm = () => {
    setEditingId(null);
    setForm(emptyItem);
  };

  const openEdit = (item) => {
    setEditingId(item.id);
    setForm({
      title: item.title || '',
      description: item.description || '',
      displayOrder: item.displayOrder ?? 0,
      sessionNumber: item.sessionNumber ?? '',
      sessionPlan: item.sessionPlan || '',
      homeworkNotes: item.homeworkNotes || '',
      quizNotes: item.quizNotes || '',
      teacherNotes: item.teacherNotes || '',
      linkedSessionId: item.linkedSessionId ?? '',
      status: item.status || 'DRAFT',
    });
  };

  const saveItem = async () => {
    if (!selectedId || !form.title.trim()) {
      setError('Vui lòng chọn lớp và nhập tiêu đề mục giáo trình.');
      return;
    }
    setWorking(true);
    setError('');
    setSuccess('');
    const payload = {
      ...form,
      displayOrder: Number(form.displayOrder || 0),
      sessionNumber: form.sessionNumber === '' ? null : Number(form.sessionNumber),
      linkedSessionId: form.linkedSessionId === '' ? null : Number(form.linkedSessionId),
    };
    try {
      if (editingId) {
        await classroomApi.updateContentManagerSyllabusItem(editingId, payload);
      } else {
        await classroomApi.createContentManagerSyllabusItem(selectedId, payload);
      }
      await loadSyllabus(selectedId);
      resetForm();
      setSuccess('Đã lưu mục giáo trình.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu được mục giáo trình.');
    } finally {
      setWorking(false);
    }
  };

  const submitReview = async (itemId) => {
    setWorking(true);
    setError('');
    try {
      await classroomApi.submitContentManagerSyllabusReview(itemId);
      await loadSyllabus(selectedId);
      setSuccess('Đã gửi duyệt mục giáo trình.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không gửi được yêu cầu duyệt.');
    } finally {
      setWorking(false);
    }
  };

  const deleteItem = async (itemId) => {
    if (!window.confirm('Xóa mục giáo trình này?')) return;
    await classroomApi.deleteContentManagerSyllabusItem(itemId);
    await loadSyllabus(selectedId);
  };

  const sortedItems = useMemo(
    () => [...items].sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0)),
    [items],
  );

  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(sortedItems, 8, selectedId);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="font-['Manrope'] text-2xl font-extrabold text-slate-900">Biên soạn giáo trình lớp học</h2>
          <p className="mt-1 text-sm text-slate-600">Xây dựng kế hoạch buổi học, bài tập, quiz và ghi chú giáo viên theo từng lớp.</p>
        </div>
        <button type="button" onClick={loadClassrooms} className={SECONDARY_BUTTON_CLASS}>
          <RefreshCw className="h-4 w-4" /> Tải lại
        </button>
      </div>

      {error && <div className={ERROR_NOTICE_CLASS}>{error}</div>}
      {success && <div className={SUCCESS_NOTICE_CLASS}>{success}</div>}

      <div className={PANEL_CLASS}>
        <BrandedSelect label="Lớp học" value={selectedId} onChange={setSelectedId} options={classroomOptions} />
      </div>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_420px]">
        <div className="space-y-3">
          {loading ? (
            <p className="text-sm font-semibold text-slate-500">Đang tải...</p>
          ) : sortedItems.length === 0 ? (
            <div className={EMPTY_STATE_CLASS}>Chưa có mục giáo trình.</div>
          ) : pageItems.map((item) => (
            <div key={item.id} className={`${CARD_CLASS} transition hover:border-[#dfbfbd]`}>
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <BookMarked className="h-4 w-4 text-[#730014]" />
                    <h3 className="font-['Manrope'] text-lg font-extrabold text-slate-900">{item.title}</h3>
                    <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-600">
                      {item.reviewStatus || item.status}
                    </span>
                  </div>
                  <p className="mt-1 text-sm text-slate-600">{item.description}</p>
                  {item.sessionPlan && (
                    <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-slate-700">{item.sessionPlan}</p>
                  )}
                </div>
                <div className="flex flex-wrap gap-2">
                  <button type="button" onClick={() => openEdit(item)} className={GHOST_BUTTON_CLASS}>Sửa</button>
                  {(item.reviewStatus === 'DRAFT' || item.reviewStatus === 'REJECTED') && (
                    <button
                      type="button"
                      onClick={() => submitReview(item.id)}
                      className="inline-flex items-center gap-1 rounded-xl border border-amber-200 bg-amber-50 px-3 py-1.5 text-xs font-semibold text-amber-700 transition hover:bg-amber-100"
                    >
                      <Send className="h-3.5 w-3.5" /> Gửi duyệt
                    </button>
                  )}
                  <button type="button" onClick={() => deleteItem(item.id)} className={DANGER_BUTTON_CLASS}>
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
            </div>
          ))}
          <Pagination page={page} totalPages={totalPages} onChange={setPage} totalItems={totalItems} pageSize={8} />
        </div>

        <div className={PANEL_CLASS}>
          <h3 className="mb-4 font-['Manrope'] text-lg font-extrabold text-slate-900">
            {editingId ? 'Chỉnh sửa mục' : 'Thêm mục giáo trình'}
          </h3>
          <div className="space-y-4">
            {Object.entries({
              title: 'Tiêu đề',
              description: 'Mô tả',
              sessionPlan: 'Kế hoạch buổi học',
              homeworkNotes: 'Ghi chú bài tập',
              quizNotes: 'Ghi chú quiz',
              teacherNotes: 'Ghi chú giáo viên',
            }).map(([key, label]) => (
              <label key={key} className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">{label}</span>
                {['sessionPlan', 'homeworkNotes', 'quizNotes', 'teacherNotes', 'description'].includes(key) ? (
                  <textarea
                    value={form[key]}
                    onChange={(e) => setForm({ ...form, [key]: e.target.value })}
                    rows={3}
                    className={TEXTAREA_CLASS}
                  />
                ) : (
                  <input
                    value={form[key]}
                    onChange={(e) => setForm({ ...form, [key]: e.target.value })}
                    className={FIELD_CLASS}
                  />
                )}
              </label>
            ))}
            <div className="grid grid-cols-2 gap-3">
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Thứ tự</span>
                <input
                  type="number"
                  value={form.displayOrder}
                  onChange={(e) => setForm({ ...form, displayOrder: e.target.value })}
                  className={FIELD_CLASS}
                />
              </label>
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Buổi số</span>
                <input
                  type="number"
                  value={form.sessionNumber}
                  onChange={(e) => setForm({ ...form, sessionNumber: e.target.value })}
                  className={FIELD_CLASS}
                />
              </label>
            </div>
            <div className="flex flex-wrap gap-2 border-t border-slate-100 pt-4">
              <button type="button" onClick={saveItem} disabled={working} className={PRIMARY_BUTTON_CLASS}>
                <Save className="h-4 w-4" /> Lưu
              </button>
              <button type="button" onClick={resetForm} className={SECONDARY_BUTTON_CLASS}>
                <Plus className="h-4 w-4" /> Mới
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
