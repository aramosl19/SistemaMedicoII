package org.umg.sistemamedicoii.service.examenes_laboratorio;

import org.umg.sistemamedicoii.dto.examenes_laboratorio.OrdenLaboratorioRequestDTO;
import org.umg.sistemamedicoii.dto.examenes_laboratorio.OrdenLaboratorioResponseDTO;

public interface OrdenLaboratorioService {

    OrdenLaboratorioResponseDTO generarOrden(Integer citaId, OrdenLaboratorioRequestDTO dto);
}