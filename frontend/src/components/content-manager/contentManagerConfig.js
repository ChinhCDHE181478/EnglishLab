import {
  BarChart3,
  BadgePercent,
  BookMarked,
  BookText,
  BookOpen,
  Brain,
  Building2,
  FileCheck2,
  FileQuestion,
  FileStack,
  ClipboardCheck,
  FolderKanban,
  Headphones,
  LayoutDashboard,
  Mic2,
  NotebookPen,
  Route,
  School,
  SlidersHorizontal,
  Dumbbell,
  Video,
  ShieldAlert,
} from 'lucide-react';

export const contentManagerNav = [
  {
    title: 'Không gian làm việc',
    items: [
      { label: 'Tổng quan', href: '/content-manager/dashboard', icon: LayoutDashboard },
      { label: 'Khóa học Online', href: '/content-manager/courses', icon: BookOpen },
      { label: 'Khóa học Offline', href: '/content-manager/offline-programs', icon: Building2 },
      { label: 'Khóa học Virtual', href: '/content-manager/virtual-programs', icon: Video },
      { label: 'Chương trình đào tạo', href: '/content-manager/syllabus-builder', icon: BookMarked },
      { label: 'Mã giảm giá', href: '/content-manager/discount-codes', icon: BadgePercent },
      { label: 'Danh mục khóa học', href: '/content-manager/categories', icon: FolderKanban },
      { label: 'Lộ trình học', href: '/content-manager/learning-paths', icon: Route },
      { label: 'Kho học liệu', href: '/content-manager/materials', icon: FileStack },
    ],
  },
  {
    title: 'Luyện tập và kiểm tra',
    items: [
      { label: 'Thẻ ghi nhớ', href: '/content-manager/flashcards', icon: Brain },
      { label: 'Luyện nghe', href: '/content-manager/listening', icon: Headphones },
      { label: 'Luyện đọc', href: '/content-manager/reading', icon: BookText },
      { label: 'Luyện viết', href: '/content-manager/writing', icon: NotebookPen },
      { label: 'Luyện nói', href: '/content-manager/speaking', icon: Mic2 },
      { label: 'Bài đánh giá đầu vào', href: '/content-manager/placement-test', icon: ClipboardCheck },
      { label: 'Ngân hàng đề thi thử', href: '/content-manager/mock-exams', icon: FileQuestion },
      { label: 'Ngân hàng bài tập', href: '/content-manager/exercise-bank', icon: Dumbbell },
      { label: 'Rubrics chấm điểm', href: '/content-manager/rubrics', icon: SlidersHorizontal },
    ],
  },
  {
    title: 'Quản trị',
    items: [
      { label: 'Hàng chờ xuất bản', href: '/content-manager/publication', icon: FileCheck2 },
      { label: 'Phân tích nội dung', href: '/content-manager/analytics', icon: BarChart3 },
      { label: 'Báo cáo hỏi đáp', href: '/content-manager/discussion-moderation', icon: ShieldAlert },
    ],
  },
];

