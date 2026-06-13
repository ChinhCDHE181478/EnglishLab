package fu.sap490.g23.backend.service.impl;

import fu.sap490.g23.backend.dto.request.UpdateProfileRequest;
import fu.sap490.g23.backend.dto.response.UserResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserRepository userRepository;

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

        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(request.getPhoneNumber().trim());
        user.setTargetExam(request.getTargetExam().trim());
        user.setTargetScore(trimToNull(request.getTargetScore()));
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
                .phoneNumber(user.getPhoneNumber())
                .targetExam(user.getTargetExam())
                .targetScore(user.getTargetScore())
                .currentBand(user.getCurrentBand())
                .studyGoal(user.getStudyGoal())
                .profileCompleted(user.isProfileCompleted())
                .build();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
