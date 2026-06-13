export const getStoredUser = () => {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null');
  } catch {
    return null;
  }
};

export const hasAccessToken = () => Boolean(localStorage.getItem('accessToken'));

export const needsProfileCompletion = (user) => {
  if (!user) {
    return true;
  }

  return !user.profileCompleted || !user.fullName || !user.phoneNumber || !user.targetExam;
};

export const isContentManagerUser = (user) =>
  ['CONTENT_MANAGER', 'MANAGER', 'ADMIN'].includes(String(user?.role || '').toUpperCase());

export const clearSession = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('user');
  localStorage.removeItem('rememberMe');
};
