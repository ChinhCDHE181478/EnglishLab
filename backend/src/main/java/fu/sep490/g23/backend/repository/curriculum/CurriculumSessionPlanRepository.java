package fu.sep490.g23.backend.repository.curriculum;

import fu.sep490.g23.backend.entity.curriculum.CurriculumSessionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CurriculumSessionPlanRepository extends JpaRepository<CurriculumSessionPlan, Long> {

    List<CurriculumSessionPlan> findByUnitIdOrderBySessionNumberAscDisplayOrderAscIdAsc(Long unitId);

    @Query("""
            SELECT sessionPlan
            FROM CurriculumSessionPlan sessionPlan
            WHERE sessionPlan.unit.program.id = :programId
            ORDER BY sessionPlan.sessionNumber ASC, sessionPlan.displayOrder ASC, sessionPlan.id ASC
            """)
    List<CurriculumSessionPlan> findByProgramIdOrderBySessionNumberAsc(@Param("programId") Long programId);

    @Query("""
            SELECT COUNT(sessionPlan)
            FROM CurriculumSessionPlan sessionPlan
            WHERE sessionPlan.unit.program.id = :programId
            """)
    long countByProgramId(@Param("programId") Long programId);

    @Query("""
            SELECT CASE WHEN COUNT(sessionPlan) > 0 THEN true ELSE false END
            FROM CurriculumSessionPlan sessionPlan
            WHERE sessionPlan.unit.program.id = :programId
              AND sessionPlan.sessionNumber = :sessionNumber
              AND (:excludeId IS NULL OR sessionPlan.id <> :excludeId)
            """)
    boolean existsDuplicateSessionNumber(
            @Param("programId") Long programId,
            @Param("sessionNumber") Integer sessionNumber,
            @Param("excludeId") Long excludeId
    );
}
