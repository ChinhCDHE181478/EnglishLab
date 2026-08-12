package fu.sep490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseDiscussionReactionSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureReactionTypeConstraint() {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF to_regclass('public.course_discussion_reactions') IS NOT NULL THEN
                        ALTER TABLE course_discussion_reactions
                            DROP CONSTRAINT IF EXISTS course_discussion_reactions_reaction_type_check;

                        ALTER TABLE course_discussion_reactions
                            ADD CONSTRAINT course_discussion_reactions_reaction_type_check
                            CHECK (reaction_type IN ('LIKE', 'LOVE', 'CARE', 'LAUGH', 'WOW', 'SAD', 'ANGRY'));
                    END IF;
                END $$;
                """);
    }
}
