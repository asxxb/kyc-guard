package io.github.kycguard.customer.repository;

import io.github.kycguard.common.enums.CustomerStatus;
import io.github.kycguard.common.enums.RiskBand;
import io.github.kycguard.customer.domain.CustomerEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID>, JpaSpecificationExecutor<CustomerEntity> {

    @Query("""
        SELECT c FROM CustomerEntity c
        WHERE (:status IS NULL OR c.status = :status)
          AND (:riskBand IS NULL OR c.riskBand = :riskBand)
    """)
    Page<CustomerEntity> findByStatusAndRiskBand(
            @Param("status") CustomerStatus status,
            @Param("riskBand") RiskBand riskBand,
            Pageable pageable
    );
}
