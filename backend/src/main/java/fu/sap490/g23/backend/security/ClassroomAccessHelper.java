package fu.sap490.g23.backend.security;

import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ClassroomAccessHelper {

    private static final Set<RoleEnum> MANAGER_ROLES = EnumSet.of(RoleEnum.STAFF, RoleEnum.MANAGER, RoleEnum.TRAINING_MANAGER, RoleEnum.ADMIN);
    private static final Set<RoleEnum> TEACHER_ROLES = EnumSet.of(RoleEnum.TEACHER, RoleEnum.TRAINING_MANAGER, RoleEnum.MANAGER, RoleEnum.ADMIN);
    private static final Set<RoleEnum> CONTENT_MANAGER_ROLES = EnumSet.of(RoleEnum.CONTENT_MANAGER, RoleEnum.MANAGER, RoleEnum.ADMIN);

    private final UserRepository userRepository;

    public User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
    }

    public boolean canManageClassroom(User user) {
        return user.hasAnyRole(MANAGER_ROLES) || user.hasAnyRole(CONTENT_MANAGER_ROLES);
    }

    public boolean canTeach(User user) {
        return user.hasAnyRole(TEACHER_ROLES);
    }

    public boolean canApproveRequests(User user) {
        return TrainingRolePolicy.canOperate(user);
    }

    public boolean canManageTrainingOperations(User user) {
        return TrainingRolePolicy.canOperate(user);
    }

    public void assertTrainingManager(User user) {
        if (!canManageTrainingOperations(user)) {
            throw new RuntimeException("Bạn không có quyền vận hành đào tạo.");
        }
    }

    public void assertManager(User user) {
        if (!canManageTrainingOperations(user)) {
            throw new RuntimeException("Bạn không có quyền truy cập nội dung này.");
        }
    }

    public void assertTeacher(User user) {
        if (!canTeach(user)) {
            throw new RuntimeException("Bạn không có quyền truy cập nội dung này.");
        }
    }
}
