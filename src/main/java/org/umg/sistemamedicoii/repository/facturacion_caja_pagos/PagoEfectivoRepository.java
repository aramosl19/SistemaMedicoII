package org.umg.sistemamedicoii.repository.facturacion_caja_pagos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.enums.TipoConceptoCobro;
import org.umg.sistemamedicoii.models.facturacion_caja_pagos.PagoEfectivo;

public interface PagoEfectivoRepository extends JpaRepository<PagoEfectivo, Integer> {
    boolean existsByTipoConceptoAndReferenciaId(TipoConceptoCobro tipoConcepto, Integer referenciaId);
}