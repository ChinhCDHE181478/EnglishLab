package fu.sap490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PackageBundleSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureBundleTables() {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF to_regclass('public.package_types') IS NOT NULL THEN
                        INSERT INTO package_types (code, name, description, active)
                        SELECT 'BUNDLE', 'Bundle', 'A bundle containing multiple learning products.', TRUE
                        WHERE NOT EXISTS (SELECT 1 FROM package_types WHERE code = 'BUNDLE');
                    END IF;

                    IF to_regclass('public.packages') IS NOT NULL THEN
                        ALTER TABLE packages DROP CONSTRAINT IF EXISTS packages_status_check;
                        ALTER TABLE packages
                            ADD CONSTRAINT packages_status_check
                            CHECK (status::text = ANY (ARRAY[
                                'DRAFT'::character varying,
                                'PENDING_REVIEW'::character varying,
                                'REJECTED'::character varying,
                                'PUBLISHED'::character varying,
                                'ARCHIVED'::character varying
                            ]::text[]));
                    END IF;

                    IF to_regclass('public.packages') IS NOT NULL
                       AND to_regclass('public.package_bundle_items') IS NULL THEN
                        CREATE TABLE package_bundle_items (
                            id BIGSERIAL PRIMARY KEY,
                            bundle_package_id BIGINT NOT NULL REFERENCES packages(id),
                            child_package_id BIGINT NOT NULL REFERENCES packages(id),
                            display_order INTEGER NOT NULL DEFAULT 0,
                            created_at TIMESTAMP,
                            CONSTRAINT uk_package_bundle_item UNIQUE (bundle_package_id, child_package_id)
                        );
                        CREATE INDEX IF NOT EXISTS idx_package_bundle_items_bundle
                            ON package_bundle_items (bundle_package_id, display_order);
                    END IF;
                END $$;
                """);
    }
}
