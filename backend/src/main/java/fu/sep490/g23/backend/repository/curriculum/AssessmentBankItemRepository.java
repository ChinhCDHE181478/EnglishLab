package fu.sep490.g23.backend.repository.curriculum;

import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.curriculum.AssessmentBankItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssessmentBankItemRepository extends JpaRepository<AssessmentBankItem, Long> {
    List<AssessmentBankItem> findAllByOrderByUpdatedAtDescIdDesc();

    List<AssessmentBankItem> findBySkillOrderByUpdatedAtDescIdDesc(AssessmentSkill skill);

    @Query(value = """
            SELECT *
            FROM content_bank_items
            WHERE bank_type = 'ASSESSMENT'
              AND (:skill = '' OR skill = :skill)
              AND (:type = '' OR payload_jsonb->>'type' = :type)
              AND (:status = '' OR status = :status)
              AND (
                  :keyword = ''
                  OR LOWER(title) LIKE :keyword
                  OR LOWER(COALESCE(description, '')) LIKE :keyword
                  OR LOWER(COALESCE(payload_jsonb->>'instructions', '')) LIKE :keyword
              )
              AND (
                  :examCategory = ''
                  OR (
                      :examCategory = 'TOEIC'
                      AND (
                          LOWER(COALESCE(payload_jsonb->>'uiConfigJson', '')) LIKE '%toeic%'
                          OR LOWER(title) LIKE '%toeic%'
                      )
                  )
                  OR (
                      :examCategory <> 'TOEIC'
                      AND NOT (
                          LOWER(COALESCE(payload_jsonb->>'uiConfigJson', '')) LIKE '%toeic%'
                          OR LOWER(title) LIKE '%toeic%'
                      )
                  )
              )
            ORDER BY updated_at DESC NULLS LAST, id DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM content_bank_items
            WHERE bank_type = 'ASSESSMENT'
              AND (:skill = '' OR skill = :skill)
              AND (:type = '' OR payload_jsonb->>'type' = :type)
              AND (:status = '' OR status = :status)
              AND (
                  :keyword = ''
                  OR LOWER(title) LIKE :keyword
                  OR LOWER(COALESCE(description, '')) LIKE :keyword
                  OR LOWER(COALESCE(payload_jsonb->>'instructions', '')) LIKE :keyword
              )
              AND (
                  :examCategory = ''
                  OR (
                      :examCategory = 'TOEIC'
                      AND (
                          LOWER(COALESCE(payload_jsonb->>'uiConfigJson', '')) LIKE '%toeic%'
                          OR LOWER(title) LIKE '%toeic%'
                      )
                  )
                  OR (
                      :examCategory <> 'TOEIC'
                      AND NOT (
                          LOWER(COALESCE(payload_jsonb->>'uiConfigJson', '')) LIKE '%toeic%'
                          OR LOWER(title) LIKE '%toeic%'
                      )
                  )
              )
            """,
            nativeQuery = true)
    Page<AssessmentBankItem> searchPage(
            @Param("skill") String skill,
            @Param("type") String type,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("examCategory") String examCategory,
            Pageable pageable
    );

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
