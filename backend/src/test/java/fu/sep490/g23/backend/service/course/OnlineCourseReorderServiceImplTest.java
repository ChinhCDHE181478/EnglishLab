package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.dto.request.course.ModuleOrderItemRequest;
import fu.sep490.g23.backend.dto.request.course.ReorderModulesRequest;
import fu.sep490.g23.backend.dto.response.course.ModuleResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.OnlineCourseModule;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sep490.g23.backend.entity.course.enums.CourseVersionStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.service.course.impl.OnlineCourseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnlineCourseReorderServiceImplTest {

    @Mock
    private OnlineCourseRepository onlineCourseRepository;

    @Mock
    private UserRepository userRepository;
    @Mock
    private OnlineCourseVersionService onlineCourseVersionService;

    @Mock
    private OnlineCourseMapper mapper;

    @InjectMocks
    private OnlineCourseServiceImpl service;

    @Test
    void reorderModulesNormalizesAndPersistsInTwoPhases() {
        OnlineCourse course = draftCourse();
        OnlineCourseVersion editableVersion = editableVersionFor(course);
        User contentManager = User.builder().email("content@englishlab.vn")
                .roles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.CONTENT_MANAGER)).build();
        List<ModuleResponse> responseModules = List.of(
                ModuleResponse.builder().id(12L).displayOrder(1).build(),
                ModuleResponse.builder().id(11L).displayOrder(2).build()
        );
        when(userRepository.findByEmail(contentManager.getEmail())).thenReturn(Optional.of(contentManager));
        when(onlineCourseRepository.findWithModulesById(1L)).thenReturn(Optional.of(course));
        when(onlineCourseVersionService.requireEditableVersion(course)).thenReturn(editableVersion);
        when(mapper.toResponse(course)).thenReturn(OnlineCourseResponse.builder().modules(responseModules).build());

        List<ModuleResponse> result = service.reorderModules(
                1L,
                ReorderModulesRequest.builder().items(List.of(
                        ModuleOrderItemRequest.builder().moduleId(12L).orderIndex(1).build(),
                        ModuleOrderItemRequest.builder().moduleId(11L).orderIndex(2).build()
                )).build(),
                contentManager.getEmail()
        );

        assertThat(result).isEqualTo(responseModules);
        assertThat(editableVersion.getModules()).extracting(OnlineCourseModule::getDisplayOrder).containsExactly(2, 1);
        verify(onlineCourseRepository, org.mockito.Mockito.times(2)).flush();
    }

    @Test
    void duplicateOrderIsRejectedBeforeDatabaseMutation() {
        OnlineCourse course = draftCourse();
        OnlineCourseVersion editableVersion = editableVersionFor(course);
        User contentManager = User.builder().email("content@englishlab.vn")
                .roles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.CONTENT_MANAGER)).build();
        when(userRepository.findByEmail(contentManager.getEmail())).thenReturn(Optional.of(contentManager));
        when(onlineCourseRepository.findWithModulesById(1L)).thenReturn(Optional.of(course));
        when(onlineCourseVersionService.requireEditableVersion(course)).thenReturn(editableVersion);

        assertThatThrownBy(() -> service.reorderModules(
                1L,
                ReorderModulesRequest.builder().items(List.of(
                        ModuleOrderItemRequest.builder().moduleId(11L).orderIndex(1).build(),
                        ModuleOrderItemRequest.builder().moduleId(12L).orderIndex(1).build()
                )).build(),
                contentManager.getEmail()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không được trùng");
    }

    @Test
    void learnerCannotBypassReorderPermissionAtServiceLayer() {
        User learner = User.builder().email("learner@englishlab.vn")
                .roles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.LEARNER)).build();
        when(userRepository.findByEmail(learner.getEmail())).thenReturn(Optional.of(learner));

        assertThatThrownBy(() -> service.reorderModules(
                1L,
                ReorderModulesRequest.builder().items(List.of(
                        ModuleOrderItemRequest.builder().moduleId(11L).orderIndex(1).build()
                )).build(),
                learner.getEmail()
        )).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    private OnlineCourse draftCourse() {
        OnlineCourseModule first = OnlineCourseModule.builder()
                .id(11L)
                .title("Module A")
                .sequenceNumber(1)
                .lessons(new ArrayList<>())
                .build();
        OnlineCourseModule second = OnlineCourseModule.builder()
                .id(12L)
                .title("Module B")
                .sequenceNumber(2)
                .lessons(new ArrayList<>())
                .build();
        OnlineCourse course = OnlineCourse.builder()
                .id(1L)
                .status(PackageStatus.DRAFT)
                .modules(new ArrayList<>(List.of(first, second)))
                .build();
        first.setOnlineCourse(course);
        second.setOnlineCourse(course);
        return course;
    }

    private OnlineCourseVersion editableVersionFor(OnlineCourse course) {
        OnlineCourseVersion version = OnlineCourseVersion.builder()
                .id(99L)
                .onlineCourse(course)
                .versionNumber(1)
                .status(CourseVersionStatus.DRAFT)
                .modules(course.getModules())
                .build();
        course.getModules().forEach(module -> module.setOnlineCourseVersion(version));
        return version;
    }
}
