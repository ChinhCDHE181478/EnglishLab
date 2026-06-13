package fu.sap490.g23.backend.config;

import fu.sap490.g23.backend.dto.request.course.LessonRequest;
import fu.sap490.g23.backend.dto.request.course.ModuleRequest;
import fu.sap490.g23.backend.dto.request.course.OnlineCourseRequest;
import fu.sap490.g23.backend.entity.Role;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.*;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.course.CourseCategoryRepository;
import fu.sap490.g23.backend.repository.course.LearningPackageRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sap490.g23.backend.repository.course.PackageTypeRepository;
import fu.sap490.g23.backend.service.course.OnlineCourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class OnlineCourseDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final LearningPackageRepository learningPackageRepository;
    private final PackageTypeRepository packageTypeRepository;
    private final CourseCategoryRepository courseCategoryRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final OnlineCourseService onlineCourseService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        seedPackageTypes();
        seedCourseCategories();
        backfillMissingCourseCategories();

        User contentManager = userRepository.findByEmail("content.manager@englishlab.edu.vn")
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("Content Manager")
                        .email("content.manager@englishlab.edu.vn")
                        .password(passwordEncoder.encode("123456"))
                        .role(Role.CONTENT_MANAGER)
                        .profileCompleted(true)
                        .build()));

        if (learningPackageRepository.countByDeletedFalse() > 0) {
            return;
        }

        List<OnlineCourseRequest> seedCourses = List.of(
                course("IELTS Foundation", "Xây dựng nền tảng vững chắc 4 kỹ năng chuẩn học thuật cho người mới bắt đầu.", CourseCategoryCode.IELTS, CourseLevel.BEGINNER, "IELTS 4.0 - 5.0", "12 Tuần", "Online + Mentor", 1290000, 24, 36, 1, true, "https://lh3.googleusercontent.com/aida-public/AB6AXuDRAdoUP7vB31FSLT499I6zjFvudpAf-lZ7R3FInQasq73Ei_QKhKNT3BZmpVy9p6zsyo1eRN5Yr_CXmXpCfJHFD6r7PoQ6v1rvBcUkF5_s8Gv-lyzXxDyRbMjB43k5nWIrugE8ruTwfypxLMqmCmWnKnG9RhPQmQ_eYCoaIXbGhUdImte8PNhrVrLpfoZHhcRq_WC0JYzirxpWgInlWfP6-ozgGYenE3C9O74Y7oK529v2b78F3W-1z1zpe3dnSX1JkFTimk7H6PI"),
                course("IELTS Intensive", "Khóa học cấp tốc tập trung vào chiến thuật làm bài và nâng cao band điểm nhanh chóng.", CourseCategoryCode.IELTS, CourseLevel.ADVANCED, "IELTS 6.5+", "8 Tuần", "Online cấp tốc", 1890000, 32, 48, 2, true, "https://lh3.googleusercontent.com/aida-public/AB6AXuDjBydaQSMS4RLvcU1w7wqme_suKd-qC4Pu9Jqh1KDjMobdQIktyyXTqy6SY0s4dCSC2ZRZD55t-3sq-YANaQric08DJ6SB1lm0CbpcNqLD7s5eULDK6t3K8Y51h6zz4LND-rV37liCF790ZMzdPcrefMN0UB13H7j_Y9XemUVIR-htB20erL5IboK3mfLqF8j7_UpDC3FUkZIlifiYI7EQTdRKzPj73GnBisF7oHX-H3gzZYGXqJIkGzsP5p6H2b-oWn3JpBs_KJg"),
                course("TOEIC Master", "Lộ trình bứt phá điểm số TOEIC cho môi trường công sở và thăng tiến sự nghiệp.", CourseCategoryCode.TOEIC, CourseLevel.INTERMEDIATE, "TOEIC 750+", "10 Tuần", "Online", 1490000, 28, 40, 3, false, "https://lh3.googleusercontent.com/aida-public/AB6AXuCve_bWMIZ3mkiTsbdf862_uYRiushINo6Pk2zQmZ-XgXyE6HMsPNwnns0m-LWt-3ZIvSegmSC8qYBsimXR92fna0tn_IKc1YoN8eAWruFwILlg4v0xVgSTlZkIu38aV9vBaqiNlha9YWi5rYICeuHp9pZr2Nm2WttdQqUu636uP79sw44kL2NeA3B45pNxUYeft_1BHjVShIjVk4NeetOomtBt-aVHyvKnca81T38w6RrcE8x-F2WUykau5ZlBXBoev9CLK1KW5Dg"),
                course("English Communication", "Giao tiếp tự tin, phản xạ tự nhiên và phát âm chuẩn bản xứ trong 3 tháng.", CourseCategoryCode.COMMUNICATION, CourseLevel.INTERMEDIATE, "Intermediate", "14 Tuần", "Live Online", 1690000, 30, 42, 4, false, "https://lh3.googleusercontent.com/aida-public/AB6AXuAcwby3fvb8ZTClDeZbTrRTooCOrA-u1sUJSsf1vNwQWHdnWhmZgqAnJO7w7hpJEKbOscjvbHkfyn4aIxtXKatMZBXC2-nwMyhhg-am32EwgeIpIzF7kTIcAWiGGpSDst0tpXzG0Gena_wPRVA8T2RU9DtInVQ2ZSBvRvoECKW92kl-PziH465bqZXdU9wXpH2vgR5tx8rzooYzPw0EIJ-nXp0WIeroD2I4tFcRiEwqJkDM-iIK99eN-XjPIi2nyW8jAYVFjsflmzU")
        );

        seedCourses.forEach(course -> onlineCourseService.createCourse(course, contentManager.getEmail()));
    }

    private void seedPackageTypes() {
        seedPackageType(PackageTypeCode.ONLINE_COURSE, "Online Course", "Self-paced or mentor-supported online learning package.");
        seedPackageType(PackageTypeCode.CLASSROOM, "Classroom", "Offline or blended classroom package.");
        seedPackageType(PackageTypeCode.BUNDLE, "Bundle", "A bundle containing multiple learning products.");
        seedPackageType(PackageTypeCode.MOCK_TEST, "Mock Test", "Certification exam simulation package.");
        seedPackageType(PackageTypeCode.SUBSCRIPTION, "Subscription", "Recurring access package.");
    }

    private void seedPackageType(PackageTypeCode code, String name, String description) {
        if (!packageTypeRepository.existsByCode(code)) {
            packageTypeRepository.save(PackageType.builder().code(code).name(name).description(description).build());
        }
    }

    private void seedCourseCategories() {
        seedCategory(CourseCategoryCode.IELTS, "IELTS", "IELTS preparation courses", 1);
        seedCategory(CourseCategoryCode.TOEIC, "TOEIC", "TOEIC preparation courses", 2);
        seedCategory(CourseCategoryCode.COMMUNICATION, "Giao tiếp", "English communication courses", 3);
        seedCategory(CourseCategoryCode.FOUNDATION, "Mất gốc", "Foundation English courses", 4);
        seedCategory(CourseCategoryCode.ONLINE, "Online", "Online-first learning programs", 5);
    }

    private void seedCategory(CourseCategoryCode code, String name, String description, int order) {
        if (!courseCategoryRepository.existsByCode(code)) {
            courseCategoryRepository.save(CourseCategory.builder()
                    .code(code)
                    .name(name)
                    .description(description)
                    .displayOrder(order)
                    .build());
        }
    }

    private void backfillMissingCourseCategories() {
        CourseCategory defaultCategory = courseCategoryRepository.findByCode(CourseCategoryCode.ONLINE)
                .orElseThrow(() -> new IllegalStateException("ONLINE course category is missing"));

        for (OnlineCourse course : onlineCourseRepository.findAllByCategoryIsNull()) {
            course.setCategory(resolveCategory(course, defaultCategory));
        }
    }

    private CourseCategory resolveCategory(OnlineCourse course, CourseCategory defaultCategory) {
        String normalized = (course.getLearningPackage().getTitle() + " " + course.getLearningPackage().getSlug())
                .toLowerCase(Locale.ROOT);
        if (normalized.contains("ielts")) {
            return findCategory(CourseCategoryCode.IELTS, defaultCategory);
        }
        if (normalized.contains("toeic")) {
            return findCategory(CourseCategoryCode.TOEIC, defaultCategory);
        }
        if (normalized.contains("communication")) {
            return findCategory(CourseCategoryCode.COMMUNICATION, defaultCategory);
        }
        if (normalized.contains("foundation")) {
            return findCategory(CourseCategoryCode.FOUNDATION, defaultCategory);
        }
        return defaultCategory;
    }

    private CourseCategory findCategory(CourseCategoryCode code, CourseCategory fallback) {
        return courseCategoryRepository.findByCode(code).orElse(fallback);
    }

    private OnlineCourseRequest course(String title, String shortDescription, CourseCategoryCode category, CourseLevel level, String targetScore,
                                       String duration, String studyMode, int price, int lessons, int hours, int order, boolean featured, String thumbnailUrl) {
        return OnlineCourseRequest.builder()
                .title(title)
                .shortDescription(shortDescription)
                .description(shortDescription + " Nội dung gồm video bài giảng, tài liệu học tập, bài tập thực hành và theo dõi tiến độ cá nhân.")
                .category(category)
                .level(level)
                .status(PackageStatus.PUBLISHED)
                .targetScore(targetScore)
                .duration(duration)
                .studyMode(studyMode)
                .price(BigDecimal.valueOf(price))
                .thumbnailUrl(thumbnailUrl)
                .totalLessons(lessons)
                .totalHours(hours)
                .displayOrder(order)
                .featured(featured)
                .modules(List.of(
                        ModuleRequest.builder()
                                .title("Tổng quan khóa học")
                                .description("Làm quen mục tiêu, cách học và tài liệu cần chuẩn bị.")
                                .displayOrder(1)
                                .lessons(List.of(lesson("Giới thiệu lộ trình", 20, true), lesson("Chiến lược học hiệu quả", 35, false)))
                                .build(),
                        ModuleRequest.builder()
                                .title("Bài học trọng tâm")
                                .description("Các nội dung cốt lõi giúp học viên đạt mục tiêu đầu ra.")
                                .displayOrder(2)
                                .lessons(List.of(lesson("Kỹ thuật nền tảng", 45, false), lesson("Thực hành có hướng dẫn", 50, false)))
                                .build()
                ))
                .build();
    }

    private LessonRequest lesson(String title, int minutes, boolean preview) {
        return LessonRequest.builder()
                .title(title)
                .description("Video bài giảng và tài liệu kèm theo.")
                .durationMinutes(minutes)
                .displayOrder(minutes)
                .preview(preview)
                .build();
    }
}
