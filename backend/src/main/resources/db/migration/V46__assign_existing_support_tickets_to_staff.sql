-- Backfill ownership using the existing assignee_id; no schema changes are made.
WITH active_staff AS (
    SELECT
        users.id,
        row_number() OVER (ORDER BY users.id) - 1 AS staff_index,
        count(*) OVER () AS staff_count
    FROM users
    JOIN user_roles ON user_roles.user_id = users.id
    JOIN roles ON roles.code = user_roles.role_code
    WHERE user_roles.role_code = 'STAFF'
      AND roles.active = true
      AND users.email_verified = true
),
unassigned_tickets AS (
    SELECT
        ticket.id,
        row_number() OVER (ORDER BY ticket.created_at, ticket.id) - 1 AS ticket_index
    FROM support_tickets ticket
    WHERE ticket.assignee_id IS NULL
      AND ticket.status NOT IN ('RESOLVED', 'CLOSED')
),
assignments AS (
    SELECT
        ticket.id AS ticket_id,
        staff.id AS staff_id
    FROM unassigned_tickets ticket
    JOIN active_staff staff
      ON staff.staff_index = ticket.ticket_index % staff.staff_count
)
UPDATE support_tickets ticket
SET assignee_id = assignments.staff_id
FROM assignments
WHERE ticket.id = assignments.ticket_id;
