UPDATE class_sections
SET google_meet_space_name = NULL,
    google_meet_url = NULL,
    google_meet_status = 'NOT_CREATED',
    google_meet_sync_error = NULL
WHERE google_meet_url ~* '^https://meet\.google\.com/englishlab-sheet-';
