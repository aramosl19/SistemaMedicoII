package org.umg.sistemamedicoii.dto;

public enum ResultadoBusquedaRecepcionResponseDTO {
    CITA_ENCONTRADA,
    PACIENTE_NO_REGISTRADO, // FA03 el frontend debe llevar a CU-02 (POST /api/portal/registro)
    SIN_CITAS_ACTIVAS // FA04 el frontend debe llevar a CU-03 (POST /api/citas)
}