export const contentManagerPageMeta = {
  '/content-manager/dashboard': {
    title: 'Không gian quản lý nội dung',
    subtitle: 'Theo dõi tiến độ biên soạn, xuất bản và tình hình vận hành của toàn bộ kho nội dung EnglishLab.',
    searchPlaceholder: 'Tìm khóa học, bài học hoặc học liệu...',
  },
  '/content-manager/courses': {
    title: 'Quản lý khóa học Online',
    subtitle: '',
    searchPlaceholder: 'Tìm theo tên khóa học hoặc slug...',
  },
  '/content-manager/offline-programs': {
    title: 'Khóa học Offline',
    subtitle: 'Tạo và quản lý khóa học tại trung tâm theo chương trình đào tạo, mục tiêu và cấp độ đầu vào.',
    searchPlaceholder: 'Tìm khóa học Offline...',
  },
  '/content-manager/virtual-programs': {
    title: 'Khóa học Virtual',
    subtitle: 'Tạo và quản lý khóa học trực tuyến có lịch, giảng viên và mục tiêu rõ ràng.',
    searchPlaceholder: 'Tìm khóa học Virtual...',
  },
  '/content-manager/syllabus-builder': {
    title: 'Chương trình đào tạo',
    subtitle: 'Biên soạn unit/buổi học và gắn học liệu, bài tập, đề, flashcard từ các kho dùng chung.',
    searchPlaceholder: 'Tìm unit hoặc tài nguyên...',
  },
  '/content-manager/exercise-bank': {
    title: 'Ngân hàng bài tập',
    subtitle: 'Tạo và tái sử dụng đề bài tập, quiz theo kỹ năng và cấp độ.',
    searchPlaceholder: 'Tìm bài tập hoặc kỹ năng...',
  },
  '/content-manager/courses/new': {
    title: 'Tạo khóa học Online',
    subtitle: 'Thiết lập thông tin khóa học, cấu trúc học tập và các nội dung cần thiết trước khi xuất bản.',
    searchPlaceholder: 'Tìm trường dữ liệu hoặc thiết lập...',
  },
  '/content-manager/courses/:slugOrId/edit': {
    title: 'Chỉnh sửa khóa học Online',
    subtitle: 'Cập nhật thông tin khóa học, đầu ra, mô-đun và trạng thái sẵn sàng xuất bản của khóa học.',
    searchPlaceholder: 'Tìm trường dữ liệu hoặc thiết lập...',
  },
  '/content-manager/courses/:slugOrId/builder': {
    title: 'Biên soạn nội dung khóa học',
    subtitle: 'Sắp xếp mô-đun, bài học và tài nguyên liên kết trong không gian biên soạn nội dung.',
    searchPlaceholder: 'Tìm mô-đun, bài học hoặc tài nguyên...',
  },
  '/content-manager/discount-codes': {
    title: 'Quản lý mã giảm giá',
    subtitle: 'Tạo mã ưu đãi, đặt giới hạn sử dụng và theo dõi hiệu quả của từng chương trình.',
    searchPlaceholder: 'Tìm mã giảm giá...',
  },
  '/content-manager/materials': {
    title: 'Kho học liệu trung tâm',
    subtitle: 'Quản lý học liệu tái sử dụng theo band IELTS, dải điểm TOEIC, kỹ năng và loại tài nguyên.',
    searchPlaceholder: 'Tìm học liệu, nguồn cung cấp hoặc kỹ năng...',
  },
  '/content-manager/flashcards': {
    title: 'Quản lý thẻ ghi nhớ',
    subtitle: 'Tạo và quản lý bộ flashcard dùng chung để curriculum/course tham chiếu lại.',
    searchPlaceholder: 'Tìm bộ thẻ hoặc chủ đề...',
  },
  '/content-manager/listening': {
    title: 'Quản lý luyện nghe',
    subtitle: 'Theo dõi bài luyện nghe, transcript, đáp án và phần giải thích đi kèm.',
    searchPlaceholder: 'Tìm bài luyện nghe...',
  },
  '/content-manager/writing': {
    title: 'Quản lý luyện viết',
    subtitle: 'Quản lý đề bài, bài mẫu, tiêu chí chấm và nội dung hỗ trợ cho phần luyện viết.',
    searchPlaceholder: 'Tìm đề bài hoặc dạng bài...',
  },
  '/content-manager/reading': {
    title: 'Quản lý luyện đọc',
    subtitle: 'Theo dõi bài đọc, bộ câu hỏi, đáp án và phần giải thích gắn với từng khóa học.',
    searchPlaceholder: 'Tìm bài luyện đọc...',
  },
  '/content-manager/speaking': {
    title: 'Quản lý luyện nói',
    subtitle: 'Quản lý chủ đề nói, câu hỏi, thời lượng và tiêu chí chấm cho từng bài luyện.',
    searchPlaceholder: 'Tìm bài luyện nói...',
  },
  '/content-manager/learning-paths': {
    title: 'Quản lý lộ trình học',
    subtitle: 'Sắp xếp khóa học theo lộ trình, thứ tự học và khóa học được đề xuất tiếp theo.',
    searchPlaceholder: 'Tìm lộ trình hoặc khóa học...',
  },
  '/content-manager/mock-exams': {
    title: 'Ngân hàng đề thi thử',
    subtitle: 'Biên soạn và kiểm soát câu hỏi, bộ đề và lời giải cho IELTS hoặc TOEIC.',
    searchPlaceholder: 'Tìm câu hỏi hoặc chủ đề...',
  },
  '/content-manager/placement-test': {
    title: 'Quản lý bài đánh giá đầu vào',
    subtitle: 'Thiết lập bài đánh giá đầu vào IELTS hoặc TOEIC, theo dõi kết quả và biên soạn nội dung trực quan.',
    searchPlaceholder: 'Tìm phần thi hoặc câu hỏi...',
  },
  '/content-manager/rubrics': {
    title: 'Quản lý rubrics chấm điểm',
    subtitle: 'Tạo, sửa và tạm ngưng các bộ tiêu chí chấm điểm cho giáo viên, bài tập và AI feedback.',
    searchPlaceholder: 'Tìm rubric hoặc rule...',
  },
  '/content-manager/publication': {
    title: 'Kiểm soát xuất bản',
    subtitle: 'Rà soát bản nháp và quản lý nội dung đang được xuất bản.',
    searchPlaceholder: 'Tìm loại nội dung hoặc người phụ trách...',
  },
  '/content-manager/analytics': {
    title: 'Phân tích nội dung',
    subtitle: 'Theo dõi khối lượng nội dung, tiến độ xuất bản và các chỉ số vận hành quan trọng.',
    searchPlaceholder: 'Tìm chỉ số hoặc nhóm nội dung...',
  },
  '/content-manager/discussion-moderation': {
    title: 'Báo cáo hỏi đáp',
    subtitle: 'Xem xét báo cáo của học viên, ẩn nội dung vi phạm hoặc bỏ qua báo cáo không phù hợp.',
    searchPlaceholder: 'Tìm báo cáo thảo luận...',
  },
  '/content-manager/categories': {
    title: 'Danh mục khóa học',
    subtitle: 'Tạo, sắp xếp và cập nhật các nhóm khóa học được sử dụng trên toàn nền tảng.',
    searchPlaceholder: 'Tìm danh mục...',
  },
};
