package org.umg.sistemamedicoii.repository;

import org.umg.sistemamedicoii.models.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Integer> {
    Optional<IdempotencyKey> findByClave(String clave);
}