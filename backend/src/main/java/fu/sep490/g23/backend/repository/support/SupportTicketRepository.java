package fu.sep490.g23.backend.repository.support;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.support.SupportTicket;
import fu.sep490.g23.backend.entity.support.enums.SupportTicketPriority;
import fu.sep490.g23.backend.entity.support.enums.SupportTicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long>, JpaSpecificationExecutor<SupportTicket> {

    @Override
    @EntityGraph(attributePaths = {"requester", "assignee"})
    Page<SupportTicket> findAll(Specification<SupportTicket> specification, Pageable pageable);

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
              and (:assignee is null or ticket.assignee = :assignee)
            order by ticket.updatedAt desc
            """)
    List<SupportTicket> findQueue(
            @Param("status") SupportTicketStatus status,
            @Param("priority") SupportTicketPriority priority,
            @Param("assignee") User assignee
    );

    Optional<SupportTicket> findFirstByAssigneeInOrderByCreatedAtDescIdDesc(Collection<User> assignees);
}
