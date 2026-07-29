import { BrowserRouter, Navigate, Route, Routes, useLocation, useParams } from 'react-router-dom';
import AuthLayout from './components/auth/AuthLayout';
import ProtectedRoute from './components/auth/ProtectedRoute';
import { AppDialogProvider } from './components/ui/AppDialog';
import { AuthProvider } from './context/AuthContext';
import { LearnerExperienceProvider } from './context/LearnerExperienceContext';
import CartPage from './pages/CartPage';
import CheckoutPage from './pages/CheckoutPage';
import CertificateVerifyPage from './pages/CertificateVerifyPage';
import CompleteProfile from './pages/CompleteProfile';
import CourseDetail from './pages/CourseDetail';
import CourseHome from './pages/CourseHome';
import CourseWorkspace from './pages/CourseWorkspace';
import Courses from './pages/Courses';
import ForgotPassword from './pages/ForgotPassword';
import Home from './pages/Home';
import FlashcardPracticePage from './pages/FlashcardPracticePage';
import Login from './pages/Login';
import MyCoursesPage from './pages/MyCoursesPage';
import LearningPathPage from './pages/LearningPathPage';
import LearningPathReferencePage from './pages/LearningPathReferencePage';
import LearningPathCatalogPage from './pages/LearningPathCatalogPage';
import MockTestsPage from './pages/MockTestsPage';
import NotificationsPage from './pages/NotificationsPage';
import DictionaryPage from './pages/DictionaryPage';
import PlacementTestPage from './pages/PlacementTestPage';
import Register from './pages/Register';
import ResetPassword from './pages/ResetPassword';
import TransactionHistoryPage from './pages/TransactionHistoryPage';
import VerifyEmail from './pages/VerifyEmail';
import WishlistPage from './pages/WishlistPage';
import ContentManagerRoutes from './pages/content-manager/ContentManagerRoutes';
import ClassroomsCatalogPage from './pages/classroom/ClassroomsCatalogPage';
import MyEnrollmentRequestsPage from './pages/classroom/MyEnrollmentRequestsPage';
import MyClassroomsPage from './pages/classroom/MyClassroomsPage';
import MyClassroomDetailPage from './pages/classroom/MyClassroomDetailPage';
import TeacherFeedbackPage from './pages/classroom/TeacherFeedbackPage';
import MySchedulePage from './pages/classroom/MySchedulePage';
import MyHomeworkPage from './pages/classroom/MyHomeworkPage';
import MyPracticePage from './pages/classroom/MyPracticePage';
import PracticeRunnerPage from './pages/classroom/PracticeRunnerPage';
import TeacherDashboardPage from './pages/teacher/TeacherDashboardPage';
import TeacherClassroomPage from './pages/teacher/TeacherClassroomPage';
import TeacherProfessionalProfilePage from './pages/teacher/TeacherProfessionalProfilePage';
import ManagerTeacherFeedbackPage from './pages/manager/ManagerTeacherFeedbackPage';
import TeacherSessionPage from './pages/teacher/TeacherSessionPage';
import TeacherRequestsPage from './pages/teacher/TeacherRequestsPage';
import TeacherSchedulePage from './pages/teacher/TeacherSchedulePage';
import StaffLayout from './components/staff/StaffUi';
import StaffDashboardPage from './pages/staff/StaffDashboardPage';
import StaffClassroomDetailPage from './pages/staff/StaffClassroomDetailPage';
import StaffRequestsPage from './pages/staff/StaffRequestsPage';
import StaffInfrastructurePage from './pages/staff/StaffInfrastructurePage';
import StaffRecordingsPage from './pages/staff/StaffRecordingsPage';
import StaffClassroomsPage from './pages/staff/StaffClassroomsPage';
import TeacherManagementPage from './pages/staff/TeacherManagementPage';
import ManagerClassroomProposalsPage from './pages/manager/ManagerClassroomProposalsPage';
import ManagerOnlineEnrollmentsPage from './pages/manager/ManagerOnlineEnrollmentsPage';
import ManagerSupportTicketsPage from './pages/manager/ManagerSupportTicketsPage';
import SupportTicketsPage from './pages/SupportTicketsPage';
import StaffEnrollmentRequestsPage from './pages/staff/StaffEnrollmentRequestsPage';
import StaffClassroomProposalsPage from './pages/staff/StaffClassroomProposalsPage';
import AdminRoutes from './pages/admin/AdminRoutes';

function CourseDetailRoute() {
  const { slugOrId } = useParams();
  return <CourseDetail key={`course-detail-${slugOrId}`} />;
}

function CourseWorkspaceRoute() {
  const { slugOrId } = useParams();
  return <CourseWorkspace key={`course-workspace-${slugOrId}`} />;
}

function CourseHomeRoute() {
  const { slugOrId } = useParams();
  return <CourseHome key={`course-home-${slugOrId}`} />;
}

