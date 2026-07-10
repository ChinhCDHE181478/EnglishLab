import { useEffect, useMemo, useState } from 'react';
import { BookMarked, Link2, Plus, RefreshCw, Save, Search, Trash2 } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import courseApi from '../../api/courseApi';
import curriculumApi from '../../api/curriculumApi';
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
  SEARCH_INPUT_CLASS,
  SECONDARY_BUTTON_CLASS,
  SUCCESS_NOTICE_CLASS,
  TEXTAREA_CLASS,
} from '../../utils/formStyles';

const emptyUnit = {
  title: '',
  description: '',
  displayOrder: 0,
  sessionPlan: '',
};

const emptyAttach = {
  unitId: '',
  type: 'MATERIAL',
  resourceId: '',
  displayOrder: 0,
  note: '',
};

const typeOptions = [
  { label: 'Học liệu', value: 'MATERIAL', description: 'Gắn file/tài liệu từ kho học liệu trung tâm.' },
  { label: 'Bài tập', value: 'EXERCISE', description: 'Gắn bài tập dùng chung từ ngân hàng bài tập.' },
  { label: 'Đề luyện tập/kiểm tra', value: 'ASSESSMENT', description: 'Gắn đề dùng chung từ ngân hàng đề.' },
  { label: 'Flashcard', value: 'FLASHCARD', description: 'Gắn bộ flashcard độc lập.' },
];

const refGroups = [
  { key: 'materials', title: 'Học liệu' },
  { key: 'exercises', title: 'Bài tập' },
  { key: 'assessments', title: 'Đề' },
  { key: 'flashcards', title: 'Flashcard' },
];

const asList = (value) => (Array.isArray(value) ? value : value?.content || value?.items || []);

