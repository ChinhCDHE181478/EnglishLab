package fu.sep490.g23.backend.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 220)
@RequiredArgsConstructor
public class CurriculumResourceRefSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS curriculum_resource_refs (
                    id BIGSERIAL PRIMARY KEY,
                    resource_type VARCHAR(20) NOT NULL,
                    unit_id BIGINT NOT NULL REFERENCES curriculum_units(id),
                    material_id BIGINT REFERENCES center_material_library_items(id),
                    exercise_id BIGINT REFERENCES exercise_bank_items(id),
                    assessment_id BIGINT REFERENCES assessment_bank_items(id),
                    flashcard_set_id BIGINT REFERENCES flashcard_sets(id),
                    display_order INTEGER NOT NULL DEFAULT 0,
                    note VARCHAR(500)
                );

                CREATE UNIQUE INDEX IF NOT EXISTS uk_curriculum_ref_material
                    ON curriculum_resource_refs(unit_id, material_id)
                    WHERE resource_type = 'MATERIAL';
                CREATE UNIQUE INDEX IF NOT EXISTS uk_curriculum_ref_exercise
                    ON curriculum_resource_refs(unit_id, exercise_id)
                    WHERE resource_type = 'EXERCISE';
                CREATE UNIQUE INDEX IF NOT EXISTS uk_curriculum_ref_assessment
                    ON curriculum_resource_refs(unit_id, assessment_id)
                    WHERE resource_type = 'ASSESSMENT';
                CREATE UNIQUE INDEX IF NOT EXISTS uk_curriculum_ref_flashcard
                    ON curriculum_resource_refs(unit_id, flashcard_set_id)
                    WHERE resource_type = 'FLASHCARD';

                ALTER TABLE curriculum_resource_refs
                    ALTER COLUMN material_id DROP NOT NULL,
                    ALTER COLUMN exercise_id DROP NOT NULL,
                    ALTER COLUMN assessment_id DROP NOT NULL,
                    ALTER COLUMN flashcard_set_id DROP NOT NULL;

                DO $$
                DECLARE
                    source_count BIGINT := 0;
                    copied_count BIGINT := 0;
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1 FROM pg_constraint
                        WHERE conname = 'ck_curriculum_resource_ref_target'
                          AND conrelid = 'curriculum_resource_refs'::regclass
                    ) THEN
                        ALTER TABLE curriculum_resource_refs
                            ADD CONSTRAINT ck_curriculum_resource_ref_target CHECK (
                                (resource_type = 'MATERIAL' AND material_id IS NOT NULL
                                    AND exercise_id IS NULL AND assessment_id IS NULL AND flashcard_set_id IS NULL)
                                OR (resource_type = 'EXERCISE' AND exercise_id IS NOT NULL
                                    AND material_id IS NULL AND assessment_id IS NULL AND flashcard_set_id IS NULL)
                                OR (resource_type = 'ASSESSMENT' AND assessment_id IS NOT NULL
                                    AND material_id IS NULL AND exercise_id IS NULL AND flashcard_set_id IS NULL)
                                OR (resource_type = 'FLASHCARD' AND flashcard_set_id IS NOT NULL
                                    AND material_id IS NULL AND exercise_id IS NULL AND assessment_id IS NULL)
                            );
                    END IF;

                    IF to_regclass('public.curriculum_material_refs') IS NOT NULL THEN
                        SELECT COUNT(*) INTO copied_count FROM curriculum_material_refs;
                        source_count := source_count + copied_count;
                        INSERT INTO curriculum_resource_refs
                            (resource_type, unit_id, material_id, display_order, note)
                        SELECT 'MATERIAL', unit_id, material_id, display_order, note
                        FROM curriculum_material_refs
                        ON CONFLICT DO NOTHING;
                    END IF;
                    IF to_regclass('public.curriculum_exercise_refs') IS NOT NULL THEN
                        SELECT COUNT(*) INTO copied_count FROM curriculum_exercise_refs;
                        source_count := source_count + copied_count;
                        INSERT INTO curriculum_resource_refs
                            (resource_type, unit_id, exercise_id, display_order, note)
                        SELECT 'EXERCISE', unit_id, exercise_id, display_order, note
                        FROM curriculum_exercise_refs
                        ON CONFLICT DO NOTHING;
                    END IF;
                    IF to_regclass('public.curriculum_assessment_refs') IS NOT NULL THEN
                        SELECT COUNT(*) INTO copied_count FROM curriculum_assessment_refs;
                        source_count := source_count + copied_count;
                        INSERT INTO curriculum_resource_refs
                            (resource_type, unit_id, assessment_id, display_order, note)
                        SELECT 'ASSESSMENT', unit_id, assessment_id, display_order, note
                        FROM curriculum_assessment_refs
                        ON CONFLICT DO NOTHING;
                    END IF;
                    IF to_regclass('public.curriculum_flashcard_refs') IS NOT NULL THEN
                        SELECT COUNT(*) INTO copied_count FROM curriculum_flashcard_refs;
                        source_count := source_count + copied_count;
                        INSERT INTO curriculum_resource_refs
                            (resource_type, unit_id, flashcard_set_id, display_order, note)
                        SELECT 'FLASHCARD', unit_id, flashcard_set_id, display_order, note
                        FROM curriculum_flashcard_refs
                        ON CONFLICT DO NOTHING;
                    END IF;

                    IF source_count > 0 AND (
                        SELECT COUNT(*) FROM curriculum_resource_refs
                    ) < source_count THEN
                        RAISE EXCEPTION 'Curriculum reference backfill incomplete';
                    END IF;

                    DROP TABLE IF EXISTS curriculum_material_refs;
                    DROP TABLE IF EXISTS curriculum_exercise_refs;
                    DROP TABLE IF EXISTS curriculum_assessment_refs;
                    DROP TABLE IF EXISTS curriculum_flashcard_refs;
                END $$;
                """);
    }
}
