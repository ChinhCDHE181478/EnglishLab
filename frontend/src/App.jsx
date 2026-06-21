import { BrowserRouter, Navigate, Route, Routes, useLocation, useParams } from 'react-router-dom';
import AuthLayout from './components/auth/AuthLayout';
import ProtectedRoute from './components/auth/ProtectedRoute';
import { LearnerExperienceProvider } from './context/LearnerExperienceContext';
import CartPage from './pages/CartPage';
import CheckoutPage from './pages/CheckoutPage';
import CompleteProfile from './pages/CompleteProfile';
import CourseDetail from './pages/CourseDetail';
import CourseHome from './pages/CourseHome';
import CourseWorkspace from './pages/CourseWorkspace';
import Courses from './pages/Courses';
import ForgotPassword from './pages/ForgotPassword';
import Home from './pages/Home';
import Login from './pages/Login';
import MyCoursesPage from './pages/MyCoursesPage';
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
import MySchedulePage from './pages/classroom/MySchedulePage';
import MyHomeworkPage from './pages/classroom/MyHomeworkPage';
import TeacherDashboardPage from './pages/teacher/TeacherDashboardPage';
import TeacherClassroomPage from './pages/teacher/TeacherClassroomPage';
import TeacherSessionPage from './pages/teacher/TeacherSessionPage';
import TeacherRequestsPage from './pages/teacher/TeacherRequestsPage';
import TeacherSchedulePage from './pages/teacher/TeacherSchedulePage';
import TrainingManagerRequestsPage from './pages/training-manager/TrainingManagerRequestsPage';
import TrainingManagerClassroomRegistrationsPage from './pages/training-manager/TrainingManagerClassroomRegistrationsPage';
import ManagerClassroomsPage from './pages/manager/ManagerClassroomsPage';

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
    <Routes location={location} key={location.pathname}>
      <Route path="/" element={<Home />} />
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
        <Route path="/my-classrooms" element={<MyClassroomsPage />} />
        <Route path="/my-classrooms/:id" element={<MyClassroomDetailPage />} />
        <Route path="/my-schedule" element={<MySchedulePage />} />
        <Route path="/my-homework" element={<MyHomeworkPage />} />
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
        <Route path="/training-manager/requests" element={<TrainingManagerRequestsPage />} />
        <Route path="/training-manager/classroom-registrations" element={<TrainingManagerClassroomRegistrationsPage />} />
      </Route>

      {/* Manager routes */}
      <Route element={<ProtectedRoute allowedRoles={['MANAGER', 'ADMIN']} />}>
        <Route path="/manager/classrooms" element={<ManagerClassroomsPage />} />
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
