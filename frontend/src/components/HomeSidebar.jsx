import { useState } from 'react';
import {
  LayoutDashboard,
  BookOpen,
  Route,
  ClipboardList,
  FileCheck,
  Layers,
  BookOpenText,
  CalendarDays,
  BarChart3,
  HelpCircle,
  Menu,
  X,
  GraduationCap,
} from 'lucide-react';
import '../styles/home.css';

const NAV_ITEMS = [
  { key: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { key: 'courses', label: 'My Courses', icon: BookOpen },
  { key: 'path', label: 'Learning Path', icon: Route },
  { key: 'assignments', label: 'Assignments', icon: ClipboardList },
  { key: 'tests', label: 'Mock Tests', icon: FileCheck },
  { key: 'flashcards', label: 'Flashcards', icon: Layers },
  { key: 'dictionary', label: 'Dictionary', icon: BookOpenText },
  { key: 'schedule', label: 'Schedule', icon: CalendarDays },
  { key: 'reports', label: 'Reports', icon: BarChart3 },
  { key: 'support', label: 'Support', icon: HelpCircle },
];

const HomeSidebar = ({ activeItem = 'dashboard', onNavigate }) => {
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleClick = (key) => {
    onNavigate?.(key);
    setMobileOpen(false);
  };

  return (
    <>
      {/* Mobile toggle button */}
      <button
        className="sidebar-mobile-toggle"
        onClick={() => setMobileOpen(!mobileOpen)}
        aria-label="Toggle menu"
      >
        {mobileOpen ? <X size={22} /> : <Menu size={22} />}
      </button>

      {/* Overlay for mobile */}
      {mobileOpen && (
        <div
          className="sidebar-overlay"
          onClick={() => setMobileOpen(false)}
        />
      )}

      <aside className={`home-sidebar ${mobileOpen ? 'open' : ''}`}>
        {/* Brand */}
        <div className="sidebar-brand">
          <div className="sidebar-brand-icon">
            <GraduationCap size={20} />
          </div>
          <span className="sidebar-brand-text">EnglishLab</span>
        </div>

        {/* Navigation */}
        <nav className="sidebar-nav">
          {NAV_ITEMS.map(({ key, label, icon: Icon }) => (
            <button
              key={key}
              className={`sidebar-nav-item ${activeItem === key ? 'active' : ''}`}
              onClick={() => handleClick(key)}
            >
              <Icon size={18} />
              <span>{label}</span>
            </button>
          ))}
        </nav>
      </aside>
    </>
  );
};

export default HomeSidebar;
