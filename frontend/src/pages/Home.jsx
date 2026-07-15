import React from 'react';
import AILearningSection from '../components/ai-learning/AILearningSection';
import CoursesSection from '../components/ai-learning/CoursesSection';
import CTASection from '../components/ai-learning/CTASection';
import CourseFooter from '../components/course/CourseFooter';
import Header from '../components/ai-learning/Header';
import HeroSection from '../components/ai-learning/HeroSection';
import MarqueeRibbon from '../components/ai-learning/MarqueeRibbon';
import TeachersSection from '../components/ai-learning/TeachersSection';
import TestimonialsSection from '../components/ai-learning/TestimonialsSection';

const HomePage = () => (
  <div className="flex min-h-screen flex-col bg-[#f9f9f9] font-['Inter'] text-[#1a1c1c] antialiased">
    <Header />
    <main className="flex-1">
      <HeroSection />
      <MarqueeRibbon />
      <AILearningSection />
      <CoursesSection />
      <TeachersSection />
      <TestimonialsSection />
      <CTASection />
    </main>
    <CourseFooter />
  </div>
);

export default HomePage;
