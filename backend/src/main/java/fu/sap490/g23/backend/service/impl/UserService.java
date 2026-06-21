package fu.sap490.g23.backend.service.impl;

import fu.sap490.g23.backend.dto.request.UpdateProfileRequest;
import fu.sap490.g23.backend.dto.response.UserResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sap490.g23.backend.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private static final String PLACEMENT_TEST_CODE = "IELTS_PLACEMENT_MOCK_1";

    private final UserRepository userRepository;
    private final PlacementTestAttemptRepository placementTestAttemptRepository;

    @Override
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return toResponse(user);
    }

    @Override
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String targetExam = request.getTargetExam().trim().toUpperCase();
        String targetScore = trimToNull(request.getTargetScore());
        if (!"IELTS".equals(targetExam) && !"TOEIC".equals(targetExam)) {
            throw new IllegalArgumentException("Mục tiêu học chỉ có thể là IELTS hoặc TOEIC.");
        }
        if (!isAllowedTargetScore(targetExam, targetScore)) {
            throw new IllegalArgumentException("Điểm mục tiêu không hợp lệ cho kỳ thi đã chọn.");
        }

        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(request.getPhoneNumber().trim());
        user.setTargetExam(targetExam);
        user.setTargetScore(targetScore);
        user.setCurrentBand(request.getCurrentBand());
        user.setStudyGoal(trimToNull(request.getStudyGoal()));
        user.setProfileCompleted(true);

        return toResponse(userRepository.save(user));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .roles(user.getRoleCodes().stream().map(Enum::name).sorted().toList())
                .phoneNumber(user.getPhoneNumber())
                .targetExam(user.getTargetExam())
                .targetScore(user.getTargetScore())
                .currentBand(user.getCurrentBand())
                .studyGoal(user.getStudyGoal())
                .profileCompleted(user.isProfileCompleted())
                .placementTestCompleted(placementTestAttemptRepository.existsByStudentAndTestCode(user, PLACEMENT_TEST_CODE))
                .build();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean isAllowedTargetScore(String targetExam, String targetScore) {
        if (targetScore == null) {
            return false;
        }
        try {
            if ("IELTS".equals(targetExam)) {
                double value = Double.parseDouble(targetScore);
                return value >= 0 && value <= 9 && Math.abs(value * 2 - Math.rint(value * 2)) < 0.000001;
            }
            int value = Integer.parseInt(targetScore);
            return value >= 10 && value <= 990 && value % 5 == 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
