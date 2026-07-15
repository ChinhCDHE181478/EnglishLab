package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.response.course.VocabularyTermResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.VocabularyProgress;
import fu.sap490.g23.backend.entity.course.enums.FlashcardPracticeSource;
import fu.sap490.g23.backend.entity.course.enums.VocabularyProgressStatus;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sap490.g23.backend.repository.course.VocabularyProgressRepository;
import fu.sap490.g23.backend.service.course.impl.OnlineCourseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnlineCourseVocabularyProgressTest {
    private static final String EMAIL = "learner@example.com";
    private static final String TERM_KEY = "flashcard-set-2-0-immediate-family";

    @Mock
    private OnlineCourseRepository onlineCourseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VocabularyProgressRepository vocabularyProgressRepository;
    @Mock
    private CourseEnrollmentAccessPolicy courseEnrollmentAccessPolicy;
    @Mock
    private FlashcardPracticeService flashcardPracticeService;

    @InjectMocks
    private OnlineCourseServiceImpl service;

    @Test
    void updateVocabularyProgressAcceptsTermReturnedByFlashcardPracticeSource() {
        User learner = User.builder().id(9L).email(EMAIL).build();
        OnlineCourse course = OnlineCourse.builder()
                .id(6L)
                .learningPackage(LearningPackage.builder().deleted(false).build())
                .build();
        VocabularyTermResponse term = VocabularyTermResponse.builder()
                .courseId(6L)
                .termKey(TERM_KEY)
                .term("immediate family")
                .meaning("gia đình ruột thịt")
                .status(VocabularyProgressStatus.NEW)
                .build();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(learner));
        when(onlineCourseRepository.findWithModulesById(6L)).thenReturn(Optional.of(course));
        when(flashcardPracticeService.getPracticeTerms(
                FlashcardPracticeSource.ENROLLED, 6L, false, EMAIL
        )).thenReturn(List.of(term));
        when(vocabularyProgressRepository.findByStudentAndCourseAndTermKey(learner, course, TERM_KEY))
                .thenReturn(Optional.empty());
        when(vocabularyProgressRepository.save(any(VocabularyProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VocabularyTermResponse response = service.updateVocabularyProgress(
                6L, TERM_KEY, null, true, null, null, EMAIL
        );

        ArgumentCaptor<VocabularyProgress> progressCaptor = ArgumentCaptor.forClass(VocabularyProgress.class);
        verify(vocabularyProgressRepository).save(progressCaptor.capture());
        assertThat(progressCaptor.getValue().isStarred()).isTrue();
        assertThat(response.isStarred()).isTrue();
    }
}
