package fu.sep490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentOrderRefundSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureRefundColumns() {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF to_regclass('public.payment_orders') IS NOT NULL THEN
                        ALTER TABLE payment_orders
                            ADD COLUMN IF NOT EXISTS refunded_amount_vnd BIGINT DEFAULT 0,
                            ADD COLUMN IF NOT EXISTS refunded_at TIMESTAMP,
                            ADD COLUMN IF NOT EXISTS refund_reason VARCHAR(500),
                            ADD COLUMN IF NOT EXISTS refunded_by_id BIGINT;

                        IF NOT EXISTS (
                            SELECT 1 FROM pg_constraint
                            WHERE conname = 'fk_payment_orders_refunded_by'
                        ) THEN
                            ALTER TABLE payment_orders
                                ADD CONSTRAINT fk_payment_orders_refunded_by
                                FOREIGN KEY (refunded_by_id) REFERENCES users(id);
                        END IF;

                        -- Hibernate @Enumerated(STRING) + DB CHECK may omit REFUNDED until Task 6.
                        ALTER TABLE payment_orders DROP CONSTRAINT IF EXISTS payment_orders_status_check;
                        ALTER TABLE payment_orders
                            ADD CONSTRAINT payment_orders_status_check
                            CHECK (status::text = ANY (ARRAY[
                                'PENDING'::character varying,
                                'PROCESSING'::character varying,
                                'PAID'::character varying,
                                'CANCELLED'::character varying,
                                'FAILED'::character varying,
                                'EXPIRED'::character varying,
                                'REFUNDED'::character varying
                            ]::text[]));
                    END IF;
                END $$;
                """);
    }
}
