import { Navigate, Route, Routes, useParams } from 'react-router-dom';
import { ContentManagerLayout } from '../../components/content-manager/ContentManagerUi';
import ContentManagerCourseBuilderPage from './ContentManagerCourseBuilderPage';
import ContentManagerCourseEditorPage from './ContentManagerCourseEditorPage';
import ContentManagerCoursesPage from './ContentManagerCoursesPage';
import ContentManagerDashboardPage from './ContentManagerDashboardPage';
import ContentManagerDiscountCodesPage from './ContentManagerDiscountCodesPage';
import ContentManagerClassroomsPage from './ContentManagerClassroomsPage';
import ContentManagerFlashcardsPage from './ContentManagerFlashcardsPage';
import ContentManagerAssessmentsHubPage from './ContentManagerAssessmentsHubPage';
import ContentManagerMaterialsPage from './ContentManagerMaterialsPage';
import ContentManagerCategoriesPage from './ContentManagerCategoriesPage';
import ContentManagerLearningPathsPage from './ContentManagerLearningPathsPage';
import ContentManagerPublicationPage from './ContentManagerPublicationPage';
import ContentManagerAnalyticsPage from './ContentManagerAnalyticsPage';
import ContentManagerPlacementTestPage from './ContentManagerPlacementTestPage';
import ContentManagerRubricsPage from './ContentManagerRubricsPage';
import ContentManagerExerciseBankPage from './ContentManagerExerciseBankPage';
import ContentManagerTrainingProgramsPage from './ContentManagerTrainingProgramsPage';
import ContentManagerSyllabusBuilderPage from './ContentManagerSyllabusBuilderPage';
import ContentManagerDiscussionModerationPage from './ContentManagerDiscussionModerationPage';

export default function ContentManagerRoutes() {
  return (
    <ContentManagerLayout>
      <Routes>
        <Route index element={<Navigate replace to="dashboard" />} />
        <Route path="dashboard" element={<ContentManagerDashboardPage />} />
        <Route path="courses" element={<ContentManagerCoursesPage />} />
        <Route path="courses/:slugOrId" element={<Navigate replace to="edit" />} />
        <Route path="classrooms" element={<ContentManagerClassroomsPage />} />
        <Route path="offline-programs" element={<ContentManagerTrainingProgramsPage mode="OFFLINE" />} />
        <Route path="virtual-programs" element={<ContentManagerTrainingProgramsPage mode="VIRTUAL" />} />
        <Route path="syllabus-builder" element={<ContentManagerSyllabusBuilderPage />} />
        <Route path="exercise-bank" element={<ContentManagerExerciseBankPage />} />
        <Route path="courses/new" element={<ContentManagerCourseEditorPage />} />
        <Route path="courses/:slugOrId/edit" element={<ContentManagerCourseEditorPage />} />
        <Route path="courses/:slugOrId/builder" element={<ContentManagerCourseBuilderPage />} />
        <Route path="discount-codes" element={<ContentManagerDiscountCodesPage />} />
        <Route path="materials" element={<ContentManagerMaterialsPage />} />
        <Route path="flashcards" element={<ContentManagerFlashcardsPage />} />
        <Route path="flashcards/:courseSlug" element={<ContentManagerFlashcardsPage />} />
        <Route path="flashcards/:courseSlug/modules/:moduleId" element={<LegacyFlashcardsModuleRedirect />} />
        <Route path="listening" element={<ContentManagerAssessmentsHubPage pageKey="listening" />} />
        <Route path="reading" element={<ContentManagerAssessmentsHubPage pageKey="reading" />} />
        <Route path="writing" element={<ContentManagerAssessmentsHubPage pageKey="writing" />} />
        <Route path="speaking" element={<ContentManagerAssessmentsHubPage pageKey="speaking" />} />
        <Route path="placement-test" element={<ContentManagerPlacementTestPage />} />
        <Route path="rubrics" element={<ContentManagerRubricsPage />} />
        <Route path="learning-paths" element={<ContentManagerLearningPathsPage />} />
        <Route path="syllabus" element={<Navigate replace to="../learning-paths" />} />
        <Route path="mock-exams" element={<ContentManagerAssessmentsHubPage pageKey="mockExams" />} />
        <Route path="publication" element={<ContentManagerPublicationPage />} />
        <Route path="analytics" element={<ContentManagerAnalyticsPage />} />
        <Route path="discussion-moderation" element={<ContentManagerDiscussionModerationPage />} />
        <Route path="settings" element={<Navigate replace to="../dashboard" />} />
        <Route path="categories" element={<ContentManagerCategoriesPage />} />
        <Route path="*" element={<Navigate replace to="dashboard" />} />
      </Routes>
    </ContentManagerLayout>
  );
}

function LegacyFlashcardsModuleRedirect() {
  const { courseSlug, moduleId } = useParams();
  return <Navigate replace to={`/content-manager/flashcards/${courseSlug}?module=${moduleId}`} />;
}
