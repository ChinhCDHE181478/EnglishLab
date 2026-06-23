import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Bell, BookMarked, Check, FileStack, Library, Pencil, Plus, RefreshCw, Trash2, X } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import { ContentManagerLoadingState, Panel, SectionTitle, TextField } from '../../components/content-manager/ContentManagerUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { ClassroomEmptyState, ClassroomErrorState } from '../../components/classroom/ClassroomUi';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { formatClassroomDateTime } from '../../utils/classroomHelpers';

const PAGE_SIZE = 8;
const TABS = [
  { id: 'materials', label: 'Tài liệu', icon: FileStack },
  { id: 'announcements', label: 'Thông báo', icon: Bell },
  { id: 'syllabus', label: 'Giáo trình', icon: BookMarked },
];

const emptyAnnouncementForm = { title: '', content: '' };
const emptySyllabusForm = { title: '', description: '', displayOrder: '0', sessionPlan: '', status: 'DRAFT' };

export default function ContentManagerClassroomsPage() {
  const [classrooms, setClassrooms] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [activeTab, setActiveTab] = useState('materials');
  const [materials, setMaterials] = useState([]);
  const [announcements, setAnnouncements] = useState([]);
  const [syllabusItems, setSyllabusItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [reloading, setReloading] = useState(false);
  const [page, setPage] = useState(1);
  const [announcementForm, setAnnouncementForm] = useState(emptyAnnouncementForm);
  const [announcementEditorOpen, setAnnouncementEditorOpen] = useState(false);
  const [syllabusForm, setSyllabusForm] = useState(emptySyllabusForm);
  const [syllabusEditingId, setSyllabusEditingId] = useState(null);
  const [syllabusEditorOpen, setSyllabusEditorOpen] = useState(false);
  const [saving, setSaving] = useState(false);

  const selectedClassroom = useMemo(
    () => classrooms.find((item) => String(item.id) === selectedId) || null,
    [classrooms, selectedId],
  );

  const loadClassrooms = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await classroomApi.getContentManagerClassrooms();
      setClassrooms(data);
      if (data.length > 0) {
        setSelectedId((current) => (data.some((item) => String(item.id) === current) ? current : String(data[0].id)));
      } else {
        setSelectedId('');
      }
    } catch (err) {
      setClassrooms([]);
      setError(getClassroomErrorMessage(err, 'Không thể tải danh sách lớp học.'));
    } finally {
      setLoading(false);
    }
  };

  const loadTabData = async (classroomId, tab = activeTab) => {
    if (!classroomId) {
      setMaterials([]);
      setAnnouncements([]);
      setSyllabusItems([]);
      return;
    }
    setReloading(true);
    setMessage('');
    try {
      if (tab === 'materials') {
        setMaterials(await classroomApi.getContentManagerMaterials(classroomId));
      } else if (tab === 'announcements') {
        setAnnouncements(await classroomApi.getContentManagerAnnouncements(classroomId));
      } else {
        setSyllabusItems(await classroomApi.getContentManagerSyllabus(classroomId));
      }
    } catch (err) {
      if (tab === 'materials') setMaterials([]);
      if (tab === 'announcements') setAnnouncements([]);
      if (tab === 'syllabus') setSyllabusItems([]);
      setMessage(getClassroomErrorMessage(err, 'Không thể tải dữ liệu lớp học.'));
    } finally {
      setReloading(false);
    }
  };

  useEffect(() => {
    loadClassrooms();
  }, []);

  useEffect(() => {
    if (selectedId) {
      setPage(1);
      loadTabData(selectedId, activeTab);
    }
  }, [selectedId, activeTab]);

  const stats = useMemo(() => ({
    total: materials.length,
    center: materials.filter((item) => item.sourceType === 'CENTER_LIBRARY').length,
    custom: materials.filter((item) => item.sourceType !== 'CENTER_LIBRARY').length,
  }), [materials]);

  const listForTab = activeTab === 'materials' ? materials : activeTab === 'announcements' ? announcements : syllabusItems;
  const totalPages = Math.max(1, Math.ceil(listForTab.length / PAGE_SIZE));
  const visibleItems = listForTab.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const saveAnnouncement = async () => {
    if (!selectedId || !announcementForm.title.trim() || !announcementForm.content.trim()) {
      setMessage('Hãy nhập tiêu đề và nội dung thông báo.');
      return;
    }
    setSaving(true);
    setMessage('');
    try {
      await classroomApi.createContentManagerAnnouncement(selectedId, {
        title: announcementForm.title.trim(),
        content: announcementForm.content.trim(),
      });
      setAnnouncementForm(emptyAnnouncementForm);
      setAnnouncementEditorOpen(false);
      setMessage('Đã đăng thông báo cho lớp học.');
      await loadTabData(selectedId, 'announcements');
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, 'Không thể đăng thông báo.'));
    } finally {
      setSaving(false);
    }
  };

  const deleteAnnouncement = async (announcementId) => {
    if (!window.confirm('Xóa thông báo này?')) return;
    setSaving(true);
    setMessage('');
    try {
      await classroomApi.deleteContentManagerAnnouncement(announcementId);
      setMessage('Đã xóa thông báo.');
      await loadTabData(selectedId, 'announcements');
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, 'Không thể xóa thông báo.'));
    } finally {
      setSaving(false);
    }
  };

  const openSyllabusCreate = () => {
    setSyllabusEditingId(null);
    setSyllabusForm(emptySyllabusForm);
    setSyllabusEditorOpen(true);
  };

  const openSyllabusEdit = (item) => {
    setSyllabusEditingId(item.id);
    setSyllabusForm({
      title: item.title || '',
      description: item.description || '',
      displayOrder: String(item.displayOrder ?? 0),
      sessionPlan: item.sessionPlan || '',
      status: item.status || 'DRAFT',
    });
    setSyllabusEditorOpen(true);
  };

  const saveSyllabus = async () => {
    if (!selectedId || !syllabusForm.title.trim()) {
      setMessage('Hãy nhập tiêu đề mục giáo trình.');
      return;
    }
    setSaving(true);
    setMessage('');
    const payload = {
      title: syllabusForm.title.trim(),
      description: syllabusForm.description.trim() || null,
      displayOrder: Number(syllabusForm.displayOrder || 0),
      sessionPlan: syllabusForm.sessionPlan.trim() || null,
      status: syllabusForm.status || 'DRAFT',
    };
    try {
      if (syllabusEditingId) {
        await classroomApi.updateContentManagerSyllabusItem(syllabusEditingId, payload);
        setMessage('Đã cập nhật mục giáo trình.');
      } else {
        await classroomApi.createContentManagerSyllabusItem(selectedId, payload);
        setMessage('Đã thêm mục giáo trình.');
      }
      setSyllabusEditorOpen(false);
      setSyllabusEditingId(null);
      setSyllabusForm(emptySyllabusForm);
      await loadTabData(selectedId, 'syllabus');
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, 'Không thể lưu giáo trình.'));
    } finally {
      setSaving(false);
    }
  };

  const deleteSyllabus = async (itemId) => {
    if (!window.confirm('Xóa mục giáo trình này?')) return;
    setSaving(true);
    setMessage('');
    try {
      await classroomApi.deleteContentManagerSyllabusItem(itemId);
      setMessage('Đã xóa mục giáo trình.');
      await loadTabData(selectedId, 'syllabus');
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, 'Không thể xóa giáo trình.'));
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <ContentManagerLoadingState message="Đang tải dữ liệu lớp học..." />;
  }

  if (error) {
    return <ClassroomErrorState message={error} onRetry={loadClassrooms} />;
  }

  if (!classrooms.length) {
    return (
      <ClassroomEmptyState
        title="Chưa có lớp học nào"
        description="Khi lớp học tại trung tâm được mở, bạn sẽ quản lý tài liệu, thông báo và giáo trình tại đây."
      />
    );
  }

  return (
    <motion.div
      className="space-y-6"
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.32, ease: 'easeOut' }}
    >
      {message ? (
        <div className={`rounded-2xl border px-4 py-3 text-sm ${
          /Không thể|Hãy nhập/.test(message) ? 'border-rose-100 bg-rose-50 text-rose-800' : 'border-emerald-100 bg-emerald-50 text-emerald-800'
        }`}>
          {message}
        </div>
      ) : null}

      <Panel className="p-6">
        <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_auto] xl:items-end">
          <div className="space-y-2">
            <label className="block text-xs font-bold uppercase tracking-[0.16em] text-[#8b706e]">
              Lớp đang quản lý
            </label>
            <BrandedSelect
              value={selectedId}
              onChange={(event) => setSelectedId(event.target.value)}
              options={classrooms.map((item) => ({ label: item.title, value: String(item.id) }))}
              placeholder="Chọn lớp học..."
            />
          </div>

          <div className="flex flex-wrap gap-3">
            <button
              className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd]/70 bg-white px-4 py-3 text-sm font-bold text-[#730014] transition hover:bg-[#fff2f3]"
              onClick={() => loadTabData(selectedId, activeTab)}
              type="button"
            >
              <RefreshCw className={`h-4 w-4 ${reloading ? 'animate-spin' : ''}`} />
              Làm mới
            </button>
            <Link
              className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-bold text-white transition hover:bg-[#730014]"
              to="/content-manager/materials"
            >
              <Library className="h-4 w-4" />
              Kho học liệu trung tâm
            </Link>
          </div>
        </div>

        <div className="mt-5 flex flex-wrap gap-2">
          {TABS.map((tab) => {
            const Icon = tab.icon;
            const active = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                className={`inline-flex items-center gap-2 rounded-2xl px-4 py-2.5 text-sm font-bold transition ${
                  active ? 'bg-[#4b0009] text-white' : 'border border-[#dfbfbd]/70 bg-white text-[#730014] hover:bg-[#fff2f3]'
                }`}
                onClick={() => setActiveTab(tab.id)}
                type="button"
              >
                <Icon className="h-4 w-4" />
                {tab.label}
              </button>
            );
          })}
        </div>
      </Panel>

      {selectedClassroom && activeTab === 'materials' ? (
        <div className="space-y-6">
          <Panel className="p-6">
            <SectionTitle title="Tóm tắt tài liệu đã gắn" />
            <p className="mt-2 text-sm leading-6 text-[#584140]">
              Lớp <span className="font-semibold text-[#2b2828]">{selectedClassroom.title}</span> hiện đang dùng {stats.total} tài liệu.
            </p>
            <div className="mt-5 grid gap-4 md:grid-cols-3">
              <OverviewCard icon={FileStack} label="Tổng tài liệu" value={stats.total} />
              <OverviewCard icon={Library} label="Từ kho trung tâm" value={stats.center} />
              <OverviewCard icon={FileStack} label="Riêng của lớp" value={stats.custom} />
            </div>
          </Panel>

          <Panel className="overflow-hidden">
            <div className="border-b border-[#f0e3e4] px-6 py-5">
              <SectionTitle title="Danh sách tài liệu đang gắn" />
            </div>
            {!materials.length ? (
              <div className="p-6">
                <ClassroomEmptyState
                  title="Lớp này chưa có tài liệu"
                  description="Giáo viên có thể chọn học liệu từ kho trung tâm hoặc tải thêm tài liệu riêng của lớp."
                />
              </div>
            ) : (
              <ItemPager list={visibleItems} page={page} totalPages={totalPages} onPageChange={setPage} renderItem={(item) => (
                <article key={item.id} className="space-y-4 px-6 py-5">
                  <div className="flex flex-wrap items-start justify-between gap-4">
                    <div className="space-y-2">
                      <div className="flex flex-wrap items-center gap-2">
                        <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">{item.title}</h3>
                        <SourceBadge value={item.sourceType} />
                      </div>
                      <p className="text-sm leading-6 text-[#584140]">{item.description || 'Tài liệu này hiện đã sẵn sàng cho học viên của lớp.'}</p>
                    </div>
                    {item.fileUrl ? (
                      <a className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd]/70 bg-white px-4 py-2.5 text-sm font-bold text-[#730014]" href={item.fileUrl} rel="noreferrer" target="_blank">
                        <FileStack className="h-4 w-4" />
                        Mở tài liệu
                      </a>
                    ) : null}
                  </div>
                  <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                    <InfoPill label="Loại" value={item.materialType || item.fileType || 'Tài liệu'} />
                    <InfoPill label="Nguồn" value={item.provider || 'Giáo viên / EnglishLab'} />
                    <InfoPill label="Buổi học gắn kèm" value={item.sessionTitle || 'Không gắn buổi cụ thể'} />
                    <InfoPill label="Cập nhật" value={formatClassroomDateTime(item.updatedAt || item.createdAt)} />
                  </div>
                </article>
              )} />
            )}
          </Panel>
        </div>
      ) : null}

      {selectedClassroom && activeTab === 'announcements' ? (
        <div className="space-y-6">
          <div className="flex flex-wrap justify-end gap-3">
            <button
              className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-bold text-white"
              onClick={() => { setAnnouncementEditorOpen(true); setAnnouncementForm(emptyAnnouncementForm); }}
              type="button"
            >
              <Plus className="h-4 w-4" />
              Đăng thông báo
            </button>
          </div>

          {announcementEditorOpen ? (
            <Panel className="p-6">
              <div className="mb-4 flex items-center justify-between gap-3">
                <h2 className="font-['Manrope'] text-xl font-extrabold text-[#4b0009]">Thông báo mới</h2>
                <button className="rounded-xl p-2 text-[#730014] hover:bg-[#fff2f3]" onClick={() => setAnnouncementEditorOpen(false)} type="button">
                  <X className="h-5 w-5" />
                </button>
              </div>
              <div className="space-y-4">
                <TextField label="Tiêu đề" onChange={(e) => setAnnouncementForm((c) => ({ ...c, title: e.target.value }))} value={announcementForm.title} />
                <TextField label="Nội dung" onChange={(e) => setAnnouncementForm((c) => ({ ...c, content: e.target.value }))} rows={5} textarea value={announcementForm.content} />
                <div className="flex justify-end gap-3">
                  <button className="rounded-2xl border border-[#dfbfbd] px-4 py-3 text-sm font-bold text-[#730014]" onClick={() => setAnnouncementEditorOpen(false)} type="button">Hủy</button>
                  <button className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-bold text-white disabled:opacity-60" disabled={saving} onClick={saveAnnouncement} type="button">
                    <Check className="h-4 w-4" />
                    {saving ? 'Đang đăng...' : 'Đăng thông báo'}
                  </button>
                </div>
              </div>
            </Panel>
          ) : null}

          <Panel className="overflow-hidden">
            {!announcements.length ? (
              <div className="p-6">
                <ClassroomEmptyState title="Chưa có thông báo" description="Đăng thông báo để học viên và giáo viên nắm lịch, tài liệu hoặc nhắc nhở quan trọng." />
              </div>
            ) : (
              <ItemPager list={visibleItems} page={page} totalPages={totalPages} onPageChange={setPage} renderItem={(item) => (
                <article key={item.id} className="space-y-3 px-6 py-5">
                  <div className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                      <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">{item.title}</h3>
                      <p className="mt-1 text-xs text-[#8b706e]">
                        {item.createdByName || 'EnglishLab'} · {formatClassroomDateTime(item.createdAt)}
                      </p>
                    </div>
                    <button className="inline-flex items-center gap-2 rounded-xl border border-rose-200 px-3 py-2 text-sm font-bold text-rose-700 disabled:opacity-50" disabled={saving} onClick={() => deleteAnnouncement(item.id)} type="button">
                      <Trash2 className="h-4 w-4" />
                      Xóa
                    </button>
                  </div>
                  <p className="whitespace-pre-wrap text-sm leading-6 text-[#584140]">{item.content}</p>
                </article>
              )} />
            )}
          </Panel>
        </div>
      ) : null}

      {selectedClassroom && activeTab === 'syllabus' ? (
        <div className="space-y-6">
          <div className="flex flex-wrap justify-end gap-3">
            <button className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-bold text-white" onClick={openSyllabusCreate} type="button">
              <Plus className="h-4 w-4" />
              Thêm mục giáo trình
            </button>
          </div>

          {syllabusEditorOpen ? (
            <Panel className="p-6">
              <div className="mb-4 flex items-center justify-between gap-3">
                <h2 className="font-['Manrope'] text-xl font-extrabold text-[#4b0009]">
                  {syllabusEditingId ? 'Sửa mục giáo trình' : 'Mục giáo trình mới'}
                </h2>
                <button className="rounded-xl p-2 text-[#730014] hover:bg-[#fff2f3]" onClick={() => setSyllabusEditorOpen(false)} type="button">
                  <X className="h-5 w-5" />
                </button>
              </div>
              <div className="grid gap-4 md:grid-cols-2">
                <TextField label="Tiêu đề" onChange={(e) => setSyllabusForm((c) => ({ ...c, title: e.target.value }))} value={syllabusForm.title} />
                <TextField label="Thứ tự hiển thị" onChange={(e) => setSyllabusForm((c) => ({ ...c, displayOrder: e.target.value }))} value={syllabusForm.displayOrder} />
                <div>
                  <span className="mb-2 block text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">Trạng thái</span>
                  <BrandedSelect
                    onChange={(event) => setSyllabusForm((c) => ({ ...c, status: event.target.value }))}
                    options={[
                      { label: 'Bản nháp', value: 'DRAFT' },
                      { label: 'Đã xuất bản', value: 'PUBLISHED' },
                    ]}
                    value={syllabusForm.status}
                  />
                </div>
                <TextField label="Kế hoạch buổi học" onChange={(e) => setSyllabusForm((c) => ({ ...c, sessionPlan: e.target.value }))} value={syllabusForm.sessionPlan} />
                <div className="md:col-span-2">
                  <TextField label="Mô tả" onChange={(e) => setSyllabusForm((c) => ({ ...c, description: e.target.value }))} rows={4} textarea value={syllabusForm.description} />
                </div>
              </div>
              <div className="mt-5 flex justify-end gap-3">
                <button className="rounded-2xl border border-[#dfbfbd] px-4 py-3 text-sm font-bold text-[#730014]" onClick={() => setSyllabusEditorOpen(false)} type="button">Hủy</button>
                <button className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-bold text-white disabled:opacity-60" disabled={saving} onClick={saveSyllabus} type="button">
                  <Check className="h-4 w-4" />
                  {saving ? 'Đang lưu...' : 'Lưu giáo trình'}
                </button>
              </div>
            </Panel>
          ) : null}

          <Panel className="overflow-hidden">
            {!syllabusItems.length ? (
              <div className="p-6">
                <ClassroomEmptyState title="Chưa có giáo trình" description="Thêm các mục đề cương để học viên theo dõi tiến trình và nội dung từng phần của lớp." />
              </div>
            ) : (
              <ItemPager list={visibleItems} page={page} totalPages={totalPages} onPageChange={setPage} renderItem={(item) => (
                <article key={item.id} className="space-y-3 px-6 py-5">
                  <div className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">{item.title}</h3>
                        <span className="rounded-full bg-[#fff2f3] px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-[#730014]">
                          {item.status === 'PUBLISHED' ? 'Đã xuất bản' : 'Bản nháp'}
                        </span>
                      </div>
                      <p className="mt-1 text-xs text-[#8b706e]">Thứ tự: {item.displayOrder ?? 0}</p>
                    </div>
                    <div className="flex gap-2">
                      <button className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] px-3 py-2 text-sm font-bold text-[#730014]" onClick={() => openSyllabusEdit(item)} type="button">
                        <Pencil className="h-4 w-4" />
                        Sửa
                      </button>
                      <button className="inline-flex items-center gap-2 rounded-xl border border-rose-200 px-3 py-2 text-sm font-bold text-rose-700 disabled:opacity-50" disabled={saving} onClick={() => deleteSyllabus(item.id)} type="button">
                        <Trash2 className="h-4 w-4" />
                        Xóa
                      </button>
                    </div>
                  </div>
                  {item.description ? <p className="text-sm leading-6 text-[#584140]">{item.description}</p> : null}
                  {item.sessionPlan ? <p className="text-xs text-[#8b706e]">Kế hoạch buổi: {item.sessionPlan}</p> : null}
                </article>
              )} />
            )}
          </Panel>
        </div>
      ) : null}
    </motion.div>
  );
}

