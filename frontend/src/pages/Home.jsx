import { useEffect, useState } from 'react';
import homeApi from '../api/homeApi';
import AILearningSection from '../components/ai-learning/AILearningSection';
import CoursesSection from '../components/ai-learning/CoursesSection';
import CTASection from '../components/ai-learning/CTASection';
import CourseFooter from '../components/course/CourseFooter';
import Header from '../components/ai-learning/Header';
import HeroSection from '../components/ai-learning/HeroSection';
import MarqueeRibbon from '../components/ai-learning/MarqueeRibbon';
import TeachersSection from '../components/ai-learning/TeachersSection';
import TestimonialsSection from '../components/ai-learning/TestimonialsSection';

const HomePage = () => {
  const [homeContent, setHomeContent] = useState({ teachers: [], testimonials: [] });
  const [loadingHighlights, setLoadingHighlights] = useState(true);

  useEffect(() => {
    let active = true;

    const loadHomeContent = async () => {
      try {
        const data = await homeApi.getHomePage();
        if (active) {
          setHomeContent({
            teachers: Array.isArray(data.teachers) ? data.teachers : [],
            testimonials: Array.isArray(data.testimonials) ? data.testimonials : [],
          });
        }
      } catch {
        if (active) setHomeContent({ teachers: [], testimonials: [] });
      } finally {
        if (active) setLoadingHighlights(false);
      }
    };

    loadHomeContent();
    return () => {
      active = false;
    };
  }, []);

  return (
    <div className="flex min-h-screen flex-col bg-[#f9f9f9] font-['Inter'] text-[#1a1c1c] antialiased">
      <Header />
      <main className="flex-1">
        <HeroSection />
        <MarqueeRibbon />
        <AILearningSection />
        <CoursesSection />
        <TeachersSection loading={loadingHighlights} teachers={homeContent.teachers} />
        <TestimonialsSection loading={loadingHighlights} testimonials={homeContent.testimonials} />
        <CTASection />
      </main>
      <CourseFooter />
    </div>
  );
};

export default HomePage;
