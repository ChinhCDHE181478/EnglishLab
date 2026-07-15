import { useEffect, useMemo, useState } from 'react';
import { Plus, RefreshCw } from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import classroomApi from '../../api/classroomApi';
import {
  ProgramFilterBar,
  ProgramPageHero,
  ProgramTable,
} from '../../components/curriculum/CurriculumProgramUi';
import { usePagination } from '../../components/ui/Pagination';
import {
  ERROR_NOTICE_CLASS,
  SUCCESS_NOTICE_CLASS,
} from '../../utils/formStyles';

const modeConfig = {
  OFFLINE: {
    title: 'Danh sách chương trình',
    subtitle: 'Lọc, theo dõi trạng thái và thao tác nhanh trên chương trình đào tạo.',
    deliveryMode: 'OFFLINE',
  },
  VIRTUAL: {
    title: 'Danh sách chương trình',
    subtitle: 'Lọc, theo dõi trạng thái và thao tác nhanh trên chương trình đào tạo.',
    deliveryMode: 'VIRTUAL',
  },
};

const examOptions = [
  { label: 'IELTS', value: 'IELTS' },
  { label: 'TOEIC', value: 'TOEIC' },
  { label: 'General English', value: 'GENERAL' },
];

const statusOptions = [
  { label: 'Nháp', value: 'DRAFT' },
  { label: 'Chờ duyệt', value: 'PENDING_REVIEW' },
  { label: 'Đã xuất bản', value: 'PUBLISHED' },
  { label: 'Từ chối', value: 'REJECTED' },
  { label: 'Lưu trữ', value: 'ARCHIVED' },
];

const usageFilterOptions = [
  { label: 'Tất cả', value: 'ALL' },
  { label: 'Đang có lớp dùng', value: 'ACTIVE' },
  { label: 'Chưa có lớp dùng', value: 'UNUSED' },
  { label: 'Đã từng có lớp', value: 'USED' },
];

const sortOptions = [
  { label: 'Mới cập nhật', value: 'UPDATED_DESC' },
  { label: 'Tên A-Z', value: 'TITLE_ASC' },
  { label: 'Nhiều lớp đang dùng', value: 'ACTIVE_DESC' },
  { label: 'Thứ tự hiển thị', value: 'DISPLAY_ORDER' },
];

const platformOptions = [
  { label: 'Lark', value: 'LARK' },
  { label: 'Zoom', value: 'ZOOM' },
  { label: 'Google Meet', value: 'GOOGLE_MEET' },
  { label: 'Liên kết thủ công', value: 'MANUAL' },
];

const toUpdatePayload = (program, deliveryMode, status) => ({
  title: program.title,
  code: program.code,
  slug: program.slug,
  deliveryMode,
  curriculumProgramId: Number(program.curriculumProgramId),
  shortDescription: program.shortDescription || null,
  description: program.description || null,
  price: program.price ?? 0,
  salePrice: program.salePrice ?? null,
  duration: program.duration || null,
  studyMode: program.studyMode || null,
  thumbnailUrl: program.thumbnailUrl || null,
  status,
  displayOrder: Number(program.displayOrder || 0),
  featured: Boolean(program.featured),
});