function ItemPager({ list, page, totalPages, onPageChange, renderItem }) {
  return (
    <>
      <div className="divide-y divide-[#f0e3e4]">{list.map(renderItem)}</div>
      {totalPages > 1 ? (
        <div className="flex items-center justify-center gap-3 border-t border-[#f0e3e4] px-6 py-4">
          <button className="rounded-xl border border-[#dfbfbd] px-4 py-2 text-sm font-bold text-[#730014] disabled:opacity-40" disabled={page <= 1} onClick={() => onPageChange(page - 1)} type="button">Trang trước</button>
          <span className="text-sm font-semibold text-[#584140]">Trang {page} / {totalPages}</span>
          <button className="rounded-xl border border-[#dfbfbd] px-4 py-2 text-sm font-bold text-[#730014] disabled:opacity-40" disabled={page >= totalPages} onClick={() => onPageChange(page + 1)} type="button">Trang sau</button>
        </div>
      ) : null}
    </>
  );
}

function OverviewCard({ icon: Icon, label, value }) {
  return (
    <div className="rounded-2xl border border-[#f0e3e4] bg-[#fcfbfb] p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm font-medium text-[#584140]">{label}</p>
          <p className="mt-2 text-base font-extrabold text-[#2b2828]">{value}</p>
        </div>
        <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-[#fff1f2] text-[#730014]">
          <Icon className="h-4.5 w-4.5" />
        </span>
      </div>
    </div>
  );
}

function SourceBadge({ value }) {
  const isCenter = value === 'CENTER_LIBRARY';
  return (
    <span className={`inline-flex rounded-full px-3 py-1 text-xs font-bold ${
      isCenter ? 'bg-emerald-100 text-emerald-700' : 'bg-[#fff2f3] text-[#730014]'
    }`}>
      {isCenter ? 'Từ kho trung tâm' : 'Riêng của lớp'}
    </span>
  );
}

function InfoPill({ label, value }) {
  return (
    <div className="rounded-2xl border border-[#f0e3e4] bg-[#fcfbfb] px-4 py-3">
      <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-[#8b706e]">{label}</p>
      <p className="mt-2 text-sm font-semibold text-[#2b2828]">{value}</p>
    </div>
  );
}
