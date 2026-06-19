package fu.sap490.g23.backend.security;

import fu.sap490.g23.backend.entity.Role;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ClassroomAccessHelper {

    private static final Set<Role> TRAINING_MANAGER_ROLES = EnumSet.of(Role.TRAINING_MANAGER, Role.MANAGER, Role.ADMIN);
    private static final Set<Role> MANAGER_ROLES = EnumSet.of(Role.MANAGER, Role.TRAINING_MANAGER, Role.ADMIN);
    private static final Set<Role> TEACHER_ROLES = EnumSet.of(Role.TEACHER, Role.TRAINING_MANAGER, Role.MANAGER, Role.ADMIN);
    private static final Set<Role> CONTENT_MANAGER_ROLES = EnumSet.of(Role.CONTENT_MANAGER, Role.MANAGER, Role.ADMIN);

    private final UserRepository userRepository;

    public User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
    }

    public boolean canManageClassroom(User user) {
        return MANAGER_ROLES.contains(user.getRole()) || CONTENT_MANAGER_ROLES.contains(user.getRole());
    }

    public boolean canTeach(User user) {
        return TEACHER_ROLES.contains(user.getRole());
    }

    public boolean canApproveRequests(User user) {
        return TRAINING_MANAGER_ROLES.contains(user.getRole());
    }

    public boolean canManageTrainingOperations(User user) {
        return TRAINING_MANAGER_ROLES.contains(user.getRole());
    }

    public void assertTrainingManager(User user) {
        if (!canManageTrainingOperations(user)) {
            throw new RuntimeException("Bạn không có quyền Training Manager.");
        }
    }

    public void assertManager(User user) {
        if (!canManageClassroom(user) && !canApproveRequests(user)) {
            throw new RuntimeException("Bạn không có quyền truy cập nội dung này.");
        }
    }

    public void assertTeacher(User user) {
        if (!canTeach(user)) {
            throw new RuntimeException("Bạn không có quyền truy cập nội dung này.");
        }
    }
}
