import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  BookOpen,
  TrendingUp,
  ClipboardList,
  FileCheck,
  PlayCircle,
  CalendarDays,
  Sparkles,
  Headphones,
  BookOpenText,
  Layers,
  BarChart3,
  Clock,
  ChevronRight,
  Zap,
} from 'lucide-react';
import { getCurrentUser, getHomeMessage } from '../api/authApi';
import HomeSidebar from '../components/HomeSidebar';
import HomeHeader from '../components/HomeHeader';
import StatCard from '../components/StatCard';
import DashboardCard from '../components/DashboardCard';
import ProgressBar from '../components/ProgressBar';
import '../styles/home.css';

const Home = () => {
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [homeMessage, setHomeMessage] = useState('Welcome to EnglishLab.');
  const [homeDescription, setHomeDescription] = useState('');
  const [loading, setLoading] = useState(true);
  const [activeNav, setActiveNav] = useState('dashboard');

  useEffect(() => {
    let cancelled = false;

    const fetchData = async () => {
      /* Fetch current user */
      try {
        const res = await getCurrentUser();
        if (!cancelled) setUser(res.data);
      } catch {
        const stored = localStorage.getItem('user');
        if (stored) {
          try {
            if (!cancelled) setUser(JSON.parse(stored));
          } catch {
            handleLogout();
            return;
          }
        } else {
          handleLogout();
          return;
        }
      }

      /* Fetch home message */
      try {
        const res = await getHomeMessage();
        if (!cancelled) {
          setHomeMessage(res.data?.message || 'Welcome to EnglishLab.');
          setHomeDescription(res.data?.description || '');
        }
      } catch {
        /* keep fallback */
      }

      if (!cancelled) setLoading(false);
    };

    fetchData();
    return () => { cancelled = true; };
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('user');
    navigate('/login');
  };

  const skillData = useMemo(() => [
    { label: 'Listening', value: 72, icon: Headphones },
    { label: 'Reading', value: 68, icon: BookOpenText },
    { label: 'Writing', value: 60, icon: ClipboardList },
    { label: 'Speaking', value: 55, icon: BarChart3 },
  ], []);

  if (loading) {
    return (
      <div className="home-loading">
        <div className="loading-spinner" />
        <p>Loading your dashboard...</p>
      </div>
    );
  }

  return (
    <div className="home-page">
      {/* Decorative leaves */}
      <div className="home-leaf home-leaf-1" />
      <div className="home-leaf home-leaf-2" />
      <div className="home-leaf home-leaf-3" />

      <HomeSidebar activeItem={activeNav} onNavigate={setActiveNav} />

      <div className="home-main">
        <HomeHeader user={user} onLogout={handleLogout} />

        <main className="home-content">
          {/* Backend message banner */}
          {homeMessage && (
            <div className="home-banner">
              <Sparkles size={16} />
              <div className="home-banner-text">
                <span className="home-banner-msg">{homeMessage}</span>
                {homeDescription && (
                  <span className="home-banner-desc">{homeDescription}</span>
                )}
              </div>
            </div>
          )}

          {/* Welcome hero card */}
          <section className="welcome-hero">
            <div className="welcome-hero-text">
              <span className="welcome-hero-badge">
                <Zap size={12} />
                TOEIC Foundation
              </span>
              <h1 className="welcome-hero-title">Continue your English journey</h1>
              <p className="welcome-hero-desc">
                Pick up where you left off and keep improving your TOEIC and IELTS skills.
              </p>
              <button className="btn-primary">
                <PlayCircle size={18} />
                Continue Learning
              </button>
            </div>
            <div className="welcome-hero-visual">
              <div className="hero-circle hero-circle-lg" />
              <div className="hero-circle hero-circle-sm" />
              <div className="hero-circle hero-circle-xs" />
              <div className="hero-icon">📚</div>
            </div>
          </section>

          {/* Quick stats */}
          <section className="stats-row">
            <StatCard icon={BookOpen} label="Current Course" value="TOEIC Foundation" accent="accent-green" />
            <StatCard icon={TrendingUp} label="Learning Progress" value="65%" accent="accent-blue" />
            <StatCard icon={ClipboardList} label="Pending Assignments" value="2" accent="accent-orange" />
            <StatCard icon={FileCheck} label="Upcoming Mock Tests" value="1" accent="accent-purple" />
          </section>

          {/* Dashboard grid */}
          <div className="dashboard-grid">
            {/* Continue Learning */}
            <DashboardCard title="Continue Learning" icon={PlayCircle} className="card-continue">
              <div className="continue-course-name">TOEIC Foundation</div>
              <div className="continue-lesson">
                <BookOpenText size={14} className="continue-lesson-icon" />
                Unit 5 — Listening for Details
              </div>
              <div className="continue-progress-row">
                <ProgressBar value={65} height={10} />
                <span className="continue-progress-pct">65%</span>
              </div>
              <button className="btn-primary btn-sm">
                <PlayCircle size={15} />
                Resume Lesson
              </button>
            </DashboardCard>

            {/* Today's Schedule */}
            <DashboardCard title="Today's Schedule" icon={CalendarDays} className="card-schedule">
              <div className="schedule-item">
                <div className="schedule-time-badge">
                  <Clock size={14} />
                  <span>19:30</span>
                </div>
                <div className="schedule-detail">
                  <div className="schedule-name">TOEIC Listening Practice</div>
                  <div className="schedule-meta">Teacher: Mr. Long</div>
                  <div className="schedule-meta">Mode: Online Class</div>
                </div>
              </div>
              <button className="btn-outline btn-sm">
                View Schedule
                <ChevronRight size={14} />
              </button>
            </DashboardCard>

            {/* Pending Assignments */}
            <DashboardCard title="Pending Assignments" icon={ClipboardList} className="card-assignments">
              <ul className="task-list">
                <li className="task-item">
                  <div className="task-left">
                    <div className="task-dot urgent" />
                    <span className="task-name">TOEIC Part 2 Practice</span>
                  </div>
                  <span className="task-due urgent">Due tonight</span>
                </li>
                <li className="task-item">
                  <div className="task-left">
                    <div className="task-dot" />
                    <span className="task-name">Vocabulary Unit 5</span>
                  </div>
                  <span className="task-due">Due tomorrow</span>
                </li>
              </ul>
              <button className="btn-outline btn-sm">
                View Assignments
                <ChevronRight size={14} />
              </button>
            </DashboardCard>

            {/* Recommended Practice */}
            <DashboardCard title="Recommended Practice" icon={Sparkles} className="card-recommended">
              <ul className="practice-list">
                <li className="practice-item">
                  <div className="practice-item-icon-wrap">
                    <Headphones size={16} />
                  </div>
                  <span>TOEIC Part 3 Listening</span>
                  <ChevronRight size={14} className="practice-chevron" />
                </li>
                <li className="practice-item">
                  <div className="practice-item-icon-wrap">
                    <BookOpenText size={16} />
                  </div>
                  <span>Grammar: Conditional Sentences</span>
                  <ChevronRight size={14} className="practice-chevron" />
                </li>
                <li className="practice-item">
                  <div className="practice-item-icon-wrap">
                    <Layers size={16} />
                  </div>
                  <span>Flashcards: Business Vocabulary</span>
                  <ChevronRight size={14} className="practice-chevron" />
                </li>
              </ul>
              <button className="btn-outline btn-sm">
                Start Practice
                <ChevronRight size={14} />
              </button>
            </DashboardCard>

            {/* Skill Overview */}
            <DashboardCard title="Skill Overview" icon={BarChart3} className="card-skills">
              <div className="skill-list">
                {skillData.map(({ label, value, icon: SkillIcon }) => (
                  <div className="skill-row" key={label}>
                    <div className="skill-label">
                      <SkillIcon size={15} />
                      <span>{label}</span>
                    </div>
                    <div className="skill-bar-area">
                      <ProgressBar value={value} height={8} />
                    </div>
                    <span className="skill-pct">{value}%</span>
                  </div>
                ))}
              </div>
            </DashboardCard>
          </div>
        </main>
      </div>
    </div>
  );
};

export default Home;
