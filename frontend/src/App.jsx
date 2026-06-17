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
import Register from './pages/Register';
import ResetPassword from './pages/ResetPassword';
import TransactionHistoryPage from './pages/TransactionHistoryPage';
import VerifyEmail from './pages/VerifyEmail';
import WishlistPage from './pages/WishlistPage';
import ContentManagerRoutes from './pages/content-manager/ContentManagerRoutes';

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
      <Route path="/courses" element={<Courses />} />
      <Route path="/courses/:slugOrId" element={<CourseDetailRoute />} />
      <Route path="/courses/:slugOrId/home" element={<CourseHomeRoute />} />
      <Route path="/courses/:slugOrId/learn" element={<CourseWorkspaceRoute />} />
      <Route path="/cart" element={<CartPage />} />
      <Route path="/wishlist" element={<WishlistPage />} />
      <Route path="/checkout" element={<CheckoutPage />} />
      <Route path="/notifications" element={<NotificationsPage />} />
      <Route path="/my-courses" element={<MyCoursesPage />} />
      <Route path="/transaction-history" element={<TransactionHistoryPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/home" element={<Home />} />
      </Route>
      <Route element={<ProtectedRoute requireCompleteProfile={false} />}>
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
