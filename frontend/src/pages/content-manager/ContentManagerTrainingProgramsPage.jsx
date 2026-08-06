import { useEffect, useMemo, useState } from 'react';
import { useAppDialog } from '../../components/ui/AppDialog';
import {
  CheckCircle2,
  Download,
  FileSpreadsheet,
  LoaderCircle,
  Plus,
  RefreshCw,
  UploadCloud,
  X,
} from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import classroomApi from '../../api/classroomApi';
import curriculumApi from '../../api/curriculumApi';
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

const downloadProgramExcelTemplate = async () => {
  const XLSX = await import('@e965/xlsx');
  const rows = [
    ['Tên chương trình đào tạo / Khóa học', 'Tên unit/buổi học', 'Mô tả & Mục tiêu buổi học'],
    ['IELTS Intensive 6.5+', 'Buổi 1: Tổng quan IELTS Writing Task 2', 'Phân tích dạng đề Opinion Essay và tiêu chí Task Response.'],
    ['', 'Buổi 2: Phương pháp phát triển ý tưởng', 'Học kỹ thuật PEEL (Point, Explanation, Example, Link)'],
    ['', 'Buổi 3: Vocabulary & Collocations về Topic Education', 'Tích lũy 25 collocations C1/C2 chủ đề Giáo dục'],
    ['', 'Buổi 4: IELTS Speaking Part 2 Strategy', 'Luyện tập Mindmap và cách kéo dài câu trả lời 2 phút.'],
  ];
  const worksheet = XLSX.utils.aoa_to_sheet(rows);
  worksheet['!cols'] = [{ wch: 30 }, { wch: 45 }, { wch: 60 }];
  worksheet['!autofilter'] = { ref: 'A1:C5' };
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, 'Khung_Chuong_Trinh');
  XLSX.writeFile(workbook, 'Mau_Import_Chuong_Trinh_Dao_Tao.xlsx');
};

