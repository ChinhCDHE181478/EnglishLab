import { Search, Bell, LogOut, User } from 'lucide-react';
import '../styles/home.css';

const HomeHeader = ({ user, onLogout }) => {
  const getInitials = (name) => {
    if (!name) return '?';
    return name
      .split(' ')
      .map((w) => w[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  return (
    <header className="dashboard-header">
      <div className="dashboard-header-left">
        <div className="header-greeting">
          <h2 className="header-greeting-title">
            Welcome back, <span className="header-greeting-name">{user?.fullName || 'Learner'}</span>!
          </h2>
          <p className="header-greeting-sub">
            Your English learning journey starts here.
          </p>
        </div>
      </div>

      <div className="dashboard-header-right">
        <div className="header-search">
          <Search size={16} className="header-search-icon" />
          <input
            type="text"
            className="header-search-input"
            placeholder="Search courses, lessons, tests..."
          />
        </div>

        <button className="header-icon-btn" aria-label="Notifications">
          <Bell size={18} />
          <span className="header-notif-dot" />
        </button>

        <div className="header-avatar" title={user?.fullName || ''}>
          {getInitials(user?.fullName)}
        </div>

        <button className="header-logout-btn" onClick={onLogout}>
          <LogOut size={16} />
          <span>Logout</span>
        </button>
      </div>
    </header>
  );
};

export default HomeHeader;
