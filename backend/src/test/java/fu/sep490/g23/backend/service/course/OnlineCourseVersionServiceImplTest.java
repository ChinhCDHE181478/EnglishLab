package fu.sep490.g23.backend.service.course;

import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.response.course.LessonResponse;
import fu.sep490.g23.backend.dto.response.course.ModuleResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCoursePreviewWarningResponse;
import fu.sep490.g23.backend.dto.request.course.CreateCourseVersionRequest;
import fu.sep490.g23.backend.dto.response.curriculum.FlashcardSetResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.CourseAssessment;
import fu.sep490.g23.backend.entity.course.OnlineCourseModule;
import fu.sep490.g23.backend.entity.course.OnlineLesson;
import fu.sep490.g23.backend.entity.course.LessonProgress;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.enums.CourseVersionStatus;
import fu.sep490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sep490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseVersionRepository;
import fu.sep490.g23.backend.repository.course.OnlineLessonRepository;
import fu.sep490.g23.backend.repository.course.LessonProgressRepository;
import fu.sep490.g23.backend.service.course.impl.OnlineCourseVersionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OnlineCourseVersionServiceImplTest {
    @Mock
    private OnlineCourseRepository onlineCourseRepository;
    @Mock
    private OnlineCourseVersionRepository versionRepository;
    @Mock
    private CourseAssessmentRepository courseAssessmentRepository;
    @Mock
    private AssessmentSubmissionRepository assessmentSubmissionRepository;
    @Mock
    private LessonProgressRepository lessonProgressRepository;
    @Mock
    private OnlineLessonRepository lessonRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OnlineCourseMapper mapper;
    @Mock
    private OnlineCoursePreviewValidator previewValidator;

    private OnlineCourseVersionServiceImpl service;
    private OnlineCourse course;
    private OnlineCourseVersion versionOne;
    private OnlineCourseVersion versionTwo;

    @BeforeEach
    void setUp() throws Exception {
        service = new OnlineCourseVersionServiceImpl(
                onlineCourseRepository,
                versionRepository,
                courseAssessmentRepository,
                assessmentSubmissionRepository,
                lessonProgressRepository,
                lessonRepository,
                userRepository,
                mapper,
                previewValidator
        );
        course = OnlineCourse.builder()
                .id(21L)
                .title("IELTS Foundation")
                .status(PackageStatus.PUBLISHED)
                .modules(new ArrayList<>())
                .build();
        versionOne = OnlineCourseVersion.builder()
                .id(31L)
                .onlineCourse(course)
                .versionNumber(1)
                .status(CourseVersionStatus.PUBLISHED)
                .contentSnapshotJson(new ObjectMapper().writeValueAsString(
                        OnlineCourseResponse.builder().id(21L).title("Nội dung v1").modules(new ArrayList<>()).build()
                ))
                .totalRequiredLessons(10)
                .build();
        versionTwo = OnlineCourseVersion.builder()
                .id(32L)
                .onlineCourse(course)
                .versionNumber(2)
                .status(CourseVersionStatus.DRAFT)
                .contentSnapshotJson("{}")
                .build();
    }

    @Test
    void previewReturnsExactlyTheSelectedVersionSnapshot() {
        User contentManager = User.builder().id(4L).email("content@test.com").fullName("Content Manager").build();
        contentManager.setRoles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.CONTENT_MANAGER));
        when(userRepository.findByEmail(contentManager.getEmail())).thenReturn(Optional.of(contentManager));
        when(onlineCourseRepository.findWithModulesById(course.getId())).thenReturn(Optional.of(course));
        when(versionRepository.findByIdAndOnlineCourseId(versionOne.getId(), course.getId()))
                .thenReturn(Optional.of(versionOne));
        when(courseAssessmentRepository.findAllById(anyList())).thenReturn(List.of());
        when(previewValidator.validate(any(), anyList())).thenReturn(List.of());

        var preview = service.getVersionPreview(course.getId(), versionOne.getId(), contentManager.getEmail());

        assertThat(preview.getCourse().getTitle()).isEqualTo("Nội dung v1");
        assertThat(preview.getModules()).isEmpty();
        assertThat(preview.isPreviewMode()).isTrue();
    }

    @Test
    void previewCanReadSnapshotContainingFlashcardSets() throws Exception {
        User contentManager = User.builder().id(4L).email("content@test.com").fullName("Content Manager").build();
        contentManager.setRoles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.CONTENT_MANAGER));
        FlashcardSetResponse flashcardSet = FlashcardSetResponse.builder()
                .id(71L)
                .title("IELTS Listening Vocabulary")
                .cardsJson("[]")
                .build();
        LessonResponse lesson = LessonResponse.builder()
                .id(61L)
                .title("OnlineLesson có flashcard")
                .flashcardSets(List.of(flashcardSet))
                .transcriptSegments(List.of())
                .build();
        ModuleResponse module = ModuleResponse.builder()
                .id(51L)
                .title("Module 1")
                .lessons(List.of(lesson))
                .build();
        versionOne.setContentSnapshotJson(new ObjectMapper().findAndRegisterModules().writeValueAsString(
                OnlineCourseResponse.builder()
                        .id(course.getId())
                        .title("Nội dung có flashcard")
                        .modules(List.of(module))
                        .build()
        ));
        when(userRepository.findByEmail(contentManager.getEmail())).thenReturn(Optional.of(contentManager));
        when(onlineCourseRepository.findWithModulesById(course.getId())).thenReturn(Optional.of(course));
        when(versionRepository.findByIdAndOnlineCourseId(versionOne.getId(), course.getId()))
                .thenReturn(Optional.of(versionOne));
        when(courseAssessmentRepository.findAllById(anyList())).thenReturn(List.of());
        when(previewValidator.validate(any(), anyList())).thenReturn(List.of());

        var preview = service.getVersionPreview(course.getId(), versionOne.getId(), contentManager.getEmail());

        assertThat(preview.getModules()).singleElement()
                .satisfies(item -> assertThat(item.getLessons()).singleElement()
                        .satisfies(snapshotLesson -> assertThat(snapshotLesson.getFlashcardSets()).singleElement()
                                .extracting(FlashcardSetResponse::getTitle)
                                .isEqualTo("IELTS Listening Vocabulary")));
    }

    @Test
    void previewIgnoresFieldsFromOlderSnapshotSchema() {
        User contentManager = User.builder().id(4L).email("content@test.com").fullName("Content Manager").build();
        contentManager.setRoles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.CONTENT_MANAGER));
        versionOne.setContentSnapshotJson(
                "{\"id\":21,\"title\":\"Nội dung cũ\",\"legacyField\":\"không còn dùng\",\"modules\":[]}"
        );
        when(userRepository.findByEmail(contentManager.getEmail())).thenReturn(Optional.of(contentManager));
        when(onlineCourseRepository.findWithModulesById(course.getId())).thenReturn(Optional.of(course));
        when(versionRepository.findByIdAndOnlineCourseId(versionOne.getId(), course.getId()))
                .thenReturn(Optional.of(versionOne));
        when(courseAssessmentRepository.findAllById(anyList())).thenReturn(List.of());
        when(previewValidator.validate(any(), anyList())).thenReturn(List.of());

        var preview = service.getVersionPreview(course.getId(), versionOne.getId(), contentManager.getEmail());

        assertThat(preview.getCourse().getTitle()).isEqualTo("Nội dung cũ");
    }

    @Test
    void invalidDraftSnapshotFallsBackToCurrentEditableContent() {
        User contentManager = User.builder().id(4L).email("content@test.com").fullName("Content Manager").build();
        contentManager.setRoles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.CONTENT_MANAGER));
        versionTwo.setContentSnapshotJson("{invalid-json");
        when(userRepository.findByEmail(contentManager.getEmail())).thenReturn(Optional.of(contentManager));
        when(onlineCourseRepository.findWithModulesById(course.getId())).thenReturn(Optional.of(course));
        when(versionRepository.findByIdAndOnlineCourseId(versionTwo.getId(), course.getId()))
                .thenReturn(Optional.of(versionTwo));
        when(mapper.toResponse(course)).thenReturn(
                OnlineCourseResponse.builder().id(course.getId()).title("Nội dung bản nháp hiện tại").build()
        );

        var version = service.getVersion(course.getId(), versionTwo.getId(), contentManager.getEmail());

        assertThat(version.getContent().getTitle()).isEqualTo("Nội dung bản nháp hiện tại");
    }

    @Test
    void publishRetiresOldVersionWhileExistingEnrollmentKeepsPinnedSnapshot() {
        User contentManager = User.builder().id(3L).email("content@test.com").fullName("Content Manager").build();
        contentManager.setRoles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.CONTENT_MANAGER));
        when(userRepository.findByEmail(contentManager.getEmail())).thenReturn(Optional.of(contentManager));
        when(onlineCourseRepository.findWithModulesById(course.getId())).thenReturn(Optional.of(course));
        when(versionRepository.findByIdAndOnlineCourseId(versionTwo.getId(), course.getId()))
                .thenReturn(Optional.of(versionTwo));
        when(versionRepository.findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(
                course,
                CourseVersionStatus.PUBLISHED
        )).thenReturn(Optional.of(versionOne), Optional.of(versionTwo));
        when(courseAssessmentRepository.countByOnlineCourseAndActiveTrue(course)).thenReturn(0L);
        when(mapper.toResponse(course)).thenReturn(
                OnlineCourseResponse.builder().id(course.getId()).title("Nội dung v2").modules(new ArrayList<>()).build()
        );

        service.publish(course.getId(), versionTwo.getId(), contentManager.getEmail());

        OnlineCourseEnrollment existingEnrollment = OnlineCourseEnrollment.builder()
                .id(41L)
                .courseVersion(versionOne)
                .progressPercent(80)
                .build();
        OnlineCourseResponse pinnedContent = service.readLatestPublishedForEnrollment(existingEnrollment, course);
        OnlineCourseEnrollment newEnrollment = OnlineCourseEnrollment.builder()
                .id(42L)
                .courseVersion(service.requirePublishedVersion(course))
                .progressPercent(0)
                .build();

        assertThat(versionOne.getStatus()).isEqualTo(CourseVersionStatus.RETIRED);
        assertThat(versionTwo.getStatus()).isEqualTo(CourseVersionStatus.PUBLISHED);
        assertThat(existingEnrollment.getCourseVersion()).isSameAs(versionOne);
        assertThat(newEnrollment.getCourseVersion()).isSameAs(versionTwo);
        assertThat(pinnedContent.getTitle()).isEqualTo("Nội dung v1");
        assertThat(pinnedContent.getProgressPercent()).isEqualTo(80);
    }

    @Test
    void learnerCanMarkEarlierLessonIncompleteWhileUsingPinnedPublishedVersion() throws Exception {
        LessonResponse firstLesson = LessonResponse.builder().id(61L).title("Bài 1").build();
        LessonResponse secondLesson = LessonResponse.builder().id(62L).title("Bài 2").build();
        ModuleResponse module = ModuleResponse.builder()
                .id(51L)
                .title("Module mới nhất")
                .lessons(List.of(firstLesson, secondLesson))
                .build();
        versionTwo.setStatus(CourseVersionStatus.PUBLISHED);
        versionTwo.setContentSnapshotJson(new ObjectMapper().writeValueAsString(
                OnlineCourseResponse.builder().id(course.getId()).modules(List.of(module)).build()
        ));
        OnlineCourseEnrollment enrollment = OnlineCourseEnrollment.builder()
                .id(41L)
                .onlineCourse(course)
                .courseVersion(versionTwo)
                .progressPercent(80)
                .build();

        assertThatCode(() -> service.assertLessonProgressTransitionAllowed(enrollment, firstLesson.getId(), false))
                .doesNotThrowAnyException();
        verifyNoInteractions(lessonProgressRepository);
    }

    @Test
    void newlyInsertedLessonBeforeCompletedContentDoesNotRelockLearnerProgress() throws Exception {
        LessonResponse insertedLesson = LessonResponse.builder().id(61L).title("Bài mới").build();
        LessonResponse completedLaterLesson = LessonResponse.builder().id(62L).title("Bài đã học").build();
        ModuleResponse module = ModuleResponse.builder()
                .id(51L)
                .title("Module mới nhất")
                .lessons(List.of(insertedLesson, completedLaterLesson))
                .build();
        versionTwo.setStatus(CourseVersionStatus.PUBLISHED);
        versionTwo.setContentSnapshotJson(new ObjectMapper().writeValueAsString(
                OnlineCourseResponse.builder().id(course.getId()).modules(List.of(module)).build()
        ));
        OnlineCourseEnrollment enrollment = OnlineCourseEnrollment.builder()
                .id(41L)
                .onlineCourse(course)
                .courseVersion(versionTwo)
                .build();
        LessonProgress completedProgress = LessonProgress.builder()
                .enrollment(enrollment)
                .lesson(OnlineLesson.builder().id(completedLaterLesson.getId()).build())
                .status(LessonProgressStatus.COMPLETED)
                .build();
        when(lessonProgressRepository.findByEnrollmentAndStatusOrderByCompletedAtDesc(
                enrollment,
                LessonProgressStatus.COMPLETED
        )).thenReturn(List.of(completedProgress));

        assertThatCode(() -> service.assertLessonProgressTransitionAllowed(
                enrollment,
                insertedLesson.getId(),
                true
        )).doesNotThrowAnyException();
    }

    @Test
    void cannotCompleteNextModuleLessonBeforePreviousModuleTestPassed() throws Exception {
        LessonResponse firstModuleLesson = LessonResponse.builder().id(61L).title("Bài 1").build();
        LessonResponse nextModuleLesson = LessonResponse.builder().id(71L).title("Bài mô-đun 2").build();
        ModuleResponse firstModule = ModuleResponse.builder()
                .id(51L)
                .title("Module 1")
                .lessons(List.of(firstModuleLesson))
                .build();
        ModuleResponse secondModule = ModuleResponse.builder()
                .id(52L)
                .title("Module 2")
                .lessons(List.of(nextModuleLesson))
                .build();
        versionTwo.setStatus(CourseVersionStatus.PUBLISHED);
        versionTwo.setContentSnapshotJson(new ObjectMapper().writeValueAsString(
                OnlineCourseResponse.builder().id(course.getId()).modules(List.of(firstModule, secondModule)).build()
        ));
        User student = User.builder().id(7L).email("learner@englishlab.vn").build();
        OnlineCourseEnrollment enrollment = OnlineCourseEnrollment.builder()
                .id(41L)
                .student(student)
                .onlineCourse(course)
                .courseVersion(versionTwo)
                .build();
        CourseAssessment moduleTest = new CourseAssessment();
        moduleTest.setId(101L);
        moduleTest.setModule(fu.sep490.g23.backend.entity.course.OnlineCourseModule.builder().id(51L).build());
        LessonProgress completedProgress = LessonProgress.builder()
                .enrollment(enrollment)
                .lesson(OnlineLesson.builder().id(firstModuleLesson.getId()).build())
                .status(LessonProgressStatus.COMPLETED)
                .build();
        when(lessonProgressRepository.findByEnrollmentAndStatusOrderByCompletedAtDesc(
                enrollment,
                LessonProgressStatus.COMPLETED
        )).thenReturn(List.of(completedProgress));
        when(courseAssessmentRepository.findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course))
                .thenReturn(List.of(moduleTest));
        when(assessmentSubmissionRepository.existsByAssessmentAndStudentAndStatusIn(
                org.mockito.ArgumentMatchers.eq(moduleTest),
                org.mockito.ArgumentMatchers.eq(student),
                org.mockito.ArgumentMatchers.anySet()
        )).thenReturn(false);

        assertThatThrownBy(() -> service.assertLessonProgressTransitionAllowed(
                enrollment,
                nextModuleLesson.getId(),
                true
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bài đánh giá của mô-đun trước");
    }

    @Test
    void publishedAssessmentIdsAreProtectedFromDraftMutation() {
        versionOne.setAssessmentIdsJson("[91]");
        versionTwo.setAssessmentIdsJson("[92]");
        when(versionRepository.findByOnlineCourseOrderByVersionNumberDesc(course))
                .thenReturn(List.of(versionTwo, versionOne));

        assertThat(service.isAssessmentReferencedByPublishedHistory(course, 91L)).isTrue();
        assertThat(service.isAssessmentReferencedByPublishedHistory(course, 92L)).isFalse();
    }

    @Test
    void legacyAssessmentRowsReceiveTheSameProgressKeyAcrossPublishedVersions() {
        CourseAssessment oldAssessment = new CourseAssessment();
        oldAssessment.setId(91L);
        oldAssessment.setOnlineCourse(course);
        oldAssessment.setDisplayOrder(1);
        oldAssessment.setProgressKey(null);
        CourseAssessment latestAssessment = new CourseAssessment();
        latestAssessment.setId(92L);
        latestAssessment.setOnlineCourse(course);
        latestAssessment.setDisplayOrder(1);
        latestAssessment.setProgressKey(null);
        versionOne.setStatus(CourseVersionStatus.RETIRED);
        versionOne.setAssessmentIdsJson("[91]");
        versionTwo.setStatus(CourseVersionStatus.PUBLISHED);
        versionTwo.setAssessmentIdsJson("[92]");
        OnlineCourseEnrollment enrollment = OnlineCourseEnrollment.builder()
                .onlineCourse(course)
                .courseVersion(versionOne)
                .build();

        when(versionRepository.findByOnlineCourseOrderByVersionNumberDesc(course))
                .thenReturn(List.of(versionTwo, versionOne));
        when(versionRepository.findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(
                course,
                CourseVersionStatus.PUBLISHED
        )).thenReturn(Optional.of(versionTwo));
        when(courseAssessmentRepository.findAllById(List.of(91L))).thenReturn(List.of(oldAssessment));
        when(courseAssessmentRepository.findAllById(List.of(92L))).thenReturn(List.of(latestAssessment));

        assertThat(service.getLatestPublishedAssessmentIds(enrollment)).containsExactly(92L);
        assertThat(oldAssessment.getProgressKey()).isNotBlank();
        assertThat(latestAssessment.getProgressKey()).isEqualTo(oldAssessment.getProgressKey());
        verify(courseAssessmentRepository).saveAll(org.mockito.ArgumentMatchers.argThat(items ->
                java.util.stream.StreamSupport.stream(items.spliterator(), false).count() == 2
        ));
    }

    @Test
    void managerCannotPublishContentManagerCourseVersion() {
        User manager = User.builder().id(5L).email("manager@test.com").fullName("Manager").build();
        manager.setRoles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.MANAGER));
        when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> service.publish(course.getId(), versionTwo.getId(), manager.getEmail()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("chỉnh sửa phiên bản khóa học");
    }

    @Test
    void directPublishStillBlocksInvalidContent() {
        User contentManager = User.builder().id(6L).email("content@test.com").fullName("Content Manager").build();
        contentManager.setRoles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.CONTENT_MANAGER));
        when(userRepository.findByEmail(contentManager.getEmail())).thenReturn(Optional.of(contentManager));
        when(onlineCourseRepository.findWithModulesById(course.getId())).thenReturn(Optional.of(course));
        when(versionRepository.findByIdAndOnlineCourseId(versionTwo.getId(), course.getId()))
                .thenReturn(Optional.of(versionTwo));
        when(courseAssessmentRepository.findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course))
                .thenReturn(List.of());
        when(mapper.toResponse(course)).thenReturn(OnlineCourseResponse.builder().modules(new ArrayList<>()).build());
        when(previewValidator.validate(any(), anyList())).thenReturn(List.of(
                OnlineCoursePreviewWarningResponse.builder()
                        .severity("ERROR")
                        .message("Khóa học chưa có mô-đun nào.")
                        .build()
        ));

        assertThatThrownBy(() -> service.publish(course.getId(), versionTwo.getId(), contentManager.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Khóa học chưa có mô-đun nào");
    }

    @Test
    void pendingVersionFromLegacyFlowRemainsEditable() {
        User contentManager = User.builder()
                .id(4L)
                .email("content@test.com")
                .fullName("Content Manager")
                .build();
        contentManager.setRoles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.CONTENT_MANAGER));
        versionTwo.setStatus(CourseVersionStatus.PENDING_REVIEW);
        when(userRepository.findByEmail(contentManager.getEmail())).thenReturn(Optional.of(contentManager));
        when(versionRepository.findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(
                course,
                CourseVersionStatus.DRAFT
        )).thenReturn(Optional.empty());
        when(versionRepository.findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(
                course,
                CourseVersionStatus.PENDING_REVIEW
        )).thenReturn(Optional.of(versionTwo));

        service.assertEditableDraft(course, contentManager.getEmail());
    }

    @Test
    void synchronizeDraftSnapshotAlsoUpdatesLegacyPendingVersion() {
        versionTwo.setStatus(CourseVersionStatus.PENDING_REVIEW);
        when(versionRepository.findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(
                course,
                CourseVersionStatus.DRAFT
        )).thenReturn(Optional.empty());
        when(versionRepository.findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(
                course,
                CourseVersionStatus.PENDING_REVIEW
        )).thenReturn(Optional.of(versionTwo));
        when(mapper.toResponse(course)).thenReturn(
                OnlineCourseResponse.builder().id(course.getId()).title("Nội dung vừa lưu").modules(new ArrayList<>()).build()
        );
        when(courseAssessmentRepository.findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course))
                .thenReturn(List.of());

        service.synchronizeDraftSnapshot(course);

        assertThat(versionTwo.getContentSnapshotJson()).contains("Nội dung vừa lưu");
    }

    @Test
    void createDraftClonesAssessmentsAndPreservesPublishedAssessmentIds() {
        User contentManager = User.builder()
                .id(4L)
                .email("content@test.com")
                .fullName("Content Manager")
                .build();
        contentManager.setRoles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.CONTENT_MANAGER));
        CourseAssessment publishedAssessment = CourseAssessment.builder()
                .id(51L)
                .onlineCourse(course)
                .title("Mock test v1")
                .displayOrder(1)
                .active(true)
                .build();
        versionOne.setTotalRequiredAssessments(1);

        when(userRepository.findByEmail(contentManager.getEmail())).thenReturn(Optional.of(contentManager));
        when(onlineCourseRepository.findWithModulesById(course.getId())).thenReturn(Optional.of(course));
        when(versionRepository.existsByOnlineCourseAndStatusIn(course, java.util.List.of(
                CourseVersionStatus.DRAFT,
                CourseVersionStatus.PENDING_REVIEW
        ))).thenReturn(false);
        when(versionRepository.findByOnlineCourseOrderByVersionNumberDesc(course)).thenReturn(java.util.List.of(versionOne));
        when(courseAssessmentRepository.findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course))
                .thenReturn(java.util.List.of(publishedAssessment));
        when(courseAssessmentRepository.saveAll(anyList())).thenAnswer(invocation -> {
            java.util.List<?> rawItems = invocation.getArgument(0);
            java.util.List<CourseAssessment> items = rawItems.stream()
                    .map(CourseAssessment.class::cast)
                    .toList();
            items.stream().filter(item -> item.getId() == null).forEach(item -> item.setId(61L));
            return items;
        });
        when(versionRepository.save(org.mockito.ArgumentMatchers.any(OnlineCourseVersion.class)))
                .thenAnswer(invocation -> {
                    OnlineCourseVersion version = invocation.getArgument(0);
                    version.setId(32L);
                    return version;
                });

        CreateCourseVersionRequest request = new CreateCourseVersionRequest();
        request.setChangeNote("Cập nhật mock test");
        service.createDraft(course.getId(), request, contentManager.getEmail());

        ArgumentCaptor<OnlineCourseVersion> versionCaptor = ArgumentCaptor.forClass(OnlineCourseVersion.class);
        verify(versionRepository).save(versionCaptor.capture());
        assertThat(versionOne.getAssessmentIdsJson()).isEqualTo("[51]");
        assertThat(publishedAssessment.isActive()).isFalse();
        assertThat(versionCaptor.getValue().getAssessmentIdsJson()).isEqualTo("[61]");
        assertThat(versionCaptor.getValue().getChangeNote()).isEqualTo("Cập nhật mock test");
    }

    @Test
    void createDraftClonesPublishedModulesOntoNewDraft() {
        User contentManager = User.builder()
                .id(4L)
                .email("content@test.com")
                .fullName("Content Manager")
                .build();
        contentManager.setRoles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.CONTENT_MANAGER));
        OnlineLesson publishedLesson = OnlineLesson.builder()
                .id(61L)
                .stableLessonKey("stable-lesson-1")
                .title("Listening intro")
                .contentText("Body")
                .sequenceNumber(1)
                .preview(true)
                .durationMinutes(12)
                .flashcardRefs(new ArrayList<>())
                .build();
        OnlineCourseModule publishedModule = OnlineCourseModule.builder()
                .id(51L)
                .title("Module 1")
                .sequenceNumber(1)
                .lessons(new ArrayList<>(List.of(publishedLesson)))
                .build();
        publishedLesson.setModule(publishedModule);
        versionOne.setModules(new ArrayList<>(List.of(publishedModule)));
        publishedModule.setOnlineCourseVersion(versionOne);
        publishedModule.setOnlineCourse(course);

        when(userRepository.findByEmail(contentManager.getEmail())).thenReturn(Optional.of(contentManager));
        when(onlineCourseRepository.findWithModulesById(course.getId())).thenReturn(Optional.of(course));
        when(versionRepository.existsByOnlineCourseAndStatusIn(course, List.of(
                CourseVersionStatus.DRAFT,
                CourseVersionStatus.PENDING_REVIEW
        ))).thenReturn(false);
        when(versionRepository.findByOnlineCourseOrderByVersionNumberDesc(course)).thenReturn(List.of(versionOne));
        when(courseAssessmentRepository.findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course))
                .thenReturn(List.of());
        when(versionRepository.save(any(OnlineCourseVersion.class))).thenAnswer(invocation -> {
            OnlineCourseVersion version = invocation.getArgument(0);
            if (version.getId() == null) {
                version.setId(32L);
            }
            return version;
        });

        service.createDraft(course.getId(), null, contentManager.getEmail());

        ArgumentCaptor<OnlineCourseVersion> versionCaptor = ArgumentCaptor.forClass(OnlineCourseVersion.class);
        verify(versionRepository, org.mockito.Mockito.atLeastOnce()).save(versionCaptor.capture());
        OnlineCourseVersion savedDraft = versionCaptor.getAllValues().get(versionCaptor.getAllValues().size() - 1);
        assertThat(savedDraft.getModules()).hasSize(1);
        OnlineCourseModule clonedModule = savedDraft.getModules().get(0);
        assertThat(clonedModule.getId()).isNull();
        assertThat(clonedModule.getTitle()).isEqualTo("Module 1");
        assertThat(clonedModule.getLessons()).singleElement().satisfies(lesson -> {
            assertThat(lesson.getId()).isNull();
            assertThat(lesson.getStableLessonKey()).isEqualTo("stable-lesson-1");
            assertThat(lesson.getTitle()).isEqualTo("Listening intro");
            assertThat(lesson.getContentText()).isEqualTo("Body");
        });
        assertThat(versionOne.getModules()).hasSize(1);
        assertThat(versionOne.getModules().get(0).getId()).isEqualTo(51L);
    }

    @Test
    void requireEditableVersionRejectsPublishedOnlyCourse() {
        when(versionRepository.findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(
                course,
                CourseVersionStatus.DRAFT
        )).thenReturn(Optional.empty());
        when(versionRepository.findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(
                course,
                CourseVersionStatus.PENDING_REVIEW
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireEditableVersion(course))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Hãy tạo phiên bản nháp mới");
    }

    @Test
    void assertLessonBelongsToEnrollmentUsesPinnedVersionModules() {
        OnlineLesson pinnedLesson = OnlineLesson.builder().id(61L).title("Pinned").build();
        OnlineCourseModule pinnedModule = OnlineCourseModule.builder()
                .id(51L)
                .lessons(new ArrayList<>(List.of(pinnedLesson)))
                .build();
        pinnedLesson.setModule(pinnedModule);
        versionOne.setModules(new ArrayList<>(List.of(pinnedModule)));
        pinnedModule.setOnlineCourseVersion(versionOne);
        OnlineCourseEnrollment enrollment = OnlineCourseEnrollment.builder()
                .id(41L)
                .onlineCourse(course)
                .courseVersion(versionOne)
                .build();

        assertThatCode(() -> service.assertLessonBelongsToEnrollment(enrollment, 61L))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> service.assertLessonBelongsToEnrollment(enrollment, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phiên bản đã đăng ký");
    }
}
