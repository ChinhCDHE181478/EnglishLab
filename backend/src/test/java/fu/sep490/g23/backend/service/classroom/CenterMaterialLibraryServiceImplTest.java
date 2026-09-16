package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.request.classroom.CenterMaterialLibraryUpsertRequest;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.CenterMaterialLibraryItem;
import fu.sep490.g23.backend.repository.classroom.CenterMaterialLibraryItemRepository;
import fu.sep490.g23.backend.repository.course.CourseUnitContentRefRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.classroom.impl.CenterMaterialLibraryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CenterMaterialLibraryServiceImplTest {

    @Mock
    private CenterMaterialLibraryItemRepository repository;

    @Mock
    private CourseUnitContentRefRepository courseUnitContentRefRepository;

    @Mock
    private ClassroomAccessHelper accessHelper;

    private CenterMaterialLibraryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CenterMaterialLibraryServiceImpl(
                repository,
                courseUnitContentRefRepository,
                accessHelper
        );
    }

    @Test
    void create_persistsTheUploadedR2UrlWithoutRewritingIt() {
        User actor = new User();
        String r2Url = "https://files.example.test/classroom-attachments/homework-test.png";
        CenterMaterialLibraryUpsertRequest request = CenterMaterialLibraryUpsertRequest.builder()
                .title("IELTS lesson image")
                .fileUrl(r2Url)
                .fileType("PNG")
                .materialType("IMAGE")
                .provider("EnglishLab")
                .examCategory("IELTS")
                .status("PUBLISHED")
                .build();
        when(accessHelper.requireUser("manager@englishlab.vn")).thenReturn(actor);
        when(accessHelper.canManageClassroom(actor)).thenReturn(true);
        when(repository.save(any(CenterMaterialLibraryItem.class))).thenAnswer(invocation -> {
            CenterMaterialLibraryItem item = invocation.getArgument(0);
            item.setId(101L);
            return item;
        });

        var response = service.create(request, "manager@englishlab.vn");

        ArgumentCaptor<CenterMaterialLibraryItem> captor = ArgumentCaptor.forClass(CenterMaterialLibraryItem.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getFileUrl()).isEqualTo(r2Url);
        assertThat(response.getFileUrl()).isEqualTo(r2Url);
    }
}