export default function ContentManagerTrainingProgramsPage({ mode = 'OFFLINE' }) {
  const location = useLocation();
  const navigate = useNavigate();
  const resolvedMode = location.pathname.includes('virtual') ? 'VIRTUAL' : mode;
  const config = modeConfig[resolvedMode] || modeConfig.OFFLINE;

  const [programs, setPrograms] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [examFilter, setExamFilter] = useState('ALL');
  const [levelFilter, setLevelFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [usageFilter, setUsageFilter] = useState('ALL');
  const [platformFilter, setPlatformFilter] = useState('ALL');
  const [sortBy, setSortBy] = useState('UPDATED_DESC');
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadPrograms = async () => {
    setLoading(true);
    setError('');
    try {
      const programData = await classroomApi.getContentManagerPrograms(config.deliveryMode);
      setPrograms(programData);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được danh sách chương trình.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPrograms();
  }, [config.deliveryMode]);

  const filteredPrograms = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    return programs.filter((item) => {
      if (normalized) {
        const haystack = [
          item.title,
          item.code,
          item.slug,
          item.curriculumProgramTitle,
          item.curriculumProgramCode,
          item.curriculumProgramExamCategory,
          item.entryLevel,
          item.status,
        ].filter(Boolean).map((value) => String(value).toLowerCase());
        if (!haystack.some((value) => value.includes(normalized))) return false;
      }
      if (examFilter !== 'ALL' && item.curriculumProgramExamCategory !== examFilter) return false;
      if (levelFilter.trim() && !String(item.entryLevel || '').toLowerCase().includes(levelFilter.trim().toLowerCase())) return false;
      if (statusFilter !== 'ALL' && item.status !== statusFilter) return false;
      if (usageFilter === 'ACTIVE' && !(item.activeClassroomCount > 0)) return false;
      if (usageFilter === 'UNUSED' && (item.activeClassroomCount > 0 || item.classroomCount > 0)) return false;
      if (usageFilter === 'USED' && !(item.classroomCount > 0)) return false;
      return true;
    });
  }, [programs, keyword, examFilter, levelFilter, statusFilter, usageFilter, platformFilter, config.deliveryMode]);

  const sortedPrograms = useMemo(() => {
    const list = [...filteredPrograms];
    if (sortBy === 'TITLE_ASC') {
      return list.sort((a, b) => String(a.title).localeCompare(String(b.title)));
    }
    if (sortBy === 'ACTIVE_DESC') {
      return list.sort((a, b) => (b.activeClassroomCount ?? 0) - (a.activeClassroomCount ?? 0) || String(a.title).localeCompare(String(b.title)));
    }
    if (sortBy === 'DISPLAY_ORDER') {
      return list.sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0) || String(a.title).localeCompare(String(b.title)));
    }
    return list.sort((a, b) => new Date(b.updatedAt || 0) - new Date(a.updatedAt || 0) || String(a.title).localeCompare(String(b.title)));
  }, [filteredPrograms, sortBy]);

  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    sortedPrograms,
    8,
    `${keyword}-${examFilter}-${levelFilter}-${statusFilter}-${usageFilter}-${platformFilter}-${sortBy}-${config.deliveryMode}`,
  );

  const resetFilters = () => {
    setKeyword('');
    setExamFilter('ALL');
    setLevelFilter('');
    setStatusFilter('ALL');
    setUsageFilter('ALL');
    setPlatformFilter('ALL');
    setSortBy('UPDATED_DESC');
    setPage(1);
  };

  const detailBasePath = config.deliveryMode === 'VIRTUAL'
    ? '/content-manager/virtual-programs'
    : '/content-manager/offline-programs';

  const openCreate = () => {
    navigate(`${detailBasePath}/new`);
  };

  const openEdit = (program) => {
    navigate(`${detailBasePath}/${program.id}/builder`);
  };

  const cloneProgram = async (program) => {
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      const clone = await classroomApi.cloneContentManagerProgram(program.id);
      navigate(`${detailBasePath}/${clone.id}/builder`);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không nhân bản được chương trình.');
    } finally {
      setWorking(false);
    }
  };

  const archiveProgram = async (program) => {
    if (program.activeClassroomCount > 0) {
      setError(`Chương trình đang được ${program.activeClassroomCount} lớp sắp khai giảng / đang diễn ra sử dụng, không thể lưu trữ.`);
      return;
    }
    if (!window.confirm(`Lưu trữ chương trình "${program.title}"?`)) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      await classroomApi.archiveContentManagerProgram(program.id);
      setPrograms((current) => current.map((item) => (
        String(item.id) === String(program.id) ? { ...item, status: 'ARCHIVED' } : item
      )));
      setSuccess('Đã lưu trữ chương trình.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu trữ được chương trình.');
    } finally {
      setWorking(false);
    }
  };

  const publishProgram = async (program) => {
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      const detail = await classroomApi.getContentManagerProgram(program.id);
      const saved = await classroomApi.updateContentManagerProgram(
        program.id,
        toUpdatePayload(detail, config.deliveryMode, 'PUBLISHED'),
      );
      setPrograms((current) => current.map((item) => (String(item.id) === String(saved.id) ? saved : item)));
      setSuccess('Đã xuất bản chương trình. Training Manager có thể dùng chương trình này để mở lớp.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể xuất bản chương trình.');
    } finally {
      setWorking(false);
    }
  };

  const publishedCount = programs.filter((p) => p.status === 'PUBLISHED').length;
  const activeUsageCount = programs.filter((p) => (p.activeClassroomCount ?? 0) > 0).length;

  return (
    <div className="space-y-5">
      <ProgramPageHero
        mode={config.deliveryMode}
        stats={[
          { label: 'Tổng chương trình', value: programs.length },
          { label: 'Đã xuất bản', value: publishedCount },
          { label: 'Lớp đang dùng', value: activeUsageCount },
        ]}
        subtitle={config.subtitle}
        title={config.title}
        actions={(
          <>
            <button className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg bg-[#4b0009] px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-[#730014] active:scale-[0.98]" onClick={openCreate} type="button">
              <Plus className="h-4 w-4" />
              Tạo chương trình
            </button>
            <button className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg border border-[#dcc0bf]/40 bg-white px-5 py-3 text-sm font-bold text-[#4b0009] shadow-sm transition hover:bg-[#eff4ff] active:scale-[0.98]" onClick={loadPrograms} type="button">
              <RefreshCw className="h-4 w-4" />
              Tải lại
            </button>
          </>
        )}
      />

      {error ? <div className={ERROR_NOTICE_CLASS}>{error}</div> : null}
      {success ? <div className={SUCCESS_NOTICE_CLASS}>{success}</div> : null}

      <ProgramFilterBar
        examFilter={examFilter}
        examOptions={examOptions}
        keyword={keyword}
        levelFilter={levelFilter}
        loading={loading}
        onExamFilterChange={(event) => setExamFilter(event.target.value)}
        onKeywordChange={setKeyword}
        onLevelFilterChange={setLevelFilter}
        onPlatformFilterChange={(event) => setPlatformFilter(event.target.value)}
        onRefresh={loadPrograms}
        onReset={resetFilters}
        onSortChange={(event) => setSortBy(event.target.value)}
        onStatusFilterChange={(event) => setStatusFilter(event.target.value)}
        onUsageFilterChange={(event) => setUsageFilter(event.target.value)}
        platformFilter={platformFilter}
        platformOptions={platformOptions}
        resultCount={totalItems}
        showPlatform={false}
        sortBy={sortBy}
        sortOptions={sortOptions}
        statusFilter={statusFilter}
        statusOptions={statusOptions}
        usageFilter={usageFilter}
        usageOptions={usageFilterOptions}
      />

      <ProgramTable
        detailBasePath={detailBasePath}
        loading={loading}
        onArchive={archiveProgram}
        onClone={cloneProgram}
        onEdit={openEdit}
        onPageChange={setPage}
        onPublish={publishProgram}
        page={page}
        pageSize={8}
        programs={pageItems}
        totalItems={totalItems}
        totalPages={totalPages}
        working={working}
      />

    </div>
  );
}
