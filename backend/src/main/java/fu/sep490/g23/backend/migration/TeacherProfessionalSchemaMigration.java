package fu.sep490.g23.backend.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(500)
public class TeacherProfessionalSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        if (consolidated()) {
            return;
        }
        jdbcTemplate.execute("""
                create table if not exists teacher_professional_profiles (
                    id bigserial primary key,
                    teacher_id bigint not null unique references users(id) on delete cascade,
                    headline varchar(180),
                    biography text,
                    specializations varchar(700),
                    teaching_languages varchar(300),
                    years_of_experience integer,
                    highest_qualification varchar(250),
                    public_profile boolean not null default false,
                    created_at timestamp not null default current_timestamp,
                    updated_at timestamp not null default current_timestamp,
                    constraint ck_teacher_years_experience check (
                        years_of_experience is null or years_of_experience between 0 and 60
                    )
                )
                """);
        jdbcTemplate.execute("""
                create table if not exists teacher_credentials (
                    id bigserial primary key,
                    teacher_id bigint not null references users(id) on delete cascade,
                    type varchar(40) not null,
                    title varchar(250) not null,
                    issuer varchar(250) not null,
                    credential_number varchar(150),
                    issued_date date,
                    expiry_date date,
                    document_url varchar(700),
                    verification_status varchar(20) not null default 'PENDING',
                    verified_by_id bigint references users(id),
                    verified_at timestamp,
                    verification_note varchar(700),
                    created_at timestamp not null default current_timestamp,
                    updated_at timestamp not null default current_timestamp,
                    constraint ck_teacher_credential_status check (
                        verification_status in ('PENDING', 'VERIFIED', 'REJECTED', 'EXPIRED')
                    ),
                    constraint ck_teacher_credential_dates check (
                        expiry_date is null or issued_date is null or expiry_date >= issued_date
                    )
                )
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_teacher_credentials_teacher
                on teacher_credentials(teacher_id, verification_status)
                """);
        jdbcTemplate.execute("""
                create table if not exists teacher_performance_evaluations (
                    id bigserial primary key,
                    teacher_id bigint not null references users(id) on delete cascade,
                    evaluator_id bigint not null references users(id),
                    period_start date not null,
                    period_end date not null,
                    lesson_delivery_score numeric(3,2) not null,
                    learner_support_score numeric(3,2) not null,
                    grading_timeliness_score numeric(3,2) not null,
                    professionalism_score numeric(3,2) not null,
                    overall_score numeric(3,2) not null,
                    strengths varchar(1500),
                    improvement_areas varchar(1500),
                    action_plan varchar(1500),
                    status varchar(20) not null default 'DRAFT',
                    published_at timestamp,
                    created_at timestamp not null default current_timestamp,
                    updated_at timestamp not null default current_timestamp,
                    constraint ck_teacher_evaluation_period check (period_end >= period_start),
                    constraint ck_teacher_evaluation_status check (status in ('DRAFT', 'PUBLISHED')),
                    constraint ck_teacher_evaluation_scores check (
                        lesson_delivery_score between 1 and 5
                        and learner_support_score between 1 and 5
                        and grading_timeliness_score between 1 and 5
                        and professionalism_score between 1 and 5
                        and overall_score between 1 and 5
                    )
                )
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_teacher_evaluations_teacher_period
                on teacher_performance_evaluations(teacher_id, period_end desc)
                """);
    }

    private boolean consolidated() {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.user_auxiliary_records') IS NOT NULL",
                Boolean.class
        ));
    }
}
