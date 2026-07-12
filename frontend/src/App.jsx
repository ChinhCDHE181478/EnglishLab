import { BrowserRouter, Navigate, Route, Routes, useLocation, useParams } from 'react-router-dom';
import AuthLayout from './components/auth/AuthLayout';
import ProtectedRoute from './components/auth/ProtectedRoute';
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
import WritingFeedbackPage from './pages/WritingFeedbackPage';
import FlashcardPracticePage from './pages/FlashcardPracticePage';
import Login from './pages/Login';
import MyCoursesPage from './pages/MyCoursesPage';
import LearningPathPage from './pages/LearningPathPage';
import NotificationsPage from './pages/NotificationsPage';
import PlacementTestPage from './pages/PlacementTestPage';
import Register from './pages/Register';
import ResetPassword from './pages/ResetPassword';
import TransactionHistoryPage from './pages/TransactionHistoryPage';
import VerifyEmail from './pages/VerifyEmail';
import WishlistPage from './pages/WishlistPage';
import ContentManagerRoutes from './pages/content-manager/ContentManagerRoutes';
import ClassroomsCatalogPage from './pages/classroom/ClassroomsCatalogPage';
import ClassroomPublicDetailPage from './pages/classroom/ClassroomPublicDetailPage';
import MyClassroomsPage from './pages/classroom/MyClassroomsPage';
import MyClassroomDetailPage from './pages/classroom/MyClassroomDetailPage';
import MyClassroomQuizzesPage from './pages/classroom/MyClassroomQuizzesPage';
import MySchedulePage from './pages/classroom/MySchedulePage';
import MyHomeworkPage from './pages/classroom/MyHomeworkPage';
import TeacherDashboardPage from './pages/teacher/TeacherDashboardPage';
import TeacherClassroomPage from './pages/teacher/TeacherClassroomPage';
import TeacherSessionPage from './pages/teacher/TeacherSessionPage';
import TeacherRequestsPage from './pages/teacher/TeacherRequestsPage';
import TeacherSchedulePage from './pages/teacher/TeacherSchedulePage';
import TrainingManagerLayout from './components/training-manager/TrainingManagerUi';
import TrainingManagerDashboardPage from './pages/training-manager/TrainingManagerDashboardPage';
import TrainingManagerClassroomDetailPage from './pages/training-manager/TrainingManagerClassroomDetailPage';
import TrainingManagerRequestsPage from './pages/training-manager/TrainingManagerRequestsPage';
import TrainingManagerClassroomRegistrationsPage from './pages/training-manager/TrainingManagerClassroomRegistrationsPage';
import TrainingManagerInfrastructurePage from './pages/training-manager/TrainingManagerInfrastructurePage';
import TrainingManagerAttendanceDisputesPage from './pages/training-manager/TrainingManagerAttendanceDisputesPage';
import TrainingManagerRecordingsPage from './pages/training-manager/TrainingManagerRecordingsPage';
import ManagerClassroomsPage from './pages/manager/ManagerClassroomsPage';
import ManagerCourseApprovalPage from './pages/manager/ManagerCourseApprovalPage';
import ManagerContentApprovalPage from './pages/manager/ManagerContentApprovalPage';
import ManagerOnlineEnrollmentsPage from './pages/manager/ManagerOnlineEnrollmentsPage';

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
      <Route path="/ai/writing-feedback" element={<WritingFeedbackPage />} />
      <Route path="/certificates/:code" element={<CertificateVerifyPage />} />
      <Route element={<ProtectedRoute requireCompleteProfile={false} allowedRoles={['CONTENT_MANAGER', 'MANAGER', 'ADMIN']} />}>
        <Route path="/content-manager/*" element={<ContentManagerRoutes />} />
      </Route>
      {/* Public / student-facing marketing pages */}
      <Route path="/opening-schedule" element={<ClassroomsCatalogPage />} />
      <Route path="/opening-schedule/:slugOrId" element={<ClassroomPublicDetailPage />} />
      <Route path="/courses" element={<Courses />} />
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
        <Route path="/my-classrooms/:id" element={<MyClassroomDetailPage />} />
        <Route path="/my-schedule" element={<MySchedulePage />} />
        <Route path="/my-homework" element={<MyHomeworkPage />} />
        <Route path="/my-quizzes" element={<MyClassroomQuizzesPage />} />
        <Route path="/transaction-history" element={<TransactionHistoryPage />} />
      </Route>

      {/* Shared authenticated routes */}
      <Route element={<ProtectedRoute requireCompleteProfile={false} requirePlacementTest />}>
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="/home" element={<Home />} />
      </Route>

      {/* Teacher routes */}
      <Route element={<ProtectedRoute allowedRoles={['TEACHER', 'TRAINING_MANAGER', 'MANAGER', 'ADMIN']} />}>
        <Route path="/teacher" element={<TeacherDashboardPage />} />
        <Route path="/teacher/schedule" element={<TeacherSchedulePage />} />
        <Route path="/teacher/classrooms/:id" element={<TeacherClassroomPage />} />
        <Route path="/teacher/sessions/:sessionId" element={<TeacherSessionPage />} />
        <Route path="/teacher/requests" element={<TeacherRequestsPage />} />
      </Route>

      {/* Training manager routes */}
      <Route element={<ProtectedRoute allowedRoles={['TRAINING_MANAGER', 'MANAGER', 'ADMIN']} />}>
        <Route element={<TrainingManagerLayout />}>
          <Route path="/training-manager" element={<TrainingManagerDashboardPage />} />
          <Route path="/training-manager/classrooms" element={<ManagerClassroomsPage />} />
          <Route path="/training-manager/classrooms/:id" element={<TrainingManagerClassroomDetailPage />} />
          <Route path="/training-manager/registrations" element={<TrainingManagerClassroomRegistrationsPage />} />
          <Route path="/training-manager/requests" element={<TrainingManagerRequestsPage />} />
          <Route path="/training-manager/infrastructure" element={<TrainingManagerInfrastructurePage />} />
          <Route path="/training-manager/recordings" element={<TrainingManagerRecordingsPage />} />
          <Route path="/training-manager/attendance-disputes" element={<TrainingManagerAttendanceDisputesPage />} />
          <Route path="/training-manager/classroom-registrations" element={<Navigate to="/training-manager/registrations" replace />} />
        </Route>
      </Route>

      {/* Manager routes */}
      <Route element={<ProtectedRoute allowedRoles={['MANAGER', 'ADMIN']} />}>
        <Route element={<TrainingManagerLayout />}>
          <Route path="/manager/course-approvals" element={<ManagerCourseApprovalPage />} />
          <Route path="/manager/content-approvals" element={<ManagerContentApprovalPage />} />
          <Route path="/manager/online-enrollments" element={<ManagerOnlineEnrollmentsPage />} />
        </Route>
        <Route path="/manager/classrooms" element={<Navigate to="/training-manager/classrooms" replace />} />
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
      <LearnerExperienceProvider>
        <AppRoutes />
      </LearnerExperienceProvider>
    </BrowserRouter>
  );
}

export default App;
