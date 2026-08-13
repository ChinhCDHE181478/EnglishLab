import React, { useEffect } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  getDefaultAuthenticatedPath,
  hasAccessToken,
  hasAnyUserRole,
  needsProfileCompletion,
} from '../../utils/auth';

const ProtectedRoute = ({ requireCompleteProfile = true, allowedRoles = null }) => {
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

  if (requireCompleteProfile && needsProfileCompletion(user)) {
    return <Navigate to="/complete-profile" replace />;
  }

  if (allowedRoles?.length && !hasAnyUserRole(user, allowedRoles)) {
    return <Navigate to={getDefaultAuthenticatedPath(user)} replace />;
  }

  if (
    !allowedRoles?.length &&
    !requireCompleteProfile &&
    !needsProfileCompletion(user) &&
    location.pathname === '/complete-profile'
  ) {
    return <Navigate to={getDefaultAuthenticatedPath(user)} replace />;
  }

  return <Outlet />;
};

export default ProtectedRoute;
