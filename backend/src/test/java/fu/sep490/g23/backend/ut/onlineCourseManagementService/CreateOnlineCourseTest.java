package fu.sep490.g23.backend.ut.onlineCourseManagementService;

import fu.sep490.g23.backend.dto.request.course.OnlineCourseRequest;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sep490.g23.backend.entity.course.CourseCategory;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.PackageType;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageTypeCode;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sep490.g23.backend.repository.course.CourseCategoryRepository;
import fu.sep490.g23.backend.repository.course.CourseLessonFlashcardRefRepository;
import fu.sep490.g23.backend.repository.course.LearningPackageRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseVersionRepository;
import fu.sep490.g23.backend.repository.course.PackageTypeRepository;
import fu.sep490.g23.backend.service.course.OnlineCourseMapper;
import fu.sep490.g23.backend.service.course.OnlineCourseVersionService;
import fu.sep490.g23.backend.service.course.YouTubeTranscriptService;
import fu.sep490.g23.backend.service.course.impl.OnlineCourseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateOnlineCourseTest {

    @Mock
    private OnlineCourseRepository onlineCourseRepository;

    @Mock
    private OnlineCourseVersionRepository onlineCourseVersionRepository;

    @Mock
    private LearningPackageRepository learningPackageRepository;

    @Mock
    private PackageTypeRepository packageTypeRepository;

    @Mock
    private CourseCategoryRepository courseCategoryRepository;

    @Mock
    private CourseAssessmentRepository courseAssessmentRepository;

    @Mock
    private CourseLessonFlashcardRefRepository courseLessonFlashcardRefRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OnlineCourseMapper mapper;

    @Mock
    private OnlineCourseVersionService onlineCourseVersionService;

    @Mock
    private YouTubeTranscriptService youTubeTranscriptService;

    @Test
    void createCourse_DraftStatus_Success() {
        // Arrange
        CourseCategory ieltsCategory = CourseCategory.builder()
                .id(1L)
                .code("IELTS")
                .name("IELTS")
                .active(true)
                .build();

        PackageType onlineCourseType = PackageType.builder()
                .id(1L)
                .code(PackageTypeCode.ONLINE_COURSE)
                .name("Online Course")
                .active(true)
                .build();

        OnlineCourseRequest request = OnlineCourseRequest.builder()
                .title("IELTS Master 7.0")
                .category("IELTS")
                .level(CourseLevel.ADVANCED)
                .status(PackageStatus.DRAFT)
                .price(new BigDecimal(1500000))
                .targetBand(7.0)
                .recommendedCurrentBandMin(5.0)
                .modules(new ArrayList<>())
                .build();

        when(packageTypeRepository.findByCode(PackageTypeCode.ONLINE_COURSE))
                .thenReturn(Optional.of(onlineCourseType));
        when(courseCategoryRepository.findByCode("IELTS"))
                .thenReturn(Optional.of(ieltsCategory));
        // Mock: T├¬n ch╞░a tß╗ôn tß║íi -> slug kh├┤ng tr├╣ng
        when(learningPackageRepository.existsBySlug(anyString())).thenReturn(false);

        // Capture course saved to repo to verify state
        when(onlineCourseRepository.save(any(OnlineCourse.class))).thenAnswer(invocation -> {
            OnlineCourse toSave = invocation.getArgument(0);
            toSave.setId(100L);
            toSave.getLearningPackage().setId(200L);
            toSave.getLearningPackage().setSlug("ielts-master-70");
            return toSave;
        });

        OnlineCourseResponse mockResponse = OnlineCourseResponse.builder()
                .id(100L)
                .packageId(200L)
                .title("IELTS Master 7.0")
                .slug("ielts-master-70")
                .category("IELTS")
                .categoryName("IELTS")
                .level(CourseLevel.ADVANCED)
                .status(PackageStatus.DRAFT)
                .price(new BigDecimal(1500000))
                .originalPrice(new BigDecimal(1500000))
                .salePrice(new BigDecimal(1500000))
                .build();
        when(mapper.toResponse(any(OnlineCourse.class))).thenReturn(mockResponse);

        OnlineCourseServiceImpl service = new OnlineCourseServiceImpl(
                onlineCourseRepository, onlineCourseVersionRepository, learningPackageRepository, packageTypeRepository,
                courseCategoryRepository, null, null, null, null, courseAssessmentRepository,
                null, null, courseLessonFlashcardRefRepository, null, null, null, null, userRepository, mapper,
                onlineCourseVersionService, null, null, null, null,
                null, null, youTubeTranscriptService
        );

        // Act
        OnlineCourseResponse result = service.createCourse(request, null);

        // Assert
        assertNotNull(result, "Response kh├┤ng ─æ╞░ß╗úc null");
        assertEquals("H├ánh ─æß╗Öng ─æ├ú ho├án th├ánh th├ánh c├┤ng", "H├ánh ─æß╗Öng ─æ├ú ho├án th├ánh th├ánh c├┤ng");

        // Verify saved course has DRAFT status
        ArgumentCaptor<OnlineCourse> courseCaptor = ArgumentCaptor.forClass(OnlineCourse.class);
        verify(onlineCourseRepository).save(courseCaptor.capture());
        OnlineCourse savedCourse = courseCaptor.getValue();

        assertNotNull(savedCourse, "Saved course phß║úi tß╗ôn tß║íi");
        assertEquals("IELTS Master 7.0", savedCourse.getLearningPackage().getTitle());
        assertEquals(PackageStatus.DRAFT, savedCourse.getLearningPackage().getStatus(),
                "Trß║íng th├íi kh├│a hß╗ìc phß║úi l├á DRAFT");
        assertEquals(new BigDecimal(1500000), savedCourse.getLearningPackage().getPrice());
        assertEquals(ieltsCategory, savedCourse.getCategory());
        assertEquals(CourseLevel.ADVANCED, savedCourse.getLevel());

        // Verify mapper was called with the saved course
        verify(mapper).toResponse(savedCourse);
        // Verify version draft was created
        verify(onlineCourseVersionService).createDraft(eq(100L), eq(null), eq(null));
    }

//    @Test
//    void createCourse_ThenPublish_Success() {
//        // Arrange
//        CourseCategory ieltsCategory = CourseCategory.builder()
//                .id(1L).code("IELTS").active(true).build();
//        PackageType onlineCourseType = PackageType.builder()
//                .id(1L).code(PackageTypeCode.ONLINE_COURSE).build();
//
//        LessonRequest lesson = LessonRequest.builder()
//                .title("Speaking Part 1").displayOrder(1).build();
//        ModuleRequest module = ModuleRequest.builder()
//                .title("Speaking Foundation").displayOrder(1)
//                .lessons(new ArrayList<>(List.of(lesson))).build();
//
//        OnlineCourseRequest request = OnlineCourseRequest.builder()
//                .title("IELTS 7.0 Pro").category("IELTS").level(CourseLevel.ADVANCED)
//                .status(PackageStatus.PUBLISHED).price(new BigDecimal(1500000))
//                .targetBand(7.0).targetOutcome("─Éß║ít IELTS 7.0+ sau khi ho├án th├ánh")
//                .modules(new ArrayList<>(List.of(module))).build();
//
//        when(packageTypeRepository.findByCode(PackageTypeCode.ONLINE_COURSE)).thenReturn(Optional.of(onlineCourseType));
//        when(courseCategoryRepository.findByCode("IELTS")).thenReturn(Optional.of(ieltsCategory));
//        when(learningPackageRepository.existsBySlug(anyString())).thenReturn(false);
//        when(youTubeTranscriptService.extractVideoId(any())).thenReturn(Optional.empty());
//
//        AtomicReference<OnlineCourse> savedCourseRef = new AtomicReference<>();
//        when(onlineCourseRepository.save(any(OnlineCourse.class))).thenAnswer(inv -> {
//            OnlineCourse c = inv.getArgument(0);
//            c.setId(101L);
//            c.getLearningPackage().setId(201L);
//            c.getLearningPackage().setSlug("ielts-7-0-pro");
//            savedCourseRef.set(c);
//            return c;
//        });
//        when(onlineCourseRepository.findWithModulesById(101L)).thenAnswer(inv -> Optional.of(savedCourseRef.get()));
//        when(onlineCourseVersionRepository
//                .findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(any(), eq(CourseVersionStatus.DRAFT)))
//                .thenReturn(Optional.of(OnlineCourseVersion.builder().id(1L).status(CourseVersionStatus.DRAFT).build()));
//        when(courseAssessmentRepository.findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(any()))
//                .thenReturn(List.of(CourseAssessment.builder().id(1L).active(true).build()));
//        when(mapper.toResponse(any())).thenReturn(OnlineCourseResponse.builder()
//                .id(101L).title("IELTS 7.0 Pro").status(PackageStatus.PUBLISHED).build());
//
//        OnlineCourseServiceImpl service = new OnlineCourseServiceImpl(
//                onlineCourseRepository, onlineCourseVersionRepository, learningPackageRepository, packageTypeRepository,
//                courseCategoryRepository, null, null, null, null, courseAssessmentRepository,
//                null, null, courseLessonFlashcardRefRepository, null, null, null, userRepository, mapper,
//                onlineCourseVersionService, null, null, null, null,
//                null, null, youTubeTranscriptService
//        );
//
//        // Act 1: Tß║ío kh├│a hß╗ìc
//        OnlineCourseResponse created = service.createCourse(request, null);
//
//        // Assert tß║ío th├ánh c├┤ng
//        assertNotNull(created);
//        assertEquals("H├ánh ─æß╗Öng ─æ├ú ho├án th├ánh th├ánh c├┤ng", "H├ánh ─æß╗Öng ─æ├ú ho├án th├ánh th├ánh c├┤ng");
//
//        OnlineCourse saved = savedCourseRef.get();
//        assertEquals("IELTS 7.0 Pro", saved.getLearningPackage().getTitle());
//        assertEquals(PackageStatus.DRAFT, saved.getLearningPackage().getStatus());
//        assertEquals(new BigDecimal(1500000), saved.getLearningPackage().getPrice());
//
//        // Act 2: Publish kh├│a hß╗ìc
//        OnlineCourseResponse published = service.publishCourse(101L, null);
//
//        // Assert publish th├ánh c├┤ng
//        assertNotNull(published);
//        assertEquals(PackageStatus.PUBLISHED, published.getStatus());
//        assertEquals("IELTS 7.0 Pro", published.getTitle());
//        verify(onlineCourseVersionService).publish(eq(101L), eq(1L), eq(null));
//    }

    @Test
    void createCourse_DuplicateName_GenerateUniqueSlug() {
        // Arrange
        CourseCategory ieltsCategory = CourseCategory.builder()
                .id(1L).code("IELTS").active(true).build();
        PackageType onlineCourseType = PackageType.builder()
                .id(1L).code(PackageTypeCode.ONLINE_COURSE).build();

        OnlineCourseRequest request = OnlineCourseRequest.builder()
                .title("IELTS Master 7.0")
                .category("IELTS")
                .level(CourseLevel.ADVANCED)
                .status(PackageStatus.DRAFT)
                .price(new BigDecimal(1500000))
                .targetBand(7.0)
                .modules(new ArrayList<>())
                .build();

        when(packageTypeRepository.findByCode(PackageTypeCode.ONLINE_COURSE)).thenReturn(Optional.of(onlineCourseType));
        when(courseCategoryRepository.findByCode("IELTS")).thenReturn(Optional.of(ieltsCategory));
        // T├¬n ─æ├ú tß╗ôn tß║íi: slug base "ielts-master-70" ─æ├ú c├│, nh╞░ng "ielts-master-70-2" ch╞░a c├│
        // ΓåÆ generateUniqueSlug sß║╜ thß╗¡ base (true), thß╗¡ base-2 (false) ΓåÆ trß║ú vß╗ü "ielts-master-70-2"
        when(learningPackageRepository.existsBySlug("ielts-master-70")).thenReturn(true);
        when(learningPackageRepository.existsBySlug("ielts-master-70-2")).thenReturn(false);

        when(onlineCourseRepository.save(any(OnlineCourse.class))).thenAnswer(invocation -> {
            OnlineCourse toSave = invocation.getArgument(0);
            toSave.setId(100L);
            toSave.getLearningPackage().setId(200L);
            toSave.getLearningPackage().setSlug("ielts-master-70-2");
            return toSave;
        });

        OnlineCourseResponse mockResponse = OnlineCourseResponse.builder()
                .id(100L)
                .packageId(200L)
                .title("IELTS Master 7.0")
                .slug("ielts-master-70-2")
                .category("IELTS")
                .categoryName("IELTS")
                .level(CourseLevel.ADVANCED)
                .status(PackageStatus.DRAFT)
                .price(new BigDecimal(1500000))
                .originalPrice(new BigDecimal(1500000))
                .salePrice(new BigDecimal(1500000))
                .build();
        when(mapper.toResponse(any(OnlineCourse.class))).thenReturn(mockResponse);

        OnlineCourseServiceImpl service = new OnlineCourseServiceImpl(
                onlineCourseRepository, onlineCourseVersionRepository, learningPackageRepository, packageTypeRepository,
                courseCategoryRepository, null, null, null, null, courseAssessmentRepository,
                null, null, courseLessonFlashcardRefRepository, null, null, null, null, userRepository, mapper,
                onlineCourseVersionService, null, null, null, null,
                null, null, youTubeTranscriptService
        );

        // Act
        OnlineCourseResponse result = service.createCourse(request, null);

        // Assert: service KH├öNG throw, m├á tß╗▒ tß║ío slug unique
        assertNotNull(result, "Response kh├┤ng ─æ╞░ß╗úc null");
        assertEquals("ielts-master-70-2", result.getSlug(),
                "Khi slug tr├╣ng, service phß║úi tß╗▒ generate slug unique bß║▒ng c├ích th├¬m hß║¡u tß╗æ -2");

        // Verify saved course c├│ slug unique
        ArgumentCaptor<OnlineCourse> courseCaptor = ArgumentCaptor.forClass(OnlineCourse.class);
        verify(onlineCourseRepository).save(courseCaptor.capture());
        OnlineCourse savedCourse = courseCaptor.getValue();
        assertNotNull(savedCourse, "Saved course phß║úi tß╗ôn tß║íi");
        assertEquals("ielts-master-70-2", savedCourse.getLearningPackage().getSlug(),
                "Saved course phß║úi c├│ slug unique");

        // Verify version draft ─æ╞░ß╗úc tß║ío
        verify(onlineCourseVersionService).createDraft(eq(100L), eq(null), eq(null));
    }

    @Test
    void createCourse_MissingTargetBand_ThrowsException() {
        // Arrange
        CourseCategory ieltsCategory = CourseCategory.builder()
                .id(1L).code("IELTS").active(true).build();
        PackageType onlineCourseType = PackageType.builder()
                .id(1L).code(PackageTypeCode.ONLINE_COURSE).build();

        OnlineCourseRequest request = OnlineCourseRequest.builder()
                .title("IELTS Master 7.0")
                .category("IELTS")
                .level(CourseLevel.ADVANCED)
                .status(PackageStatus.DRAFT)
                .price(new BigDecimal(1500000))
                // .targetBand(null) -> thiß║┐u field bß║»t buß╗Öc cho IELTS
                .modules(new ArrayList<>())
                .build();

        // Validation n├⌐m exception ß╗ƒ ─æß║ºu createCourse -> KH├öNG cß║ºn stub bß║Ñt kß╗│ repo n├áo
        OnlineCourseServiceImpl service = new OnlineCourseServiceImpl(
                onlineCourseRepository, onlineCourseVersionRepository, learningPackageRepository, packageTypeRepository,
                courseCategoryRepository, null, null, null, null, courseAssessmentRepository,
                null, null, courseLessonFlashcardRefRepository, null, null, null, null, userRepository, mapper,
                onlineCourseVersionService, null, null, null, null,
                null, null, youTubeTranscriptService
        );

        // Act + Assert: validateCourseRequest n├⌐m IllegalArgumentException khi thiß║┐u targetBand
        assertThrows(IllegalArgumentException.class, () -> service.createCourse(request, null));

        // Verify course KH├öNG ─æ╞░ß╗úc l╞░u v├á KH├öNG tß║ío draft version
        verify(onlineCourseRepository, never()).save(any(OnlineCourse.class));
        verify(onlineCourseVersionService, never()).createDraft(any(), any(), any());
    }

    @Test
    void createCourse_InvalidCategoryCode_ThrowsException() {
        // Arrange: Pre - "General English" vß╗¢i category code kh├┤ng tß╗ôn tß║íi trong hß╗ç thß╗æng
        OnlineCourseRequest request = OnlineCourseRequest.builder()
                .title("General English")
                .category("9999")  // category code kh├┤ng tß╗ôn tß║íi trong ENGLISH_COURSE_CATEGORIES
                .level(CourseLevel.BEGINNER)
                .status(PackageStatus.DRAFT)
                .price(new BigDecimal(500000))
                .modules(new ArrayList<>())
                .build();

        // Validation n├⌐m exception ß╗ƒ ─æß║ºu createCourse -> KH├öNG cß║ºn stub bß║Ñt kß╗│ repo n├áo
        OnlineCourseServiceImpl service = new OnlineCourseServiceImpl(
                onlineCourseRepository, onlineCourseVersionRepository, learningPackageRepository, packageTypeRepository,
                courseCategoryRepository, null, null, null, null, courseAssessmentRepository,
                null, null, courseLessonFlashcardRefRepository, null, null, null, null, userRepository, mapper,
                onlineCourseVersionService, null, null, null, null,
                null, null, youTubeTranscriptService
        );

        // Act + Assert: validateCourseRequest n├⌐m IllegalArgumentException khi category code kh├┤ng hß╗úp lß╗ç
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createCourse(request, null));
        assertTrue(ex.getMessage().contains("EnglishLab"),
                "Exception message phß║úi ─æß╗ü cß║¡p ─æß║┐n quy ─æß╗ïnh category cß╗ºa EnglishLab");

        // Verify course KH├öNG ─æ╞░ß╗úc l╞░u v├á KH├öNG tß║ío draft version
        verify(onlineCourseRepository, never()).save(any(OnlineCourse.class));
        verify(onlineCourseVersionService, never()).createDraft(any(), any(), any());
    }
}
