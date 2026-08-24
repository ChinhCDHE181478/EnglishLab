package fu.sep490.g23.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

@MappedSuperclass
public abstract class DomainRecord {

    @Column(name = "record_type", nullable = false, length = 100, updatable = false)
    private String recordType;

    @PrePersist
    protected void assignRecordType() {
        recordType = domainRecordType();
    }

    protected abstract String domainRecordType();
}
