import { useEffect, useMemo, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { BookOpen, CheckCircle2, Circle, Play, RefreshCw, Search, GraduationCap, History } from 'lucide-react';
import { Link } from 'react-router-dom';
import classroomApi from '../../api/classroomApi';
import { ClassroomEmptyState, ClassroomErrorState } from '../../components/classroom/ClassroomUi';
import LearnerPageShell from '../../components/learner/LearnerPageShell';
import BrandedSelect from '../../components/ui/BrandedSelect';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';

const PAGE_SIZE = 9;

const statusOptions = [
  { label: 'Tất cả trạng thái', value: 'ALL' },
  { label: 'Chưa luyện tập', value: 'PENDING' },
  { label: 'Đã hoàn thành', value: 'COMPLETED' },
];

const containerVariants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: {
      staggerChildren: 0.05,
    },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 15 },
  show: { opacity: 1, y: 0, transition: { duration: 0.3, ease: 'easeOut' } },
};

const getPracticeSummary = (practice) => {
  const instruction = String(practice.instruction || '').trim();
  if (instruction.startsWith('{')) {
    try {
      const config = JSON.parse(instruction);
      const parts = Array.isArray(config.parts) ? config.parts : [];
      const questionCount = parts.reduce((total, part) => (
        total + (part.questionGroups || []).reduce((partTotal, group) => (
          partTotal + (group.questionNumbers?.length || group.questions?.length || 0)
        ), 0)
      ), 0);
      const details = [];
      if (config.durationMinutes) details.push(`${config.durationMinutes} phút`);
      if (questionCount) details.push(`${questionCount} câu hỏi`);
      if (parts.length) details.push(`${parts.length} phần làm bài`);
      return details.length
        ? `Làm trực tiếp trên hệ thống · ${details.join(' · ')}`
        : 'Làm bài luyện tập trực tiếp trên hệ thống.';
    } catch {
      return 'Làm bài luyện tập trực tiếp trên hệ thống.';
    }
  }
  return practice.note || instruction || 'Nội dung luyện tập đang được cập nhật.';
};

const getUnitLabel = (practice) => {
  const title = String(practice.unitTitle || '').trim();
  const unitPrefix = `Unit ${practice.unitDisplayOrder}`;
  return title.toLowerCase().startsWith(unitPrefix.toLowerCase())
    ? title
    : `${unitPrefix}: ${title}`;
};

