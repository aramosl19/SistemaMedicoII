package org.umg.sistemamedicoii.service.gestion_usuarios_acceso.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.BitacoraOperacionResponseDTO;
import org.umg.sistemamedicoii.models.gestion_usuarios_accesos.Auditoria;
import org.umg.sistemamedicoii.repository.gestion_usuarios_accesos.AuditoriaRepository;
import org.umg.sistemamedicoii.repository.gestion_usuarios_accesos.AuditoriaSpecification;
import org.umg.sistemamedicoii.service.gestion_usuarios_acceso.BitacoraOperacionService;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class BitacoraOperacionServiceImpl implements BitacoraOperacionService {

    private static final int TAMANO_PAGINA = 20;

    @Autowired private AuditoriaRepository auditoriaRepository;

    @Override
    public Page<BitacoraOperacionResponseDTO> buscar(String campo, String valor, LocalDate fechaDesde,
                                                     LocalDate fechaHasta, LocalTime horaDesde, LocalTime horaHasta, int pagina) {

        var spec = AuditoriaSpecification.conFiltros(campo, valor, fechaDesde, fechaHasta, horaDesde, horaHasta);
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), TAMANO_PAGINA, Sort.by(Sort.Direction.DESC, "fechaHora"));
        return auditoriaRepository.findAll(spec, pageable).map(this::toDTO);
    }

    private BitacoraOperacionResponseDTO toDTO(Auditoria a) {
        BitacoraOperacionResponseDTO dto = new BitacoraOperacionResponseDTO();
        dto.setId(a.getId());
        dto.setNombreUsuario(a.getNombreUsuario());
        dto.setNombreReal(a.getNombreReal());
        dto.setRol(a.getRol());
        dto.setOperacion(a.getAccion());
        dto.setEntidadAfectada(a.getEntidadAfectada());
        dto.setEntidadId(a.getEntidadId());
        dto.setFecha(a.getFechaHora().toLocalDate());
        dto.setHora(a.getFechaHora().toLocalTime());
        return dto;
    }
}