package fu.sap490.g23.backend.service.user.impl;

import fu.sap490.g23.backend.service.user.*;

import fu.sap490.g23.backend.dto.request.ChangePasswordRequest;
import fu.sap490.g23.backend.dto.request.UpdateProfileRequest;
import fu.sap490.g23.backend.dto.response.UserResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sap490.g23.backend.service.assessment.PlacementTestDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PlacementTestAttemptRepository placementTestAttemptRepository;
    private final AvatarStorageService avatarStorageService;
    private final PasswordEncoder passwordEncoder;

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
        if (request.getCurrentBand() != null) {
            user.setCurrentBand(request.getCurrentBand());
        }
        user.setStudyGoal(trimToNull(request.getStudyGoal()));
        user.setProfileCompleted(true);

        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse updateAvatar(String email, MultipartFile file, String publicUrlBase) {
        User user = requireUser(email);
        String oldAvatarUrl = user.getAvatarUrl();
        String fileName = avatarStorageService.store(file);
        String avatarUrl = publicUrlBase.endsWith("/") ? publicUrlBase + fileName : publicUrlBase + "/" + fileName;

        try {
            user.setAvatarUrl(avatarUrl);
            User savedUser = userRepository.save(user);
            avatarStorageService.deleteByUrl(oldAvatarUrl);
            return toResponse(savedUser);
        } catch (RuntimeException exception) {
            avatarStorageService.delete(fileName);
            throw exception;
        }
    }

    @Override
    @Transactional
    public UserResponse deleteAvatar(String email) {
        User user = requireUser(email);
        String oldAvatarUrl = user.getAvatarUrl();
        user.setAvatarUrl(null);
        User savedUser = userRepository.save(user);
        avatarStorageService.deleteByUrl(oldAvatarUrl);
        return toResponse(savedUser);
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = requireUser(email);
        if (user.getPassword() == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
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
                .avatarUrl(user.getAvatarUrl())
                .profileCompleted(user.isProfileCompleted())
                .placementTestCompleted(placementTestAttemptRepository.existsByStudentAndTestCode(user, PlacementTestDefinitionService.TEST_CODE))
                .build();
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản."));
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