const parseProgramExcelFile = async (file) => {
  const XLSX = await import('@e965/xlsx');
  const workbook = XLSX.read(await file.arrayBuffer(), { type: 'array' });
  const sheet = workbook.Sheets[workbook.SheetNames[0]];
  const rows = XLSX.utils.sheet_to_json(sheet, { header: 1, defval: '', raw: false });

  if (!rows || rows.length < 2) {
    throw new Error('Tệp Excel không chứa dữ liệu hoặc sai định dạng.');
  }

  let programTitle = '';
  const units = [];
  const secondHeader = String(rows[0]?.[1] || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();
  const hasLegacyCodeColumn = secondHeader.includes('ma giao trinh')
    || secondHeader.includes('ma chuong trinh');
  const unitColumn = hasLegacyCodeColumn ? 2 : 1;
  const descriptionColumn = hasLegacyCodeColumn ? 3 : 2;

  for (let i = 1; i < rows.length; i++) {
    const row = rows[i];
    if (!row || !row.length) continue;
    const c0 = String(row[0] || '').trim();
    const unitTitle = String(row[unitColumn] || '').trim();
    const description = String(row[descriptionColumn] || '').trim();

    if (c0 && !programTitle) programTitle = c0;

    if (unitTitle) {
      units.push({
        displayOrder: units.length + 1,
        title: unitTitle,
        description: description || null,
      });
    } else if (c0) {
      if (!programTitle) {
        programTitle = c0;
      } else if (c0 !== programTitle) {
        units.push({
          displayOrder: units.length + 1,
          title: c0,
          description: description || null,
        });
      }
    }
  }

  if (!programTitle && !units.length) {
    throw new Error('Không tìm thấy tên khóa học hoặc danh sách unit trong tệp Excel.');
  }

  return {
    programTitle: programTitle || file.name.replace(/\.[^/.]+$/, ''),
    units,
    fileName: file.name,
  };
};

const modeConfig = {
  OFFLINE: {
    title: 'Khóa học Offline',
    subtitle: 'Quản lý các khóa học tại trung tâm dùng để tiếp nhận nhu cầu và đề xuất mở lớp.',
    deliveryMode: 'OFFLINE',
  },
  VIRTUAL: {
    title: 'Khóa học Virtual',
    subtitle: 'Quản lý các khóa học trực tuyến có lịch và giảng viên, tách biệt với khóa online tự học.',
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
  { label: 'Sẵn sàng xuất bản (cũ)', value: 'PENDING_REVIEW' },
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
  { label: 'Google Meet', value: 'GOOGLE_MEET' },
  { label: 'Zoom', value: 'ZOOM' },
  { label: 'Google Meet', value: 'GOOGLE_MEET' },
  { label: 'Liên kết thủ công', value: 'MANUAL' },
];

const toUpdatePayload = (program, deliveryType, status) => ({
  title: program.title,
  code: program.code,
  slug: program.slug,
  deliveryType,
  curriculumProgramId: Number(program.curriculumProgramId),
  shortDescription: program.shortDescription || null,
  description: program.description || null,
  price: program.price ?? 0,
  salePrice: program.salePrice ?? null,
  duration: program.duration || null,
  studyMode: program.studyMode || null,
  capacity: program.capacity ?? program.maxCapacity ?? 30,
  plannedStartDate: program.plannedStartDate || null,
  plannedSchedule: program.plannedSchedule || null,
  thumbnailUrl: program.thumbnailUrl || null,
  status,
  displayOrder: Number(program.displayOrder || 0),
  featured: Boolean(program.featured),
});

export default function ContentManagerTrainingProgramsPage({ mode = 'OFFLINE' }) {
  const { confirm: confirmDialog } = useAppDialog();
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

  // Excel import modal state
  const [excelModalOpen, setExcelModalOpen] = useState(false);
  const [parsedExcel, setParsedExcel] = useState(null);
  const [excelReading, setExcelReading] = useState(false);
  const [excelImporting, setExcelImporting] = useState(false);
  const [excelError, setExcelError] = useState('');

  const loadPrograms = async () => {
    setLoading(true);
    setError('');
    try {
      const programData = await classroomApi.getContentManagerPrograms(config.deliveryMode);
      setPrograms(programData);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được danh sách khóa học.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPrograms();
  }, [config.deliveryMode]);

  const handleExcelFileChange = async (file) => {
    if (!file) return;
    if (!/\.(xlsx|xls)$/i.test(file.name)) {
      setExcelError('Chỉ hỗ trợ tệp Excel định dạng .xlsx hoặc .xls.');
      return;
    }
    setExcelReading(true);
    setExcelError('');
    setParsedExcel(null);
    try {
      const parsed = await parseProgramExcelFile(file);
      setParsedExcel(parsed);
    } catch (err) {
      setExcelError(err.message || 'Không đọc được tệp Excel.');
    } finally {
      setExcelReading(false);
    }
  };

  const handleImportExcelSubmit = async () => {
    if (!parsedExcel) return;
    setExcelImporting(true);
    setExcelError('');
    try {
      const curriculum = await curriculumApi.createCurriculumProgram({
        title: parsedExcel.programTitle,
        examCategory: 'GENERAL_ENGLISH',
        programTrack: 'GENERAL_ENGLISH_COMMUNICATION',
        focusSkills: 'LISTENING,SPEAKING,VOCABULARY,COMMUNICATION',
        entryLevel: 'B1',
        deliveryMode: config.deliveryMode,
        outcomes: 'Nội dung khởi tạo từ tệp Excel',
        totalSessions: parsedExcel.units.length,
        status: 'DRAFT',
      });

      if (parsedExcel.units?.length) {
        for (const unit of parsedExcel.units) {
          try {
            await curriculumApi.createCurriculumUnit(curriculum.id, unit);
          } catch {}
        }
      }

      const createdProgram = await classroomApi.createContentManagerProgram({
        title: parsedExcel.programTitle,
        deliveryType: config.deliveryMode,
        curriculumProgramId: curriculum.id,
        status: 'DRAFT',
        capacity: 30,
        price: 0,
      });

      setExcelModalOpen(false);
      setParsedExcel(null);
      navigate(`${detailBasePath}/${createdProgram.id}/builder`);
    } catch (err) {
      setExcelError(err?.response?.data?.message || 'Chưa thể tạo khóa học từ Excel. Bạn vẫn có thể tạo thủ công.');
    } finally {
      setExcelImporting(false);
    }
  };

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
      setError(err?.response?.data?.message || 'Không nhân bản được khóa học.');
    } finally {
      setWorking(false);
    }
  };

  const archiveProgram = async (program) => {
    if (program.activeClassroomCount > 0) {
      setError(`Khóa học đang được ${program.activeClassroomCount} lớp sắp khai giảng / đang diễn ra sử dụng, không thể lưu trữ.`);
      return;
    }
    if (!await confirmDialog(`Lưu trữ khóa học “${program.title}”?`, {
      title: 'Lưu trữ khóa học',
      confirmLabel: 'Lưu trữ',
      tone: 'danger',
    })) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      await classroomApi.archiveContentManagerProgram(program.id);
      setSuccess(`Đã lưu trữ khóa học “${program.title}”.`);
      await loadPrograms();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu trữ được khóa học.');
    } finally {
      setWorking(false);
    }
  };

  const publishProgram = async (program) => {
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      await classroomApi.updateContentManagerProgram(
        program.id,
        toUpdatePayload(program, config.deliveryMode, 'PUBLISHED')
      );
      setSuccess(`Đã xuất bản khóa học “${program.title}”.`);
      await loadPrograms();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không xuất bản được khóa học.');
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
          { label: 'Tổng khóa học', value: programs.length },
          { label: 'Đã xuất bản', value: publishedCount },
          { label: 'Lớp đang dùng', value: activeUsageCount },
        ]}
        subtitle={config.subtitle}
        title={config.title}
        actions={(
          <>
            <button className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg bg-[#4b0009] px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-[#730014] active:scale-[0.98]" onClick={openCreate} type="button">
              <Plus className="h-4 w-4" />
              Tạo khóa học
            </button>
            <button
              className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg border border-[#dfbfbd] bg-[#fff8f8] px-5 py-3 text-sm font-bold text-[#730014] shadow-sm transition hover:bg-[#fff0f1] active:scale-[0.98]"
              onClick={() => setExcelModalOpen(true)}
              type="button"
            >
              <FileSpreadsheet className="h-4 w-4" />
              Import từ Excel
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

      {excelModalOpen && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-[#1a0004]/50 p-4 backdrop-blur-sm">
          <div className="w-full max-w-xl rounded-[28px] border border-[#ead9db] bg-white p-6 shadow-2xl space-y-5">
            <div className="flex items-start justify-between border-b border-[#f1e4e5] pb-4">
              <div>
                <span className="text-[10px] font-extrabold uppercase tracking-widest text-[#8a0018]">Khởi tạo nhanh</span>
                <h3 className="font-['Manrope'] text-xl font-black text-[#2b2828]">Import {config.title || 'Chương trình đào tạo'} từ Excel</h3>
              </div>
              <button
                aria-label="Đóng"
                className="flex h-8 w-8 items-center justify-center rounded-full bg-slate-100 text-slate-500 hover:bg-rose-50 hover:text-rose-700"
                onClick={() => {
                  setExcelModalOpen(false);
                  setParsedExcel(null);
                  setExcelError('');
                }}
                type="button"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <p className="text-xs leading-5 text-[#584140]">
              Tải lên tệp Excel (.xlsx). Hệ thống sẽ trích xuất {config.deliveryMode === 'CURRICULUM' ? 'Tên chương trình' : 'Tên khóa học'} và danh sách Unit/Buổi học để tạo bản nháp ngay lập tức.
            </p>

            <div className="flex flex-col items-center justify-center rounded-2xl border-2 border-dashed border-[#dfbfbd] bg-[#fffafb] p-6 text-center transition hover:border-[#8a0018]">
              <UploadCloud className="h-10 w-10 text-[#8a0018]" />
              <p className="mt-3 text-sm font-bold text-[#2b2828]">Chọn hoặc kéo thả tệp Excel vào đây</p>
              <p className="mt-1 text-xs text-slate-400">Chỉ nhận định dạng .xlsx hoặc .xls</p>

              <label className="mt-4 cursor-pointer rounded-xl bg-[#4b0009] px-4 py-2 text-xs font-extrabold text-white transition hover:bg-[#730014]">
                Browse File Excel
                <input
                  type="file"
                  accept=".xlsx,.xls"
                  className="hidden"
                  onChange={(event) => handleExcelFileChange(event.target.files?.[0])}
                />
              </label>
            </div>

            <div className="flex items-center justify-between border-t border-slate-100 pt-3">
              <button
                type="button"
                className="inline-flex items-center gap-1.5 text-xs font-bold text-[#8a0018] hover:underline"
                onClick={downloadProgramExcelTemplate}
              >
                <Download className="h-3.5 w-3.5" /> Tải bản mẫu Excel chuẩn
              </button>
            </div>

            {excelReading && (
              <div className="flex items-center gap-2 rounded-xl bg-slate-50 p-3 text-xs font-semibold text-slate-600">
                <LoaderCircle className="h-4 w-4 animate-spin text-[#8a0018]" /> Đang đọc tệp Excel...
              </div>
            )}

            {excelError ? (
              <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs font-bold text-rose-700">
                {excelError}
              </div>
            ) : null}

            {parsedExcel && (
              <div className="rounded-2xl border border-emerald-200 bg-emerald-50/60 p-4 space-y-2">
                <div className="flex items-center gap-2 text-emerald-800 font-extrabold text-xs">
                  <CheckCircle2 className="h-4 w-4 text-emerald-600" /> Đã đọc tệp thành công: {parsedExcel.fileName}
                </div>
                <div className="text-xs text-slate-700 space-y-1 pl-6">
                  <p><strong>Tên khóa học:</strong> {parsedExcel.programTitle}</p>
                  <p><strong>Mã:</strong> Hệ thống sẽ tự tạo khi khởi tạo</p>
                  <p><strong>Số lượng Unit trích xuất:</strong> {parsedExcel.units.length} buổi học</p>
                </div>
              </div>
            )}

            <div className="flex justify-end gap-2 border-t border-slate-100 pt-3">
              <button
                className="rounded-xl border border-[#dfbfbd] px-4 py-2 text-xs font-bold text-[#730014] hover:bg-slate-50"
                onClick={() => {
                  setExcelModalOpen(false);
                  setParsedExcel(null);
                }}
                type="button"
              >
                Hủy
              </button>
              <button
                className="rounded-xl bg-[#4b0009] px-5 py-2 text-xs font-extrabold text-white transition hover:bg-[#730014] disabled:opacity-60"
                disabled={!parsedExcel || excelImporting}
                onClick={handleImportExcelSubmit}
                type="button"
              >
                {excelImporting ? 'Đang tạo khóa học...' : 'Khởi tạo khóa học từ Excel'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
