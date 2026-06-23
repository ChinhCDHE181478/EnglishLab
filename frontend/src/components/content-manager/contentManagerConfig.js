import {
  BarChart3,
  BadgePercent,
  BookText,
  BookOpen,
  Brain,
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
} from 'lucide-react';

export const contentManagerNav = [
  {
    title: 'Không gian làm việc',
    items: [
      { label: 'Tổng quan', href: '/content-manager/dashboard', icon: LayoutDashboard },
      { label: 'Khóa học online', href: '/content-manager/courses', icon: BookOpen },
      { label: 'Lớp học tại trung tâm', href: '/content-manager/classrooms', icon: School },
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
    ],
  },
  {
    title: 'Quản trị',
    items: [
      { label: 'Hàng chờ xuất bản', href: '/content-manager/publication', icon: FileCheck2 },
      { label: 'Phân tích nội dung', href: '/content-manager/analytics', icon: BarChart3 },
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
    title: 'Quản lý khóa học online',
    subtitle: 'Tạo mới, chỉnh sửa, xuất bản và theo dõi toàn bộ khóa học online trong hệ thống.',
    searchPlaceholder: 'Tìm theo tên khóa học hoặc slug...',
  },
  '/content-manager/classrooms': {
    title: 'Quản lý nội dung lớp học',
    subtitle: 'Theo dõi tài liệu, đăng thông báo và biên soạn giáo trình cho từng lớp tại trung tâm.',
    searchPlaceholder: 'Tìm lớp học, thông báo hoặc mục giáo trình...',
  },
  '/content-manager/courses/new': {
    title: 'Tạo khóa học online',
    subtitle: 'Thiết lập thông tin khóa học, cấu trúc học tập và các nội dung cần thiết trước khi xuất bản.',
    searchPlaceholder: 'Tìm trường dữ liệu hoặc thiết lập...',
  },
  '/content-manager/courses/:slugOrId/edit': {
    title: 'Chỉnh sửa khóa học online',
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
    subtitle: 'Biên soạn bộ thẻ theo khóa học, mô-đun và bài học để học viên học đúng ngữ cảnh.',
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
    subtitle: 'Thiết lập bài đánh giá đầu vào, giới hạn lượt làm và nội dung của bốn kỹ năng IELTS.',
    searchPlaceholder: 'Tìm phần thi hoặc câu hỏi...',
  },
  '/content-manager/publication': {
    title: 'Kiểm soát xuất bản',
    subtitle: 'Rà soát nội dung ở trạng thái nháp, chờ duyệt, đã xuất bản hoặc lưu trữ.',
    searchPlaceholder: 'Tìm loại nội dung hoặc người phụ trách...',
  },
  '/content-manager/analytics': {
    title: 'Phân tích nội dung',
    subtitle: 'Theo dõi khối lượng nội dung, tiến độ xuất bản và các chỉ số vận hành quan trọng.',
    searchPlaceholder: 'Tìm chỉ số hoặc nhóm nội dung...',
  },
  '/content-manager/categories': {
    title: 'Danh mục khóa học',
    subtitle: 'Tạo, sắp xếp và cập nhật các nhóm khóa học được sử dụng trên toàn nền tảng.',
    searchPlaceholder: 'Tìm danh mục...',
  },
};
