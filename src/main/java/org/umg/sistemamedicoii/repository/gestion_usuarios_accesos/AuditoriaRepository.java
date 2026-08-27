package org.umg.sistemamedicoii.repository.gestion_usuarios_accesos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.gestion_usuarios_accesos.Auditoria;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Integer> {}