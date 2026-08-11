package fu.sep490.g23.backend.repository.payment;

import fu.sep490.g23.backend.entity.payment.DiscountCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface DiscountCodeRepository extends JpaRepository<DiscountCode, Long> {
    Optional<DiscountCode> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);

    org.springframework.data.domain.Page<DiscountCode> findByActiveTrue(org.springframework.data.domain.Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select discountCode from DiscountCode discountCode where lower(discountCode.code) = lower(:code)")
    Optional<DiscountCode> findByCodeIgnoreCaseForUpdate(@Param("code") String code);
}
