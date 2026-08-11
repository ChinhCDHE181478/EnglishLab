package fu.sep490.g23.backend.service.admin.impl;

import fu.sep490.g23.backend.dto.response.admin.AuditLogResponse;
import fu.sep490.g23.backend.entity.admin.AuditLog;
import fu.sep490.g23.backend.repository.admin.AuditLogRepository;
import fu.sep490.g23.backend.service.admin.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional
public class AuditLogServiceImpl implements AuditLogService {
    private final AuditLogRepository repository;
    public void record(String actorEmail,String action,String targetType,String targetId,String detail){repository.save(AuditLog.builder().actorEmail(actorEmail).action(action).targetType(targetType).targetId(targetId).detail(detail).build());}
    @Transactional(readOnly=true)
    public Page<AuditLogResponse> getLogs(String keyword,String actor,String action,Pageable pageable){
        Specification<AuditLog> spec=(root,q,cb)->cb.conjunction();
        if(keyword!=null&&!keyword.isBlank()){String p="%"+keyword.trim().toLowerCase()+"%";spec=spec.and((r,q,cb)->cb.or(cb.like(cb.lower(r.get("detail")),p),cb.like(cb.lower(r.get("targetType")),p),cb.like(cb.lower(r.get("targetId")),p)));}
        if(actor!=null&&!actor.isBlank()){String p="%"+actor.trim().toLowerCase()+"%";spec=spec.and((r,q,cb)->cb.like(cb.lower(r.get("actorEmail")),p));}
        if(action!=null&&!action.isBlank()) spec=spec.and((r,q,cb)->cb.equal(r.get("action"),action.trim()));
        return repository.findAll(spec,pageable).map(this::response);
    }
    private AuditLogResponse response(AuditLog log){return AuditLogResponse.builder().id(log.getId()).actorEmail(log.getActorEmail()).action(log.getAction()).targetType(log.getTargetType()).targetId(log.getTargetId()).detail(log.getDetail()).createdAt(log.getCreatedAt()).build();}
}
