package fu.sep490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClassroomTuitionSettlementSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureSettlementColumns() {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF to_regclass('public.classroom_enrollments') IS NOT NULL THEN
                        ALTER TABLE classroom_enrollments
                            ADD COLUMN IF NOT EXISTS tuition_settlement_status VARCHAR(30) DEFAULT 'NONE',
                            ADD COLUMN IF NOT EXISTS tuition_settlement_resolved_at TIMESTAMP,
                            ADD COLUMN IF NOT EXISTS tuition_settlement_resolved_by_id BIGINT,
                            ADD COLUMN IF NOT EXISTS tuition_settlement_resolution_note VARCHAR(700);

                        UPDATE classroom_enrollments
                        SET tuition_settlement_status = 'PENDING'
                        WHERE tuition_settlement_type IS NOT NULL
                          AND tuition_settlement_type <> 'NONE'
                          AND (tuition_settlement_status IS NULL OR tuition_settlement_status = 'NONE');

                        UPDATE classroom_enrollments
                        SET tuition_settlement_status = 'NONE'
                        WHERE tuition_settlement_status IS NULL;

                        IF NOT EXISTS (
                            SELECT 1 FROM pg_constraint
                            WHERE conname = 'fk_classroom_enrollments_settlement_resolved_by'
                        ) THEN
                            ALTER TABLE classroom_enrollments
                                ADD CONSTRAINT fk_classroom_enrollments_settlement_resolved_by
                                FOREIGN KEY (tuition_settlement_resolved_by_id) REFERENCES users(id);
                        END IF;

                        CREATE INDEX IF NOT EXISTS idx_classroom_enrollment_settlement_pending
                            ON classroom_enrollments (tuition_settlement_status);
                    END IF;

                    IF to_regclass('public.classroom_tuition_payments') IS NOT NULL THEN
                        ALTER TABLE classroom_tuition_payments
                            DROP CONSTRAINT IF EXISTS classroom_tuition_payments_payment_kind_check;
                        ALTER TABLE classroom_tuition_payments
                            ADD CONSTRAINT classroom_tuition_payments_payment_kind_check
                            CHECK (payment_kind::text = ANY (ARRAY[
                                'DEPOSIT'::character varying,
                                'PARTIAL'::character varying,
                                'FULL'::character varying,
                                'MANUAL_CONFIRMATION'::character varying,
                                'REFUND'::character varying
                            ]::text[]));
                    END IF;
                END $$;
                """);
    }
}
