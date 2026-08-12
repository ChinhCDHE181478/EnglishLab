import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  Check,
  ChevronDown,
  GripVertical,
  Plus,
  RefreshCw,
  Route,
  Search,
  X,
} from "lucide-react";
import courseApi from "../../api/courseApi";
import {
  ContentManagerLoadingState,
  HeaderActions,
  Panel,
  TextField,
} from "../../components/content-manager/ContentManagerUi";
import Pagination, { usePagination } from "../../components/ui/Pagination";
import { useAppDialog } from "../../components/ui/AppDialog";
import BrandedSelect from "../../components/ui/BrandedSelect";

export default function ContentManagerLearningPathsPage() {
  const { confirm: confirmDialog } = useAppDialog();
  const [courses, setCourses] = useState([]);
  const [paths, setPaths] = useState([]);
  const [expanded, setExpanded] = useState({});
  const [modal, setModal] = useState(null);
  const [form, setForm] = useState({ code: "", name: "", examCategory: "IELTS", targetBand: "", targetScore: "" });
  const [courseIds, setCourseIds] = useState([]);
  const [modalSearch, setModalSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadData = async () => {
    setLoading(true);
    try {
      const [courseResult, pathResult] = await Promise.all([
        courseApi.getManagedOnlineCourses({ page: 0, size: 500 }),
        courseApi.getManagedLearningPaths({ page: 0, size: 500 }),
      ]);
      setCourses(courseResult.content || []);
      setPaths(pathResult.content || []);
    } catch (err) {
      setError(
        err?.response?.data?.message || "Không thể tải dữ liệu lộ trình.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const {
    page: pathPage,
    setPage: setPathPage,
    totalPages: pathTotalPages,
    pageItems: pathPageItems,
    totalItems: pathTotalItems,
  } = usePagination(paths, 10);

  const availableCourses = useMemo(() => {
    if (!modal || modal.mode !== "add" || !modal.group) return [];
    const filtered = courses.filter(
      (course) => !modal.group.courses.some((item) => item.courseId === course.id)
    );
    const query = modalSearch.trim().toLowerCase();
    if (!query) return filtered;
    return filtered.filter((course) =>
      course.title.toLowerCase().includes(query)
    );
  }, [courses, modal, modalSearch]);

  const {
    page: modalPage,
    setPage: setModalPage,
    totalPages: modalTotalPages,
    pageItems: modalPageItems,
    totalItems: modalTotalItems,
  } = usePagination(availableCourses, 10, modal?.group?.id);

  const openModal = (group = null, mode = "create") => {
    setModal({ group, mode });
    setForm({
      code: group?.code || "",
      name: group?.name || "",
      examCategory: group?.examCategory || "IELTS",
      targetBand: group?.targetBand ?? "",
      targetScore: group?.targetScore ?? "",
    });
    setCourseIds(mode === "edit" ? group?.courses.map((course) => course.courseId) || [] : []);
    setModalSearch("");
    setError("");
  };

  const toggleCourse = (id) =>
    setCourseIds((current) =>
      current.includes(id)
        ? current.filter((value) => value !== id)
        : [...current, id],
    );
  const handleDraftDrop = (event, targetId) => {
    event.preventDefault();
    const sourceId = Number(event.dataTransfer.getData("text/plain"));
    if (!sourceId || sourceId === targetId) return;
    setCourseIds((current) => {
      const source = current.indexOf(sourceId);
      const target = current.indexOf(targetId);
      if (source < 0 || target < 0) return current;
      const next = [...current];
      next.splice(source, 1);
      next.splice(target, 0, sourceId);
      return next;
    });
  };

  const savePath = async () => {
    const code = form.code.trim();
    const name = form.name.trim();
    const pathPayload = {
      code,
      name,
      examCategory: form.examCategory || null,
      targetBand: form.examCategory === "IELTS" && form.targetBand !== "" ? Number(form.targetBand) : null,
      targetScore: form.examCategory === "TOEIC" && form.targetScore !== "" ? Number(form.targetScore) : null,
    };
    if (modal.mode === "create" && (!code || !name)) {
      setError("Nhập mã và tên lộ trình.");
      return;
    }
    if (modal.mode === "add" && !courseIds.length) {
      setError("Chọn ít nhất một khóa học để thêm.");
      return;
    }
    setSaving(true);
    try {
      if (modal.mode === "create") {
        await courseApi.createManagedLearningPath(pathPayload);
      } else if (modal.mode === "add") {
        await courseApi.addManagedLearningPathCourses(modal.group.id, courseIds);
      } else {
        await Promise.all([
          courseApi.updateManagedLearningPath(modal.group.id, pathPayload),
          courseApi.reorderManagedLearningPathCourses(modal.group.id, courseIds),
        ]);
      }
      setModal(null);
      setSuccess(modal.mode === "add" ? "Đã thêm khóa học vào lộ trình." : "Đã lưu lộ trình.");
      await loadData();
    } catch (err) {
      setError(err?.response?.data?.message || "Không thể lưu lộ trình.");
    } finally {
      setSaving(false);
    }
  };

  const deletePath = async (group) => {
    if (
      !await confirmDialog(
        `Xóa lộ trình “${group.name}”? Các khóa học sẽ không bị xóa.`,
        {
          title: 'Xóa lộ trình',
          confirmLabel: 'Xóa lộ trình',
          tone: 'danger',
        },
      )
    )
      return;
    setSaving(true);
    try {
      await courseApi.deleteManagedLearningPath(group.id);
      setSuccess("Đã xóa lộ trình.");
      await loadData();
    } catch (err) {
      setError(err?.response?.data?.message || "Không thể xóa lộ trình.");
    } finally {
      setSaving(false);
    }
  };

  if (loading && !courses.length)
    return <ContentManagerLoadingState message="Đang tải lộ trình học..." />;

  return (
    <div className="space-y-6">
      <HeaderActions>
        <button
          className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg bg-[#4b0009] px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-[#730014] active:scale-[0.98]"
          onClick={() => openModal()}
          type="button"
        >
          <Plus className="h-4 w-4" />
          Tạo lộ trình
        </button>
      </HeaderActions>
      {error ? <Notice tone="error">{error}</Notice> : null}
      {success ? <Notice>{success}</Notice> : null}
      {pathPageItems.map((group) => {
        const isExpanded = expanded[group.code] !== false;
        return (
          <Panel key={group.code} className="overflow-hidden transition-all duration-200 hover:border-[#dfbfbd] hover:shadow-md">
            <div className="flex items-center gap-3 px-6 py-5">
              <button
                className="flex min-w-0 flex-1 items-center gap-3 text-left"
                onClick={() =>
                  setExpanded((current) => ({
                    ...current,
                    [group.code]: !isExpanded,
                  }))
                }
                type="button"
              >
                <span className="min-w-0">
                  <span className="block truncate font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">
                    {group.name}
                  </span>
                  <span className="mt-0.5 block text-xs text-[#8b706e]">
                    {group.courses.length} khóa học · {group.examCategory || "Chưa chọn kỳ thi"}
                    {group.targetBand != null ? ` · Band ${group.targetBand}` : group.targetScore != null ? ` · ${group.targetScore} điểm` : ""}
                  </span>
                </span>
              </button>
              <div className="flex shrink-0 items-center gap-2">
                  <button
                    className="rounded-xl border border-[#dfbfbd] px-3 py-1.5 text-xs font-bold text-[#730014] transition hover:bg-[#fff2f3]"
                    onClick={() => openModal(group, "add")}
                    type="button"
                  >
                    <Plus className="mr-1 inline h-3.5 w-3.5" />
                    Thêm
                  </button>
                  <button
                    className="rounded-xl border border-[#dfbfbd] px-3 py-1.5 text-xs font-bold text-[#730014] transition hover:bg-[#fff2f3]"
                    onClick={() => openModal(group, "edit")}
                    type="button"
                  >
                    Sửa
                  </button>
                  <button
                    className="rounded-xl border border-rose-200 px-3 py-1.5 text-xs font-bold text-rose-700 transition hover:bg-rose-50"
                    onClick={() => deletePath(group)}
                    type="button"
                  >
                    Xóa
                  </button>
              </div>
              <button
                className="rounded-xl p-2 text-[#730014]"
                onClick={() =>
                  setExpanded((current) => ({
                    ...current,
                    [group.code]: !isExpanded,
                  }))
                }
                type="button"
              >
                <ChevronDown
                  className={`h-5 w-5 transition ${isExpanded ? "rotate-180" : ""}`}
                />
              </button>
            </div>
            {isExpanded ? (
              <div className="divide-y divide-[#f0e3e4] border-t border-[#f0e3e4]">
                {group.courses.map((course, index) => (
                  <div
                    className="grid gap-4 px-6 py-5 lg:grid-cols-[max-content_1fr]"
                    key={course.courseId}
                  >
                    <span className="whitespace-nowrap text-xs font-bold text-[#730014]">
                      Giai đoạn {course.displayOrder || index + 1}
                    </span>
                    <div>
                      <Link
                        className="text-sm font-bold text-[#0b1c30] hover:text-[#730014] hover:underline"
                        rel="noreferrer"
                        target="_blank"
                        to={`/content-manager/courses/${course.slug}/edit`}
                      >
                        {course.title}
                      </Link>
                      <p className="mt-1 text-xs text-[#584140]">
                        {course.targetOutcome ||
                          course.shortDescription ||
                          "Chưa có mô tả đầu ra."}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            ) : null}
          </Panel>
        );
      })}
      <Pagination
        page={pathPage}
        totalPages={pathTotalPages}
        onChange={setPathPage}
        totalItems={pathTotalItems}
        pageSize={10}
        alwaysVisible
      />
      {modal ? (
        <div aria-modal="true" className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 backdrop-blur-sm bg-black/45 animate-fade-in" role="dialog">
          <button aria-label="Đóng modal" className="absolute inset-0 cursor-default" onClick={() => setModal(null)} type="button" />
          <div className="relative z-10 flex max-h-[calc(100dvh-2.5rem)] w-full max-w-2xl min-h-0 flex-col overflow-hidden rounded-3xl border border-[#dcc0bf]/50 bg-white shadow-2xl pointer-events-auto">
            {/* Header */}
            <div className="flex shrink-0 items-start justify-between gap-4 border-b border-[#f0e3e4] px-6 py-5 bg-white">
              <div>
                <p className="text-[11px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">
                  Quản lý lộ trình
                </p>
                <h2 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">
                  {modal.group
                    ? modal.mode === "add"
                      ? "Thêm khóa học vào lộ trình"
                      : "Cập nhật lộ trình"
                    : "Tạo lộ trình mới"}
                </h2>
              </div>
              <button
                className="rounded-2xl border border-[#dfbfbd]/65 p-2 text-[#730014] transition hover:bg-[#fff2f3]"
                onClick={() => setModal(null)}
                type="button"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {/* Scrollable Body */}
            <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain p-6 space-y-5">
              {modal.mode !== "add" ? (
                <div className="grid gap-4 md:grid-cols-2">
                  <TextField
                    label="Mã lộ trình"
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        code: event.target.value,
                      }))
                    }
                    value={form.code}
                  />
                  <TextField
                    label="Tên lộ trình"
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        name: event.target.value,
                      }))
                    }
                    value={form.name}
                  />
                  <label className="block space-y-2">
                    <span className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Kỳ thi</span>
                    <BrandedSelect
                      onChange={(event) => setForm((current) => ({
                        ...current,
                        examCategory: event.target.value,
                        targetBand: "",
                        targetScore: "",
                      }))}
                      options={[{ value: "IELTS", label: "IELTS" }, { value: "TOEIC", label: "TOEIC" }]}
                      value={form.examCategory}
                    />
                  </label>
                  <label className="block space-y-2">
                    <span className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">{form.examCategory === "TOEIC" ? "Điểm mục tiêu" : "Band mục tiêu"}</span>
                    <input
                      className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-[#730014]"
                      min={form.examCategory === "TOEIC" ? 10 : 0}
                      max={form.examCategory === "TOEIC" ? 990 : 9}
                      step={form.examCategory === "TOEIC" ? 5 : 0.5}
                      onChange={(event) => setForm((current) => ({
                        ...current,
                        [form.examCategory === "TOEIC" ? "targetScore" : "targetBand"]: event.target.value,
                      }))}
                      type="number"
                      value={form.examCategory === "TOEIC" ? form.targetScore : form.targetBand}
                    />
                  </label>
                </div>
              ) : null}

              {modal.mode === "add" ? (
                <div className="rounded-2xl border border-[#ead9db] bg-[#fffdfc] p-4">
                  <p className="text-sm font-bold text-[#0b1c30]">Chọn khóa học để thêm</p>
                  <p className="mt-0.5 text-xs text-slate-500">Khóa học có thể được thêm vào nhiều lộ trình.</p>
                  <div className="relative mt-3">
                    <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                    <input
                      className="w-full rounded-xl border border-slate-200 bg-white pl-10 pr-4 py-2.5 text-sm text-slate-900 outline-none transition focus:border-[#730014]"
                      onChange={(e) => setModalSearch(e.target.value)}
                      placeholder="Tìm kiếm khóa học theo tên..."
                      type="text"
                      value={modalSearch}
                    />
                  </div>
                  <div className="mt-3 max-h-64 space-y-2 overflow-y-auto pr-1">
                    {modalPageItems.map((course) => (
                      <div className="flex items-center gap-3 rounded-xl border border-slate-100 bg-white p-3 transition-all duration-150 hover:border-[#dfbfbd]/50 hover:bg-[#fffdfd]" key={course.id}>
                        <input
                          className="h-4 w-4 rounded border-slate-300 text-[#730014] focus:ring-[#730014] cursor-pointer"
                          checked={courseIds.includes(course.id)}
                          onChange={() => toggleCourse(course.id)}
                          type="checkbox"
                        />
                        <Link
                          className="min-w-0 flex-1 truncate text-sm font-bold text-[#0b1c30] hover:text-[#730014] hover:underline"
                          rel="noreferrer"
                          target="_blank"
                          to={`/content-manager/courses/${course.slug}/edit`}
                        >
                          {course.title}
                        </Link>
                      </div>
                    ))}
                  </div>
                  <Pagination
                    page={modalPage}
                    totalPages={modalTotalPages}
                    onChange={setModalPage}
                    totalItems={modalTotalItems}
                    pageSize={10}
                    className="mt-4"
                    alwaysVisible
                  />
                </div>
              ) : null}

              {modal.mode === "edit" ? (
                <div className="rounded-2xl border border-[#ead9db] bg-[#fffdfc] p-4">
                  <p className="text-sm font-bold text-[#0b1c30]">Thứ tự khóa học</p>
                  <p className="mt-0.5 text-xs text-slate-500">Kéo biểu tượng ở đầu dòng để sắp xếp. Thay đổi chỉ được lưu sau khi bấm Lưu lộ trình.</p>
                  <div className="mt-3 max-h-64 space-y-2 overflow-y-auto pr-1">
                    {courseIds.map((courseId) => {
                      const course = modal.group.courses.find((item) => item.courseId === courseId);
                      if (!course) return null;
                      return (
                        <div
                          className="flex cursor-grab items-center gap-3 rounded-xl border border-slate-100 bg-white p-3 active:cursor-grabbing hover:border-[#dfbfbd]"
                          draggable
                          key={courseId}
                          onDragOver={(event) => {
                            event.preventDefault();
                            event.dataTransfer.dropEffect = "move";
                          }}
                          onDragStart={(event) => {
                            event.dataTransfer.effectAllowed = "move";
                            event.dataTransfer.setData("text/plain", String(courseId));
                          }}
                          onDrop={(event) => handleDraftDrop(event, courseId)}
                        >
                          <GripVertical className="h-4 w-4 shrink-0 text-[#b99593]" />
                          <span className="min-w-0 flex-1 truncate text-sm font-bold text-[#0b1c30]">{course.title}</span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              ) : null}
            </div>

            {/* Footer */}
            <div className="flex shrink-0 items-center justify-end gap-3 border-t border-slate-100 bg-slate-50/50 px-6 py-4">
              <button
                className="rounded-xl border border-[#dfbfbd] px-4 py-2.5 text-sm font-bold text-[#730014] transition hover:bg-[#fff2f3]"
                onClick={() => setModal(null)}
                type="button"
              >
                Hủy
              </button>
              <button
                className="inline-flex items-center gap-2 rounded-xl bg-[#4b0009] px-5 py-2.5 text-sm font-bold text-white transition hover:bg-[#730014] disabled:opacity-60"
                disabled={saving}
                onClick={savePath}
                type="button"
              >
                <Check className="h-4 w-4" />
                {saving ? "Đang lưu..." : modal.mode === "add" ? "Lưu" : modal.mode === "create" ? "Tạo lộ trình" : "Lưu lộ trình"}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function Notice({ children, tone }) {
  return (
    <div
      className={`rounded-2xl border px-5 py-4 text-sm font-semibold ${tone === "error" ? "border-[#ba1a1a]/20 bg-[#ffdad6] text-[#93000a]" : "border-emerald-200 bg-emerald-50 text-emerald-700"}`}
    >
      {children}
    </div>
  );
}
