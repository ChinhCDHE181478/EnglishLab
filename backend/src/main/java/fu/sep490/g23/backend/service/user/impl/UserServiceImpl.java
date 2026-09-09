package fu.sep490.g23.backend.service.user.impl;

import fu.sep490.g23.backend.dto.request.ChangePasswordRequest;
import fu.sep490.g23.backend.dto.request.UpdateProfileRequest;
import fu.sep490.g23.backend.dto.response.UserResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.service.assessment.PlacementTestDefinitionService;
import fu.sep490.g23.backend.service.storage.ObjectStore;
import fu.sep490.g23.backend.service.user.AvatarStorageService;
import fu.sep490.g23.backend.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PlacementTestAttemptRepository placementTestAttemptRepository;
    private final AvatarStorageService avatarStorageService;
    private final ObjectStore objectStore;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

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
        // Upload to R2 first so we can roll back the upload if the DB save fails below.
        String fileName = avatarStorageService.store(file);
        // When the object store exposes objects publicly (e.g. R2 with a public bucket) we use the
        // canonical object URL; otherwise we fall back to the backend proxy URL.
        String avatarUrl = objectStore.isPublic()
                ? objectStore.publicUrl(objectStore.objectKey(AvatarStorageServiceImpl.PREFIX, fileName))
                : (publicUrlBase.endsWith("/") ? publicUrlBase + fileName : publicUrlBase + "/" + fileName);

        try {
            user.setAvatarUrl(avatarUrl);
            User savedUser = userRepository.save(user);
            // Publish event AFTER DB commit succeeds — deletes the previous avatar asynchronously.
            // This avoids holding a DB transaction open during the R2 delete call.
            if (oldAvatarUrl != null) {
                String objectKey = objectStore.objectKey(AvatarStorageServiceImpl.PREFIX,
                        extractAvatarFileName(oldAvatarUrl).orElse(null));
                if (objectKey != null) {
                    eventPublisher.publishEvent(
                            new fu.sep490.g23.backend.service.user.event.AvatarUpdatedEvent(objectKey));
                }
            }
            return toResponse(savedUser);
        } catch (RuntimeException exception) {
            // DB save failed AFTER the file was already uploaded. Roll back the upload so we
            // do not leave a one-off orphan. Any previously published event has not fired yet
            // because Spring delays @TransactionalEventListener until commit.
            try {
                objectStore.delete(objectStore.objectKey(AvatarStorageServiceImpl.PREFIX, fileName));
            } catch (RuntimeException cleanupException) {
                log.warn("Failed to roll back orphan avatar upload {}: {}", fileName, cleanupException.getMessage());
            }
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

        // Publish event AFTER DB commit succeeds — deletes the avatar file asynchronously.
        if (oldAvatarUrl != null) {
            String objectKey = objectStore.objectKey(AvatarStorageServiceImpl.PREFIX,
                    extractAvatarFileName(oldAvatarUrl).orElse(null));
            if (objectKey != null) {
                eventPublisher.publishEvent(
                        new fu.sep490.g23.backend.service.user.event.AvatarDeletedEvent(objectKey));
            }
        }
        return toResponse(savedUser);
    }

    /**
     * Extracts just the file name (without path or query params) from a stored avatar URL.
     * Used to compute the R2 object key for event-driven cleanup.
     */
    private java.util.Optional<String> extractAvatarFileName(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return java.util.Optional.empty();
        }
        String normalized = avatarUrl.trim().replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == normalized.length() - 1) {
            return java.util.Optional.empty();
        }
        String fileName = normalized.substring(lastSlash + 1);
        int queryIndex = fileName.indexOf('?');
        if (queryIndex >= 0) {
            fileName = fileName.substring(0, queryIndex);
        }
        try {
            fileName = java.net.URLDecoder.decode(fileName, java.nio.charset.StandardCharsets.UTF_8).trim();
        } catch (Exception ignored) {
        }
        if (fileName.isBlank() || fileName.contains("..") || fileName.contains("/")) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(fileName);
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = requireUser(email);
        if (user.isPasswordSet() && (user.getPassword() == null
                || request.getCurrentPassword() == null
                || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword()))) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        }
        if (user.isPasswordSet() && passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordSet(true);
        userRepository.save(user);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getPrimaryRoleCode())
                .roles(user.getRoleCodes().stream().sorted().toList())
                .phoneNumber(user.getPhoneNumber())
                .targetExam(user.getTargetExam())
                .targetScore(user.getTargetScore())
                .currentBand(user.getCurrentBand())
                .studyGoal(user.getStudyGoal())
                .avatarUrl(user.getAvatarUrl())
                .passwordSet(user.isPasswordSet())
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
