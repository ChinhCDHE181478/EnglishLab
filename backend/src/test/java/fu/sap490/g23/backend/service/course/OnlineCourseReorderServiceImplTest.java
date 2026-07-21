package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.request.course.ModuleOrderItemRequest;
import fu.sap490.g23.backend.dto.request.course.ReorderModulesRequest;
import fu.sap490.g23.backend.dto.response.course.ModuleResponse;
import fu.sap490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.CourseModule;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sap490.g23.backend.service.course.impl.OnlineCourseServiceImpl;
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
        User contentManager = User.builder().email("content@englishlab.vn").role(RoleEnum.CONTENT_MANAGER).build();
        List<ModuleResponse> responseModules = List.of(
                ModuleResponse.builder().id(12L).displayOrder(1).build(),
                ModuleResponse.builder().id(11L).displayOrder(2).build()
        );
        when(userRepository.findByEmail(contentManager.getEmail())).thenReturn(Optional.of(contentManager));
        when(onlineCourseRepository.findWithModulesById(1L)).thenReturn(Optional.of(course));
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
        assertThat(course.getModules()).extracting(CourseModule::getDisplayOrder).containsExactly(2, 1);
        verify(onlineCourseRepository, org.mockito.Mockito.times(2)).flush();
    }

    @Test
    void duplicateOrderIsRejectedBeforeDatabaseMutation() {
        OnlineCourse course = draftCourse();
        User contentManager = User.builder().email("content@englishlab.vn").role(RoleEnum.CONTENT_MANAGER).build();
        when(userRepository.findByEmail(contentManager.getEmail())).thenReturn(Optional.of(contentManager));
        when(onlineCourseRepository.findWithModulesById(1L)).thenReturn(Optional.of(course));

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
        User learner = User.builder().email("learner@englishlab.vn").role(RoleEnum.LEARNER).build();
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
        CourseModule first = CourseModule.builder()
                .id(11L)
                .title("Module A")
                .displayOrder(1)
                .lessons(new ArrayList<>())
                .build();
        CourseModule second = CourseModule.builder()
                .id(12L)
                .title("Module B")
                .displayOrder(2)
                .lessons(new ArrayList<>())
                .build();
        OnlineCourse course = OnlineCourse.builder()
                .id(1L)
                .learningPackage(LearningPackage.builder().status(PackageStatus.DRAFT).deleted(false).build())
                .modules(new ArrayList<>(List.of(first, second)))
                .build();
        first.setOnlineCourse(course);
        second.setOnlineCourse(course);
        return course;
    }
}
