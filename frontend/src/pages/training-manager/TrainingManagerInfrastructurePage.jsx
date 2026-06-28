import { useEffect, useMemo, useState } from 'react';
import { Building2, Clock3, DoorOpen, Plus, RefreshCw, Save, Trash2 } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import BrandedSelect from '../../components/ui/BrandedSelect';

const tabs = [
  { id: 'campuses', label: 'Cơ sở', icon: Building2 },
  { id: 'rooms', label: 'Phòng học', icon: DoorOpen },
  { id: 'templates', label: 'Mẫu lịch', icon: Clock3 },
];

const dayOptions = [
  { label: 'Thứ 2', value: '1' },
  { label: 'Thứ 3', value: '2' },
  { label: 'Thứ 4', value: '3' },
  { label: 'Thứ 5', value: '4' },
  { label: 'Thứ 6', value: '5' },
  { label: 'Thứ 7', value: '6' },
  { label: 'Chủ nhật', value: '7' },
];

const emptyCampusForm = { name: '', address: '', note: '', active: true };
const emptyRoomForm = { name: '', campusId: '', capacity: '', active: true };
const emptyTemplateForm = {
  name: '',
  description: '',
  teacherGuide: '',
  interactionActivities: '',
  postSessionHomework: '',
  defaultDurationMinutes: '90',
  slots: [{ dayOfWeek: '1', startTime: '18:00', endTime: '20:00', roomId: '', teacherId: '' }],
};

