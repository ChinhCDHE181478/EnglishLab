import { useEffect, useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import {
  Building2,
  CheckCircle2,
  DoorOpen,
  PencilLine,
  Plus,
  RefreshCw,
  Save,
  Search,
  Users,
  X,
  XCircle,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import { ClassroomEmptyState, ClassroomErrorState, ClassroomLoadingState } from '../../components/classroom/ClassroomUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import ManagementToast from '../../components/ui/ManagementToast';

const emptyRoomForm = { name: '', locationName: '', locationAddress: '', capacity: '', active: true };

const statusOptions = [
  { label: 'Tất cả trạng thái', value: 'ALL' },
  { label: 'Đang hoạt động', value: 'ACTIVE' },
  { label: 'Tạm ngưng', value: 'INACTIVE' },
];

const capacityFilterOptions = [
  { label: 'Tất cả sức chứa', value: 'ALL' },
  { label: 'Nhỏ (Dưới 20 chỗ)', value: 'SMALL' },
  { label: 'Vừa (20 - 30 chỗ)', value: 'MEDIUM' },
  { label: 'Lớn (Trên 30 chỗ)', value: 'LARGE' },
];

const activeFormOptions = [
  { label: 'Đang hoạt động', value: 'true' },
  { label: 'Tạm ngưng', value: 'false' },
];

export default function StaffInfrastructurePage() {
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [editorError, setEditorError] = useState('');
  const [editorOpen, setEditorOpen] = useState(false);
  const [roomForm, setRoomForm] = useState(emptyRoomForm);
  const [editingRoomId, setEditingRoomId] = useState(null);

  // Filters & Search
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [capacityFilter, setCapacityFilter] = useState('ALL');

  const loadInfrastructure = async () => {
    setLoading(true);
    setError('');
    try {
      const roomData = await classroomApi.listRooms();
      setRooms(roomData || []);
    } catch (err) {
      setRooms([]);
      setError(err?.response?.data?.message || 'Không tải được danh sách phòng học.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadInfrastructure();
  }, []);

  // Filter logic
  const filteredRooms = useMemo(() => {
    const search = keyword.trim().toLowerCase();
    return rooms.filter((room) => {
      const matchesKeyword = !search || String(room.name || '').toLowerCase().includes(search);
      const isActive = room.active !== false;
      const matchesStatus =
        statusFilter === 'ALL' ||
        (statusFilter === 'ACTIVE' && isActive) ||
        (statusFilter === 'INACTIVE' && !isActive);

      const cap = Number(room.capacity || 0);
      let matchesCapacity = true;
      if (capacityFilter === 'SMALL') matchesCapacity = cap < 20;
      else if (capacityFilter === 'MEDIUM') matchesCapacity = cap >= 20 && cap <= 30;
      else if (capacityFilter === 'LARGE') matchesCapacity = cap > 30;

      return matchesKeyword && matchesStatus && matchesCapacity;
    });
  }, [rooms, keyword, statusFilter, capacityFilter]);

  const resetKey = `${keyword}|${statusFilter}|${capacityFilter}`;
  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(filteredRooms, 8, resetKey);

  // Stats computation
  const stats = useMemo(() => {
    const activeCount = rooms.filter((r) => r.active !== false).length;
    const inactiveCount = rooms.length - activeCount;
    const totalCapacity = rooms
      .filter((r) => r.active !== false)
      .reduce((sum, r) => sum + Number(r.capacity || 0), 0);

    return {
      total: rooms.length,
      active: activeCount,
      inactive: inactiveCount,
      totalCapacity,
    };
  }, [rooms]);

  const closeEditor = () => {
    setEditorOpen(false);
    setEditingRoomId(null);
    setRoomForm(emptyRoomForm);
    setEditorError('');
  };

  const openCreate = () => {
    setError('');
    setSuccess('');
    setEditorError('');
    setEditingRoomId(null);
    setRoomForm(emptyRoomForm);
    setEditorOpen(true);
  };

  const editRoom = (room) => {
    setError('');
    setSuccess('');
    setEditorError('');
    setEditingRoomId(room.id);
    setRoomForm({
      name: room.name || '',
      locationName: room.locationName || '',
      locationAddress: room.locationAddress || '',
      capacity: room.capacity || '',
      active: room.active !== false,
    });
    setEditorOpen(true);
  };

  const saveRoom = async (event) => {
    event.preventDefault();
    setEditorError('');

    if (!roomForm.name.trim()) {
      setEditorError('Vui lòng nhập tên phòng học.');
      return;
    }

    const capacity = roomForm.capacity ? Number(roomForm.capacity) : null;
    if (capacity != null && (!Number.isInteger(capacity) || capacity <= 0)) {
      setEditorError('Sức chứa phải là số nguyên lớn hơn 0.');
      return;
    }

    setWorking(true);
    try {
      const payload = {
        name: roomForm.name.trim(),
        locationName: roomForm.locationName.trim() || null,
        locationAddress: roomForm.locationAddress.trim() || null,
        capacity,
        active: Boolean(roomForm.active),
      };
      if (editingRoomId) {
        await classroomApi.updateRoom(editingRoomId, payload);
        setSuccess('Đã cập nhật thông tin phòng học.');
      } else {
        await classroomApi.createRoom(payload);
        setSuccess('Đã thêm phòng học mới thành công.');
      }
      closeEditor();
      await loadInfrastructure();
    } catch (err) {
      setEditorError(err?.response?.data?.message || 'Không lưu được phòng học.');
    } finally {
      setWorking(false);
    }
  };

  return (
    <div className="space-y-5">
      {/* Top Notifications */}
      {!editorOpen ? <ManagementToast message={error} onClose={() => setError('')} /> : null}
      <ManagementToast message={success} onClose={() => setSuccess('')} tone="success" title="Đã cập nhật phòng học" />

      {/* Metric Cards Grid */}
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard icon={DoorOpen} label="Tổng phòng học" value={stats.total} />
        <MetricCard icon={CheckCircle2} label="Đang hoạt động" value={stats.active} />
        <MetricCard icon={XCircle} label="Tạm ngưng" value={stats.inactive} />
        <MetricCard icon={Users} label="Tổng sức chứa" value={stats.totalCapacity ? `${stats.totalCapacity} chỗ` : '0 chỗ'} />
      </section>

      {/* Filter and Action Bar */}
      <section className="flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="relative min-w-[240px] flex-1">
          <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            className={`${inputClass} h-11 pl-10`}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="Tìm theo tên phòng học..."
            value={keyword}
          />
        </div>
        <div className="w-full sm:w-48">
          <BrandedSelect
            onChange={(event) => setStatusFilter(event.target.value)}
            options={statusOptions}
            value={statusFilter}
          />
        </div>
        <div className="w-full sm:w-52">
          <BrandedSelect
            onChange={(event) => setCapacityFilter(event.target.value)}
            options={capacityFilterOptions}
            value={capacityFilter}
          />
        </div>
        <div className="flex items-center gap-2">
          <button
            aria-label="Làm mới danh sách phòng học"
            className="inline-flex h-11 w-11 items-center justify-center rounded-xl border border-slate-200 text-[#730014] transition hover:bg-slate-50 active:scale-95"
            disabled={loading}
            onClick={loadInfrastructure}
            type="button"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <button
            className="inline-flex h-11 items-center gap-2 rounded-xl bg-[#4b0009] px-5 text-xs font-extrabold text-white transition hover:bg-[#730014] active:scale-95 whitespace-nowrap shadow-sm"
            onClick={openCreate}
            type="button"
          >
            <Plus className="h-4 w-4" />
            Thêm phòng học
          </button>
        </div>
      </section>

      {/* Content Section */}
      {loading ? (
        <ClassroomLoadingState message="Đang tải danh sách cơ sở vật chất..." />
      ) : null}

      {!loading && error ? (
        <ClassroomErrorState message={error} onRetry={loadInfrastructure} />
      ) : null}

      {!loading && !error && !filteredRooms.length ? (
        <ClassroomEmptyState
          description="Không tìm thấy phòng học nào phù hợp với điều kiện lọc."
          title="Không có phòng học"
        />
      ) : null}

      {!loading && !error && filteredRooms.length ? (
        <section className="overflow-hidden rounded-xl border border-[#dfbfbd]/40 bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[780px] text-left text-sm">
              <thead className="border-b border-[#dfbfbd]/30 bg-[#fbf3f4] text-[11px] font-extrabold uppercase tracking-wider text-[#8b706e]">
                <tr>
                  <th className="px-5 py-4">Phòng học</th>
                  <th className="px-5 py-4">Địa điểm trung tâm</th>
                  <th className="px-5 py-4">Sức chứa</th>
                  <th className="px-5 py-4">Trạng thái</th>
                  <th className="px-5 py-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#dfbfbd]/20">
                {pageItems.map((room) => {
                  const isActive = room.active !== false;
                  return (
                    <tr className="transition hover:bg-[#fffafb]" key={room.id}>
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-3">
                          <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-[#fff1f3] text-[#730014]">
                            <DoorOpen className="h-4 w-4" />
                          </span>
                          <div>
                            <p className="font-extrabold text-[#2b2828]">{room.name}</p>
                            <p className="text-xs text-[#8b706e]">Mã phòng #{room.id}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-2 text-xs font-semibold text-[#584140]">
                          <Building2 className="h-3.5 w-3.5 text-[#8b706e]" />
                          <span>{room.locationName || 'EnglishLab Center'}</span>
                        </div>
                      </td>
                      <td className="px-5 py-4">
                        <span className="inline-flex items-center gap-1.5 rounded-lg bg-slate-100 px-3 py-1 text-xs font-bold text-slate-700">
                          <Users className="h-3.5 w-3.5 text-slate-500" />
                          {room.capacity ? `${room.capacity} chỗ` : 'Chưa cập nhật'}
                        </span>
                      </td>
                      <td className="whitespace-nowrap px-5 py-4">
                        <span
                          className={`inline-flex items-center rounded-full border px-3 py-1 text-xs font-bold tracking-wide ${
                            isActive
                              ? 'bg-emerald-50 border-emerald-200 text-emerald-800 font-extrabold'
                              : 'bg-slate-100 border-slate-200 text-slate-600'
                          }`}
                        >
                          {isActive ? 'Đang hoạt động' : 'Tạm ngưng'}
                        </span>
                      </td>
                      <td className="whitespace-nowrap px-5 py-4 text-right">
                        <button
                          className="inline-flex items-center gap-1.5 rounded-lg border border-[#dfbfbd] bg-white px-3 py-2 text-xs font-bold text-[#730014] transition hover:bg-[#fff3f4] active:scale-95"
                          onClick={() => editRoom(room)}
                          type="button"
                        >
                          <PencilLine className="h-3.5 w-3.5" />
                          Sửa
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          <div className="border-t border-[#dfbfbd]/30 px-5 py-4">
            <Pagination
              onChange={setPage}
              page={page}
              pageSize={8}
              totalItems={totalItems}
              totalPages={totalPages}
            />
          </div>
        </section>
      ) : null}

      {/* Room Editor Modal */}
      {editorOpen ? (
        <RoomEditorModal onClose={closeEditor}>
          <form className="flex min-h-0 flex-1 flex-col overflow-hidden" onSubmit={saveRoom}>
            <div className="flex items-start justify-between gap-4 border-b border-gray-100 px-6 py-5 shrink-0 bg-white">
              <div>
                <h3 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828]">
                  {editingRoomId ? 'Sửa thông tin phòng học' : 'Thêm phòng học mới'}
                </h3>
                <p className="mt-1 text-xs text-[#8b706e]">
                  Cập nhật các thông số phòng học để sẵn sàng xếp lịch thi và lớp học trực tiếp.
                </p>
              </div>
              <button
                aria-label="Đóng"
                className="rounded-xl border border-gray-200 p-2 text-[#584140] transition hover:bg-gray-50"
                onClick={closeEditor}
                type="button"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="min-h-0 flex-1 overflow-y-auto p-6 space-y-4">
              {editorError ? <Notice tone="error">{editorError}</Notice> : null}

              <TextField
                label="Tên phòng học"
                onChange={(value) => setRoomForm((current) => ({ ...current, name: value }))}
                placeholder="Ví dụ: Phòng A01, Lab 3..."
                value={roomForm.name}
              />
              <TextField
                label="Tên địa điểm"
                onChange={(value) => setRoomForm((current) => ({ ...current, locationName: value }))}
                placeholder="Ví dụ: EnglishLab Center"
                value={roomForm.locationName}
              />
              <TextField
                label="Địa chỉ"
                onChange={(value) => setRoomForm((current) => ({ ...current, locationAddress: value }))}
                placeholder="Nhập địa chỉ phòng học"
                value={roomForm.locationAddress}
              />
              <TextField
                label="Sức chứa (số chỗ ngồi)"
                onChange={(value) => setRoomForm((current) => ({ ...current, capacity: value }))}
                placeholder="Ví dụ: 24"
                type="number"
                value={roomForm.capacity}
              />
              <label className="block space-y-2">
                <span className="text-[11px] font-bold uppercase tracking-wider text-[#8b706e]">Trạng thái hoạt động</span>
                <BrandedSelect
                  onChange={(e) => setRoomForm((current) => ({ ...current, active: e.target.value === 'true' }))}
                  options={activeFormOptions}
                  value={String(roomForm.active)}
                />
              </label>

              <div className="flex justify-end gap-3 border-t border-gray-100 pt-4">
                <button
                  className="rounded-xl border border-gray-200 px-5 py-2.5 text-sm font-bold text-[#584140] transition hover:bg-gray-50"
                  onClick={closeEditor}
                  type="button"
                >
                  Hủy
                </button>
                <button
                  className="inline-flex items-center gap-2 rounded-xl bg-[#4b0009] px-5 py-2.5 text-sm font-extrabold text-white transition hover:bg-[#730014] disabled:opacity-60"
                  disabled={working}
                  type="submit"
                >
                  <Save className="h-4 w-4" />
                  {working ? 'Đang lưu...' : editingRoomId ? 'Lưu thay đổi' : 'Thêm phòng'}
                </button>
              </div>
            </div>
          </form>
        </RoomEditorModal>
      ) : null}
    </div>
  );
}

function RoomEditorModal({ children, onClose }) {
  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm bg-black/45" role="dialog" aria-modal="true">
      <button aria-label="Đóng modal" className="absolute inset-0" onClick={onClose} type="button" />
      <section className="relative z-10 flex max-h-[90vh] w-full max-w-lg flex-col overflow-hidden rounded-3xl border border-gray-100 bg-white shadow-2xl pointer-events-auto">
        {children}
      </section>
    </div>,
    document.body,
  );
}

function MetricCard({ icon: Icon, label, value }) {
  return (
    <article className="rounded-xl border border-[#dfbfbd]/35 bg-white p-4 shadow-sm">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-[11px] font-bold uppercase tracking-wider text-[#8b706e]">{label}</p>
          <p className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">{value}</p>
        </div>
        <span className="rounded-xl bg-[#fff1f3] p-2.5 text-[#730014]">
          <Icon className="h-5 w-5" />
        </span>
      </div>
    </article>
  );
}

function TextField({ label, onChange, placeholder, type = 'text', value }) {
  return (
    <label className="block space-y-2">
      <span className="text-[11px] font-bold uppercase tracking-wider text-[#8b706e]">{label}</span>
      <input
        className={inputClass}
        min={type === 'number' ? '1' : undefined}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        type={type}
        value={value}
      />
    </label>
  );
}

function Notice({ children, tone }) {
  const className = tone === 'error'
    ? 'border-rose-200 bg-rose-50 text-rose-700'
    : 'border-emerald-200 bg-emerald-50 text-emerald-700';
  return <div className={`rounded-xl border px-4 py-3 text-sm font-bold ${className}`}>{children}</div>;
}

const inputClass = 'w-full rounded-xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-3 py-2.5 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white placeholder:text-[#8b706e]';
