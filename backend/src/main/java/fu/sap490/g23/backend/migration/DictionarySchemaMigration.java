package fu.sap490.g23.backend.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DictionarySchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("""
                create table if not exists saved_vocabularies (
                    id bigserial primary key,
                    user_id bigint not null references users(id) on delete cascade,
                    word varchar(120) not null,
                    phonetic varchar(180),
                    primary_definition varchar(1200) not null,
                    note varchar(1000),
                    status varchar(20) not null default 'LEARNING',
                    created_at timestamp not null default current_timestamp,
                    updated_at timestamp not null default current_timestamp,
                    constraint uk_saved_vocabulary_user_word unique (user_id, word),
                    constraint ck_saved_vocabulary_status check (status in ('LEARNING', 'MASTERED'))
                )
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_saved_vocabularies_user_status
                on saved_vocabularies(user_id, status, updated_at desc)
                """);
    }
}
