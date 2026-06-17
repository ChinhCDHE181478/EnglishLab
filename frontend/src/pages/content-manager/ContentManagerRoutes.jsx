import { Navigate, Route, Routes } from 'react-router-dom';
import { ContentManagerLayout } from '../../components/content-manager/ContentManagerUi';
import ContentManagerCourseBuilderPage from './ContentManagerCourseBuilderPage';
import ContentManagerCourseEditorPage from './ContentManagerCourseEditorPage';
import ContentManagerCoursesPage from './ContentManagerCoursesPage';
import ContentManagerDashboardPage from './ContentManagerDashboardPage';
import ContentManagerDiscountCodesPage from './ContentManagerDiscountCodesPage';
import ContentManagerStaticPage from './ContentManagerStaticPage';

export default function ContentManagerRoutes() {
  return (
    <ContentManagerLayout>
      <Routes>
        <Route index element={<Navigate replace to="dashboard" />} />
        <Route path="dashboard" element={<ContentManagerDashboardPage />} />
        <Route path="courses" element={<ContentManagerCoursesPage />} />
        <Route path="courses/new" element={<ContentManagerCourseEditorPage />} />
        <Route path="courses/:slugOrId/edit" element={<ContentManagerCourseEditorPage />} />
        <Route path="courses/:slugOrId/builder" element={<ContentManagerCourseBuilderPage />} />
        <Route path="discount-codes" element={<ContentManagerDiscountCodesPage />} />
        <Route path="materials" element={<ContentManagerStaticPage pageKey="materials" />} />
        <Route path="flashcards" element={<ContentManagerStaticPage pageKey="flashcards" />} />
        <Route path="listening" element={<ContentManagerStaticPage pageKey="listening" />} />
        <Route path="writing" element={<ContentManagerStaticPage pageKey="writing" />} />
        <Route path="syllabus" element={<ContentManagerStaticPage pageKey="syllabus" />} />
        <Route path="mock-exams" element={<ContentManagerStaticPage pageKey="mockExams" />} />
        <Route path="publication" element={<ContentManagerStaticPage pageKey="publication" />} />
        <Route path="analytics" element={<ContentManagerStaticPage pageKey="analytics" />} />
        <Route path="settings" element={<ContentManagerStaticPage pageKey="settings" />} />
        <Route path="categories" element={<ContentManagerStaticPage pageKey="categories" />} />
        <Route path="*" element={<Navigate replace to="dashboard" />} />
      </Routes>
    </ContentManagerLayout>
  );
}
