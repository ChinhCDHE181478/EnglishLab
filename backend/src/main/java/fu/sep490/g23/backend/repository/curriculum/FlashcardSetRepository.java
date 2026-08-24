package fu.sep490.g23.backend.repository.curriculum;

import fu.sep490.g23.backend.entity.curriculum.FlashcardSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface FlashcardSetRepository extends JpaRepository<FlashcardSet, Long>, JpaSpecificationExecutor<FlashcardSet> {
    List<FlashcardSet> findAllByOrderByUpdatedAtDescIdDesc();

    Optional<FlashcardSet> findByTitleIgnoreCase(String title);
}
