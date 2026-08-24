package fu.sep490.g23.backend.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(20)
@RequiredArgsConstructor
public class DomainRecordConsolidationMigration implements CommandLineRunner {

    private static final Map<String, List<String>> DOMAIN_TABLES = new LinkedHashMap<>();
    private static final Map<String, String> RENAMED_COLUMNS = Map.ofEntries(
            Map.entry("teacher_course_feedback.learner_support_score", "feedback_learner_support_score"),
            Map.entry("teacher_course_feedback.professionalism_score", "feedback_professionalism_score"),
            Map.entry("teacher_performance_evaluations.learner_support_score", "evaluation_learner_support_score"),
            Map.entry("teacher_performance_evaluations.professionalism_score", "evaluation_professionalism_score"),
            Map.entry("teacher_google_meet_connections.status", "meet_connection_status"),
            Map.entry("teacher_performance_evaluations.status", "evaluation_status"),
            Map.entry("classroom_attendance_disputes.reason", "dispute_reason"),
            Map.entry("classroom_attendance_disputes.status", "dispute_status"),
            Map.entry("classroom_gradebook_entries.status", "gradebook_status"),
            Map.entry("course_enrollment_request_history.reason", "transition_reason"),
            Map.entry("lesson_progress.status", "lesson_progress_status"),
            Map.entry("vocabulary_progress.status", "vocabulary_status")
    );

