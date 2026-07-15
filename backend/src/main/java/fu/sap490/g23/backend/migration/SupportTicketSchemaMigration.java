package fu.sap490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupportTicketSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureSupportTicketSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS support_tickets (
                    id BIGSERIAL PRIMARY KEY,
                    requester_id BIGINT NOT NULL,
                    assignee_id BIGINT,
                    resolved_by_id BIGINT,
                    subject VARCHAR(160) NOT NULL,
                    category VARCHAR(30) NOT NULL,
                    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
                    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
                    resolved_at TIMESTAMP,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_support_tickets_requester FOREIGN KEY (requester_id) REFERENCES users(id),
                    CONSTRAINT fk_support_tickets_assignee FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL,
                    CONSTRAINT fk_support_tickets_resolved_by FOREIGN KEY (resolved_by_id) REFERENCES users(id) ON DELETE SET NULL
                );

                CREATE TABLE IF NOT EXISTS support_ticket_messages (
                    id BIGSERIAL PRIMARY KEY,
                    ticket_id BIGINT NOT NULL,
                    author_id BIGINT NOT NULL,
                    body TEXT NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_support_ticket_messages_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets(id) ON DELETE CASCADE,
                    CONSTRAINT fk_support_ticket_messages_author FOREIGN KEY (author_id) REFERENCES users(id)
                );

                CREATE INDEX IF NOT EXISTS idx_support_tickets_requester_updated
                    ON support_tickets (requester_id, updated_at DESC);
                CREATE INDEX IF NOT EXISTS idx_support_tickets_queue
                    ON support_tickets (status, priority, updated_at DESC);
                CREATE INDEX IF NOT EXISTS idx_support_ticket_messages_ticket_created
                    ON support_ticket_messages (ticket_id, created_at);

                DO $$
                BEGIN
                    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'support_tickets_category_check') THEN
                        ALTER TABLE support_tickets ADD CONSTRAINT support_tickets_category_check
                            CHECK (category IN ('ACCOUNT', 'PAYMENT', 'ONLINE_COURSE', 'CLASSROOM', 'TECHNICAL', 'OTHER'));
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'support_tickets_status_check') THEN
                        ALTER TABLE support_tickets ADD CONSTRAINT support_tickets_status_check
                            CHECK (status IN ('OPEN', 'IN_PROGRESS', 'WAITING_FOR_LEARNER', 'RESOLVED', 'CLOSED'));
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'support_tickets_priority_check') THEN
                        ALTER TABLE support_tickets ADD CONSTRAINT support_tickets_priority_check
                            CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT'));
                    END IF;
                END $$;
                """);
    }
}
