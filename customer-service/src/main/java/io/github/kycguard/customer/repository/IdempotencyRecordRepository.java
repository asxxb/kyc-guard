package io.github.kycguard.customer.repository;

import io.github.kycguard.customer.domain.IdempotencyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecordEntity, String> {
}
