package org.umg.sistemamedicoii.service.gestion_usuarios_acceso;

import org.springframework.data.domain.Page;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.BitacoraOperacionResponseDTO;

import java.time.LocalDate;
import java.time.LocalTime;

public interface BitacoraOperacionService {
    Page<BitacoraOperacionResponseDTO> buscar(String campo, String valor, LocalDate fechaDesde,
                                              LocalDate fechaHasta, LocalTime horaDesde, LocalTime horaHasta, int pagina);
}