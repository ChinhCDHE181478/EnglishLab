package fu.sep490.g23.backend.service.home.impl;

import fu.sep490.g23.backend.dto.response.home.HomePageResponse;
import fu.sep490.g23.backend.dto.response.home.HomeTeacherResponse;
import fu.sep490.g23.backend.dto.response.home.HomeTestimonialResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.entity.teacher.enums.CredentialVerificationStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.repository.teacher.TeacherCredentialRepository;
import fu.sep490.g23.backend.service.home.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final UserRepository userRepository;
    private final TeacherCredentialRepository credentialRepository;
    private final OnlineCourseEnrollmentRepository enrollmentRepository;

    @Override
    @Transactional(readOnly = true)
    public HomePageResponse getHomePage() {
        List<User> teachers = userRepository.findPublicTeachersByRoleCode(RoleCodes.TEACHER);
        Map<Long, List<String>> badgesByTeacher = loadVerifiedBadges(teachers);

        return HomePageResponse.builder()
                .teachers(teachers.stream()
                        .map(teacher -> toTeacherResponse(teacher, badgesByTeacher.getOrDefault(teacher.getId(), List.of())))
                        .toList())
                .testimonials(enrollmentRepository
                        .findTop5ByReviewRatingAndReviewCommentIsNotNullAndOnlineCourse_StatusOrderByReviewedAtDescIdDesc(
                                5,
                                PackageStatus.PUBLISHED
                        ).stream()
                        .map(this::toTestimonialResponse)
                        .toList())
                .build();
    }

    private Map<Long, List<String>> loadVerifiedBadges(List<User> teachers) {
        if (teachers.isEmpty()) {
            return Map.of();
        }
        List<Long> teacherIds = teachers.stream().map(User::getId).toList();
        Map<Long, List<String>> badgesByTeacher = new LinkedHashMap<>();
        credentialRepository.findByTeacherIdInAndVerificationStatusOrderByIssuedDateDescIdDesc(
                teacherIds,
                CredentialVerificationStatus.VERIFIED
        ).forEach(credential -> badgesByTeacher
                .computeIfAbsent(credential.getTeacher().getId(), ignored -> new ArrayList<>())
                .add(credential.getTitle()));
        return badgesByTeacher;
    }

    private HomeTeacherResponse toTeacherResponse(User teacher, List<String> verifiedCredentials) {
        List<String> badges = new ArrayList<>(verifiedCredentials);
        if (hasText(teacher.getTeacherHighestQualification())) {
            badges.add(teacher.getTeacherHighestQualification().trim());
        }
        if (teacher.getTeacherYearsOfExperience() != null && teacher.getTeacherYearsOfExperience() > 0) {
            badges.add(teacher.getTeacherYearsOfExperience() + " năm kinh nghiệm");
        }

        return HomeTeacherResponse.builder()
                .id(teacher.getId())
                .name(teacher.getFullName())
                .avatarUrl(teacher.getAvatarUrl())
                .headline(firstNonBlank(teacher.getTeacherHeadline(), teacher.getTeacherSpecializations(), "Giảng viên EnglishLab"))
                .biography(teacher.getTeacherBiography())
                .specializations(teacher.getTeacherSpecializations())
                .yearsOfExperience(teacher.getTeacherYearsOfExperience())
                .highestQualification(teacher.getTeacherHighestQualification())
                .badges(badges.stream().filter(this::hasText).map(String::trim).distinct().limit(3).toList())
                .build();
    }

    private HomeTestimonialResponse toTestimonialResponse(OnlineCourseEnrollment enrollment) {
        return HomeTestimonialResponse.builder()
                .id(enrollment.getId())
                .learnerName(shortenLearnerName(enrollment.getStudent().getFullName()))
                .courseTitle(enrollment.getOnlineCourse().getTitle())
                .rating(enrollment.getReviewRating())
                .comment(enrollment.getReviewComment().trim())
                .reviewedAt(enrollment.getReviewedAt())
                .build();
    }

    private String shortenLearnerName(String fullName) {
        if (!hasText(fullName)) {
            return "Học viên EnglishLab";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        String familyInitial = parts[0].substring(0, 1).toUpperCase(Locale.forLanguageTag("vi"));
        return parts[parts.length - 1] + " " + familyInitial + ".";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
