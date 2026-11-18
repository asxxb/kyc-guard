package io.github.kycguard.customer.service;

import io.github.kycguard.customer.domain.IdempotencyRecordEntity;
import io.github.kycguard.customer.exception.IdempotencyConflictException;
import io.github.kycguard.customer.repository.IdempotencyRecordRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;

    public IdempotencyService(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    public String computeHash(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<IdempotencyRecordEntity> findExisting(String key, String currentHash) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }

        return repository.findById(key).map(record -> {
            if (!record.getRequestHash().equals(currentHash)) {
                throw new IdempotencyConflictException(key);
            }
            return record;
        });
    }

    @Transactional
    public void record(String key, String hash, int responseStatus, String responseBody) {
        if (key == null || key.isBlank()) {
            return;
        }
        IdempotencyRecordEntity record = new IdempotencyRecordEntity(key, hash, responseStatus, responseBody, Instant.now());
        repository.save(record);
    }
}
