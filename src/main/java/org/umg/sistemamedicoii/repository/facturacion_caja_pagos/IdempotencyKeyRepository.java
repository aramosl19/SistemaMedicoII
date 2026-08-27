package org.umg.sistemamedicoii.repository.facturacion_caja_pagos;

import org.umg.sistemamedicoii.models.facturacion_caja_pagos.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Integer> {
    Optional<IdempotencyKey> findByClave(String clave);
}