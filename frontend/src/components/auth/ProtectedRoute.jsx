import React, { useEffect } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { hasAccessToken, hasAnyUserRole, needsPlacementTest, needsProfileCompletion } from '../../utils/auth';

const ProtectedRoute = ({ requireCompleteProfile = true, requirePlacementTest = false, allowedRoles = null }) => {
  const location = useLocation();
  const { refreshUser, status, user } = useAuth();

  useEffect(() => {
    let active = true;

    if (hasAccessToken()) {
      const refreshCurrentUser = async () => {
        try {
          await refreshUser();
        } catch {
          if (!active) return;
        }
      };

      refreshCurrentUser();
    }

    return () => {
      active = false;
    };
  }, [location.pathname, refreshUser]);

  if (!hasAccessToken() || status === 'guest') {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (status === 'checking' && !user) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#f9f9f9] font-['Inter'] text-[#584140]">
        Đang kiểm tra tài khoản...
      </div>
    );
  }

  if (requirePlacementTest && needsPlacementTest(user)) {
    return <Navigate to="/placement-test" replace />;
  }

  if (requireCompleteProfile && needsProfileCompletion(user)) {
    return <Navigate to="/complete-profile" replace />;
  }

  const getDefaultPage = (u) => {
    const role = String(u?.role || '').toUpperCase();
    if (role === 'ADMIN') return '/admin';
    if (role === 'CONTENT_MANAGER') return '/content-manager';
    if (role === 'STAFF') return '/staff';
    if (role === 'MANAGER') return '/manager/classroom-proposals';
    if (role === 'TEACHER') return '/teacher';
    return '/home';
  };

  if (allowedRoles?.length && !hasAnyUserRole(user, allowedRoles)) {
    return <Navigate to={getDefaultPage(user)} replace />;
  }

  if (
    !allowedRoles?.length &&
    !requireCompleteProfile &&
    !needsProfileCompletion(user) &&
    location.pathname === '/complete-profile'
  ) {
    return <Navigate to={getDefaultPage(user)} replace />;
  }

  return <Outlet />;
};

export default ProtectedRoute;