export default function TrainingManagerInfrastructurePage() {
  const [activeTab, setActiveTab] = useState('campuses');
  const [campuses, setCampuses] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [campusForm, setCampusForm] = useState(emptyCampusForm);
  const [roomForm, setRoomForm] = useState(emptyRoomForm);
  const [templateForm, setTemplateForm] = useState(emptyTemplateForm);
  const [editingCampusId, setEditingCampusId] = useState(null);
  const [editingRoomId, setEditingRoomId] = useState(null);
  const [editingTemplateId, setEditingTemplateId] = useState(null);

  const campusOptions = useMemo(
    () => [
      { label: 'Chưa gắn cơ sở', value: '' },
      ...campuses.map((campus) => ({ label: campus.name, value: String(campus.id) })),
    ],
    [campuses],
  );

  const roomOptions = useMemo(
    () => [
      { label: 'Không chọn phòng mặc định', value: '' },
      ...rooms.map((room) => ({ label: `${room.name}${room.campusName ? ` · ${room.campusName}` : ''}`, value: String(room.id) })),
    ],
    [rooms],
  );

  const loadAll = async () => {
    setLoading(true);
    setError('');
    try {
      const [campusData, roomData, templateData] = await Promise.all([
        classroomApi.listCampuses(),
        classroomApi.listRooms(),
        classroomApi.listSessionTemplates(),
      ]);
      setCampuses(campusData);
      setRooms(roomData);
      setTemplates(templateData);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được dữ liệu hạ tầng lớp học.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAll();
  }, []);

  const resetMessages = () => {
    setError('');
    setSuccess('');
  };

  const saveCampus = async (event) => {
    event.preventDefault();
    resetMessages();
    if (!campusForm.name.trim()) {
      setError('Vui lòng nhập tên cơ sở.');
      return;
    }
    setWorking(true);
    try {
      const payload = { ...campusForm, name: campusForm.name.trim() };
      if (editingCampusId) {
        await classroomApi.updateCampus(editingCampusId, payload);
        setSuccess('Đã cập nhật cơ sở.');
      } else {
        await classroomApi.createCampus(payload);
        setSuccess('Đã tạo cơ sở mới.');
      }
      setCampusForm(emptyCampusForm);
      setEditingCampusId(null);
      await loadAll();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu được cơ sở.');
    } finally {
      setWorking(false);
    }
  };

  const saveRoom = async (event) => {
    event.preventDefault();
    resetMessages();
    if (!roomForm.name.trim()) {
      setError('Vui lòng nhập tên phòng.');
      return;
    }
    const capacity = roomForm.capacity ? Number(roomForm.capacity) : null;
    if (capacity != null && capacity <= 0) {
      setError('Sức chứa phòng phải lớn hơn 0.');
      return;
    }
    setWorking(true);
    try {
      const payload = {
        name: roomForm.name.trim(),
        campusId: roomForm.campusId ? Number(roomForm.campusId) : null,
        capacity,
        active: roomForm.active,
      };
      if (editingRoomId) {
        await classroomApi.updateRoom(editingRoomId, payload);
        setSuccess('Đã cập nhật phòng học.');
      } else {
        await classroomApi.createRoom(payload);
        setSuccess('Đã tạo phòng học mới.');
      }
      setRoomForm(emptyRoomForm);
      setEditingRoomId(null);
      await loadAll();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu được phòng học.');
    } finally {
      setWorking(false);
    }
  };

  const saveTemplate = async (event) => {
    event.preventDefault();
    resetMessages();
    if (!templateForm.name.trim()) {
      setError('Vui lòng nhập tên mẫu lịch.');
      return;
    }
    const invalidSlot = templateForm.slots.find((slot) => !slot.dayOfWeek || !slot.startTime || !slot.endTime || slot.startTime >= slot.endTime);
    if (invalidSlot) {
      setError('Mỗi khung giờ cần thứ, giờ bắt đầu và giờ kết thúc hợp lệ.');
      return;
    }
    setWorking(true);
    try {
      const slotsJson = JSON.stringify(templateForm.slots.map((slot) => ({
        dayOfWeek: Number(slot.dayOfWeek),
        startTime: slot.startTime,
        endTime: slot.endTime,
        roomId: slot.roomId ? Number(slot.roomId) : null,
        teacherId: slot.teacherId ? Number(slot.teacherId) : null,
      })));
      const payload = {
        name: templateForm.name.trim(),
        description: templateForm.description,
        teacherGuide: templateForm.teacherGuide,
        interactionActivities: templateForm.interactionActivities,
        postSessionHomework: templateForm.postSessionHomework,
        defaultDurationMinutes: templateForm.defaultDurationMinutes ? Number(templateForm.defaultDurationMinutes) : null,
        slotsJson,
        active: true,
      };
      if (editingTemplateId) {
        await classroomApi.updateSessionTemplate(editingTemplateId, payload);
        setSuccess('Đã cập nhật mẫu lịch.');
      } else {
        await classroomApi.createSessionTemplate(payload);
        setSuccess('Đã tạo mẫu lịch mới.');
      }
      setTemplateForm(emptyTemplateForm);
      setEditingTemplateId(null);
      await loadAll();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu được mẫu lịch.');
    } finally {
      setWorking(false);
    }
  };

  const editCampus = (campus) => {
    setActiveTab('campuses');
    setEditingCampusId(campus.id);
    setCampusForm({
      name: campus.name || '',
      address: campus.address || '',
      note: campus.note || '',
      active: campus.active !== false,
    });
  };

  const editRoom = (room) => {
    setActiveTab('rooms');
    setEditingRoomId(room.id);
    setRoomForm({
      name: room.name || '',
      campusId: room.campusId ? String(room.campusId) : '',
      capacity: room.capacity || '',
      active: room.active !== false,
    });
  };

  const editTemplate = (template) => {
    setActiveTab('templates');
    setEditingTemplateId(template.id);
    setTemplateForm({
      name: template.name || '',
      description: template.description || '',
      teacherGuide: template.teacherGuide || '',
      interactionActivities: template.interactionActivities || '',
      postSessionHomework: template.postSessionHomework || '',
      defaultDurationMinutes: template.defaultDurationMinutes ? String(template.defaultDurationMinutes) : '90',
      slots: parseSlots(template.slotsJson),
    });
  };

  const updateSlot = (index, patch) => {
    setTemplateForm((current) => ({
      ...current,
      slots: current.slots.map((slot, slotIndex) => (slotIndex === index ? { ...slot, ...patch } : slot)),
    }));
  };

  const addSlot = () => {
    setTemplateForm((current) => ({
      ...current,
      slots: [...current.slots, { dayOfWeek: '1', startTime: '18:00', endTime: '20:00', roomId: '', teacherId: '' }],
    }));
  };

  const removeSlot = (index) => {
    setTemplateForm((current) => ({
      ...current,
      slots: current.slots.length <= 1 ? current.slots : current.slots.filter((_, slotIndex) => slotIndex !== index),
    }));
  };

  return (
    <div className="space-y-6">
      <section className="overflow-hidden rounded-[28px] border border-slate-200 bg-white shadow-sm">
        <div className="grid gap-4 border-b border-slate-100 p-5 lg:grid-cols-[1fr_auto] lg:items-center">
          <div>
            <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[#730014]">Classroom infrastructure</p>
            <h2 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-slate-900">Dữ liệu nền để mở lớp không bị lỗi</h2>
            <p className="mt-2 text-sm leading-7 text-slate-500">
              Cơ sở, phòng và mẫu lịch là dữ liệu bắt buộc cho xếp phòng, kiểm tra trùng lịch và sinh buổi học hàng loạt.
            </p>
          </div>
          <button
            className="inline-flex items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm font-extrabold text-[#730014] transition hover:bg-[#fff4f5]"
            onClick={loadAll}
            type="button"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Làm mới
          </button>
        </div>
        <div className="grid gap-3 p-4 md:grid-cols-3">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            const active = activeTab === tab.id;
            return (
              <button
                className={`rounded-2xl border px-4 py-4 text-left transition ${
                  active ? 'border-[#730014] bg-[#fff4f5] text-[#4b0009] shadow-sm' : 'border-slate-200 bg-slate-50 text-slate-600 hover:bg-white'
                }`}
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                type="button"
              >
                <Icon className="h-5 w-5" />
                <p className="mt-3 font-['Manrope'] text-lg font-extrabold">{tab.label}</p>
              </button>
            );
          })}
        </div>
      </section>

      {error ? <Notice tone="error">{error}</Notice> : null}
      {success ? <Notice tone="success">{success}</Notice> : null}

      {loading ? (
        <section className="flex min-h-[360px] items-center justify-center rounded-2xl border border-slate-200 bg-white text-sm font-bold text-slate-500">
          Đang tải hạ tầng lớp học...
        </section>
      ) : (
        <div className="grid gap-6 xl:grid-cols-[420px_1fr]">
          {activeTab === 'campuses' ? (
            <>
              <CampusForm
                editing={Boolean(editingCampusId)}
                form={campusForm}
                onCancel={() => {
                  setCampusForm(emptyCampusForm);
                  setEditingCampusId(null);
                }}
                onChange={setCampusForm}
                onSubmit={saveCampus}
                working={working}
              />
              <ListPanel
                emptyText="Chưa có cơ sở nào. Hãy tạo cơ sở trước khi tạo phòng."
                items={campuses}
                renderItem={(campus) => (
                  <DataCard
                    key={campus.id}
                    meta={`${campus.roomCount || 0} phòng · ${campus.active ? 'Đang hoạt động' : 'Tạm ngưng'}`}
                    onEdit={() => editCampus(campus)}
                    title={campus.name}
                  >
                    <p>{campus.address || 'Chưa cập nhật địa chỉ.'}</p>
                    {campus.note ? <p className="mt-2 text-slate-400">{campus.note}</p> : null}
                  </DataCard>
                )}
                title="Danh sách cơ sở"
              />
            </>
          ) : null}

          {activeTab === 'rooms' ? (
            <>
              <RoomForm
                campusOptions={campusOptions}
                editing={Boolean(editingRoomId)}
                form={roomForm}
                onCancel={() => {
                  setRoomForm(emptyRoomForm);
                  setEditingRoomId(null);
                }}
                onChange={setRoomForm}
                onSubmit={saveRoom}
                working={working}
              />
              <ListPanel
                emptyText="Chưa có phòng học nào. Phòng cần được tạo trước khi xếp lịch offline."
                items={rooms}
                renderItem={(room) => (
                  <DataCard
                    key={room.id}
                    meta={`${room.capacity || '-'} chỗ · ${room.campusName || 'Chưa gắn cơ sở'} · ${room.active ? 'Hoạt động' : 'Tạm ngưng'}`}
                    onEdit={() => editRoom(room)}
                    title={room.name}
                  />
                )}
                title="Danh sách phòng học"
              />
            </>
          ) : null}

          {activeTab === 'templates' ? (
            <>
              <TemplateForm
                editing={Boolean(editingTemplateId)}
                form={templateForm}
                onAddSlot={addSlot}
                onCancel={() => {
                  setTemplateForm(emptyTemplateForm);
                  setEditingTemplateId(null);
                }}
                onChange={setTemplateForm}
                onRemoveSlot={removeSlot}
                onSlotChange={updateSlot}
                onSubmit={saveTemplate}
                roomOptions={roomOptions}
                working={working}
              />
              <ListPanel
                emptyText="Chưa có mẫu lịch. Mẫu lịch giúp sinh nhiều buổi học chỉ bằng một lần thao tác."
                items={templates}
                renderItem={(template) => (
                  <DataCard
                    key={template.id}
                    meta={`${parseSlots(template.slotsJson).length} khung giờ · ${template.active ? 'Hoạt động' : 'Tạm ngưng'}`}
                    onEdit={() => editTemplate(template)}
                    title={template.name}
                  >
                    <p>{template.description || 'Chưa có mô tả.'}</p>
                  </DataCard>
                )}
                title="Mẫu lịch"
              />
            </>
          ) : null}
        </div>
      )}
    </div>
  );
}

