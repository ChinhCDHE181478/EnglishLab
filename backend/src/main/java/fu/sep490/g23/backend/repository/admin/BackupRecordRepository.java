package fu.sep490.g23.backend.repository.admin;

import fu.sep490.g23.backend.entity.admin.BackupRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@RequiredArgsConstructor
public class BackupRecordRepository {
    private final AtomicLong sequence = new AtomicLong();
    private final ConcurrentHashMap<Long, BackupRecord> records = new ConcurrentHashMap<>();

    public BackupRecord save(BackupRecord record) {
        LocalDateTime now = LocalDateTime.now();
        if (record.getId() == null) {
            record.setId(sequence.incrementAndGet());
            record.setCreatedAt(now);
        }
        record.setUpdatedAt(now);
        records.put(record.getId(), record);
        return record;
    }

    public Optional<BackupRecord> findById(Long id) {
        return Optional.ofNullable(records.get(id));
    }

    public Page<BackupRecord> findAll(Pageable pageable) {
        var sorted = records.values().stream()
                .sorted(Comparator.comparing(BackupRecord::getCreatedAt).reversed())
                .toList();
        int start = Math.min((int) pageable.getOffset(), sorted.size());
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        return new PageImpl<>(sorted.subList(start, end), pageable, sorted.size());
    }
}
