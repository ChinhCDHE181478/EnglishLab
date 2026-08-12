package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.course.SavedVocabulary;
import fu.sep490.g23.backend.entity.course.enums.VocabularyMasteryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedVocabularyRepository extends JpaRepository<SavedVocabulary, Long> {

    Optional<SavedVocabulary> findByUserIdAndWordIgnoreCase(Long userId, String word);

    List<SavedVocabulary> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<SavedVocabulary> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, VocabularyMasteryStatus status);
}
