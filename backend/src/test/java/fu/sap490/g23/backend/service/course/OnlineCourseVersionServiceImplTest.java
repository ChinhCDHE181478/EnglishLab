package fu.sap490.g23.backend.service.course;

import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sap490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sap490.g23.backend.dto.request.course.CreateCourseVersionRequest;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.CourseAssessment;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import fu.sap490.g23.backend.entity.course.enums.CourseVersionStatus;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseVersionRepository;
import fu.sap490.g23.backend.repository.course.LessonProgressRepository;
import fu.sap490.g23.backend.service.course.impl.OnlineCourseVersionServiceImpl;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OnlineCourseVersionServiceImplTest {
    @Mock
    private OnlineCourseRepository onlineCourseRepository;
    @Mock
    private OnlineCourseVersionRepository versionRepository;
    @Mock
    private CourseAssessmentRepository courseAssessmentRepository;
    @Mock
    private LessonProgressRepository lessonProgressRepository;
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
                lessonProgressRepository,
                userRepository,
                mapper,
                previewValidator
        );
        LearningPackage learningPackage = LearningPackage.builder()
                .id(11L)
                .title("IELTS Foundation")
                .status(PackageStatus.PUBLISHED)
                .build();
        course = OnlineCourse.builder()
                .id(21L)
                .learningPackage(learningPackage)
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
                .status(CourseVersionStatus.PENDING_REVIEW)
                .contentSnapshotJson("{}")
                .build();
    }

    @Test
    void previewReturnsExactlyTheSelectedVersionSnapshot() {
        User contentManager = User.builder().id(4L).email("content@test.com").fullName("Content Manager").build();
        contentManager.setRole(RoleEnum.CONTENT_MANAGER);
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
    void publishRetiresOldVersionWhileExistingEnrollmentKeepsItsSnapshot() {
        User manager = User.builder().id(3L).email("manager@test.com").fullName("Manager").build();
        manager.setRole(RoleEnum.MANAGER);
        when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
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

        service.publish(course.getId(), versionTwo.getId(), manager.getEmail());

        PackageEnrollment existingEnrollment = PackageEnrollment.builder()
                .id(41L)
                .courseVersion(versionOne)
                .progressPercent(80)
                .build();
        OnlineCourseResponse oldContent = service.readEnrollmentSnapshot(existingEnrollment, course);
        PackageEnrollment newEnrollment = PackageEnrollment.builder()
                .id(42L)
                .courseVersion(service.requirePublishedVersion(course))
                .progressPercent(0)
                .build();

        assertThat(versionOne.getStatus()).isEqualTo(CourseVersionStatus.RETIRED);
        assertThat(versionTwo.getStatus()).isEqualTo(CourseVersionStatus.PUBLISHED);
        assertThat(existingEnrollment.getCourseVersion()).isSameAs(versionOne);
        assertThat(newEnrollment.getCourseVersion()).isSameAs(versionTwo);
        assertThat(oldContent.getTitle()).isEqualTo("Nội dung v1");
        assertThat(oldContent.getProgressPercent()).isEqualTo(80);
    }

    @Test
    void pendingVersionLocksFurtherContentEdits() {
        User contentManager = User.builder()
                .id(4L)
                .email("content@test.com")
                .fullName("Content Manager")
                .build();
        contentManager.setRole(RoleEnum.CONTENT_MANAGER);
        when(userRepository.findByEmail(contentManager.getEmail())).thenReturn(Optional.of(contentManager));
        when(versionRepository.findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(
                course,
                CourseVersionStatus.DRAFT
        )).thenReturn(Optional.empty());
        when(versionRepository.findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(
                course,
                CourseVersionStatus.PENDING_REVIEW
        )).thenReturn(Optional.of(versionTwo));

        assertThatThrownBy(() -> service.assertEditableDraft(course, contentManager.getEmail()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đang chờ duyệt");
    }

    @Test
    void createDraftClonesAssessmentsAndPreservesPublishedAssessmentIds() {
        User contentManager = User.builder()
                .id(4L)
                .email("content@test.com")
                .fullName("Content Manager")
                .build();
        contentManager.setRole(RoleEnum.CONTENT_MANAGER);
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
}
