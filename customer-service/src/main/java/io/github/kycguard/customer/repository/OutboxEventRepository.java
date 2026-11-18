package io.github.kycguard.customer.repository;

import io.github.kycguard.customer.domain.OutboxEventEntity;
import io.github.kycguard.customer.domain.OutboxStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query("SELECT o FROM OutboxEventEntity o WHERE o.status = :status ORDER BY o.createdAt ASC")
    List<OutboxEventEntity> findPendingEvents(@Param("status") OutboxStatus status, Pageable pageable);
}
