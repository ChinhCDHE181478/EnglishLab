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
  Panel,
  TextField,
} from "../../components/content-manager/ContentManagerUi";
import Pagination, { usePagination } from "../../components/ui/Pagination";

export default function ContentManagerLearningPathsPage() {
  const [courses, setCourses] = useState([]);
  const [paths, setPaths] = useState([]);
  const [expanded, setExpanded] = useState({});
  const [modal, setModal] = useState(null);
  const [form, setForm] = useState({ code: "", name: "" });
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
        await courseApi.createManagedLearningPath({ code, name });
      } else if (modal.mode === "add") {
        await courseApi.addManagedLearningPathCourses(modal.group.id, courseIds);
      } else {
        await Promise.all([
          courseApi.updateManagedLearningPath(modal.group.id, { code, name }),
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
      !window.confirm(
        `Xóa lộ trình “${group.name}”? Các khóa học sẽ không bị xóa.`,
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
      <div className="flex justify-end gap-2">
        <button
          className="rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-bold text-white transition-all hover:bg-[#730014] active:scale-95 shadow-sm"
          onClick={() => openModal()}
          type="button"
        >
          + Tạo lộ trình
        </button>
        <button
          className="rounded-2xl border border-[#dfbfbd] bg-white px-4 py-3 text-sm font-bold text-[#730014] transition-all hover:bg-[#fff4f5] active:scale-95 shadow-sm"
          onClick={() => loadData()}
          type="button"
        >
          <RefreshCw className="mr-2 inline h-4 w-4" />
          Làm mới
        </button>
      </div>
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
                  <span className="block truncate font-['Manrope'] text-xl font-extrabold text-[#4b0009]">
                    {group.name}
                  </span>
                  <span className="mt-1 block text-sm text-[#584140]">
                    {group.courses.length} khóa học
                  </span>
                </span>
              </button>
              <div className="flex shrink-0 items-center gap-2">
                  <button
                    className="rounded-xl border border-[#dfbfbd] px-3 py-2 text-sm font-semibold text-[#730014]"
                    onClick={() => openModal(group, "add")}
                    type="button"
                  >
                    <Plus className="mr-1 inline h-4 w-4" />
                    Thêm
                  </button>
                  <button
                    className="rounded-xl border border-[#dfbfbd] px-3 py-2 text-sm font-semibold text-[#730014]"
                    onClick={() => openModal(group, "edit")}
                    type="button"
                  >
                    Sửa
                  </button>
                  <button
                    className="rounded-xl border border-rose-200 px-3 py-2 text-sm font-semibold text-rose-700"
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
                    className="grid gap-4 px-6 py-5 lg:grid-cols-[80px_1fr]"
                    key={course.courseId}
                  >
                    <span className="font-bold text-[#730014]">
                      Bước {course.displayOrder || index + 1}
                    </span>
                    <div>
                      <Link
                        className="font-bold text-[#1a1c1c] hover:text-[#730014] hover:underline"
                        rel="noreferrer"
                        target="_blank"
                        to={`/content-manager/courses/${course.slug}/edit`}
                      >
                        {course.title}
                      </Link>
                      <p className="mt-1 text-sm text-[#584140]">
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
        <div className="fixed inset-0 z-[100] overflow-y-auto bg-[#240005]/40 p-4">
          <div className="flex min-h-full items-center justify-center">
            <Panel className="my-5 w-full max-w-2xl p-6 shadow-2xl">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-xs font-bold uppercase tracking-[.18em] text-[#8b706e]">
                    Quản lý lộ trình
                  </p>
                  <h2 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#4b0009]">
                    {modal.group
                      ? modal.mode === "add"
                        ? "Thêm khóa học"
                        : "Cập nhật lộ trình"
                      : "Tạo lộ trình"}
                  </h2>
                </div>
                <button
                  className="rounded-xl p-2 text-[#730014]"
                  onClick={() => setModal(null)}
                  type="button"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>
              {modal.mode !== "add" ? <div className="mt-5 grid gap-4 md:grid-cols-2">
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
              </div> : null}
              {modal.mode === "add" ? (
                <div className="mt-5 rounded-2xl border border-[#ead9db] bg-[#fffdfc] p-4">
                  <p className="font-extrabold text-[#4b0009]">Chọn khóa học để thêm</p>
                  <p className="mt-1 text-xs text-[#6a5352]">Khóa học có thể được thêm vào nhiều lộ trình.</p>
                  <div className="relative mt-3">
                    <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                    <input
                      className="w-full rounded-xl border border-slate-200 bg-slate-50 pl-10 pr-4 py-2.5 text-sm text-slate-900 outline-none transition focus:border-[#730014] focus:bg-white"
                      onChange={(e) => setModalSearch(e.target.value)}
                      placeholder="Tìm kiếm khóa học theo tên..."
                      type="text"
                      value={modalSearch}
                    />
                  </div>
                  <div className="mt-3 max-h-72 space-y-2 overflow-y-auto pr-1">
                    {modalPageItems.map((course) => (
                      <div className="flex items-center gap-3 rounded-xl border border-slate-100 bg-white p-3 transition-all duration-150 hover:border-[#dfbfbd]/50 hover:bg-[#fffdfd]" key={course.id}>
                        <input
                          className="h-4 w-4 rounded border-slate-300 text-[#730014] focus:ring-[#730014] cursor-pointer"
                          checked={courseIds.includes(course.id)}
                          onChange={() => toggleCourse(course.id)}
                          type="checkbox"
                        />
                        <Link
                          className="min-w-0 flex-1 truncate text-sm font-semibold text-slate-800 hover:text-[#730014] hover:underline"
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
                <div className="mt-5 rounded-2xl border border-[#ead9db] bg-[#fffdfc] p-4">
                  <p className="font-extrabold text-[#4b0009]">Thứ tự khóa học</p>
                  <p className="mt-1 text-xs text-[#6a5352]">Kéo biểu tượng ở đầu dòng để sắp xếp. Thay đổi chỉ được lưu sau khi bấm Lưu lộ trình.</p>
                  <div className="mt-3 max-h-72 space-y-2 overflow-y-auto pr-1">
                    {courseIds.map((courseId) => {
                      const course = modal.group.courses.find((item) => item.courseId === courseId);
                      if (!course) return null;
                      return (
                        <div
                          className="flex cursor-grab items-center gap-3 rounded-xl bg-white p-2 active:cursor-grabbing"
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
                          <span className="min-w-0 flex-1 truncate text-sm font-semibold">{course.title}</span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              ) : null}
              <div className="mt-5 flex justify-end gap-3">
                <button
                  className="rounded-2xl border border-[#dfbfbd] px-4 py-3 text-sm font-bold text-[#730014]"
                  onClick={() => setModal(null)}
                  type="button"
                >
                  Hủy
                </button>
                <button
                  className="rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-bold text-white disabled:opacity-60"
                  disabled={saving}
                  onClick={savePath}
                  type="button"
                >
                  <Check className="mr-2 inline h-4 w-4" />
                   {saving ? "Đang lưu..." : modal.mode === "add" ? "Lưu" : modal.mode === "create" ? "Tạo lộ trình" : "Lưu lộ trình"}
                </button>
              </div>
            </Panel>
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