function AppRoutes() {
  const location = useLocation();

  return (
    <Routes location={location}>
      <Route path="/" element={<Home />} />
      <Route path="/certificates/:code" element={<CertificateVerifyPage />} />
      <Route element={<ProtectedRoute requireCompleteProfile={false} allowedRoles={['CONTENT_MANAGER', 'MANAGER', 'ADMIN']} />}>
        <Route path="/content-manager/*" element={<ContentManagerRoutes />} />
      </Route>
      <Route element={<ProtectedRoute requireCompleteProfile={false} allowedRoles={['ADMIN']} />}>
        <Route path="/admin/*" element={<AdminRoutes />} />
      </Route>
      {/* Public / student-facing marketing pages */}
      <Route path="/opening-schedule" element={<ClassroomsCatalogPage />} />
      <Route path="/opening-schedule/:slugOrId" element={<Navigate replace to="/opening-schedule#dang-ky-tu-van" />} />
      <Route path="/courses" element={<Courses />} />
      <Route path="/learning-paths" element={<LearningPathCatalogPage />} />
      <Route path="/learning-paths/:code" element={<LearningPathReferencePage />} />
      <Route path="/courses/:slugOrId" element={<CourseDetailRoute />} />
      <Route path="/courses/:slugOrId/home" element={<CourseHomeRoute />} />
      <Route path="/courses/:slugOrId/learn" element={<CourseWorkspaceRoute />} />

      {/* Placement must be available before the learner completes their profile. */}
      <Route element={<ProtectedRoute requireCompleteProfile={false} allowedRoles={['LEARNER']} />}>
        <Route path="/placement-test" element={<PlacementTestPage />} />
      </Route>

      {/* Student-only routes */}
      <Route element={<ProtectedRoute allowedRoles={['LEARNER']} />}>
        <Route path="/cart" element={<CartPage />} />
        <Route path="/wishlist" element={<WishlistPage />} />
        <Route path="/checkout" element={<CheckoutPage />} />
        <Route path="/my-courses" element={<MyCoursesPage />} />
        <Route path="/learning-path" element={<LearningPathPage />} />
        <Route path="/flashcards/practice" element={<FlashcardPracticePage />} />
        <Route path="/my-classrooms" element={<MyClassroomsPage />} />
        <Route path="/my-enrollment-requests" element={<MyEnrollmentRequestsPage />} />
        <Route path="/my-classrooms/:id" element={<MyClassroomDetailPage />} />
        <Route path="/my-classrooms/:id/teacher-feedback" element={<TeacherFeedbackPage />} />
        <Route path="/my-schedule" element={<MySchedulePage />} />
        <Route path="/my-homework" element={<MyHomeworkPage />} />
        <Route path="/my-practice" element={<MyPracticePage />} />
        <Route path="/my-practice/:classroomId/:exerciseId" element={<PracticeRunnerPage />} />
        <Route path="/my-quizzes" element={<Navigate to="/my-homework?type=online-quiz" replace />} />
        <Route path="/mock-tests" element={<MockTestsPage />} />
        <Route path="/transaction-history" element={<TransactionHistoryPage />} />
        <Route path="/support" element={<SupportTicketsPage />} />
      </Route>

      {/* Shared authenticated routes */}
      <Route element={<ProtectedRoute requireCompleteProfile={false} requirePlacementTest />}>
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="/dictionary" element={<DictionaryPage />} />
        <Route path="/home" element={<Home />} />
      </Route>

      {/* Teacher routes */}
      <Route element={<ProtectedRoute allowedRoles={['TEACHER', 'MANAGER', 'ADMIN']} />}>
        <Route path="/teacher" element={<TeacherDashboardPage />} />
        <Route path="/teacher/schedule" element={<TeacherSchedulePage />} />
        <Route path="/teacher/classrooms/:id" element={<TeacherClassroomPage />} />
        <Route path="/teacher/sessions/:sessionId" element={<TeacherSessionPage />} />
        <Route path="/teacher/requests" element={<TeacherRequestsPage />} />
        <Route path="/teacher/professional-profile" element={<TeacherProfessionalProfilePage />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={['STAFF', 'ADMIN']} />}>
        <Route element={<StaffLayout />}>
          <Route path="/staff" element={<StaffDashboardPage />} />
          <Route path="/staff/classrooms" element={<StaffClassroomsPage />} />
          <Route path="/staff/classrooms/:id" element={<StaffClassroomDetailPage />} />
          <Route path="/staff/enrollment-requests" element={<StaffEnrollmentRequestsPage />} />
          <Route path="/staff/classroom-proposals" element={<StaffClassroomProposalsPage />} />
          <Route path="/staff/requests" element={<StaffRequestsPage />} />
          <Route path="/staff/infrastructure" element={<StaffInfrastructurePage />} />
          <Route path="/staff/recordings" element={<StaffRecordingsPage />} />
          <Route path="/staff/teachers" element={<TeacherManagementPage mode="STAFF" />} />
          <Route path="/staff/support-tickets" element={<ManagerSupportTicketsPage />} />
        </Route>
      </Route>

      {/* Manager routes */}
      <Route element={<ProtectedRoute allowedRoles={['MANAGER', 'ADMIN']} />}>
        <Route element={<StaffLayout />}>
          <Route path="/manager/classroom-proposals" element={<ManagerClassroomProposalsPage />} />
          <Route path="/manager/online-enrollments" element={<ManagerOnlineEnrollmentsPage />} />
          <Route path="/manager/teacher-performance" element={<ManagerTeacherFeedbackPage />} />
          <Route path="/manager/support-tickets" element={<ManagerSupportTicketsPage />} />
        </Route>
      </Route>
      <Route element={<ProtectedRoute requireCompleteProfile={false} requirePlacementTest />}>
        <Route path="/complete-profile" element={<CompleteProfile />} />
        <Route path="/profile" element={<CompleteProfile />} />
      </Route>
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        <Route path="/verify-email" element={<VerifyEmail />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AppDialogProvider>
        <AuthProvider>
          <LearnerExperienceProvider>
            <AppRoutes />
          </LearnerExperienceProvider>
        </AuthProvider>
      </AppDialogProvider>
    </BrowserRouter>
  );
}

export default App;
