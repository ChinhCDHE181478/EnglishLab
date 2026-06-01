import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import AuthLayout from './components/auth/AuthLayout';
import ProtectedRoute from './components/auth/ProtectedRoute';
import CompleteProfile from './pages/CompleteProfile';
import CourseDetail from './pages/CourseDetail';
import CourseWorkspace from './pages/CourseWorkspace';
import Courses from './pages/Courses';
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import ContentManagerRoutes from './pages/content-manager/ContentManagerRoutes';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route element={<ProtectedRoute requireCompleteProfile={false} allowedRoles={['CONTENT_MANAGER', 'MANAGER', 'ADMIN']} />}>
          <Route path="/content-manager/*" element={<ContentManagerRoutes />} />
        </Route>
        <Route path="/courses" element={<Courses />} />
        <Route path="/courses/:slugOrId" element={<CourseDetail />} />
        <Route path="/courses/:slugOrId/learn" element={<CourseWorkspace />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/home" element={<Home />} />
        </Route>
        <Route element={<ProtectedRoute requireCompleteProfile={false} />}>
          <Route path="/complete-profile" element={<CompleteProfile />} />
        </Route>
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
