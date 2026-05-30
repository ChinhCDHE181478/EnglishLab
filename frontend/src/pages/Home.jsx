import React from 'react';
import AILearningSection from '../components/ai-learning/AILearningSection';
import CoursesSection from '../components/ai-learning/CoursesSection';
import CTASection from '../components/ai-learning/CTASection';
import Footer from '../components/ai-learning/Footer';
import Header from '../components/ai-learning/Header';
import HeroSection from '../components/ai-learning/HeroSection';
import MarqueeRibbon from '../components/ai-learning/MarqueeRibbon';
import TeachersSection from '../components/ai-learning/TeachersSection';
import TestimonialsSection from '../components/ai-learning/TestimonialsSection';

const HomePage = () => (
  <div className="min-h-screen bg-[#f9f9f9] font-['Inter'] text-[#1a1c1c] antialiased">
    <Header />
    <main>
      <HeroSection />
      <MarqueeRibbon />
      <AILearningSection />
      <CoursesSection />
      <TeachersSection />
      <TestimonialsSection />
      <CTASection />
    </main>
    <Footer />
  </div>
);

export default HomePage;
