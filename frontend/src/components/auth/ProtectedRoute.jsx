import React, { useEffect, useState } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { getCurrentUser } from '../../api/authApi';
import { getStoredUser, hasAccessToken, hasAnyUserRole, needsProfileCompletion } from '../../utils/auth';

const ProtectedRoute = ({ requireCompleteProfile = true, allowedRoles = null }) => {
  const location = useLocation();
  const [user, setUser] = useState(() => getStoredUser());
  const [status, setStatus] = useState(() => (hasAccessToken() && getStoredUser() ? 'authenticated' : 'checking'));

  useEffect(() => {
    let active = true;

    if (!hasAccessToken()) {
      setStatus('guest');
      return undefined;
    }

    if (getStoredUser()) {
      setStatus('authenticated');
    }

    getCurrentUser()
      .then((response) => {
        if (!active) return;
        localStorage.setItem('user', JSON.stringify(response.data));
        setUser(response.data);
        setStatus('authenticated');
      })
      .catch(() => {
        if (!active) return;
        setStatus('guest');
      });

    return () => {
      active = false;
    };
  }, [location.pathname]);

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

  const getDefaultPage = (u) => {
    const role = String(u?.role || '').toUpperCase();
    if (['TEACHER', 'TRAINING_MANAGER', 'MANAGER', 'ADMIN'].includes(role)) return '/teacher';
    if (role === 'CONTENT_MANAGER') return '/content-manager';
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
