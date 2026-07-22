package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.PagoRequestDTO;
import org.umg.sistemamedicoii.dto.PagoResponseDTO;

public interface PagoService {
    PagoResponseDTO procesarPago(PagoRequestDTO dto);
}