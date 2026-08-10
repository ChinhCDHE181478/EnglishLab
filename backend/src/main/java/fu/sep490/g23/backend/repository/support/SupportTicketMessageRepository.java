package fu.sap490.g23.backend.repository.support;

import fu.sap490.g23.backend.entity.support.SupportTicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface SupportTicketMessageRepository extends JpaRepository<SupportTicketMessage, Long> {
    @EntityGraph(attributePaths = "author")
    List<SupportTicketMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
