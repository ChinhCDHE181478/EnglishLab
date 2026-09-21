-- Backfill only existing requests without an owner. No schema changes are made.
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
unassigned_requests AS (
    SELECT
        request.id,
        request.created_at,
        row_number() OVER (ORDER BY request.created_at, request.id) - 1 AS request_index
    FROM course_registration_requests request
    WHERE request.reviewed_by_id IS NULL
),
assignments AS (
    SELECT
        request.id AS request_id,
        request.created_at,
        staff.id AS staff_id
    FROM unassigned_requests request
    JOIN active_staff staff
      ON staff.staff_index = request.request_index % staff.staff_count
)
UPDATE course_registration_requests request
SET reviewed_by_id = assignments.staff_id,
    reviewed_at = COALESCE(request.reviewed_at, assignments.created_at, CURRENT_TIMESTAMP)
FROM assignments
WHERE request.id = assignments.request_id;
