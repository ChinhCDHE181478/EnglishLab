package fu.sep490.g23.backend.repository.commerce;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.commerce.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByStudentOrderByAddedAtDesc(User student);
    Optional<CartItem> findByStudentAndOnlineCourseId(User student, Long onlineCourseId);
    void deleteByStudentAndOnlineCourseId(User student, Long onlineCourseId);
    long countByStudent(User student);
}