function CampusForm({ editing, form, onCancel, onChange, onSubmit, working }) {
  return (
    <FormShell
      editing={editing}
      icon={Building2}
      onCancel={onCancel}
      onSubmit={onSubmit}
      submitLabel={editing ? 'Cập nhật cơ sở' : 'Tạo cơ sở'}
      title={editing ? 'Sửa cơ sở' : 'Tạo cơ sở mới'}
      working={working}
    >
      <TextField label="Tên cơ sở" onChange={(value) => onChange({ ...form, name: value })} value={form.name} />
      <TextField label="Địa chỉ" onChange={(value) => onChange({ ...form, address: value })} value={form.address} />
      <TextField label="Ghi chú vận hành" onChange={(value) => onChange({ ...form, note: value })} textarea value={form.note} />
    </FormShell>
  );
}

function RoomForm({ campusOptions, editing, form, onCancel, onChange, onSubmit, working }) {
  return (
    <FormShell
      editing={editing}
      icon={DoorOpen}
      onCancel={onCancel}
      onSubmit={onSubmit}
      submitLabel={editing ? 'Cập nhật phòng' : 'Tạo phòng'}
      title={editing ? 'Sửa phòng học' : 'Tạo phòng học'}
      working={working}
    >
      <TextField label="Tên phòng" onChange={(value) => onChange({ ...form, name: value })} value={form.name} />
      <label className="block">
        <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Cơ sở</span>
        <BrandedSelect
          onChange={(event) => onChange({ ...form, campusId: event.target.value })}
          options={campusOptions}
          value={form.campusId}
        />
      </label>
      <TextField label="Sức chứa" onChange={(value) => onChange({ ...form, capacity: value })} type="number" value={form.capacity} />
    </FormShell>
  );
}

