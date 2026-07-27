package org.umg.sistemamedicoii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.enums.TipoConceptoCobro;
import org.umg.sistemamedicoii.models.PagoTarjeta;

public interface PagoTarjetaRepository extends JpaRepository<PagoTarjeta, Integer> {
    boolean existsByTipoConceptoAndReferenciaId(TipoConceptoCobro tipoConcepto, Integer referenciaId);
}