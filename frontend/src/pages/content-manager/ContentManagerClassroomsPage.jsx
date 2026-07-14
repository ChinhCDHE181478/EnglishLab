import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Bell, BookMarked, CalendarDays, Check, FileStack, GraduationCap, Library, MapPin, Pencil, Plus, RefreshCw, Search, Trash2, Users, X } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import curriculumApi from '../../api/curriculumApi';
import { ContentManagerLoadingState, Panel, SectionTitle, TextField } from '../../components/content-manager/ContentManagerUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { ClassroomEmptyState, ClassroomErrorState } from '../../components/classroom/ClassroomUi';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { formatClassroomDateTime } from '../../utils/classroomHelpers';

const PAGE_SIZE = 8;
const TABS = [
  { id: 'profile', label: 'Hồ sơ lớp', icon: GraduationCap },
  { id: 'materials', label: 'Tài liệu', icon: FileStack },
  { id: 'announcements', label: 'Thông báo', icon: Bell },
  { id: 'syllabus', label: 'Đề cương lớp', icon: BookMarked },
];

const emptyAnnouncementForm = { title: '', content: '' };
const emptySyllabusForm = { title: '', description: '', displayOrder: '0', sessionPlan: '', status: 'DRAFT' };
const emptyMaterialForm = { title: '', description: '', fileUrl: '', fileType: '', materialType: 'PDF', provider: 'EnglishLab' };
const emptyClassroomForm = {
  title: '',
  deliveryMode: 'OFFLINE',
  classroomStatus: 'DRAFT',
  packageStatus: 'DRAFT',
  trainingProgramId: '',
  curriculumProgramId: '',
  entryLevel: '',
  targetScore: '',
  targetOutcome: '',
  maxCapacity: '18',
  startDate: '',
  endDate: '',
  primaryTeacherId: '',
  defaultRoomId: '',
  offlineAddress: '',
  locationNote: '',
  defaultLarkMeetingUrl: '',
  price: '',
  salePrice: '',
  duration: '',
  studyMode: 'Offline tại trung tâm',
  shortDescription: '',
  description: '',
  syllabusSummary: '',
  displayOrder: '0',
  featured: false,
};
const deliveryModeOptions = [
  { label: 'Tại trung tâm', value: 'OFFLINE' },
  { label: 'Virtual', value: 'VIRTUAL' },
];
const classroomStatusOptions = [
  { label: 'Bản nháp', value: 'DRAFT' },
  { label: 'Sắp khai giảng', value: 'UPCOMING' },
  { label: 'Đang hoạt động', value: 'ACTIVE' },
  { label: 'Đã kết thúc', value: 'COMPLETED' },
];
const packageStatusOptions = [
  { label: 'Bản nháp', value: 'DRAFT' },
  { label: 'Đã xuất bản', value: 'PUBLISHED' },
  { label: 'Đã lưu trữ', value: 'ARCHIVED' },
];

