export const getStoredUser = () => {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null');
  } catch {
    return null;
  }
};

export const hasAccessToken = () => Boolean(localStorage.getItem('accessToken'));

const STAFF_ROLES = ['TEACHER', 'TRAINING_MANAGER', 'CONTENT_MANAGER', 'MANAGER', 'ADMIN'];

export const getUserRoles = (user) => {
  const roles = Array.isArray(user?.roles) ? user.roles : [];
  const normalized = roles
    .map((role) => String(role || '').toUpperCase())
    .filter(Boolean);
  const primaryRole = String(user?.role || '').toUpperCase();
  if (primaryRole && !normalized.includes(primaryRole)) normalized.push(primaryRole);
  return normalized;
};

export const hasAnyUserRole = (user, roles) => {
  const assignedRoles = getUserRoles(user);
  return roles.some((role) => assignedRoles.includes(String(role || '').toUpperCase()));
};

export const needsProfileCompletion = (user) => {
  if (!user) return true;
  // Staff accounts are provisioned by admin — never require profile completion
  if (hasAnyUserRole(user, STAFF_ROLES)) return false;
  return !user.profileCompleted || !user.fullName || !user.phoneNumber || !user.targetExam;
};

export const isContentManagerUser = (user) =>
  hasAnyUserRole(user, ['CONTENT_MANAGER', 'MANAGER', 'ADMIN']);

export const clearSession = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('user');
  localStorage.removeItem('rememberMe');
};
