package org.umg.sistemamedicoii.repository.gestion_usuarios_accesos;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.umg.sistemamedicoii.models.gestion_usuarios_accesos.Auditoria;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AuditoriaSpecification {

    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static Specification<Auditoria> conFiltros(String campo, String valor,
                                                      LocalDate fechaDesde, LocalDate fechaHasta, LocalTime horaDesde, LocalTime horaHasta) {

        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (valor != null && !valor.isBlank() && campo != null) {
                String like = "%" + valor.toLowerCase() + "%";
                switch (campo.toUpperCase()) {
                    case "USUARIO" -> predicados.add(cb.like(cb.lower(root.get("nombreUsuario")), like));
                    case "NOMBRE" -> predicados.add(cb.like(cb.lower(root.get("nombreReal")), like));
                    case "ROL" -> predicados.add(cb.like(cb.lower(root.get("rol")), like));
                    case "OPERACION" -> predicados.add(cb.like(cb.lower(root.get("accion")), like));
                    default -> {}
                }
            }

            if (fechaDesde != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fechaHora"),
                        LocalDateTime.of(fechaDesde, LocalTime.MIN)));
            }
            if (fechaHasta != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("fechaHora"),
                        LocalDateTime.of(fechaHasta, LocalTime.MAX)));
            }

            if (horaDesde != null || horaHasta != null) {
                var horaTexto = cb.function("to_char", String.class, root.get("fechaHora"),
                        cb.literal("HH24:MI:SS"));
                if (horaDesde != null) predicados.add(cb.greaterThanOrEqualTo(horaTexto, horaDesde.format(HORA_FMT)));
                if (horaHasta != null) predicados.add(cb.lessThanOrEqualTo(horaTexto, horaHasta.format(HORA_FMT)));
            }

            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}