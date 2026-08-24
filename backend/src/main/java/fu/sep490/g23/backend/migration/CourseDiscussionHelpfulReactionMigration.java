package fu.sep490.g23.backend.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 200)
@RequiredArgsConstructor
public class CourseDiscussionHelpfulReactionMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        jdbcTemplate.execute("""
                ALTER TABLE course_discussion_reactions
                    ADD COLUMN IF NOT EXISTS helpful BOOLEAN;
                UPDATE course_discussion_reactions
                    SET helpful = FALSE
                    WHERE helpful IS NULL;
                ALTER TABLE course_discussion_reactions
                    ALTER COLUMN helpful SET DEFAULT FALSE;
                ALTER TABLE course_discussion_reactions
                    ALTER COLUMN helpful SET NOT NULL;
                ALTER TABLE course_discussion_reactions
                    ALTER COLUMN reaction_type DROP NOT NULL;

                DO $$
                BEGIN
                    IF to_regclass('public.course_discussion_reply_votes') IS NOT NULL THEN
                        INSERT INTO course_discussion_reactions (
                            target_type,
                            target_id,
                            user_id,
                            reaction_type,
                            helpful,
                            created_at,
                            updated_at
                        )
                        SELECT
                            'REPLY',
                            vote.reply_id,
                            vote.user_id,
                            NULL,
                            TRUE,
                            vote.created_at,
                            vote.created_at
                        FROM course_discussion_reply_votes vote
                        ON CONFLICT (target_type, target_id, user_id)
                        DO UPDATE SET helpful = TRUE;

                        DROP TABLE course_discussion_reply_votes;
                    END IF;
                END $$;
                """);
    }
}
