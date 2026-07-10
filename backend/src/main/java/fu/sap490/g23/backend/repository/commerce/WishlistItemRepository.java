package fu.sap490.g23.backend.repository.commerce;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.commerce.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByStudentOrderByAddedAtDesc(User student);
    Optional<WishlistItem> findByStudentAndOnlineCourseId(User student, Long onlineCourseId);
    void deleteByStudentAndOnlineCourseId(User student, Long onlineCourseId);
}
