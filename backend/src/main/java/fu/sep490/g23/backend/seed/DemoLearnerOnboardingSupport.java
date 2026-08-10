package fu.sap490.g23.backend.seed;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.PlacementTestAttempt;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sap490.g23.backend.service.assessment.PlacementTestDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DemoLearnerOnboardingSupport {

    static final List<String> DEMO_LEARNER_EMAILS = List.of(
            "0386852628z@gmail.com",
            "classroom.learner2@englishlab.vn",
            "classroom.learner3@englishlab.vn",
            "classroom.learner4@englishlab.vn"
    );

    private static final String PLACEMENT_TEST_CODE = PlacementTestDefinitionService.TEST_CODE;
    private static final String SEED_ANSWERS_JSON = "{\"seed\":true,\"note\":\"Demo learner — placement test bypassed in seed\"}";

    private final UserRepository userRepository;
    private final PlacementTestAttemptRepository placementTestAttemptRepository;

    public void ensureAllDemoLearners() {
        DEMO_LEARNER_EMAILS.forEach(this::ensureReadyByEmail);
    }

    public void ensureReadyByEmail(String email) {
        userRepository.findByEmail(email).ifPresent(this::ensureReady);
    }

    public User ensureReady(User user) {
        if (user == null || !user.hasRole(RoleEnum.LEARNER)) {
            return user;
        }
        applyDemoProfile(user);
        ensurePlacementAttempt(user);
        return userRepository.save(user);
    }

    private void applyDemoProfile(User user) {
        if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
            user.setPhoneNumber(demoPhoneFor(user.getEmail()));
        }
        if (user.getTargetExam() == null || user.getTargetExam().isBlank()) {
            user.setTargetExam("IELTS");
        }
        if (user.getTargetScore() == null || user.getTargetScore().isBlank()) {
            user.setTargetScore("6.5");
        }
        if (user.getStudyGoal() == null || user.getStudyGoal().isBlank()) {
            user.setStudyGoal("Luyện thi IELTS và tham gia lớp ảo EnglishLab.");
        }
        if (user.getCurrentBand() == null) {
            user.setCurrentBand(6.0);
        }
        user.setProfileCompleted(true);
    }

    private void ensurePlacementAttempt(User user) {
        if (placementTestAttemptRepository.existsByStudentAndTestCode(user, PLACEMENT_TEST_CODE)) {
            return;
        }
        BigDecimal band = BigDecimal.valueOf(6.0);
        placementTestAttemptRepository.save(PlacementTestAttempt.builder()
                .student(user)
                .testCode(PLACEMENT_TEST_CODE)
                .answersJson(SEED_ANSWERS_JSON)
                .listeningScore(band)
                .readingScore(band)
                .writingScore(band)
                .speakingScore(band)
                .overallScore(band)
                .correctListening(20)
                .correctReading(20)
                .status("COMPLETED")
                .submittedAt(LocalDateTime.now())
                .build());
    }

    private static String demoPhoneFor(String email) {
        int hash = Math.abs(email.hashCode() % 1_000_000);
        return "09" + String.format("%08d", hash).substring(0, 8);
    }
}