export default function MyPracticePage() {
  const [practices, setPractices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [classroomFilter, setClassroomFilter] = useState('ALL');

  const loadPractices = async () => {
    setLoading(true);
    setError('');
    try {
      setPractices(await classroomApi.getMyPractice());
    } catch (err) {
      setError(getClassroomErrorMessage(err, 'Không thể tải danh sách luyện tập.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPractices();
  }, []);

  const classroomOptions = useMemo(() => {
    const classrooms = new Map();
    practices.forEach((item) => classrooms.set(String(item.classroomOfferingId), item.classroomTitle));
    return [
      { label: 'Tất cả lớp đang học', value: 'ALL' },
      ...[...classrooms.entries()].map(([value, label]) => ({ value, label })),
    ];
  }, [practices]);

  const filteredPractices = useMemo(() => {
    const keyword = searchQuery.trim().toLowerCase();
    return practices.filter((item) => {
      if (statusFilter === 'COMPLETED' && !item.completed) return false;
      if (statusFilter === 'PENDING' && item.completed) return false;
      if (classroomFilter !== 'ALL' && String(item.classroomOfferingId) !== classroomFilter) return false;
      if (!keyword) return true;
      return [item.title, item.unitTitle, item.classroomTitle, item.skill]
        .some((value) => String(value || '').toLowerCase().includes(keyword));
    });
  }, [classroomFilter, practices, searchQuery, statusFilter]);

  const resetKey = `${statusFilter}|${classroomFilter}|${searchQuery}`;
  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(filteredPractices, PAGE_SIZE, resetKey);
  const completedCount = practices.filter((item) => item.completed).length;
  const classCount = new Set(practices.map((item) => item.classroomOfferingId)).size;

  return (
    <LearnerPageShell
      description="Nội dung luyện tập có sẵn trong giáo trình của các lớp bạn đang học. Kết quả không tính vào bảng điểm."
      title="Luyện tập của tôi"
    >
      {loading ? (
        <div className="space-y-6 flex-1">
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-24 w-full animate-pulse rounded-[24px] border border-gray-100 bg-white/60 p-4" />
            ))}
          </div>
          <div className="h-12 w-full animate-pulse rounded-[24px] bg-gray-100/50" />
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-72 w-full animate-pulse rounded-[24px] border border-gray-100 bg-white p-6" />
            ))}
          </div>
        </div>
      ) : error ? (
        <div className="flex flex-1 flex-col items-center justify-center py-12">
          <ClassroomErrorState message={error} onRetry={loadPractices} />
        </div>
      ) : practices.length === 0 ? (
        <div className="flex flex-1 flex-col items-center justify-center py-16">
          <ClassroomEmptyState
            actionLabel="Vào lớp của tôi"
            actionTo="/my-classrooms"
            description="Tuyệt vời! Hiện tại bạn không có bài luyện tập nào hoặc chưa được phân công luyện tập."
            title="Không có bài luyện tập nào"
          />
        </div>
      ) : (
        <div className="space-y-8 flex-1">
          {/* Glass Counter Cards */}
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
            <GlassCounterCard label="Tổng bài luyện tập" value={practices.length} dotColor="bg-blue-500" icon={<BookOpen className="h-5 w-5" />} />
            <GlassCounterCard label="Đã hoàn thành" value={completedCount} dotColor="bg-emerald-500" icon={<CheckCircle2 className="h-5 w-5" />} />
            <GlassCounterCard label="Lớp có nội dung" value={classCount} dotColor="bg-purple-500" icon={<GraduationCap className="h-5 w-5" />} />
          </div>

          <div className="space-y-6">
            {/* Filter and Search Layout in single premium bar */}
            <section className="grid gap-3 rounded-[24px] border border-[#ead9db]/85 bg-white p-4 shadow-[0_8px_30px_rgba(75,0,9,0.015)] lg:grid-cols-[1fr_240px_240px_auto]">
              <label className="relative block">
                <Search className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-[#c2acab]" />
                <input
                  className="w-full rounded-2xl border border-[#dfbfbd]/50 bg-[#fffdfd] py-3 pl-11 pr-4 text-sm outline-none transition focus:border-[#730014] focus:bg-white focus:ring-4 focus:ring-[#730014]/5"
                  onChange={(event) => setSearchQuery(event.target.value)}
                  placeholder="Tìm bài luyện tập, unit hoặc lớp..."
                  value={searchQuery}
                />
              </label>
              <BrandedSelect
                buttonClassName="h-full rounded-2xl border-[#dfbfbd]/50 bg-[#fffdfd]"
                onChange={(event) => setClassroomFilter(event.target.value)}
                options={classroomOptions}
                value={classroomFilter}
                searchable={true}
              />
              <BrandedSelect
                buttonClassName="h-full rounded-2xl border-[#dfbfbd]/50 bg-[#fffdfd]"
                onChange={(event) => setStatusFilter(event.target.value)}
                options={statusOptions}
                value={statusFilter}
              />
              <button
                aria-label="Tải lại"
                className="inline-flex items-center justify-center gap-2 rounded-2xl border border-[#dfbfbd] bg-white px-5 py-3 text-sm font-extrabold text-[#730014] shadow-sm transition hover:bg-[#fff2f3] active:scale-95"
                onClick={loadPractices}
                type="button"
              >
                <RefreshCw className="h-4 w-4" /> Tải lại
              </button>
            </section>

            {/* Premium Practice Card Grid with Motion */}
            <AnimatePresence mode="wait">
              {filteredPractices.length > 0 ? (
                <>
                  <motion.div
                    key={resetKey}
                    variants={containerVariants}
                    initial="hidden"
                    animate="show"
                    className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"
                  >
                    {pageItems.map((practice) => (
                      <motion.article
                        key={`${practice.classroomOfferingId}-${practice.exerciseId}`}
                        variants={itemVariants}
                        className="group relative flex flex-col rounded-[24px] border border-gray-200/80 bg-white shadow-[0_12px_35px_rgba(0,0,0,0.02)] transition-all duration-300 hover:border-[#dfbfbd]/50 hover:shadow-[0_18px_45px_rgba(75,0,9,0.06)] hover:-translate-y-1 overflow-hidden"
                      >
                        {/* Card Content with padded body */}
                        <div className="p-6 flex flex-col flex-1 space-y-4">
                          <div className="flex items-center justify-between gap-3">
                            <span
                              className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[9px] font-extrabold uppercase tracking-widest border ${
                                practice.completed
                                  ? 'border-emerald-100 bg-emerald-50/60 text-emerald-700'
                                  : 'border-amber-100 bg-amber-50/60 text-amber-700'
                              }`}
                            >
                              {practice.completed ? <CheckCircle2 className="h-3 w-3" /> : <Circle className="h-3 w-3" />}
                              {practice.completed ? 'Đã hoàn thành' : 'Chưa luyện tập'}
                            </span>
                            {practice.skill ? (
                              <span className={`rounded-lg px-2 py-0.5 text-[9px] font-extrabold uppercase tracking-widest ${getSkillBadgeStyle(practice.skill)}`}>
                                {practice.skill}
                              </span>
                            ) : null}
                          </div>

                          <div>
                            <p className="text-[10px] font-extrabold uppercase tracking-wider text-[#8b706e] truncate" title={practice.classroomTitle}>
                              {practice.classroomTitle}
                            </p>
                            <h3 className="mt-1 font-['Manrope'] text-sm md:text-base font-extrabold leading-snug text-[#1a1c1c] line-clamp-2 group-hover:text-[#730014] transition-colors" title={practice.title}>
                              {practice.title}
                            </h3>
                            <p className="mt-2 inline-flex items-center gap-1 text-[10px] font-bold text-[#4b0009] bg-[#fff0f1]/50 border border-[#dfbfbd]/25 px-2 py-0.5 rounded-md">
                              {getUnitLabel(practice)}
                            </p>
                          </div>

                          <p className="text-xs text-[#584140] line-clamp-3 leading-relaxed pt-1">
                            {getPracticeSummary(practice)}
                          </p>
                          <p className="inline-flex items-center gap-1.5 text-[11px] font-bold text-[#806765]"><History className="h-3.5 w-3.5" />{practice.attemptCount || 0} lượt đã làm{practice.lastScorePercent != null ? ` · Gần nhất ${Math.round(practice.lastScorePercent)}%` : ''}</p>
                        </div>

                        {/* Card Footer stretching edge-to-edge identical to MyHomeworkPage */}
                        <div className="border-t border-gray-50 bg-gray-50/40 px-6 py-4 flex items-center justify-between mt-auto">
                          <Link
                            className="text-xs font-bold text-gray-600 hover:text-[#730014] underline"
                            to={`/my-classrooms/${practice.classroomOfferingId}`}
                          >
                            Vào lớp học
                          </Link>
                          <Link
                            className={`inline-flex items-center gap-1.5 rounded-xl px-4 py-2.5 text-xs font-bold text-white transition-all shadow-sm active:scale-95 ${
                              practice.completed
                                ? 'bg-gradient-to-r from-gray-600 to-gray-700 hover:from-gray-700 hover:to-gray-800'
                                : 'bg-gradient-to-r from-[#730014] to-[#4b0009] shadow-[#4b0009]/10 hover:shadow-[#730014]/15'
                            }`}
                            to={`/my-practice/${practice.classroomOfferingId}/${practice.exerciseId}`}
                          >
                            <Play className="h-3.5 w-3.5 fill-current" />
                            {practice.completed ? 'Luyện lại' : 'Bắt đầu'}
                          </Link>
                        </div>
                      </motion.article>
                    ))}
                  </motion.div>
                </>
              ) : (
                <motion.div
                  key="empty"
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -10 }}
                >
                  <ClassroomEmptyState
                    description="Hãy chọn một bộ lọc khác hoặc kiểm tra giáo trình của lớp đang học."
                    title="Chưa có bài luyện tập phù hợp"
                  />
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          {!loading && filteredPractices.length ? (
            <div className="mt-6 flex justify-end">
              <Pagination
                onChange={setPage}
                page={page}
                pageSize={PAGE_SIZE}
                totalItems={totalItems}
                totalPages={totalPages}
                alwaysVisible={true}
              />
            </div>
          ) : null}
        </div>
      )}

    </LearnerPageShell>
  );
}

// ─── Glass stats counter component ─────────────────────────────────────────────
function GlassCounterCard({ label, value, dotColor, icon }) {
  return (
    <div className="relative overflow-hidden rounded-[24px] border border-gray-200/80 bg-white p-5 shadow-[0_10px_30px_rgba(0,0,0,0.02)] transition-all duration-300 hover:border-[#dfbfbd]/60 hover:shadow-[0_15px_35px_rgba(75,0,9,0.04)] hover:-translate-y-0.5 flex items-center justify-between group">
      <div className="flex items-center gap-3">
        <div className="rounded-xl p-2.5 shrink-0 bg-[#fff0f1] text-[#730014]">
          {icon}
        </div>
        <div>
          <span className="text-[10px] font-extrabold uppercase tracking-widest text-[#8b706e]">{label}</span>
          <p className="font-['Manrope'] text-2xl font-extrabold text-[#1a1c1c] mt-0.5">{value}</p>
        </div>
      </div>
      <span className={`h-2 w-2 rounded-full ${dotColor} opacity-70 group-hover:opacity-100 group-hover:scale-125 transition-all duration-300`} />
    </div>
  );
}

const getSkillBadgeStyle = (skill) => {
  const s = String(skill).toUpperCase();
  if (s.includes('READING')) return 'bg-sky-50 text-sky-700 border border-sky-100';
  if (s.includes('LISTENING')) return 'bg-teal-50 text-teal-700 border border-teal-100';
  if (s.includes('WRITING')) return 'bg-rose-50 text-rose-700 border border-rose-100';
  if (s.includes('SPEAKING')) return 'bg-violet-50 text-violet-700 border border-violet-100';
  return 'bg-slate-50 text-slate-700 border border-slate-100';
};
