package fu.sep490.g23.backend.repository.curriculum;

import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.curriculum.AssessmentBankItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssessmentBankItemRepository extends JpaRepository<AssessmentBankItem, Long>, JpaSpecificationExecutor<AssessmentBankItem> {
    List<AssessmentBankItem> findAllByOrderByUpdatedAtDescIdDesc();

    List<AssessmentBankItem> findBySkillOrderByUpdatedAtDescIdDesc(AssessmentSkill skill);

    @Query(value = """
            SELECT * FROM content_bank_items
            WHERE bank_type = 'ASSESSMENT'
              AND payload_jsonb->>'type' = :#{#type.name()}
            ORDER BY updated_at DESC NULLS LAST, id DESC
            """, nativeQuery = true)
    List<AssessmentBankItem> findByTypeOrderByUpdatedAtDescIdDesc(@Param("type") AssessmentType type);

    @Query(value = """
            SELECT * FROM content_bank_items
            WHERE bank_type = 'ASSESSMENT'
              AND payload_jsonb->>'type' = :#{#type.name()}
              AND status = :status
              AND active = true
            ORDER BY display_order ASC, updated_at DESC NULLS LAST, id DESC
            """, nativeQuery = true)
    List<AssessmentBankItem> findByTypeAndStatusAndActiveTrueOrderByDisplayOrderAscUpdatedAtDescIdDesc(
            @Param("type") AssessmentType type,
            @Param("status") String status
    );

    @Query(value = """
            SELECT * FROM content_bank_items
            WHERE bank_type = 'ASSESSMENT'
              AND id = :id
              AND payload_jsonb->>'type' = :#{#type.name()}
              AND status = :status
              AND active = true
            """, nativeQuery = true)
    Optional<AssessmentBankItem> findByIdAndTypeAndStatusAndActiveTrue(
            @Param("id") Long id,
            @Param("type") AssessmentType type,
            @Param("status") String status
    );

    @Query(value = """
            SELECT * FROM content_bank_items
            WHERE bank_type = 'ASSESSMENT'
              AND payload_jsonb->>'type' = :#{#type.name()}
              AND status = :status
              AND active = true
              AND skill IN (:skills)
            ORDER BY display_order ASC, updated_at DESC NULLS LAST, id DESC
            """, nativeQuery = true)
    List<AssessmentBankItem> findByTypeAndStatusAndActiveTrueAndSkillInOrderByDisplayOrderAscUpdatedAtDescIdDesc(
            @Param("type") AssessmentType type,
            @Param("status") String status,
            @Param("skills") List<AssessmentSkill> skills
    );
}