    static {
        DOMAIN_TABLES.put("user_auxiliary_records", List.of(
                "teacher_professional_profiles", "teacher_google_meet_connections",
                "learner_lesson_notes", "learner_lesson_review_flags", "teacher_credentials",
                "teacher_performance_evaluations", "teacher_course_feedback"
        ));
        DOMAIN_TABLES.put("assessment_component_records", List.of(
                "rubric_criteria", "classroom_quiz_questions", "course_lesson_flashcard_refs"
        ));
        DOMAIN_TABLES.put("classroom_operation_records", List.of(
                "course_enrollment_request_history", "classroom_proposal_members",
                "classroom_attendance_disputes", "classroom_gradebook_entries", "lark_meeting_participants"
        ));
        DOMAIN_TABLES.put("classroom_financial_records", List.of(
                "classroom_tuition_payment_proofs", "classroom_tuition_payments"
        ));
        DOMAIN_TABLES.put("learner_progress_records", List.of("lesson_progress", "vocabulary_progress"));
    }

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        DOMAIN_TABLES.forEach((domainTable, sourceTables) -> {
            sourceTables.forEach(sourceTable -> migrateSource(domainTable, sourceTable));
            makeSubtypeColumnsNullable(domainTable);
        });
        dropLegacyRelationsAndInfrastructure();
        removeObsoleteStorageColumns();
        createIntegrityConstraints();
        validateNormalizedSchema();
    }

    private void migrateSource(String domainTable, String sourceTable) {
        String relationKind = relationKind(sourceTable);
        if (relationKind == null) {
            return;
        }
        List<ColumnDefinition> columns = loadColumns(sourceTable).stream()
                .filter(column -> !"id".equals(column.name()))
                .toList();
        if ("v".equals(relationKind)) {
            migrateCompatibilityView(domainTable, sourceTable, columns);
        } else if ("r".equals(relationKind) || "p".equals(relationKind)) {
            migratePhysicalTable(domainTable, sourceTable, columns);
        } else {
            throw new IllegalStateException("Unsupported relation type for " + sourceTable + ": " + relationKind);
        }
    }

    private void migrateCompatibilityView(String domainTable, String sourceTable, List<ColumnDefinition> columns) {
        if (!columnExists(domainTable, "legacy_id")) {
            validateSubtypeCount(domainTable, sourceTable);
            return;
        }
        String assignments = columns.stream()
                .map(column -> quoteIdentifier(targetColumn(sourceTable, column.name()))
                        + " = source." + quoteIdentifier(column.name()))
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
        jdbcTemplate.update(
                "UPDATE " + quoteIdentifier(domainTable) + " target SET " + assignments
                        + " FROM " + quoteIdentifier(sourceTable) + " source"
                        + " WHERE target.record_type = ? AND target.legacy_id = source.id",
                sourceTable
        );
        validateSubtypeCount(domainTable, sourceTable);
    }

    private void migratePhysicalTable(String domainTable, String sourceTable, List<ColumnDefinition> columns) {
        Long existingCount = countSubtype(domainTable, sourceTable);
        if (existingCount != null && existingCount > 0) {
            validateSubtypeCount(domainTable, sourceTable);
            return;
        }
        String targetColumns = columns.stream()
                .map(column -> quoteIdentifier(targetColumn(sourceTable, column.name())))
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
        String sourceColumns = columns.stream()
                .map(column -> "source." + quoteIdentifier(column.name()))
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
        jdbcTemplate.update(
                "INSERT INTO " + quoteIdentifier(domainTable)
                        + " (record_type, " + targetColumns + ")"
                        + " SELECT ?, " + sourceColumns
                        + " FROM " + quoteIdentifier(sourceTable) + " source",
                sourceTable
        );
        validateSubtypeCount(domainTable, sourceTable);
    }

    private void validateSubtypeCount(String domainTable, String sourceTable) {
        Long sourceCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + quoteIdentifier(sourceTable), Long.class
        );
        Long targetCount = countSubtype(domainTable, sourceTable);
        if (!sourceCount.equals(targetCount)) {
            throw new IllegalStateException("Typed backfill incomplete for " + sourceTable
                    + ": source=" + sourceCount + ", target=" + targetCount);
        }
    }

    private Long countSubtype(String domainTable, String sourceTable) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + quoteIdentifier(domainTable) + " WHERE record_type = ?",
                Long.class, sourceTable
        );
    }

    private void makeSubtypeColumnsNullable(String domainTable) {
        List<String> columns = jdbcTemplate.queryForList(
                """
                SELECT column_name FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ?
                  AND column_name NOT IN ('id', 'record_type') AND is_nullable = 'NO'
                """,
                String.class, domainTable
        );
        columns.forEach(column -> jdbcTemplate.execute(
                "ALTER TABLE " + quoteIdentifier(domainTable)
                        + " ALTER COLUMN " + quoteIdentifier(column) + " DROP NOT NULL"
        ));
    }

    private void dropLegacyRelationsAndInfrastructure() {
        DOMAIN_TABLES.values().stream().flatMap(List::stream).forEach(sourceTable -> {
            String kind = relationKind(sourceTable);
            if ("v".equals(kind)) {
                jdbcTemplate.execute("DROP VIEW " + quoteIdentifier(sourceTable) + " CASCADE");
            } else if ("r".equals(kind) || "p".equals(kind)) {
                jdbcTemplate.execute("DROP TABLE " + quoteIdentifier(sourceTable) + " CASCADE");
            }
            jdbcTemplate.execute("DROP TYPE IF EXISTS "
                    + quoteIdentifier("compat_" + sourceTable + "_row") + " CASCADE");
        });
        jdbcTemplate.execute("DROP FUNCTION IF EXISTS sync_compatibility_domain_record() CASCADE");
        jdbcTemplate.execute("DROP SEQUENCE IF EXISTS compatibility_record_legacy_id_seq CASCADE");
    }

    private void removeObsoleteStorageColumns() {
        DOMAIN_TABLES.keySet().forEach(domainTable -> {
            dropColumn(domainTable, "legacy_id");
            dropColumn(domainTable, "payload");
        });
        dropColumn("user_auxiliary_records", "learner_support_score");
        dropColumn("user_auxiliary_records", "professionalism_score");
        dropColumn("user_auxiliary_records", "status");
        dropColumn("classroom_operation_records", "reason");
        dropColumn("classroom_operation_records", "status");
        dropColumn("learner_progress_records", "status");
        dropColumn("assessment_component_records", "created_at");
        dropColumn("assessment_component_records", "updated_at");
        dropColumn("classroom_financial_records", "updated_at");
    }

    private void createIntegrityConstraints() {
        DOMAIN_TABLES.forEach((domainTable, sourceTables) -> {
            createIndex("idx_" + domainTable + "_record_type", domainTable, "record_type");
            addCheck("ck_" + domainTable + "_record_type", domainTable,
                    "record_type IN (" + sourceTables.stream().map(this::quoteLiteral)
                            .reduce((left, right) -> left + ", " + right).orElseThrow() + ")");
        });
        createForeignKeys();
        createPartialUniqueIndexes();
        createSubtypeRequiredChecks();
    }

    private void createForeignKeys() {
        addForeignKey("user_auxiliary_records", "teacher_id", "users");
        addForeignKey("user_auxiliary_records", "user_id", "users");
        addForeignKey("user_auxiliary_records", "course_id", "online_courses");
        addForeignKey("user_auxiliary_records", "lesson_id", "lessons");
        addForeignKey("user_auxiliary_records", "enrollment_id", "classroom_enrollments");
        addForeignKey("user_auxiliary_records", "classroom_offering_id", "classroom_offerings");
        addForeignKey("user_auxiliary_records", "evaluator_id", "users");
        addForeignKey("user_auxiliary_records", "verified_by_id", "users");
        addForeignKey("assessment_component_records", "rubric_id", "assessment_rubrics");
        addForeignKey("assessment_component_records", "quiz_id", "classroom_quizzes");
        addForeignKey("assessment_component_records", "lesson_id", "lessons");
        addForeignKey("assessment_component_records", "flashcard_set_id", "flashcard_sets");
        addForeignKey("classroom_operation_records", "enrollment_request_id", "course_enrollment_requests");
        addForeignKey("classroom_operation_records", "proposal_id", "classroom_proposals");
        addForeignKey("classroom_operation_records", "classroom_enrollment_id", "classroom_enrollments");
        addForeignKey("classroom_operation_records", "actor_id", "users");
        addForeignKey("classroom_operation_records", "attendance_id", "classroom_attendances");
        addForeignKey("classroom_operation_records", "student_id", "users");
        addForeignKey("classroom_operation_records", "reviewed_by_id", "users");
        addForeignKey("classroom_operation_records", "classroom_offering_id", "classroom_offerings");
        addForeignKey("classroom_operation_records", "updated_by_id", "users");
        addForeignKey("classroom_operation_records", "classroom_session_id", "classroom_sessions");
        addForeignKey("classroom_operation_records", "user_id", "users");
        addForeignKey("classroom_financial_records", "enrollment_id", "classroom_enrollments");
        addForeignKey("classroom_financial_records", "reviewed_by_id", "users");
        addForeignKey("classroom_financial_records", "recorded_by_id", "users");
        addForeignKey("learner_progress_records", "student_id", "users");
        addForeignKey("learner_progress_records", "lesson_id", "lessons");
        addForeignKey("learner_progress_records", "enrollment_id", "package_enrollments");
        addForeignKey("learner_progress_records", "course_version_id", "online_course_versions");
        addForeignKey("learner_progress_records", "course_id", "online_courses");
    }

    private void createPartialUniqueIndexes() {
        createPartialUniqueIndex("uk_teacher_profile_teacher", "user_auxiliary_records", "teacher_id", "teacher_professional_profiles");
        createPartialUniqueIndex("uk_teacher_meet_teacher", "user_auxiliary_records", "teacher_id", "teacher_google_meet_connections");
        createPartialUniqueIndex("uk_lesson_review_user_lesson", "user_auxiliary_records", "user_id, lesson_id", "learner_lesson_review_flags");
        createPartialUniqueIndex("uk_teacher_feedback_enrollment_teacher", "user_auxiliary_records", "enrollment_id, teacher_id", "teacher_course_feedback");
        createPartialUniqueIndex("uk_lesson_flashcard_ref", "assessment_component_records", "lesson_id, flashcard_set_id", "course_lesson_flashcard_refs");
        createPartialUniqueIndex("uk_classroom_proposal_member", "classroom_operation_records", "proposal_id, enrollment_request_id", "classroom_proposal_members");
        createPartialUniqueIndex("uk_gradebook_offering_student", "classroom_operation_records", "classroom_offering_id, student_id", "classroom_gradebook_entries");
        createPartialUniqueIndex("uk_lark_session_participant", "classroom_operation_records", "classroom_session_id, participant_key", "lark_meeting_participants");
        createPartialUniqueIndex("uk_lesson_progress_student_lesson", "learner_progress_records", "student_id, lesson_id", "lesson_progress");
        createPartialUniqueIndex("uk_vocabulary_progress_student_course_term", "learner_progress_records", "student_id, course_id, term_key", "vocabulary_progress");
    }

    private void createSubtypeRequiredChecks() {
        addSubtypeCheck("user_auxiliary_records", "teacher_professional_profiles", "teacher_id IS NOT NULL");
        addSubtypeCheck("user_auxiliary_records", "teacher_google_meet_connections", "teacher_id IS NOT NULL AND google_subject IS NOT NULL AND google_email IS NOT NULL AND encrypted_refresh_token IS NOT NULL AND scopes IS NOT NULL AND meet_connection_status IS NOT NULL AND connected_at IS NOT NULL");
        addSubtypeCheck("user_auxiliary_records", "learner_lesson_notes", "user_id IS NOT NULL AND course_id IS NOT NULL AND lesson_id IS NOT NULL AND content IS NOT NULL");
        addSubtypeCheck("user_auxiliary_records", "learner_lesson_review_flags", "user_id IS NOT NULL AND course_id IS NOT NULL AND lesson_id IS NOT NULL");
        addSubtypeCheck("user_auxiliary_records", "teacher_credentials", "teacher_id IS NOT NULL AND type IS NOT NULL AND title IS NOT NULL AND issuer IS NOT NULL AND verification_status IS NOT NULL");
        addSubtypeCheck("user_auxiliary_records", "teacher_performance_evaluations", "teacher_id IS NOT NULL AND evaluator_id IS NOT NULL AND period_start IS NOT NULL AND period_end IS NOT NULL AND lesson_delivery_score IS NOT NULL AND evaluation_learner_support_score IS NOT NULL AND grading_timeliness_score IS NOT NULL AND evaluation_professionalism_score IS NOT NULL AND overall_score IS NOT NULL AND evaluation_status IS NOT NULL");
        addSubtypeCheck("user_auxiliary_records", "teacher_course_feedback", "enrollment_id IS NOT NULL AND classroom_offering_id IS NOT NULL AND teacher_id IS NOT NULL AND clarity_score IS NOT NULL AND engagement_score IS NOT NULL AND feedback_learner_support_score IS NOT NULL AND feedback_timeliness_score IS NOT NULL AND feedback_professionalism_score IS NOT NULL AND pace IS NOT NULL AND would_recommend IS NOT NULL AND strengths IS NOT NULL AND improvement_suggestions IS NOT NULL AND submitted_at IS NOT NULL");
        addSubtypeCheck("assessment_component_records", "rubric_criteria", "rubric_id IS NOT NULL AND name IS NOT NULL AND weight IS NOT NULL AND display_order IS NOT NULL");
        addSubtypeCheck("assessment_component_records", "classroom_quiz_questions", "quiz_id IS NOT NULL AND sort_order IS NOT NULL AND prompt IS NOT NULL AND options_json IS NOT NULL AND correct_answer IS NOT NULL");
        addSubtypeCheck("assessment_component_records", "course_lesson_flashcard_refs", "lesson_id IS NOT NULL AND flashcard_set_id IS NOT NULL AND display_order IS NOT NULL");
        addSubtypeCheck("classroom_operation_records", "course_enrollment_request_history", "enrollment_request_id IS NOT NULL AND to_status IS NOT NULL");
        addSubtypeCheck("classroom_operation_records", "classroom_proposal_members", "proposal_id IS NOT NULL AND enrollment_request_id IS NOT NULL");
        addSubtypeCheck("classroom_operation_records", "classroom_attendance_disputes", "attendance_id IS NOT NULL AND student_id IS NOT NULL AND dispute_reason IS NOT NULL AND dispute_status IS NOT NULL");
        addSubtypeCheck("classroom_operation_records", "classroom_gradebook_entries", "classroom_offering_id IS NOT NULL AND student_id IS NOT NULL AND gradebook_status IS NOT NULL");
        addSubtypeCheck("classroom_operation_records", "lark_meeting_participants", "classroom_session_id IS NOT NULL AND participant_key IS NOT NULL AND active IS NOT NULL");
        addSubtypeCheck("classroom_financial_records", "classroom_tuition_payment_proofs", "enrollment_id IS NOT NULL AND amount IS NOT NULL AND payment_kind IS NOT NULL AND file_url IS NOT NULL AND status IS NOT NULL");
        addSubtypeCheck("classroom_financial_records", "classroom_tuition_payments", "enrollment_id IS NOT NULL AND amount IS NOT NULL AND payment_kind IS NOT NULL");
        addSubtypeCheck("learner_progress_records", "lesson_progress", "student_id IS NOT NULL AND lesson_id IS NOT NULL AND enrollment_id IS NOT NULL AND lesson_progress_status IS NOT NULL AND progress_percent IS NOT NULL");
        addSubtypeCheck("learner_progress_records", "vocabulary_progress", "student_id IS NOT NULL AND course_id IS NOT NULL AND term_key IS NOT NULL AND vocabulary_status IS NOT NULL AND starred IS NOT NULL");
    }

    private void addSubtypeCheck(String table, String recordType, String requiredExpression) {
        addCheck("ck_" + abbreviated(table) + "_" + abbreviated(recordType) + "_required",
                table, "record_type <> " + quoteLiteral(recordType) + " OR (" + requiredExpression + ")");
    }

    private void validateNormalizedSchema() {
        DOMAIN_TABLES.forEach((domainTable, sourceTables) -> sourceTables.forEach(sourceTable -> {
            if (relationKind(sourceTable) != null) {
                throw new IllegalStateException("Legacy relation still exists after normalization: " + sourceTable);
            }
        }));
    }

    private void addForeignKey(String table, String column, String targetTable) {
        if (!columnExists(table, column) || relationKind(targetTable) == null) {
            return;
        }
        String constraint = databaseIdentifier("fk_" + abbreviated(table) + "_" + abbreviated(column));
        if (constraintExists(table, constraint)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + quoteIdentifier(table)
                + " ADD CONSTRAINT " + quoteIdentifier(constraint)
                + " FOREIGN KEY (" + quoteIdentifier(column) + ") REFERENCES "
                + quoteIdentifier(targetTable) + "(id)");
    }

    private void addCheck(String name, String table, String expression) {
        name = databaseIdentifier(name);
        if (constraintExists(table, name)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + quoteIdentifier(table)
                + " ADD CONSTRAINT " + quoteIdentifier(name) + " CHECK (" + expression + ")");
    }

    private boolean constraintExists(String table, String constraint) {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (SELECT 1 FROM information_schema.table_constraints
                    WHERE table_schema = 'public' AND table_name = ? AND constraint_name = ?)
                """,
                Boolean.class, table, constraint
        );
        return Boolean.TRUE.equals(exists);
    }

    private void createIndex(String name, String table, String columns) {
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS " + quoteIdentifier(name)
                + " ON " + quoteIdentifier(table) + " (" + columns + ")");
    }

    private void createPartialUniqueIndex(String name, String table, String columns, String recordType) {
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS " + quoteIdentifier(name)
                + " ON " + quoteIdentifier(table) + " (" + columns + ") WHERE record_type = "
                + quoteLiteral(recordType));
    }

    private void dropColumn(String table, String column) {
        jdbcTemplate.execute("ALTER TABLE " + quoteIdentifier(table)
                + " DROP COLUMN IF EXISTS " + quoteIdentifier(column) + " CASCADE");
    }

    private boolean columnExists(String table, String column) {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (SELECT 1 FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = ? AND column_name = ?)
                """,
                Boolean.class, table, column
        );
        return Boolean.TRUE.equals(exists);
    }

    private String relationKind(String relation) {
        return jdbcTemplate.query(
                """
                SELECT relation.relkind::text FROM pg_catalog.pg_class relation
                JOIN pg_catalog.pg_namespace namespace ON namespace.oid = relation.relnamespace
                WHERE namespace.nspname = 'public' AND relation.relname = ?
                """,
                resultSet -> resultSet.next() ? resultSet.getString(1) : null, relation
        );
    }

    private List<ColumnDefinition> loadColumns(String relation) {
        return jdbcTemplate.query(
                """
                SELECT attribute.attname, pg_catalog.format_type(attribute.atttypid, attribute.atttypmod)
                FROM pg_catalog.pg_attribute attribute
                WHERE attribute.attrelid = CAST(? AS regclass) AND attribute.attnum > 0
                  AND NOT attribute.attisdropped ORDER BY attribute.attnum
                """,
                (resultSet, rowNumber) -> new ColumnDefinition(resultSet.getString(1), resultSet.getString(2)),
                "public." + relation
        );
    }

    private String targetColumn(String sourceTable, String sourceColumn) {
        return RENAMED_COLUMNS.getOrDefault(sourceTable + "." + sourceColumn, sourceColumn);
    }

    private String abbreviated(String value) {
        return value.replace("classroom_", "cr_").replace("teacher_", "t_")
                .replace("learner_", "l_").replace("enrollment_", "enr_")
                .replace("professional_", "prof_").replace("progress_", "prog_")
                .replace("records", "rec");
    }

    private String databaseIdentifier(String value) {
        return value.length() <= 63 ? value : value.substring(0, 63);
    }

    private String quoteIdentifier(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private String quoteLiteral(String value) {
        return '\'' + value.replace("'", "''") + '\'';
    }

    private record ColumnDefinition(String name, String sqlType) {
    }
}
