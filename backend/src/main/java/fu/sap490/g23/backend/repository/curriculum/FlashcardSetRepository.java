package fu.sap490.g23.backend.repository.curriculum;

import fu.sap490.g23.backend.entity.curriculum.FlashcardSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlashcardSetRepository extends JpaRepository<FlashcardSet, Long> {
    List<FlashcardSet> findAllByOrderByUpdatedAtDescIdDesc();
}
