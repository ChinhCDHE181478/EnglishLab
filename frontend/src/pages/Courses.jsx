import React, { useEffect, useState } from 'react';
import { getCurrentUser } from '../api/authApi';
import Header from '../components/ai-learning/Header';
import {
  CategoryTabs,
  CourseCatalog,
  CourseFooter,
  CourseGlobalStyles,
  CourseHero,
  CurrentCourse,
  FeaturedTeachers,
  FinalCourseCta,
  LearningPaths,
  PopularCourses,
  RecommendationBanner,
} from '../components/course';
import { getStoredUser, hasAccessToken } from '../utils/auth';

const Courses = () => {
  const [user, setUser] = useState(() => getStoredUser());

  useEffect(() => {
    let active = true;

    if (!hasAccessToken()) {
      return undefined;
    }

    getCurrentUser()
      .then((response) => {
        if (!active) return;
        localStorage.setItem('user', JSON.stringify(response.data));
        window.dispatchEvent(new Event('englishlab:user-updated'));
        setUser(response.data);
      })
      .catch(() => {
        if (!active) return;
        setUser(getStoredUser());
      });

    return () => {
      active = false;
    };
  }, []);

  return (
    <div className="min-h-screen overflow-x-hidden bg-[#f9f9f9] font-['Inter'] text-[#1a1c1c] antialiased">
      <CourseGlobalStyles />
      <Header />

      <main className="mx-auto max-w-[1320px] px-4 pb-20 pt-6 md:px-10">
        <CourseHero user={user} />
        <CurrentCourse />
        <CategoryTabs />
        <RecommendationBanner />
        <PopularCourses />
        <CourseCatalog />
        <LearningPaths />
        <FeaturedTeachers />
        <FinalCourseCta />
      </main>

      <CourseFooter />
    </div>
  );
};

export default Courses;