function TemplateForm({ editing, form, onAddSlot, onCancel, onChange, onRemoveSlot, onSlotChange, onSubmit, roomOptions, working }) {
  return (
    <FormShell
      editing={editing}
      icon={Clock3}
      onCancel={onCancel}
      onSubmit={onSubmit}
      submitLabel={editing ? 'Cập nhật mẫu' : 'Tạo mẫu'}
      title={editing ? 'Sửa mẫu lịch' : 'Tạo mẫu lịch'}
      working={working}
    >
      <TextField label="Tên mẫu" onChange={(value) => onChange({ ...form, name: value })} value={form.name} />
      <TextField label="Mô tả" onChange={(value) => onChange({ ...form, description: value })} textarea value={form.description} />
      <TextField label="Hướng dẫn giảng viên" onChange={(value) => onChange({ ...form, teacherGuide: value })} textarea value={form.teacherGuide} />
      <TextField label="Hoạt động tương tác" onChange={(value) => onChange({ ...form, interactionActivities: value })} textarea value={form.interactionActivities} />
      <TextField label="Bài tập sau buổi học" onChange={(value) => onChange({ ...form, postSessionHomework: value })} textarea value={form.postSessionHomework} />
      <TextField label="Thời lượng mặc định (phút)" onChange={(value) => onChange({ ...form, defaultDurationMinutes: value })} type="number" value={form.defaultDurationMinutes} />
      <div className="space-y-3">
        <div className="flex items-center justify-between gap-3">
          <span className="text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Khung giờ</span>
          <button className="inline-flex items-center gap-1 rounded-xl bg-[#4b0009] px-3 py-2 text-xs font-extrabold text-white" onClick={onAddSlot} type="button">
            <Plus className="h-3.5 w-3.5" />
            Thêm
          </button>
        </div>
        {form.slots.map((slot, index) => (
          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-3" key={`${slot.dayOfWeek}-${index}`}>
            <div className="grid gap-3 sm:grid-cols-3">
              <BrandedSelect
                onChange={(event) => onSlotChange(index, { dayOfWeek: event.target.value })}
                options={dayOptions}
                value={slot.dayOfWeek}
              />
              <input className={inputClass} onChange={(event) => onSlotChange(index, { startTime: event.target.value })} type="time" value={slot.startTime} />
              <input className={inputClass} onChange={(event) => onSlotChange(index, { endTime: event.target.value })} type="time" value={slot.endTime} />
            </div>
            <div className="mt-3 grid gap-3 sm:grid-cols-[1fr_auto]">
              <BrandedSelect
                onChange={(event) => onSlotChange(index, { roomId: event.target.value })}
                options={roomOptions}
                value={slot.roomId}
              />
              <button className="inline-flex items-center justify-center rounded-xl border border-rose-200 px-3 py-2 text-rose-600" onClick={() => onRemoveSlot(index)} type="button">
                <Trash2 className="h-4 w-4" />
              </button>
            </div>
          </div>
        ))}
      </div>
    </FormShell>
  );
}

