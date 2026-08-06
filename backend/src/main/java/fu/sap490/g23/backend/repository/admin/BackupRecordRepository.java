package fu.sap490.g23.backend.repository.admin;

import fu.sap490.g23.backend.entity.admin.BackupRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BackupRecordRepository extends JpaRepository<BackupRecord, Long> {
}