export default function ContentManagerSyllabusBuilderPage() {
  const [programs, setPrograms] = useState([]);
  const [selectedProgramId, setSelectedProgramId] = useState('');
  const [programDetail, setProgramDetail] = useState(null);
  const [unitForm, setUnitForm] = useState(emptyUnit);
  const [editingUnitId, setEditingUnitId] = useState(null);
  const [attachForm, setAttachForm] = useState(emptyAttach);
  const [keyword, setKeyword] = useState('');
  const [banks, setBanks] = useState({
    materials: [],
    exercises: [],
    assessments: [],
    flashcards: [],
  });
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadPrograms = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await curriculumApi.getCurriculumPrograms();
      setPrograms(data);
      setSelectedProgramId((current) => current || (data[0]?.id ? String(data[0].id) : ''));
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được danh sách giáo trình.');
    } finally {
      setLoading(false);
    }
  };

  const loadProgramDetail = async (programId) => {
    if (!programId) {
      setProgramDetail(null);
      return;
    }
    setError('');
    try {
      const data = await curriculumApi.getCurriculumProgram(programId);
      setProgramDetail(data);
      setAttachForm((current) => ({
        ...current,
        unitId: current.unitId || (data?.units?.[0]?.id ? String(data.units[0].id) : ''),
      }));
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được nội dung giáo trình.');
    }
  };

  const loadBanks = async () => {
    try {
      const [materials, exercises, assessments, flashcards] = await Promise.all([
        classroomApi.getContentManagerMaterialLibrary(),
        courseApi.getExerciseBankItems({ includeInactive: true }),
        curriculumApi.getAssessmentBank(),
        curriculumApi.getFlashcardSets(),
      ]);
      setBanks({
        materials: asList(materials),
        exercises: asList(exercises),
        assessments: asList(assessments),
        flashcards: asList(flashcards),
      });
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được các kho tài nguyên.');
    }
  };

  const reloadAll = async () => {
    await Promise.all([loadPrograms(), loadBanks()]);
    if (selectedProgramId) {
      await loadProgramDetail(selectedProgramId);
    }
  };

  useEffect(() => {
    loadPrograms();
    loadBanks();
  }, []);

  useEffect(() => {
    if (selectedProgramId) {
      loadProgramDetail(selectedProgramId);
      setEditingUnitId(null);
      setUnitForm(emptyUnit);
      setAttachForm(emptyAttach);
    }
  }, [selectedProgramId]);

  const programsOptions = programs.map((program) => ({
    label: `${program.title} · ${program.code}`,
    value: String(program.id),
    description: `${program.deliveryModeLabel || program.deliveryMode} · ${program.examCategory || 'IELTS'}${program.targetBand ? ` · Band ${program.targetBand}` : ''}${program.targetScore ? ` · Target ${program.targetScore}` : ''}`,
  }));

  const units = useMemo(
    () => [...(programDetail?.units || [])].sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0) || a.id - b.id),
    [programDetail],
  );

  const filteredUnits = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    if (!normalized) return units;
    return units.filter((unit) => [unit.title, unit.description, unit.sessionPlan]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(normalized)));
  }, [units, keyword]);

  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    filteredUnits,
    6,
    `${selectedProgramId}-${keyword}`,
  );

  const unitOptions = units.map((unit) => ({
    label: `${unit.displayOrder ?? 0}. ${unit.title}`,
    value: String(unit.id),
  }));

  const currentResources = useMemo(() => {
    if (attachForm.type === 'MATERIAL') return banks.materials;
    if (attachForm.type === 'EXERCISE') return banks.exercises;
    if (attachForm.type === 'ASSESSMENT') return banks.assessments;
    return banks.flashcards;
  }, [attachForm.type, banks]);

  const resourceOptions = currentResources.map((item) => ({
    label: item.title,
    value: String(item.id),
    description: [
      item.skill,
      item.materialType || item.exerciseType || item.type || item.examCategory,
      item.status || (item.active === false ? 'INACTIVE' : 'ACTIVE'),
    ].filter(Boolean).join(' · '),
  }));

  const updateUnitInState = (savedUnit) => {
    setProgramDetail((current) => {
      if (!current) return current;
      const exists = current.units?.some((unit) => String(unit.id) === String(savedUnit.id));
      return {
        ...current,
        units: exists
          ? current.units.map((unit) => (String(unit.id) === String(savedUnit.id) ? savedUnit : unit))
          : [...(current.units || []), savedUnit],
      };
    });
    setAttachForm((current) => ({ ...current, unitId: String(savedUnit.id) }));
  };

  const resetUnitForm = () => {
    setEditingUnitId(null);
    setUnitForm(emptyUnit);
    setError('');
    setSuccess('');
  };

  const openEditUnit = (unit) => {
    setEditingUnitId(unit.id);
    setUnitForm({
      title: unit.title || '',
      description: unit.description || '',
      displayOrder: unit.displayOrder ?? 0,
      sessionPlan: unit.sessionPlan || '',
    });
    setAttachForm((current) => ({ ...current, unitId: String(unit.id) }));
  };

  const saveUnit = async () => {
    if (!selectedProgramId) {
      setError('Vui lòng chọn giáo trình.');
      return;
    }
    if (!unitForm.title.trim()) {
      setError('Vui lòng nhập tên unit/buổi học.');
      return;
    }
    setWorking(true);
    setError('');
    setSuccess('');
    const payload = {
      ...unitForm,
      displayOrder: Number(unitForm.displayOrder || 0),
    };
    try {
      const saved = editingUnitId
        ? await curriculumApi.updateCurriculumUnit(editingUnitId, payload)
        : await curriculumApi.createCurriculumUnit(selectedProgramId, payload);
      updateUnitInState(saved);
      setEditingUnitId(saved.id);
      setUnitForm({
        title: saved.title || '',
        description: saved.description || '',
        displayOrder: saved.displayOrder ?? 0,
        sessionPlan: saved.sessionPlan || '',
      });
      setSuccess(editingUnitId ? 'Đã cập nhật unit.' : 'Đã tạo unit mới.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu được unit.');
    } finally {
      setWorking(false);
    }
  };

  const deleteUnit = async (unit) => {
    if (!window.confirm(`Xóa unit "${unit.title}" khỏi giáo trình?`)) return;
    setWorking(true);
    setError('');
    try {
      await curriculumApi.deleteCurriculumUnit(unit.id);
      setProgramDetail((current) => ({
        ...current,
        units: (current?.units || []).filter((item) => String(item.id) !== String(unit.id)),
      }));
      if (String(editingUnitId) === String(unit.id)) resetUnitForm();
      setSuccess('Đã xóa unit.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không xóa được unit.');
    } finally {
      setWorking(false);
    }
  };

  const attachResource = async () => {
    if (!attachForm.unitId || !attachForm.resourceId) {
      setError('Vui lòng chọn unit và tài nguyên cần gắn.');
      return;
    }
    setWorking(true);
    setError('');
    setSuccess('');
    const payload = {
      resourceId: Number(attachForm.resourceId),
      displayOrder: Number(attachForm.displayOrder || 0),
      note: attachForm.note,
    };
    try {
      let savedUnit;
      if (attachForm.type === 'MATERIAL') {
        savedUnit = await curriculumApi.attachUnitMaterial(attachForm.unitId, payload);
      } else if (attachForm.type === 'EXERCISE') {
        savedUnit = await curriculumApi.attachUnitExercise(attachForm.unitId, payload);
      } else if (attachForm.type === 'ASSESSMENT') {
        savedUnit = await curriculumApi.attachUnitAssessment(attachForm.unitId, payload);
      } else {
        savedUnit = await curriculumApi.attachUnitFlashcard(attachForm.unitId, payload);
      }
      updateUnitInState(savedUnit);
      setAttachForm((current) => ({ ...current, resourceId: '', note: '' }));
      setSuccess('Đã gắn tài nguyên vào unit.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không gắn được tài nguyên.');
    } finally {
      setWorking(false);
    }
  };

  const detachResource = async (ref) => {
    if (!window.confirm(`Gỡ "${ref.title}" khỏi unit này?`)) return;
    setWorking(true);
    setError('');
    try {
      await curriculumApi.detachReference(ref.type, ref.id);
      await loadProgramDetail(selectedProgramId);
      setSuccess('Đã gỡ tài nguyên khỏi unit.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không gỡ được tài nguyên.');
    } finally {
      setWorking(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="font-['Manrope'] text-2xl font-extrabold text-slate-900">Biên soạn giáo trình</h2>
          <p className="mt-1 max-w-3xl text-sm text-slate-600">
            Chọn giáo trình, biên soạn unit/buổi học và gắn tài nguyên từ các kho dùng chung. Lớp học chỉ tham chiếu giáo trình này khi được mở lớp.
          </p>
        </div>
        <button type="button" onClick={reloadAll} className={SECONDARY_BUTTON_CLASS}>
          <RefreshCw className="h-4 w-4" /> Tải lại
        </button>
      </div>

      {error && <div className={ERROR_NOTICE_CLASS}>{error}</div>}
      {success && <div className={SUCCESS_NOTICE_CLASS}>{success}</div>}

      <section className={`${PANEL_CLASS} space-y-4`}>
        <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_320px]">
          <div>
            <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Giáo trình</span>
            <BrandedSelect
              value={selectedProgramId}
              onChange={(event) => setSelectedProgramId(event.target.value)}
              options={programsOptions}
              placeholder={loading ? 'Đang tải giáo trình...' : 'Chọn giáo trình'}
            />
          </div>
          <div className="relative self-end">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="Tìm unit..."
              className={SEARCH_INPUT_CLASS}
            />
          </div>
        </div>
        {programDetail ? (
          <div className="grid gap-3 text-sm text-slate-600 md:grid-cols-4">
            <div><span className="font-bold text-slate-900">{programDetail.code}</span><br />Mã giáo trình</div>
            <div><span className="font-bold text-slate-900">{programDetail.deliveryModeLabel || programDetail.deliveryMode}</span><br />Hình thức</div>
            <div><span className="font-bold text-slate-900">{programDetail.targetBand || programDetail.targetScore || 'Chưa đặt'}</span><br />Target</div>
            <div><span className="font-bold text-slate-900">{units.length}</span><br />Unit/buổi học</div>
          </div>
        ) : null}
      </section>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_440px]">
        <section className="space-y-3">
          {loading ? (
            <p className="text-sm font-semibold text-slate-500">Đang tải...</p>
          ) : !selectedProgramId ? (
            <div className={EMPTY_STATE_CLASS}>Chưa có giáo trình để biên soạn.</div>
          ) : filteredUnits.length === 0 ? (
            <div className={EMPTY_STATE_CLASS}>Giáo trình này chưa có unit/buổi học.</div>
          ) : (
            <>
              {pageItems.map((unit) => (
                <article key={unit.id} className={`${CARD_CLASS} transition hover:border-[#dfbfbd]`}>
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <BookMarked className="h-4 w-4 text-[#730014]" />
                        <h3 className="break-words font-['Manrope'] text-lg font-extrabold text-slate-900">
                          {unit.displayOrder ?? 0}. {unit.title}
                        </h3>
                      </div>
                      {unit.description ? <p className="mt-1 text-sm text-slate-600">{unit.description}</p> : null}
                      {unit.sessionPlan ? <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-700">{unit.sessionPlan}</p> : null}
                    </div>
                    <div className="flex shrink-0 flex-wrap gap-2">
                      <button type="button" onClick={() => openEditUnit(unit)} className={GHOST_BUTTON_CLASS}>Sửa</button>
                      <button type="button" onClick={() => deleteUnit(unit)} disabled={working} className={DANGER_BUTTON_CLASS}>
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </div>

                  <div className="mt-4 grid gap-3 md:grid-cols-2">
                    {refGroups.map((group) => {
                      const refs = unit[group.key] || [];
                      return (
                        <div key={group.key} className="rounded-2xl border border-slate-100 bg-slate-50 p-3">
                          <p className="text-xs font-bold uppercase tracking-[0.12em] text-slate-500">{group.title}</p>
                          {refs.length === 0 ? (
                            <p className="mt-2 text-xs text-slate-500">Chưa gắn.</p>
                          ) : (
                            <div className="mt-2 space-y-2">
                              {refs.map((ref) => (
                                <div key={`${ref.type}-${ref.id}`} className="flex items-start justify-between gap-2 rounded-xl bg-white px-3 py-2 text-sm shadow-sm">
                                  <div className="min-w-0">
                                    <p className="break-words font-semibold text-slate-900">{ref.title}</p>
                                    <p className="text-xs text-slate-500">{[ref.skill, ref.subtitle, ref.status].filter(Boolean).join(' · ')}</p>
                                  </div>
                                  <button type="button" onClick={() => detachResource(ref)} className="shrink-0 text-rose-600" title="Gỡ tài nguyên">
                                    <Trash2 className="h-4 w-4" />
                                  </button>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </article>
              ))}
              <Pagination page={page} totalPages={totalPages} onChange={setPage} totalItems={totalItems} pageSize={6} />
            </>
          )}
        </section>

        <aside className="space-y-4">
          <section className={PANEL_CLASS}>
            <h3 className="font-['Manrope'] text-lg font-extrabold text-slate-900">
              {editingUnitId ? 'Chỉnh sửa unit' : 'Thêm unit/buổi học'}
            </h3>
            <div className="mt-4 space-y-4">
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Tiêu đề</span>
                <input value={unitForm.title} onChange={(event) => setUnitForm({ ...unitForm, title: event.target.value })} className={FIELD_CLASS} />
              </label>
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Mô tả</span>
                <textarea value={unitForm.description} onChange={(event) => setUnitForm({ ...unitForm, description: event.target.value })} rows={3} className={TEXTAREA_CLASS} />
              </label>
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Kế hoạch buổi học</span>
                <textarea value={unitForm.sessionPlan} onChange={(event) => setUnitForm({ ...unitForm, sessionPlan: event.target.value })} rows={5} className={TEXTAREA_CLASS} />
              </label>
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Thứ tự</span>
                <input
                  type="number"
                  min="0"
                  value={unitForm.displayOrder}
                  onChange={(event) => setUnitForm({ ...unitForm, displayOrder: event.target.value })}
                  className={FIELD_CLASS}
                />
              </label>
              <div className="flex flex-wrap gap-2 border-t border-slate-100 pt-4">
                <button type="button" onClick={saveUnit} disabled={working || !selectedProgramId} className={PRIMARY_BUTTON_CLASS}>
                  <Save className="h-4 w-4" /> Lưu unit
                </button>
                <button type="button" onClick={resetUnitForm} className={SECONDARY_BUTTON_CLASS}>
                  <Plus className="h-4 w-4" /> Mới
                </button>
              </div>
            </div>
          </section>

          <section className={PANEL_CLASS}>
            <h3 className="font-['Manrope'] text-lg font-extrabold text-slate-900">Thêm từ kho</h3>
            <div className="mt-4 space-y-4">
              <div>
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Unit nhận tài nguyên</span>
                <BrandedSelect
                  value={attachForm.unitId}
                  onChange={(event) => setAttachForm({ ...attachForm, unitId: event.target.value })}
                  options={unitOptions}
                  placeholder="Chọn unit"
                />
              </div>
              <div>
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Loại tài nguyên</span>
                <BrandedSelect
                  value={attachForm.type}
                  onChange={(event) => setAttachForm({ ...attachForm, type: event.target.value, resourceId: '' })}
                  options={typeOptions}
                />
              </div>
              <div>
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Tài nguyên</span>
                <BrandedSelect
                  value={attachForm.resourceId}
                  onChange={(event) => setAttachForm({ ...attachForm, resourceId: event.target.value })}
                  options={resourceOptions}
                  placeholder={resourceOptions.length ? 'Chọn tài nguyên' : 'Kho này đang trống'}
                  disabled={!resourceOptions.length}
                />
              </div>
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Ghi chú</span>
                <textarea value={attachForm.note} onChange={(event) => setAttachForm({ ...attachForm, note: event.target.value })} rows={3} className={TEXTAREA_CLASS} />
              </label>
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Thứ tự trong unit</span>
                <input
                  type="number"
                  min="0"
                  value={attachForm.displayOrder}
                  onChange={(event) => setAttachForm({ ...attachForm, displayOrder: event.target.value })}
                  className={FIELD_CLASS}
                />
              </label>
              <button type="button" onClick={attachResource} disabled={working || !attachForm.unitId || !attachForm.resourceId} className={PRIMARY_BUTTON_CLASS}>
                <Link2 className="h-4 w-4" /> Gắn tài nguyên
              </button>
            </div>
          </section>
        </aside>
      </div>
    </div>
  );
}
