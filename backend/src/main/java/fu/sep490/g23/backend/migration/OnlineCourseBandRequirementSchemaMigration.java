package fu.sep490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnlineCourseBandRequirementSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void removeDeprecatedMaximumBand() {
        jdbcTemplate.execute("ALTER TABLE online_courses DROP COLUMN IF EXISTS recommended_current_band_max");
    }
}