function FormShell({ children, editing, icon: Icon, onCancel, onSubmit, submitLabel, title, working }) {
  return (
    <form className="space-y-4 rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm" onSubmit={onSubmit}>
      <div className="flex items-center gap-3">
        <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#fff4f5] text-[#730014]">
          <Icon className="h-5 w-5" />
        </span>
        <div>
          <h3 className="font-['Manrope'] text-xl font-extrabold text-slate-900">{title}</h3>
          <p className="text-xs font-semibold text-slate-400">{editing ? 'Đang chỉnh sửa bản ghi đã chọn' : 'Tạo dữ liệu mới cho vận hành lớp'}</p>
        </div>
      </div>
      {children}
      <div className="flex flex-wrap justify-end gap-3 border-t border-slate-100 pt-4">
        {editing ? (
          <button className="rounded-2xl border border-slate-200 px-4 py-3 text-sm font-extrabold text-slate-600" onClick={onCancel} type="button">
            Hủy sửa
          </button>
        ) : null}
        <button className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#730014] disabled:opacity-60" disabled={working} type="submit">
          <Save className="h-4 w-4" />
          {working ? 'Đang lưu...' : submitLabel}
        </button>
      </div>
    </form>
  );
}

function ListPanel({ emptyText, items, renderItem, title }) {
  return (
    <section className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
      <h3 className="font-['Manrope'] text-xl font-extrabold text-slate-900">{title}</h3>
      <div className="mt-4 space-y-3">
        {items.length ? items.map(renderItem) : (
          <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-5 py-10 text-center text-sm font-semibold text-slate-500">
            {emptyText}
          </div>
        )}
      </div>
    </section>
  );
}

function DataCard({ children, meta, onEdit, title }) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-slate-50 p-4 transition hover:border-[#dfbfbd] hover:bg-white">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h4 className="font-['Manrope'] text-lg font-extrabold text-slate-900">{title}</h4>
          <p className="mt-1 text-xs font-bold uppercase tracking-[0.12em] text-slate-400">{meta}</p>
        </div>
        <button className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-extrabold text-[#730014]" onClick={onEdit} type="button">
          Sửa
        </button>
      </div>
      {children ? <div className="mt-3 text-sm leading-6 text-slate-600">{children}</div> : null}
    </article>
  );
}

function TextField({ label, onChange, textarea = false, type = 'text', value }) {
  return (
    <label className="block">
      <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">{label}</span>
      {textarea ? (
        <textarea className={`${inputClass} min-h-24`} onChange={(event) => onChange(event.target.value)} value={value} />
      ) : (
        <input className={inputClass} onChange={(event) => onChange(event.target.value)} type={type} value={value} />
      )}
    </label>
  );
}

function Notice({ children, tone }) {
  const className = tone === 'error'
    ? 'border-rose-200 bg-rose-50 text-rose-700'
    : 'border-emerald-200 bg-emerald-50 text-emerald-700';
  return <div className={`rounded-2xl border px-5 py-4 text-sm font-bold ${className}`}>{children}</div>;
}

function parseSlots(slotsJson) {
  try {
    const parsed = JSON.parse(slotsJson || '[]');
    if (!Array.isArray(parsed) || !parsed.length) return emptyTemplateForm.slots;
    return parsed.map((slot) => ({
      dayOfWeek: String(slot.dayOfWeek || '1'),
      startTime: slot.startTime || '18:00',
      endTime: slot.endTime || '20:00',
      roomId: slot.roomId ? String(slot.roomId) : '',
      teacherId: slot.teacherId ? String(slot.teacherId) : '',
    }));
  } catch {
    return emptyTemplateForm.slots;
  }
}

const inputClass = 'w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-[#730014] focus:bg-white';
