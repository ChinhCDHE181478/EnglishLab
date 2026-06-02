import React, { useEffect, useState } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { getCurrentUser } from '../../api/authApi';
import { getStoredUser, hasAccessToken, needsProfileCompletion } from '../../utils/auth';

const ProtectedRoute = ({ requireCompleteProfile = true }) => {
  const location = useLocation();
  const [status, setStatus] = useState('checking');
  const [user, setUser] = useState(() => getStoredUser());

  useEffect(() => {
    let active = true;

    if (!hasAccessToken()) {
      setStatus('guest');
      return undefined;
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

  if (status === 'checking') {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#f9f9f9] font-['Inter'] text-[#584140]">
        Đang kiểm tra tài khoản...
      </div>
    );
  }

  if (requireCompleteProfile && needsProfileCompletion(user)) {
    return <Navigate to="/complete-profile" replace />;
  }

  if (!requireCompleteProfile && !needsProfileCompletion(user)) {
    return <Navigate to="/home" replace />;
  }

  return <Outlet />;
};

export default ProtectedRoute;
