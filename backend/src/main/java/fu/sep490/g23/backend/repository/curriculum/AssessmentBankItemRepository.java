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
              AND (:type = '' OR content_data->>'type' = :type)
              AND (:status = '' OR status = :status)
              AND (
                  :keyword = ''
                  OR LOWER(title) LIKE :keyword
                  OR LOWER(COALESCE(description, '')) LIKE :keyword
                  OR LOWER(COALESCE(content_data->>'instructions', '')) LIKE :keyword
              )
              AND (
                  :examCategory = ''
                  OR (
                      :examCategory = 'TOEIC'
                      AND (
                          LOWER(COALESCE(content_data->>'uiConfigJson', '')) LIKE '%toeic%'
                          OR LOWER(title) LIKE '%toeic%'
                      )
                  )
                  OR (
                      :examCategory <> 'TOEIC'
                      AND NOT (
                          LOWER(COALESCE(content_data->>'uiConfigJson', '')) LIKE '%toeic%'
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
              AND (:type = '' OR content_data->>'type' = :type)
              AND (:status = '' OR status = :status)
              AND (
                  :keyword = ''
                  OR LOWER(title) LIKE :keyword
                  OR LOWER(COALESCE(description, '')) LIKE :keyword
                  OR LOWER(COALESCE(content_data->>'instructions', '')) LIKE :keyword
              )
              AND (
                  :examCategory = ''
                  OR (
                      :examCategory = 'TOEIC'
                      AND (
                          LOWER(COALESCE(content_data->>'uiConfigJson', '')) LIKE '%toeic%'
                          OR LOWER(title) LIKE '%toeic%'
                      )
                  )
                  OR (
                      :examCategory <> 'TOEIC'
                      AND NOT (
                          LOWER(COALESCE(content_data->>'uiConfigJson', '')) LIKE '%toeic%'
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
              AND content_data->>'type' = :type
            ORDER BY updated_at DESC NULLS LAST, id DESC
            """, nativeQuery = true)
    List<AssessmentBankItem> findByTypeCodeOrderByUpdatedAtDescIdDesc(@Param("type") String type);

    default List<AssessmentBankItem> findByTypeOrderByUpdatedAtDescIdDesc(AssessmentType type) {
        return findByTypeCodeOrderByUpdatedAtDescIdDesc(type.name());
    }

    @Query(value = """
            SELECT * FROM content_bank_items
            WHERE bank_type = 'ASSESSMENT'
              AND content_data->>'type' = :type
              AND status = :status
            ORDER BY updated_at DESC NULLS LAST, id DESC
            """, nativeQuery = true)
    List<AssessmentBankItem> findByTypeCodeAndStatusOrderByUpdatedAtDescIdDesc(
            @Param("type") String type,
            @Param("status") String status
    );

    default List<AssessmentBankItem> findByTypeAndStatusOrderByUpdatedAtDescIdDesc(
            AssessmentType type,
            String status
    ) {
        return findByTypeCodeAndStatusOrderByUpdatedAtDescIdDesc(type.name(), status);
    }

    @Query(value = """
            SELECT * FROM content_bank_items
            WHERE bank_type = 'ASSESSMENT'
              AND id = :id
              AND content_data->>'type' = :type
              AND status = :status
            """, nativeQuery = true)
    Optional<AssessmentBankItem> findByIdAndTypeCodeAndStatus(
            @Param("id") Long id,
            @Param("type") String type,
            @Param("status") String status
    );

    default Optional<AssessmentBankItem> findByIdAndTypeAndStatus(
            Long id,
            AssessmentType type,
            String status
    ) {
        return findByIdAndTypeCodeAndStatus(id, type.name(), status);
    }

    @Query(value = """
            SELECT * FROM content_bank_items
            WHERE bank_type = 'ASSESSMENT'
              AND content_data->>'type' = :type
              AND status = :status
              AND skill IN (:skills)
            ORDER BY updated_at DESC NULLS LAST, id DESC
            """, nativeQuery = true)
    List<AssessmentBankItem> findByTypeCodeAndStatusAndSkillCodeInOrderByUpdatedAtDescIdDesc(
            @Param("type") String type,
            @Param("status") String status,
            @Param("skills") List<String> skills
    );

    default List<AssessmentBankItem> findByTypeAndStatusAndSkillInOrderByUpdatedAtDescIdDesc(
            AssessmentType type,
            String status,
            List<AssessmentSkill> skills
    ) {
        return findByTypeCodeAndStatusAndSkillCodeInOrderByUpdatedAtDescIdDesc(
                type.name(),
                status,
                skills.stream().map(Enum::name).toList()
        );
    }
}
