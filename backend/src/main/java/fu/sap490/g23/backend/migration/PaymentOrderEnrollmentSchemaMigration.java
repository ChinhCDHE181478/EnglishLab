package fu.sap490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentOrderEnrollmentSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureEnrollmentIdColumn() {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF to_regclass('public.payment_orders') IS NOT NULL THEN
                        ALTER TABLE payment_orders
                            ADD COLUMN IF NOT EXISTS enrollment_id BIGINT;

                        CREATE INDEX IF NOT EXISTS idx_payment_orders_enrollment_pending
                            ON payment_orders (enrollment_id, status);
                    END IF;
                END $$;
                """);
    }
}