export default function ContentManagerClassroomsPage() {
  const [classrooms, setClassrooms] = useState([]);
  const [classroomDetail, setClassroomDetail] = useState(null);
  const [teachers, setTeachers] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [trainingPrograms, setTrainingPrograms] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [activeTab, setActiveTab] = useState('profile');
  const [materials, setMaterials] = useState([]);
  const [materialLibrary, setMaterialLibrary] = useState([]);
  const [announcements, setAnnouncements] = useState([]);
  const [syllabusItems, setSyllabusItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [reloading, setReloading] = useState(false);
  const [page, setPage] = useState(1);
  const [announcementForm, setAnnouncementForm] = useState(emptyAnnouncementForm);
  const [announcementEditorOpen, setAnnouncementEditorOpen] = useState(false);
  const [classroomForm, setClassroomForm] = useState(emptyClassroomForm);
  const [classroomEditorOpen, setClassroomEditorOpen] = useState(false);
  const [classroomEditingId, setClassroomEditingId] = useState(null);
  const [materialForm, setMaterialForm] = useState(emptyMaterialForm);
  const [materialEditorOpen, setMaterialEditorOpen] = useState(false);
  const [materialEditingId, setMaterialEditingId] = useState(null);
  const [materialEditChoice, setMaterialEditChoice] = useState(null);
  const [libraryAttachOpen, setLibraryAttachOpen] = useState(false);
  const [selectedLibraryMaterialId, setSelectedLibraryMaterialId] = useState('');
  const [syllabusForm, setSyllabusForm] = useState(emptySyllabusForm);
  const [syllabusEditingId, setSyllabusEditingId] = useState(null);
  const [syllabusEditorOpen, setSyllabusEditorOpen] = useState(false);
  const [saving, setSaving] = useState(false);

  const selectedClassroom = useMemo(
    () => classrooms.find((item) => String(item.id) === selectedId) || null,
    [classrooms, selectedId],
  );
  const classroomProfile = classroomDetail || selectedClassroom;

  const teacherOptions = useMemo(
    () => [
      { label: 'Chưa chọn giáo viên', value: '' },
      ...teachers.map((item) => {
        const [name, ...rest] = String(item.label || '').split(' - ');
        return {
          label: name || item.label,
          description: rest.join(' - '),
          value: String(item.id),
        };
      }),
    ],
    [teachers],
  );

  const roomOptions = useMemo(
    () => [{ label: 'Chưa chọn phòng', value: '' }, ...rooms.map((item) => ({ label: item.label, value: String(item.id) }))],
    [rooms],
  );

  const trainingProgramOptions = useMemo(
    () => [
      { label: 'Chưa chọn chương trình', value: '' },
      ...trainingPrograms
        .filter((program) => !classroomForm.deliveryMode || program.deliveryMode === classroomForm.deliveryMode)
        .filter((program) => String(program.status || '').toUpperCase() !== 'ARCHIVED')
        .map((program) => ({
          label: program.title,
          value: String(program.id),
          description: [
            program.code,
            program.curriculumProgramTitle ? `Giáo trình lõi: ${program.curriculumProgramTitle}` : null,
            program.curriculumProgramExamCategory,
            program.targetScore ? `Target ${program.targetScore}` : null,
            program.entryLevel,
          ].filter(Boolean).join(' · '),
        })),
    ],
    [classroomForm.deliveryMode, trainingPrograms],
  );

  const loadClassrooms = async () => {
    setLoading(true);
    setError('');
    try {
      const [data, teacherData, roomData, curriculumData] = await Promise.all([
        classroomApi.getContentManagerClassrooms(),
        classroomApi.getContentManagerClassroomTeachers(),
        classroomApi.getContentManagerClassroomRooms(),
        classroomApi.getContentManagerPrograms(),
      ]);
      setClassrooms(data);
      setTeachers(teacherData);
      setRooms(roomData);
      setTrainingPrograms(curriculumData);
      if (!data.some((item) => String(item.id) === selectedId)) {
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
    if (tab === 'profile') return;
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

  const loadClassroomDetail = async (classroomId) => {
    if (!classroomId) {
      setClassroomDetail(null);
      return;
    }
    try {
      setClassroomDetail(await classroomApi.getContentManagerClassroom(classroomId));
    } catch {
      setClassroomDetail(null);
    }
  };

  const loadClassroomSnapshot = async (classroomId) => {
    if (!classroomId) return;
    setReloading(true);
    try {
      const [materialsData, announcementsData, syllabusData, libraryData] = await Promise.allSettled([
        classroomApi.getContentManagerMaterials(classroomId),
        classroomApi.getContentManagerAnnouncements(classroomId),
        classroomApi.getContentManagerSyllabus(classroomId),
        classroomApi.getContentManagerMaterialLibrary(),
      ]);
      setMaterials(materialsData.status === 'fulfilled' ? materialsData.value : []);
      setAnnouncements(announcementsData.status === 'fulfilled' ? announcementsData.value : []);
      setSyllabusItems(syllabusData.status === 'fulfilled' ? syllabusData.value : []);
      setMaterialLibrary(libraryData.status === 'fulfilled' ? libraryData.value : []);
    } catch {
      // Keep the profile usable even if one content summary endpoint fails.
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
      loadClassroomDetail(selectedId);
      if (activeTab === 'profile') {
        loadClassroomSnapshot(selectedId);
      } else {
        loadTabData(selectedId, activeTab);
      }
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

  useEffect(() => {
    if (page > totalPages) setPage(totalPages);
  }, [page, totalPages]);

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

  const updateClassroomForm = (field, value) => {
    setClassroomForm((current) => {
      const next = { ...current, [field]: value };
      if (field === 'deliveryMode') {
        next.studyMode = value === 'VIRTUAL' ? 'Virtual' : 'Offline tại trung tâm';
        next.trainingProgramId = '';
        next.curriculumProgramId = '';
        if (value === 'VIRTUAL') {
          next.defaultRoomId = '';
          next.offlineAddress = '';
        }
      }
      return next;
    });
  };

  const buildClassroomForm = (item = {}) => ({
    title: item.title || '',
    deliveryMode: item.deliveryMode || 'OFFLINE',
    classroomStatus: item.classroomStatus || 'DRAFT',
    packageStatus: item.packageStatus || 'DRAFT',
    trainingProgramId: item.trainingProgramId ? String(item.trainingProgramId) : '',
    curriculumProgramId: item.curriculumProgramId ? String(item.curriculumProgramId) : '',
    entryLevel: item.entryLevel || '',
    targetScore: item.targetScore || '',
    targetOutcome: item.targetOutcome || '',
    maxCapacity: String(item.maxCapacity ?? 18),
    startDate: item.startDate || '',
    endDate: item.endDate || '',
    primaryTeacherId: item.primaryTeacherId ? String(item.primaryTeacherId) : '',
    defaultRoomId: item.roomId ? String(item.roomId) : '',
    offlineAddress: item.offlineAddress || '',
    locationNote: item.locationNote || '',
    defaultLarkMeetingUrl: item.defaultLarkMeetingUrl || '',
    price: item.price === null || item.price === undefined ? '' : String(item.price),
    salePrice: item.salePrice === null || item.salePrice === undefined ? '' : String(item.salePrice),
    duration: item.duration || '',
    studyMode: item.studyMode || (item.deliveryMode === 'VIRTUAL' ? 'Virtual' : 'Offline tại trung tâm'),
    shortDescription: item.shortDescription || '',
    description: item.description || '',
    syllabusSummary: item.syllabusSummary || '',
    displayOrder: String(item.displayOrder ?? 0),
    featured: Boolean(item.featured),
  });

  const openClassroomCreate = () => {
    setClassroomEditingId(null);
    setClassroomForm(emptyClassroomForm);
    setClassroomEditorOpen(true);
    setMessage('');
  };

  const openClassroomEdit = async (item) => {
    setSaving(true);
    setMessage('');
    try {
      const detail = await classroomApi.getContentManagerClassroom(item.id);
      setClassroomDetail(detail);
      setClassroomEditingId(item.id);
      setClassroomForm(buildClassroomForm(detail));
      setClassroomEditorOpen(true);
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, 'Không thể tải thông tin lớp để chỉnh sửa.'));
    } finally {
      setSaving(false);
    }
  };

  const closeClassroomEditor = () => {
    setClassroomEditingId(null);
    setClassroomForm(emptyClassroomForm);
    setClassroomEditorOpen(false);
  };

  const buildClassroomPayload = () => ({
    title: classroomForm.title.trim(),
    shortDescription: classroomForm.shortDescription.trim() || null,
    description: classroomForm.description.trim() || null,
    deliveryMode: classroomForm.deliveryMode,
    classroomStatus: classroomForm.classroomStatus,
    packageStatus: classroomForm.packageStatus,
    trainingProgramId: classroomForm.trainingProgramId ? Number(classroomForm.trainingProgramId) : null,
    curriculumProgramId: null,
    entryLevel: classroomForm.entryLevel.trim() || null,
    targetScore: classroomForm.targetScore.trim() || null,
    targetOutcome: classroomForm.targetOutcome.trim() || null,
    maxCapacity: Number(classroomForm.maxCapacity || 0),
    startDate: classroomForm.startDate || null,
    endDate: classroomForm.endDate || null,
    primaryTeacherId: classroomForm.primaryTeacherId ? Number(classroomForm.primaryTeacherId) : null,
    defaultRoomId: classroomForm.deliveryMode === 'OFFLINE' && classroomForm.defaultRoomId ? Number(classroomForm.defaultRoomId) : null,
    offlineAddress: classroomForm.deliveryMode === 'OFFLINE' ? classroomForm.offlineAddress.trim() || null : null,
    locationNote: classroomForm.locationNote.trim() || null,
    defaultLarkMeetingUrl: classroomForm.deliveryMode === 'VIRTUAL' ? classroomForm.defaultLarkMeetingUrl.trim() || null : null,
    price: classroomForm.price ? Number(classroomForm.price) : 0,
    salePrice: classroomForm.salePrice ? Number(classroomForm.salePrice) : null,
    duration: classroomForm.duration.trim() || null,
    studyMode: classroomForm.studyMode.trim() || null,
    syllabusSummary: classroomForm.syllabusSummary.trim() || null,
    displayOrder: Number(classroomForm.displayOrder || 0),
    featured: classroomForm.featured,
  });

  const saveClassroom = async (event) => {
    event.preventDefault();
    if (!classroomForm.title.trim()) {
      setMessage('Hãy nhập tên lớp học.');
      return;
    }
    if (!classroomForm.deliveryMode) {
      setMessage('Hãy chọn hình thức lớp học.');
      return;
    }
    if (!classroomForm.trainingProgramId) {
      setMessage('Hãy chọn chương trình học cho lớp.');
      return;
    }
    if (Number(classroomForm.maxCapacity || 0) < 1) {
      setMessage('Sĩ số tối đa phải lớn hơn 0.');
      return;
    }
    setSaving(true);
    setMessage('');
    try {
      const saved = classroomEditingId
        ? await classroomApi.updateContentManagerClassroom(classroomEditingId, buildClassroomPayload())
        : await classroomApi.createContentManagerClassroom(buildClassroomPayload());
      await loadClassrooms();
      setSelectedId(String(saved.id));
      setClassroomDetail(saved);
      setActiveTab('profile');
      closeClassroomEditor();
      setMessage(classroomEditingId ? 'Đã cập nhật lớp học.' : 'Đã tạo lớp khai giảng mới.');
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, 'Không thể lưu lớp học.'));
    } finally {
      setSaving(false);
    }
  };

  const openClassroomWorkspace = (classroomId) => {
    setSelectedId(String(classroomId));
    setActiveTab('profile');
    setMessage('');
  };

  const closeClassroomWorkspace = () => {
    setSelectedId('');
    setClassroomDetail(null);
    setActiveTab('profile');
    setMaterials([]);
    setMaterialLibrary([]);
    setAnnouncements([]);
    setSyllabusItems([]);
    setMessage('');
    setMaterialEditorOpen(false);
    setMaterialEditingId(null);
    setMaterialEditChoice(null);
    setLibraryAttachOpen(false);
    setSelectedLibraryMaterialId('');
  };

  const buildMaterialForm = (item, clone = false) => ({
    title: clone ? `${item.title || ''} (Bản sao)` : item.title || '',
    description: item.description || '',
    fileUrl: item.fileUrl || '',
    fileType: item.fileType || '',
    materialType: item.materialType || item.fileType || 'PDF',
    provider: item.provider || 'EnglishLab',
    sourceType: clone ? 'CLASS_CUSTOM' : item.sourceType || 'CLASS_CUSTOM',
    visibility: item.visibility || 'CLASS',
    centerMaterialId: clone ? null : item.centerMaterialId || null,
    sessionId: item.sessionId || null,
  });

  const openMaterialCreate = () => {
    setMaterialForm(emptyMaterialForm);
    setMaterialEditingId(null);
    setMaterialEditChoice(null);
    setMaterialEditorOpen(true);
  };

  const openMaterialEditChoice = (item) => {
    setMaterialEditChoice(item);
    setMaterialEditorOpen(false);
  };

  const openMaterialClone = (item) => {
    setMaterialForm(buildMaterialForm(item, true));
    setMaterialEditingId(null);
    setMaterialEditChoice(null);
    setMaterialEditorOpen(true);
  };

  const openMaterialDirectEdit = (item) => {
    setMaterialForm(buildMaterialForm(item));
    setMaterialEditingId(item.id);
    setMaterialEditChoice(null);
    setMaterialEditorOpen(true);
  };

  const closeMaterialEditor = () => {
    setMaterialForm(emptyMaterialForm);
    setMaterialEditingId(null);
    setMaterialEditorOpen(false);
  };

  const saveMaterial = async () => {
    if (!selectedId || !materialForm.title.trim()) {
      setMessage('Hãy nhập tên tài liệu.');
      return;
    }
    if (!materialForm.fileUrl.trim()) {
      setMessage('Hãy nhập URL hoặc đường dẫn file tài liệu.');
      return;
    }
    setSaving(true);
    setMessage('');
    const payload = {
      ...materialForm,
      title: materialForm.title.trim(),
      description: materialForm.description.trim() || null,
      fileUrl: materialForm.fileUrl.trim() || null,
      fileType: materialForm.fileType.trim() || null,
      materialType: materialForm.materialType || null,
      provider: materialForm.provider.trim() || 'EnglishLab',
      sourceType: materialForm.sourceType || 'CLASS_CUSTOM',
      visibility: materialForm.visibility || 'CLASS',
      centerMaterialId: materialForm.centerMaterialId || null,
      sessionId: materialForm.sessionId || null,
    };
    try {
      if (materialEditingId) {
        await classroomApi.updateContentManagerMaterial(materialEditingId, payload);
      } else {
        await classroomApi.createContentManagerMaterial(selectedId, payload);
      }
      setMaterialForm(emptyMaterialForm);
      setMaterialEditorOpen(false);
      setMaterialEditingId(null);
      setMessage(materialEditingId ? 'Đã cập nhật tài liệu.' : 'Đã thêm tài liệu cho lớp.');
      await loadTabData(selectedId, 'materials');
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, 'Không thể lưu tài liệu cho lớp.'));
    } finally {
      setSaving(false);
    }
  };

  const deleteMaterial = async (materialId) => {
    if (!window.confirm('Xóa tài liệu này khỏi lớp?')) return;
    setSaving(true);
    setMessage('');
    try {
      await classroomApi.deleteContentManagerMaterial(materialId);
      setMessage('Đã xóa tài liệu khỏi lớp.');
      await loadTabData(selectedId, 'materials');
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, 'Không thể xóa tài liệu.'));
    } finally {
      setSaving(false);
    }
  };

  const attachLibraryMaterial = async () => {
    const item = materialLibrary.find((material) => String(material.id) === selectedLibraryMaterialId);
    if (!selectedId || !item) {
      setMessage('Hãy chọn học liệu từ kho.');
      return;
    }
    if (materials.some((material) => material.sourceType === 'CENTER_LIBRARY' && String(material.centerMaterialId) === String(item.id) && !material.sessionId)) {
      setMessage('Học liệu này đã được gắn vào lớp.');
      return;
    }
    setSaving(true);
    setMessage('');
    try {
      await classroomApi.createContentManagerMaterial(selectedId, {
        title: item.title,
        description: item.description || null,
        fileUrl: item.fileUrl || null,
        fileType: item.fileType || null,
        materialType: item.materialType || null,
        provider: item.provider || 'EnglishLab',
        sourceType: 'CENTER_LIBRARY',
        visibility: 'CLASS',
        centerMaterialId: item.id,
      });
      setSelectedLibraryMaterialId('');
      setLibraryAttachOpen(false);
      setMessage('Đã gắn học liệu từ kho vào lớp.');
      await loadTabData(selectedId, 'materials');
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, 'Không thể gắn học liệu từ kho.'));
    } finally {
      setSaving(false);
    }
  };

  const applyProgramProfile = async (programId) => {
    if (!selectedId || !programId) {
      setMessage('Lớp chưa gắn chương trình học.');
      return;
    }
    setSaving(true);
    setMessage('');
    try {
      const trainingProgramDetail = await classroomApi.getContentManagerProgram(programId);
      const curriculumId = trainingProgramDetail?.curriculumProgramId;
      const curriculumDetail = curriculumId ? await curriculumApi.getCurriculumProgram(curriculumId) : null;
      const programUnits = [...(curriculumDetail?.units || [])].sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0) || a.id - b.id);
      const updated = await classroomApi.updateContentManagerProgramProfile(selectedId, {
        entryLevel: trainingProgramDetail?.entryLevel || '',
        targetOutcome: trainingProgramDetail?.targetOutcome || curriculumDetail?.outcomes || '',
        programOutcomes: trainingProgramDetail?.programOutcomes || curriculumDetail?.outcomes || '',
        teacherGuide: trainingProgramDetail?.teacherGuide || curriculumDetail?.teacherGuide || '',
        interactionActivities: trainingProgramDetail?.interactionActivities || curriculumDetail?.interactionActivities || '',
        syllabusSummary: [
          trainingProgramDetail?.title,
          curriculumDetail?.title ? `Giáo trình lõi: ${curriculumDetail.title}` : null,
          `${programUnits.length} buổi học`,
        ].filter(Boolean).join(' · '),
        deliveryMode: trainingProgramDetail?.deliveryMode || classroomProfile?.deliveryMode || null,
      });
      const existingTitles = new Set(syllabusItems.map((item) => String(item.title || '').trim().toLowerCase()).filter(Boolean));
      const unitsToCreate = programUnits.filter((unit) => !existingTitles.has(String(unit.title || '').trim().toLowerCase()));

      await Promise.all(unitsToCreate.map((unit, index) => classroomApi.createContentManagerSyllabusItem(selectedId, {
        title: unit.title,
        description: unit.description || null,
        displayOrder: unit.displayOrder ?? index,
        sessionPlan: unit.sessionPlan || null,
        sessionNumber: unit.displayOrder ?? index + 1,
        status: unit.status || 'DRAFT',
      })));

      setClassroomDetail(updated);
      await loadTabData(selectedId, 'syllabus');
      setMessage(unitsToCreate.length
        ? `Đã đồng bộ chương trình và thêm ${unitsToCreate.length} mục đề cương cho lớp.`
        : 'Đã cập nhật hồ sơ chương trình cho lớp. Không có mục mới cần thêm.');
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, 'Không thể cập nhật chương trình từ kho.'));
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
        description="Khi lớp học tại trung tâm được mở, bạn sẽ quản lý chương trình, tài liệu, thông báo và đề cương lớp tại đây."
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
          /Không thể|Hãy nhập|phải/.test(message) ? 'border-rose-100 bg-rose-50 text-rose-800' : 'border-emerald-100 bg-emerald-50 text-emerald-800'
        }`}>
          {message}
        </div>
      ) : null}

      {classroomEditorOpen ? (
        <EditorModal onClose={closeClassroomEditor}>
            <ClassroomEditorPanel
              trainingProgramOptions={trainingProgramOptions}
            form={classroomForm}
            isEditing={Boolean(classroomEditingId)}
            onChange={updateClassroomForm}
            onClose={closeClassroomEditor}
            onSubmit={saveClassroom}
            roomOptions={roomOptions}
            saving={saving}
            teacherOptions={teacherOptions}
          />
        </EditorModal>
      ) : null}

      {!selectedId ? (
        <ClassroomListPanel
          classrooms={classrooms}
          loading={reloading}
          onCreate={openClassroomCreate}
          onEdit={openClassroomEdit}
          onOpen={openClassroomWorkspace}
          onRefresh={loadClassrooms}
        />
      ) : (
        <Panel className="rounded-xl border-[#dcc0bf]/30 p-4 shadow-sm">
          <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_auto] xl:items-start">
            <div>
              <button
                className="inline-flex items-center gap-2 rounded-lg border border-[#dcc0bf]/40 bg-white px-3 py-2 text-sm font-bold text-[#4b0009] transition hover:bg-[#fff7f7]"
                onClick={closeClassroomWorkspace}
                type="button"
              >
                <span aria-hidden="true">&lt;</span>
                Quay lại danh sách lớp
              </button>
              <div className="mt-4">
                <p className="text-xs font-bold uppercase tracking-[0.16em] text-[#8b706e]">Không gian lớp học</p>
                <h2 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#0b1c30]">{selectedClassroom?.title}</h2>
              </div>
            </div>

            <div className="flex flex-wrap gap-3 xl:justify-end">
              <button
                className="inline-flex items-center gap-2 rounded-lg border border-[#dcc0bf]/40 bg-white px-4 py-2.5 text-sm font-bold text-[#4b0009] transition hover:bg-[#fff7f7]"
                onClick={() => (activeTab === 'profile' ? loadClassroomSnapshot(selectedId) : loadTabData(selectedId, activeTab))}
                type="button"
              >
                <RefreshCw className={`h-4 w-4 ${reloading ? 'animate-spin' : ''}`} />
                Làm mới
              </button>
              <button
                className="inline-flex items-center gap-2 rounded-lg border border-[#dcc0bf]/40 bg-white px-4 py-2.5 text-sm font-bold text-[#4b0009] transition hover:bg-[#fff7f7]"
                onClick={() => openClassroomEdit(classroomProfile || selectedClassroom)}
                type="button"
              >
                <Pencil className="h-4 w-4" />
                Sửa lớp
              </button>
              <Link
                className="inline-flex items-center gap-2 rounded-lg bg-[#4b0009] px-4 py-2.5 text-sm font-bold text-white transition hover:bg-[#730014]"
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
                  className={`inline-flex items-center gap-2 rounded-lg px-4 py-2.5 text-sm font-bold transition ${
                    active ? 'bg-[#4b0009] text-white' : 'border border-[#dcc0bf]/40 bg-white text-[#4b0009] hover:bg-[#fff7f7]'
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
      )}

      {classroomProfile && activeTab === 'profile' ? (
        <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
          <Panel className="rounded-xl border-[#dcc0bf]/30 p-6 shadow-sm">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Lớp học tại trung tâm</p>
                <h2 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#0b1c30]">{classroomProfile.title}</h2>
                {classroomProfile.shortDescription || classroomProfile.description ? (
                  <p className="mt-2 max-w-3xl text-sm leading-6 text-[#584140]">{classroomProfile.shortDescription || classroomProfile.description}</p>
                ) : null}
              </div>
              <StatusBadge value={classroomProfile.classroomStatus || classroomProfile.packageStatus} />
            </div>

            <div className="mt-6 grid gap-3 md:grid-cols-2 xl:grid-cols-4">
              <InfoPill label="Hình thức" value={classroomProfile.deliveryModeLabel || classroomProfile.deliveryMode || 'Offline'} />
              <InfoPill label="Sĩ số" value={`${classroomProfile.enrolledCount ?? 0}/${classroomProfile.maxCapacity ?? '-'}`} />
              <InfoPill label="Khai giảng" value={formatDate(classroomProfile.startDate)} />
              <InfoPill label="Học phí" value={formatMoney(classroomProfile.price)} />
            </div>

            <div className="mt-5 grid gap-3 md:grid-cols-2">
              <DetailBlock
                icon={BookMarked}
                label="Chương trình đang gắn"
                value={classroomProfile.trainingProgramTitle || classroomProfile.curriculumProgramTitle || 'Chưa gắn chương trình'}
                note={[classroomProfile.curriculumProgramCode, classroomProfile.curriculumProgramExamCategory, classroomProfile.curriculumProgramStatus].filter(Boolean).join(' · ')}
              />
              <DetailBlock
                icon={CalendarDays}
                label="Lịch học"
                value={classroomProfile.scheduleSummary || buildScheduleSummary(classroomProfile)}
                note={formatDateRange(classroomProfile.startDate, classroomProfile.endDate)}
              />
              <DetailBlock
                icon={Users}
                label="Giảng viên phụ trách"
                value={classroomProfile.primaryTeacherName || classroomProfile.teachers?.map((teacher) => teacher.fullName || teacher.name).filter(Boolean).join(', ') || 'Chưa phân công'}
                note={classroomProfile.teachers?.length ? `${classroomProfile.teachers.length} giảng viên trong lớp` : ''}
              />
              <DetailBlock
                icon={MapPin}
                label="Phòng học / địa điểm"
                value={classroomProfile.roomName || classroomProfile.offlineAddress || 'Chưa có phòng học'}
                note={classroomProfile.locationNote || classroomProfile.offlineAddress || ''}
              />
            </div>
          </Panel>

          <aside className="space-y-6">
            <Panel className="rounded-xl border-[#dcc0bf]/30 p-6 shadow-sm">
              <h3 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">Tình trạng nội dung</h3>
              <div className="mt-4 space-y-3">
                <ContentCheck label="Tài liệu đã gắn" value={materials.length} />
                <ContentCheck label="Thông báo lớp" value={announcements.length} />
                <ContentCheck label="Mục đề cương lớp" value={syllabusItems.length} />
              </div>
            </Panel>
            <Panel className="rounded-xl border-[#dcc0bf]/30 p-6 shadow-sm">
              <h3 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">Thao tác nhanh</h3>
              <div className="mt-4 grid gap-2">
                <button className="rounded-lg border border-[#dcc0bf]/40 px-4 py-2.5 text-left text-sm font-bold text-[#4b0009] hover:bg-[#fbf3f4]" onClick={() => setActiveTab('materials')} type="button">Kiểm tra tài liệu</button>
                <button className="rounded-lg border border-[#dcc0bf]/40 px-4 py-2.5 text-left text-sm font-bold text-[#4b0009] hover:bg-[#fbf3f4]" onClick={() => setActiveTab('announcements')} type="button">Đăng thông báo</button>
                <button className="rounded-lg border border-[#dcc0bf]/40 px-4 py-2.5 text-left text-sm font-bold text-[#4b0009] hover:bg-[#fbf3f4]" onClick={() => setActiveTab('syllabus')} type="button">Cập nhật đề cương lớp</button>
              </div>
            </Panel>
          </aside>
        </div>
      ) : null}

      {selectedClassroom && activeTab === 'materials' ? (
        <div className="space-y-6">
          <div className="flex flex-wrap justify-end gap-3">
            <button className="inline-flex items-center gap-2 rounded-lg border border-[#dcc0bf]/40 bg-white px-4 py-2.5 text-sm font-bold text-[#4b0009] hover:bg-[#fff7f7]" onClick={() => setLibraryAttachOpen((current) => !current)} type="button">
              <Library className="h-4 w-4" />
              Chọn từ kho
            </button>
            <button className="inline-flex items-center gap-2 rounded-lg bg-[#4b0009] px-4 py-2.5 text-sm font-bold text-white hover:bg-[#730014]" onClick={openMaterialCreate} type="button">
              <Plus className="h-4 w-4" />
              Thêm tài liệu
            </button>
          </div>

          {libraryAttachOpen ? (
            <Panel className="rounded-xl border-[#dcc0bf]/30 p-5 shadow-sm">
              <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_auto] lg:items-end">
                <div>
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Học liệu từ kho trung tâm</span>
                  <BrandedSelect
                    value={selectedLibraryMaterialId}
                    onChange={(event) => setSelectedLibraryMaterialId(event.target.value)}
                    options={materialLibrary.map((item) => ({
                      label: item.title,
                      value: String(item.id),
                      description: [item.materialType, item.skill, item.provider].filter(Boolean).join(' · '),
                    }))}
                    placeholder={materialLibrary.length ? 'Chọn học liệu' : 'Kho học liệu đang trống'}
                    disabled={!materialLibrary.length}
                  />
                </div>
                <button className="inline-flex items-center justify-center gap-2 rounded-lg bg-[#4b0009] px-4 py-2.5 text-sm font-bold text-white disabled:opacity-60" disabled={saving || !selectedLibraryMaterialId} onClick={attachLibraryMaterial} type="button">
                  <Check className="h-4 w-4" />
                  Gắn vào lớp
                </button>
              </div>
            </Panel>
          ) : null}

          {materialEditChoice ? (
            <Panel className="rounded-xl border-[#dcc0bf]/30 p-5 shadow-sm">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Chọn cách sửa tài liệu</p>
                  <h3 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#0b1c30]">{materialEditChoice.title}</h3>
                  <p className="mt-2 max-w-2xl text-sm leading-6 text-[#584140]">
                    Nhân bản sẽ tạo một tài liệu riêng của lớp để chỉnh sửa. Sửa trực tiếp sẽ cập nhật ngay dòng tài liệu đang gắn.
                  </p>
                </div>
                <button className="rounded-lg p-2 text-[#4b0009] hover:bg-[#fff7f7]" onClick={() => setMaterialEditChoice(null)} type="button">
                  <X className="h-5 w-5" />
                </button>
              </div>
              <div className="mt-5 grid gap-3 md:grid-cols-2">
                <button
                  className="rounded-xl border border-[#dcc0bf]/40 bg-white p-4 text-left transition hover:bg-[#fff7f7]"
                  onClick={() => openMaterialClone(materialEditChoice)}
                  type="button"
                >
                  <span className="text-sm font-extrabold text-[#4b0009]">Nhân bản rồi sửa</span>
                  <span className="mt-1 block text-sm leading-6 text-[#584140]">Giữ bản đang gắn, tạo bản sao riêng để thay nội dung.</span>
                </button>
                <button
                  className="rounded-xl border border-[#4b0009] bg-[#4b0009] p-4 text-left text-white transition hover:bg-[#730014]"
                  onClick={() => openMaterialDirectEdit(materialEditChoice)}
                  type="button"
                >
                  <span className="text-sm font-extrabold">Sửa trực tiếp</span>
                  <span className="mt-1 block text-sm leading-6 text-white/80">Cập nhật ngay tài liệu này trong danh sách của lớp.</span>
                </button>
              </div>
            </Panel>
          ) : null}

          {materialEditorOpen ? (
            <Panel className="rounded-xl border-[#dcc0bf]/30 p-5 shadow-sm">
              <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">
                    {materialEditingId ? 'Sửa trực tiếp' : 'Tài liệu lớp học'}
                  </p>
                  <h3 className="font-['Manrope'] text-xl font-extrabold text-[#0b1c30]">
                    {materialEditingId ? 'Cập nhật tài liệu' : 'Thêm tài liệu'}
                  </h3>
                </div>
                <button className="rounded-lg p-2 text-[#4b0009] hover:bg-[#fff7f7]" onClick={closeMaterialEditor} type="button">
                  <X className="h-5 w-5" />
                </button>
              </div>
              <div className="grid gap-4 md:grid-cols-2">
                <TextField label="Tên tài liệu" onChange={(e) => setMaterialForm((current) => ({ ...current, title: e.target.value }))} value={materialForm.title} />
                <TextField label="URL / đường dẫn file" onChange={(e) => setMaterialForm((current) => ({ ...current, fileUrl: e.target.value }))} value={materialForm.fileUrl} />
                <TextField label="Loại tài liệu" onChange={(e) => setMaterialForm((current) => ({ ...current, materialType: e.target.value }))} value={materialForm.materialType} />
                <TextField label="Nguồn" onChange={(e) => setMaterialForm((current) => ({ ...current, provider: e.target.value }))} value={materialForm.provider} />
                <div className="md:col-span-2">
                  <TextField label="Mô tả" onChange={(e) => setMaterialForm((current) => ({ ...current, description: e.target.value }))} rows={3} textarea value={materialForm.description} />
                </div>
              </div>
              <div className="mt-5 flex justify-end gap-3">
                <button className="rounded-lg border border-[#dcc0bf]/40 px-4 py-2.5 text-sm font-bold text-[#4b0009]" onClick={closeMaterialEditor} type="button">Hủy</button>
                <button className="inline-flex items-center gap-2 rounded-lg bg-[#4b0009] px-4 py-2.5 text-sm font-bold text-white disabled:opacity-60" disabled={saving} onClick={saveMaterial} type="button">
                  <Check className="h-4 w-4" />
                  {materialEditingId ? 'Cập nhật' : 'Lưu tài liệu'}
                </button>
              </div>
            </Panel>
          ) : null}

          <Panel className="rounded-xl border-[#dcc0bf]/30 p-4 shadow-sm">
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

          <Panel className="overflow-hidden rounded-xl border-[#dcc0bf]/30 shadow-sm">
            <div className="border-b border-[#dcc0bf]/30 bg-[#fbf3f4] px-6 py-4">
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
              <>
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[1040px] border-collapse text-left">
                    <thead>
                      <tr className="border-b border-[#dcc0bf]/30 bg-[#fbf3f4]">
                        {['Tài liệu', 'Nguồn', 'Loại', 'Buổi học', 'Cập nhật', 'Thao tác'].map((heading) => (
                          <th
                            className={`px-6 py-4 text-xs font-bold uppercase tracking-[0.12em] text-[#8e7371] ${heading === 'Thao tác' ? 'text-right' : ''}`}
                            key={heading}
                          >
                            {heading}
                          </th>
                        ))}
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[#dcc0bf]/15">
                      {visibleItems.map((item) => (
                        <tr className="align-top transition hover:bg-[#fff7f7]" key={item.id}>
                          <td className="px-6 py-5">
                            <div className="max-w-[360px]">
                              <p className="font-['Manrope'] text-base font-extrabold leading-6 text-[#4b0009]">{item.title}</p>
                              <p className="mt-1 line-clamp-2 text-sm leading-6 text-[#584140]">
                                {item.description || 'Tài liệu đã sẵn sàng cho lớp.'}
                              </p>
                            </div>
                          </td>
                          <td className="px-6 py-5">
                            <SourceBadge value={item.sourceType} />
                            <p className="mt-2 text-sm font-semibold text-[#0b1c30]">{item.provider || 'EnglishLab'}</p>
                          </td>
                          <td className="px-6 py-5 text-sm font-semibold text-[#0b1c30]">{item.materialType || item.fileType || 'Tài liệu'}</td>
                          <td className="px-6 py-5 text-sm text-[#584140]">{item.sessionTitle || 'Không gắn buổi cụ thể'}</td>
                          <td className="px-6 py-5 text-sm text-[#584140]">{formatClassroomDateTime(item.updatedAt || item.createdAt)}</td>
                          <td className="px-6 py-5">
                            <div className="flex flex-wrap justify-end gap-2">
                              {item.fileUrl ? (
                                <a className="inline-flex items-center gap-1.5 rounded-lg border border-[#dcc0bf]/40 bg-white px-3 py-2 text-xs font-bold text-[#4b0009] transition hover:bg-[#fff7f7]" href={item.fileUrl} rel="noreferrer" target="_blank">
                                  <FileStack className="h-4 w-4" />
                                  Mở
                                </a>
                              ) : null}
                              <button
                                className="inline-flex items-center gap-1.5 rounded-lg border border-[#dcc0bf]/40 bg-white px-3 py-2 text-xs font-bold text-[#4b0009] transition hover:bg-[#fff7f7]"
                                onClick={() => openMaterialEditChoice(item)}
                                type="button"
                              >
                                <Pencil className="h-4 w-4" />
                                Sửa
                              </button>
                              <button
                                className="inline-flex items-center gap-1.5 rounded-lg border border-rose-200 bg-white px-3 py-2 text-xs font-bold text-rose-700 transition hover:bg-rose-50 disabled:opacity-50"
                                disabled={saving}
                                onClick={() => deleteMaterial(item.id)}
                                type="button"
                              >
                                <Trash2 className="h-4 w-4" />
                                Xóa
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <div className="flex flex-wrap items-center justify-between gap-3 border-t border-[#dcc0bf]/20 bg-[#fbf3f4]/40 px-6 py-4">
                  <p className="text-sm text-[#2b2828]">
                    Trang {page} / {totalPages} · <span className="font-bold text-[#0b1c30]">{materials.length}</span> tài liệu
                  </p>
                  <div className="flex items-center gap-2">
                    <button
                      aria-label="Trang trước"
                      className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-[#dcc0bf]/35 bg-white text-[#8b706e] transition hover:bg-[#fff7f7] disabled:cursor-not-allowed disabled:opacity-40"
                      disabled={page <= 1}
                      onClick={() => setPage((current) => Math.max(1, current - 1))}
                      type="button"
                    >
                      &lt;
                    </button>
                    <button
                      aria-label="Trang sau"
                      className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-[#dcc0bf]/35 bg-white text-[#8b706e] transition hover:bg-[#fff7f7] disabled:cursor-not-allowed disabled:opacity-40"
                      disabled={page >= totalPages}
                      onClick={() => setPage((current) => Math.min(totalPages, current + 1))}
                      type="button"
                    >
                      &gt;
                    </button>
                  </div>
                </div>
              </>
            )}
          </Panel>
        </div>
      ) : null}

      {selectedClassroom && activeTab === 'announcements' ? (
        <div className="space-y-6">
          <div className="flex flex-wrap justify-end gap-3">
            <button
              className="inline-flex items-center gap-2 rounded-lg bg-[#4b0009] px-4 py-2.5 text-sm font-bold text-white"
              onClick={() => { setAnnouncementEditorOpen(true); setAnnouncementForm(emptyAnnouncementForm); }}
              type="button"
            >
              <Plus className="h-4 w-4" />
              Đăng thông báo
            </button>
          </div>

          {announcementEditorOpen ? (
            <Panel className="rounded-xl border-[#dcc0bf]/30 p-5 shadow-sm">
              <div className="mb-4 flex items-center justify-between gap-3">
                <h2 className="font-['Manrope'] text-xl font-extrabold text-[#4b0009]">Thông báo mới</h2>
                <button className="rounded-lg p-2 text-[#4b0009] hover:bg-[#eff4ff]" onClick={() => setAnnouncementEditorOpen(false)} type="button">
                  <X className="h-5 w-5" />
                </button>
              </div>
              <div className="space-y-4">
                <TextField label="Tiêu đề" onChange={(e) => setAnnouncementForm((c) => ({ ...c, title: e.target.value }))} value={announcementForm.title} />
                <TextField label="Nội dung" onChange={(e) => setAnnouncementForm((c) => ({ ...c, content: e.target.value }))} rows={5} textarea value={announcementForm.content} />
                <div className="flex justify-end gap-3">
                  <button className="rounded-lg border border-[#dcc0bf]/40 px-4 py-2.5 text-sm font-bold text-[#4b0009]" onClick={() => setAnnouncementEditorOpen(false)} type="button">Hủy</button>
                  <button className="inline-flex items-center gap-2 rounded-lg bg-[#4b0009] px-4 py-2.5 text-sm font-bold text-white disabled:opacity-60" disabled={saving} onClick={saveAnnouncement} type="button">
                    <Check className="h-4 w-4" />
                    {saving ? 'Đang đăng...' : 'Đăng thông báo'}
                  </button>
                </div>
              </div>
            </Panel>
          ) : null}

          <Panel className="overflow-hidden rounded-xl border-[#dcc0bf]/30 shadow-sm">
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
            <button
              className="inline-flex items-center gap-2 rounded-lg border border-[#dcc0bf]/40 bg-white px-4 py-2.5 text-sm font-bold text-[#4b0009] hover:bg-[#fff7f7] disabled:opacity-60"
              disabled={saving || !classroomProfile?.trainingProgramId}
              onClick={() => applyProgramProfile(classroomProfile?.trainingProgramId)}
              type="button"
            >
              <BookMarked className="h-4 w-4" />
              Đồng bộ từ chương trình
            </button>
            <button className="inline-flex items-center gap-2 rounded-lg bg-[#4b0009] px-4 py-2.5 text-sm font-bold text-white" onClick={openSyllabusCreate} type="button">
              <Plus className="h-4 w-4" />
              Thêm mục đề cương
            </button>
          </div>

          {syllabusEditorOpen ? (
            <Panel className="rounded-xl border-[#dcc0bf]/30 p-5 shadow-sm">
              <div className="mb-4 flex items-center justify-between gap-3">
                <h2 className="font-['Manrope'] text-xl font-extrabold text-[#4b0009]">
                  {syllabusEditingId ? 'Sửa mục đề cương' : 'Mục đề cương mới'}
                </h2>
                <button className="rounded-lg p-2 text-[#4b0009] hover:bg-[#eff4ff]" onClick={() => setSyllabusEditorOpen(false)} type="button">
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
                <button className="rounded-lg border border-[#dcc0bf]/40 px-4 py-2.5 text-sm font-bold text-[#4b0009]" onClick={() => setSyllabusEditorOpen(false)} type="button">Hủy</button>
                <button className="inline-flex items-center gap-2 rounded-lg bg-[#4b0009] px-4 py-2.5 text-sm font-bold text-white disabled:opacity-60" disabled={saving} onClick={saveSyllabus} type="button">
                  <Check className="h-4 w-4" />
                  {saving ? 'Đang lưu...' : 'Lưu đề cương'}
                </button>
              </div>
            </Panel>
          ) : null}

          <Panel className="overflow-hidden rounded-xl border-[#dcc0bf]/30 shadow-sm">
            {!syllabusItems.length ? (
              <div className="p-6">
                <ClassroomEmptyState title="Chưa có đề cương lớp" description="Thêm các mục đề cương để học viên theo dõi tiến trình và nội dung từng phần của lớp." />
              </div>
            ) : (
              <ItemPager list={visibleItems} page={page} totalPages={totalPages} onPageChange={setPage} renderItem={(item) => (
                <article key={item.id} className="space-y-3 px-6 py-5">
                  <div className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">{item.title}</h3>
                        <span className="rounded-lg border border-[#dcc0bf]/40 bg-[#dce9ff] px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-[#564241]">
                          {item.status === 'PUBLISHED' ? 'Đã xuất bản' : 'Bản nháp'}
                        </span>
                      </div>
                      <p className="mt-1 text-xs text-[#8b706e]">Thứ tự: {item.displayOrder ?? 0}</p>
                    </div>
                    <div className="flex gap-2">
                      <button className="inline-flex items-center gap-2 rounded-lg border border-[#dcc0bf]/40 px-3 py-2 text-sm font-bold text-[#4b0009] hover:bg-[#eff4ff]" onClick={() => openSyllabusEdit(item)} type="button">
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

function ClassroomEditorPanel({
  trainingProgramOptions,
  form,
  isEditing,
  onChange,
  onClose,
  onSubmit,
  roomOptions,
  saving,
  teacherOptions,
}) {
  return (
    <div className="flex h-[min(860px,calc(100dvh-2rem))] overflow-hidden rounded-xl bg-white shadow-2xl">
      <form className="flex min-h-0 w-full flex-col" onSubmit={onSubmit}>
        <div className="shrink-0 px-5 pb-4 pt-5">
          <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Lịch khai giảng lớp học</p>
            <h2 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#0b1c30]">
              {isEditing ? 'Chỉnh sửa lớp học' : 'Tạo lớp khai giảng mới'}
            </h2>
            <p className="mt-2 max-w-3xl text-sm leading-6 text-[#584140]">
              Thiết lập lớp cụ thể dùng tại trung tâm: chương trình áp dụng, ngày khai giảng, lịch dự kiến, sĩ số, học phí, giáo viên và phòng học.
            </p>
          </div>
          <button className="rounded-lg p-2 text-[#4b0009] hover:bg-[#fff7f7]" onClick={onClose} type="button">
            <X className="h-5 w-5" />
          </button>
          </div>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto px-5 pb-5 pr-2 [scrollbar-color:#cfa7a5_transparent] [&::-webkit-scrollbar]:w-2 [&::-webkit-scrollbar-track]:bg-transparent [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb]:bg-[#cfa7a5]">
          <div className="grid gap-5 xl:grid-cols-[minmax(0,1.2fr)_minmax(360px,0.8fr)]">
            <div className="space-y-4">
              <EditorSection title="Thông tin lớp">
                <div className="grid gap-4 md:grid-cols-2">
                  <TextField label="Tên lớp" onChange={(event) => onChange('title', event.target.value)} value={form.title} />
                  <FormField label="Hình thức">
                    <BrandedSelect onChange={(event) => onChange('deliveryMode', event.target.value)} options={deliveryModeOptions} value={form.deliveryMode} />
                  </FormField>
                  <FormField label="Chương trình áp dụng">
                    <BrandedSelect
                      menuClassName="w-[min(560px,calc(100vw-2rem))]"
                      onChange={(event) => onChange('trainingProgramId', event.target.value)}
                      options={trainingProgramOptions}
                      placeholder="Chọn chương trình"
                      value={form.trainingProgramId}
                    />
                  </FormField>
                  <TextField label="Trình độ đầu vào" onChange={(event) => onChange('entryLevel', event.target.value)} value={form.entryLevel} />
                  <TextField label="Target" onChange={(event) => onChange('targetScore', event.target.value)} placeholder="IELTS 6.0 / TOEIC 750" value={form.targetScore} />
                  <TextField label="Chuẩn đầu ra" onChange={(event) => onChange('targetOutcome', event.target.value)} value={form.targetOutcome} />
                </div>
                <TextField label="Mô tả ngắn" onChange={(event) => onChange('shortDescription', event.target.value)} rows={2} textarea value={form.shortDescription} />
                <TextField label="Ghi chú chương trình / đề cương" onChange={(event) => onChange('syllabusSummary', event.target.value)} rows={2} textarea value={form.syllabusSummary} />
              </EditorSection>

              <EditorSection title="Lịch khai giảng">
                <div className="grid gap-4 md:grid-cols-3">
                  <FormField label="Ngày khai giảng">
                    <input className={formInputClass} onChange={(event) => onChange('startDate', event.target.value)} type="date" value={form.startDate} />
                  </FormField>
                  <FormField label="Ngày kết thúc dự kiến">
                    <input className={formInputClass} onChange={(event) => onChange('endDate', event.target.value)} type="date" value={form.endDate} />
                  </FormField>
                  <TextField label="Thời lượng" onChange={(event) => onChange('duration', event.target.value)} placeholder="10 buổi / 5 tuần" value={form.duration} />
                </div>
                <TextField label="Lịch học dự kiến" onChange={(event) => onChange('studyMode', event.target.value)} placeholder="T2, T4, T6 · 18:30-20:30" value={form.studyMode} />
              </EditorSection>
            </div>

            <div className="space-y-4">
              <EditorSection title="Vận hành">
                <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-1">
                  <FormField label="Trạng thái lớp">
                    <BrandedSelect onChange={(event) => onChange('classroomStatus', event.target.value)} options={classroomStatusOptions} value={form.classroomStatus} />
                  </FormField>
                  <FormField label="Trạng thái hiển thị">
                    <BrandedSelect onChange={(event) => onChange('packageStatus', event.target.value)} options={packageStatusOptions} value={form.packageStatus} />
                  </FormField>
                  <FormField label="Giáo viên chính">
                    <BrandedSelect onChange={(event) => onChange('primaryTeacherId', event.target.value)} options={teacherOptions} value={form.primaryTeacherId} />
                  </FormField>
                  <FormField label="Sĩ số tối đa">
                    <input className={formInputClass} min="1" onChange={(event) => onChange('maxCapacity', event.target.value)} type="number" value={form.maxCapacity} />
                  </FormField>
                </div>

                {form.deliveryMode === 'OFFLINE' ? (
                  <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-1">
                    <FormField label="Phòng học">
                      <BrandedSelect onChange={(event) => onChange('defaultRoomId', event.target.value)} options={roomOptions} value={form.defaultRoomId} />
                    </FormField>
                    <TextField label="Địa điểm học" onChange={(event) => onChange('offlineAddress', event.target.value)} value={form.offlineAddress} />
                    <TextField label="Ghi chú địa điểm" onChange={(event) => onChange('locationNote', event.target.value)} value={form.locationNote} />
                  </div>
                ) : (
                  <TextField label="Link phòng virtual / Lark" onChange={(event) => onChange('defaultLarkMeetingUrl', event.target.value)} value={form.defaultLarkMeetingUrl} />
                )}
              </EditorSection>

              <EditorSection title="Học phí & sắp xếp">
                <div className="grid gap-4 sm:grid-cols-2">
                  <FormField label="Học phí">
                    <input className={formInputClass} min="0" onChange={(event) => onChange('price', event.target.value)} type="number" value={form.price} />
                  </FormField>
                  <FormField label="Giá ưu đãi">
                    <input className={formInputClass} min="0" onChange={(event) => onChange('salePrice', event.target.value)} type="number" value={form.salePrice} />
                  </FormField>
                  <FormField label="Thứ tự hiển thị">
                    <input className={formInputClass} min="0" onChange={(event) => onChange('displayOrder', event.target.value)} type="number" value={form.displayOrder} />
                  </FormField>
                  <label className="flex items-center gap-3 rounded-lg border border-[#dcc0bf]/30 bg-[#fcfbfb] px-4 py-3 text-sm font-bold text-[#4b0009]">
                    <input checked={form.featured} className="h-4 w-4 accent-[#4b0009]" onChange={(event) => onChange('featured', event.target.checked)} type="checkbox" />
                    Lớp nổi bật
                  </label>
                </div>
              </EditorSection>
            </div>
          </div>
        </div>

        <div className="shrink-0 border-t border-[#dcc0bf]/20 bg-white px-5 py-4">
          <div className="flex flex-wrap justify-end gap-3">
          <button className="rounded-lg border border-[#dcc0bf]/40 px-4 py-2.5 text-sm font-bold text-[#4b0009]" onClick={onClose} type="button">Hủy</button>
          <button className="inline-flex items-center gap-2 rounded-lg bg-[#4b0009] px-4 py-2.5 text-sm font-bold text-white disabled:opacity-60" disabled={saving} type="submit">
            <Check className="h-4 w-4" />
            {saving ? 'Đang lưu...' : isEditing ? 'Cập nhật lớp' : 'Tạo lớp'}
          </button>
          </div>
        </div>
      </form>
    </div>
  );
}

function EditorModal({ children, onClose }) {
  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-hidden px-3 py-4 sm:px-6" role="dialog" aria-modal="true">
      <button
        aria-label="Đóng modal"
        className="absolute inset-0 bg-[#1a0004]/45 backdrop-blur-sm"
        onClick={onClose}
        type="button"
      />
      <div className="relative z-10 w-full max-w-[1080px] pointer-events-auto">
        {children}
      </div>
    </div>
  );
}

function EditorSection({ children, title }) {
  return (
    <section className="space-y-4 rounded-xl border border-[#dcc0bf]/25 bg-white p-4">
      <h3 className="font-['Manrope'] text-base font-extrabold text-[#0b1c30]">{title}</h3>
      {children}
    </section>
  );
}

function FormField({ children, label }) {
  return (
    <label className="block space-y-2">
      <span className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#8b706e]">{label}</span>
      {children}
    </label>
  );
}

const formInputClass = 'w-full rounded-lg border border-[#dcc0bf]/50 bg-[#f8f9ff] px-3 py-2.5 text-sm text-[#0b1c30] outline-none transition focus:border-[#4b0009] focus:bg-white focus:ring-4 focus:ring-[#4b0009]/5';

function ClassroomListPanel({ classrooms, loading, onCreate, onEdit, onOpen, onRefresh }) {
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [listPage, setListPage] = useState(1);

  const statusOptions = useMemo(() => {
    const values = [...new Set(classrooms.map((item) => item.classroomStatus || item.packageStatus).filter(Boolean))];
    return [{ label: 'Trạng thái: Tất cả', value: 'ALL' }, ...values.map((value) => ({ label: `Trạng thái: ${formatStatusLabel(value)}`, value }))];
  }, [classrooms]);

  const filteredClassrooms = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    return classrooms.filter((item) => {
      const status = item.classroomStatus || item.packageStatus || '';
      const statusMatched = statusFilter === 'ALL' || status === statusFilter;
      const haystack = [
        item.title,
        item.slug,
        item.shortDescription,
        item.deliveryModeLabel,
        item.deliveryMode,
        item.trainingProgramTitle,
        item.curriculumProgramTitle,
        item.scheduleSummary,
      ].filter(Boolean).join(' ').toLowerCase();
      return statusMatched && (!normalizedKeyword || haystack.includes(normalizedKeyword));
    });
  }, [classrooms, keyword, statusFilter]);
  const totalListPages = Math.max(1, Math.ceil(filteredClassrooms.length / PAGE_SIZE));
  const visibleClassrooms = filteredClassrooms.slice((listPage - 1) * PAGE_SIZE, listPage * PAGE_SIZE);

  const stats = useMemo(() => {
    const activeCount = classrooms.filter((item) => ['ACTIVE', 'ONGOING', 'PUBLISHED'].includes(item.classroomStatus || item.packageStatus)).length;
    const studentCount = classrooms.reduce((total, item) => total + Number(item.enrolledCount || 0), 0);
    const programCount = classrooms.filter((item) => item.trainingProgramTitle || item.trainingProgramId).length;
    return [
      { label: 'Tổng lớp', value: classrooms.length, icon: GraduationCap, tone: 'text-[#4b0009]' },
      { label: 'Đang hoạt động', value: activeCount, icon: Check, tone: 'text-emerald-700' },
      { label: 'Tổng học viên', value: studentCount, icon: Users, tone: 'text-[#005236]' },
      { label: 'Gắn chương trình', value: programCount, icon: BookMarked, tone: 'text-[#c45500]' },
    ];
  }, [classrooms]);

  useEffect(() => {
    setListPage(1);
  }, [keyword, statusFilter]);

  useEffect(() => {
    if (listPage > totalListPages) setListPage(totalListPages);
  }, [listPage, totalListPages]);

  return (
    <div className="space-y-6">
      <div className="flex justify-end sm:-mt-[88px] sm:mb-14">
        <button
          className="inline-flex items-center gap-2 rounded-lg bg-[#4b0009] px-4 py-2.5 text-sm font-bold text-white transition hover:bg-[#730014]"
          onClick={onCreate}
          type="button"
        >
          <Plus className="h-4 w-4" />
          Tạo lớp
        </button>
      </div>

      <div className="grid gap-6 md:grid-cols-4">
        {stats.map((item) => {
          const Icon = item.icon;
          return (
            <section className="rounded-xl border border-[#dcc0bf]/30 bg-white p-4 shadow-[0_4px_12px_rgba(75,0,9,0.05)]" key={item.label}>
              <div className="mb-1 flex items-center justify-between gap-3">
                <span className={`text-xs font-bold uppercase tracking-[0.12em] ${item.tone}`}>{item.label}</span>
                <Icon className={`h-5 w-5 ${item.tone}`} />
              </div>
              <p className="font-['Manrope'] text-3xl font-extrabold text-[#0b1c30]">{item.value}</p>
            </section>
          );
        })}
      </div>

      <section className="rounded-xl border border-[#dcc0bf]/30 bg-white p-4 shadow-sm">
        <div className="flex flex-wrap items-center gap-4">
          <div className="min-w-[300px] flex-1">
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-[18px] w-[18px] -translate-y-1/2 text-[#897270]" />
              <input
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="Tìm lớp, mã lớp, chương trình hoặc lịch học..."
                className="w-full rounded-lg border border-[#dcc0bf]/50 bg-[#f8f9ff] py-2 pl-10 pr-4 text-sm text-[#0b1c30] outline-none transition focus:border-[#4b0009] focus:bg-white focus:ring-4 focus:ring-[#4b0009]/5"
              />
            </div>
          </div>
          <div className="w-full sm:w-[220px]">
            <BrandedSelect
              onChange={(event) => setStatusFilter(event.target.value)}
              options={statusOptions}
              value={statusFilter}
            />
          </div>
          <button
            aria-label="Làm mới danh sách lớp"
            type="button"
            onClick={onRefresh}
            className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-[#dcc0bf]/40 text-[#564241] transition hover:bg-[#eff4ff]"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </section>

      <section className="overflow-hidden rounded-xl border border-[#dcc0bf]/30 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full min-w-[1080px] border-collapse text-left">
            <thead>
              <tr className="border-b border-[#dcc0bf]/30 bg-[#fbf3f4]">
                {['Tên lớp', 'Hình thức', 'Sĩ số', 'Chương trình', 'Lịch học', 'Khai giảng', 'Trạng thái', 'Thao tác'].map((heading) => (
                  <th
                    className={`px-6 py-4 text-xs font-bold uppercase tracking-[0.12em] text-[#8e7371] ${heading === 'Sĩ số' ? 'text-center' : ''} ${heading === 'Thao tác' ? 'text-right' : ''}`}
                    key={heading}
                  >
                    {heading}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-[#dcc0bf]/15">
              {visibleClassrooms.map((item) => (
                <tr className="transition hover:bg-[#eff4ff]" key={item.id}>
                  <td className="px-6 py-5">
                    <div className="min-w-0">
                      <p className="max-w-[300px] overflow-hidden text-sm font-bold leading-5 text-[#4b0009] [display:-webkit-box] [-webkit-box-orient:vertical] [-webkit-line-clamp:2]">{item.title}</p>
                      <p className="mt-1 text-xs text-[#584140]">{item.slug || item.shortDescription || '-'}</p>
                    </div>
                  </td>
                  <td className="px-6 py-5 text-sm text-[#0b1c30]">{item.deliveryModeLabel || item.deliveryMode || 'Offline'}</td>
                  <td className="px-6 py-5 text-center text-sm font-semibold text-[#0b1c30]">{item.enrolledCount ?? 0}/{item.maxCapacity ?? '-'}</td>
                  <td className="px-6 py-5 text-sm text-[#564241]">{item.trainingProgramTitle || item.curriculumProgramTitle || 'Chưa gắn'}</td>
                  <td className="px-6 py-5 text-sm text-[#564241]">{item.scheduleSummary || buildScheduleSummary(item)}</td>
                  <td className="px-6 py-5 text-sm text-[#564241]">{formatDate(item.startDate)}</td>
                  <td className="px-6 py-5"><StatusBadge value={item.classroomStatus || item.packageStatus} /></td>
                  <td className="px-6 py-5 text-right">
                    <div className="flex flex-wrap justify-end gap-2">
                      <button
                        className="inline-flex items-center gap-1.5 rounded-lg border border-[#dcc0bf]/40 px-3 py-1.5 text-xs font-bold text-[#4b0009] transition hover:bg-[#fff7f7]"
                        onClick={() => onEdit(item)}
                        type="button"
                      >
                        <Pencil className="h-3.5 w-3.5" />
                        Sửa
                      </button>
                      <button
                        className="inline-flex items-center gap-1.5 rounded-lg border border-[#4b0009] px-3 py-1.5 text-xs font-bold text-[#4b0009] transition hover:bg-[#4b0009]/5"
                        onClick={() => onOpen(item.id)}
                        type="button"
                      >
                        Quản lý
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {!filteredClassrooms.length ? (
          <div className="border-t border-[#dcc0bf]/20 px-6 py-10 text-center text-sm font-semibold text-[#584140]">
            Không có lớp phù hợp với bộ lọc hiện tại.
          </div>
        ) : (
          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-[#dcc0bf]/20 bg-[#fbf3f4]/40 px-6 py-4">
            <p className="text-sm text-[#2b2828]">
              Trang {listPage} / {totalListPages} · <span className="font-bold text-[#0b1c30]">{filteredClassrooms.length}</span> lớp
            </p>
            <div className="flex items-center gap-2">
              <button
                aria-label="Trang trước"
                className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-[#dcc0bf]/35 bg-white text-[#8b706e] transition hover:bg-[#fff7f7] disabled:cursor-not-allowed disabled:opacity-40"
                disabled={listPage <= 1}
                onClick={() => setListPage((current) => Math.max(1, current - 1))}
                type="button"
              >
                &lt;
              </button>
              <button
                aria-label="Trang sau"
                className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-[#dcc0bf]/35 bg-white text-[#8b706e] transition hover:bg-[#fff7f7] disabled:cursor-not-allowed disabled:opacity-40"
                disabled={listPage >= totalListPages}
                onClick={() => setListPage((current) => Math.min(totalListPages, current + 1))}
                type="button"
              >
                &gt;
              </button>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}

function ItemPager({ list, page, totalPages, onPageChange, renderItem }) {
  return (
    <>
      <div className="divide-y divide-[#dcc0bf]/15">{list.map(renderItem)}</div>
      {totalPages > 1 ? (
        <div className="flex items-center justify-center gap-3 border-t border-[#dcc0bf]/20 bg-[#fbf3f4]/40 px-6 py-4">
          <button className="rounded-lg border border-[#dcc0bf]/40 px-4 py-2 text-sm font-bold text-[#4b0009] hover:bg-[#eff4ff] disabled:opacity-40" disabled={page <= 1} onClick={() => onPageChange(page - 1)} type="button">Trang trước</button>
          <span className="text-sm font-semibold text-[#584140]">Trang {page} / {totalPages}</span>
          <button className="rounded-lg border border-[#dcc0bf]/40 px-4 py-2 text-sm font-bold text-[#4b0009] hover:bg-[#eff4ff] disabled:opacity-40" disabled={page >= totalPages} onClick={() => onPageChange(page + 1)} type="button">Trang sau</button>
        </div>
      ) : null}
    </>
  );
}

function OverviewCard({ icon: Icon, label, value }) {
  return (
    <div className="rounded-xl border border-[#dcc0bf]/30 bg-white p-4 shadow-[0_4px_12px_rgba(75,0,9,0.04)]">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm font-medium text-[#584140]">{label}</p>
          <p className="mt-2 text-base font-extrabold text-[#2b2828]">{value}</p>
        </div>
        <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-[#dce9ff] text-[#4b0009]">
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
      isCenter ? 'bg-emerald-100 text-emerald-700' : 'bg-[#dce9ff] text-[#4b0009]'
    }`}>
      {isCenter ? 'Từ kho trung tâm' : 'Riêng của lớp'}
    </span>
  );
}

function InfoPill({ label, value }) {
  return (
    <div className="rounded-xl border border-[#dcc0bf]/30 bg-[#fcfbfb] px-4 py-3">
      <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-[#8b706e]">{label}</p>
      <p className="mt-2 text-sm font-semibold text-[#2b2828]">{value}</p>
    </div>
  );
}

function DetailBlock({ icon: Icon, label, value, note }) {
  return (
    <div className="rounded-xl border border-[#dcc0bf]/25 bg-[#fcfbfb] p-4">
      <div className="flex items-start gap-3">
        <span className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-[#fbf3f4] text-[#4b0009]">
          <Icon className="h-4.5 w-4.5" />
        </span>
        <div className="min-w-0">
          <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#8b706e]">{label}</p>
          <p className="mt-1 break-words text-sm font-extrabold text-[#0b1c30]">{value}</p>
          {note ? <p className="mt-1 break-words text-xs leading-5 text-[#584140]">{note}</p> : null}
        </div>
      </div>
    </div>
  );
}

function ContentCheck({ label, value }) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-lg border border-[#dcc0bf]/25 bg-[#fcfbfb] px-3 py-2.5">
      <span className="text-sm font-semibold text-[#584140]">{label}</span>
      <span className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">{value}</span>
    </div>
  );
}

function StatusBadge({ value }) {
  return (
    <span className="inline-flex rounded-lg border border-[#dcc0bf]/40 bg-[#fbf3f4] px-3 py-1.5 text-xs font-extrabold uppercase tracking-[0.08em] text-[#4b0009]">
      {formatStatusLabel(value)}
    </span>
  );
}

function formatStatusLabel(value) {
  const labels = {
    ACTIVE: 'Đang hoạt động',
    ONGOING: 'Đang hoạt động',
    UPCOMING: 'Sắp khai giảng',
    PUBLISHED: 'Đã xuất bản',
    DRAFT: 'Bản nháp',
    ARCHIVED: 'Đã lưu trữ',
    COMPLETED: 'Đã kết thúc',
    CANCELLED: 'Đã hủy',
  };
  return labels[value] || value || 'Chưa rõ';
}

function formatDate(value) {
  if (!value) return '-';
  return new Date(value).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

function formatDateRange(start, end) {
  if (!start && !end) return '';
  if (!end) return `Từ ${formatDate(start)}`;
  if (!start) return `Đến ${formatDate(end)}`;
  return `${formatDate(start)} - ${formatDate(end)}`;
}

function formatMoney(value) {
  if (value === null || value === undefined || value === '') return '-';
  return Number(value).toLocaleString('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 });
}

function buildScheduleSummary(classroom) {
  if (classroom?.typicalStartTime && classroom?.typicalEndTime) {
    return `${classroom.typicalStartTime} - ${classroom.typicalEndTime}`;
  }
  return 'Chưa có lịch học';
}
