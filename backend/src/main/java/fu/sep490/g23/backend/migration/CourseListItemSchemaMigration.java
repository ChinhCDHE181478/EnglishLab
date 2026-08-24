package fu.sep490.g23.backend.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 210)
@RequiredArgsConstructor
public class CourseListItemSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS course_list_items (
                    id BIGSERIAL PRIMARY KEY,
                    student_id BIGINT NOT NULL REFERENCES users(id),
                    online_course_id BIGINT NOT NULL REFERENCES online_courses(id),
                    list_type VARCHAR(20) NOT NULL,
                    added_at TIMESTAMP,
                    CONSTRAINT uk_course_list_student_course_type
                        UNIQUE (student_id, online_course_id, list_type),
                    CONSTRAINT ck_course_list_type
                        CHECK (list_type IN ('CART', 'WISHLIST'))
                );

                DO $$
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1
                        FROM pg_constraint
                        WHERE conname = 'ck_course_list_type'
                          AND conrelid = 'course_list_items'::regclass
                    ) THEN
                        ALTER TABLE course_list_items
                            ADD CONSTRAINT ck_course_list_type
                            CHECK (list_type IN ('CART', 'WISHLIST'));
                    END IF;

                    IF to_regclass('public.cart_items') IS NOT NULL THEN
                        INSERT INTO course_list_items (student_id, online_course_id, list_type, added_at)
                        SELECT student_id, online_course_id, 'CART', added_at
                        FROM cart_items
                        ON CONFLICT (student_id, online_course_id, list_type) DO NOTHING;
                        DROP TABLE cart_items;
                    END IF;

                    IF to_regclass('public.wishlist_items') IS NOT NULL THEN
                        INSERT INTO course_list_items (student_id, online_course_id, list_type, added_at)
                        SELECT student_id, online_course_id, 'WISHLIST', added_at
                        FROM wishlist_items
                        ON CONFLICT (student_id, online_course_id, list_type) DO NOTHING;
                        DROP TABLE wishlist_items;
                    END IF;
                END $$;
                """);
    }
}
