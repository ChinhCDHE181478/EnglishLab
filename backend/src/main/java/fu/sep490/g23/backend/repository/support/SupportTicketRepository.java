package fu.sep490.g23.backend.repository.support;

import fu.sep490.g23.backend.entity.support.SupportTicket;
import fu.sep490.g23.backend.entity.support.enums.SupportTicketPriority;
import fu.sep490.g23.backend.entity.support.enums.SupportTicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    @EntityGraph(attributePaths = {"requester", "assignee"})
    List<SupportTicket> findByRequesterIdOrderByUpdatedAtDesc(Long requesterId);

    @Override
    @EntityGraph(attributePaths = {"requester", "assignee", "resolvedBy"})
    Optional<SupportTicket> findById(Long id);

    @EntityGraph(attributePaths = {"requester", "assignee"})
    @Query("""
            select ticket from SupportTicket ticket
            where (:status is null or ticket.status = :status)
              and (:priority is null or ticket.priority = :priority)
            order by ticket.updatedAt desc
            """)
    List<SupportTicket> findQueue(
            @Param("status") SupportTicketStatus status,
            @Param("priority") SupportTicketPriority priority
    );
}
