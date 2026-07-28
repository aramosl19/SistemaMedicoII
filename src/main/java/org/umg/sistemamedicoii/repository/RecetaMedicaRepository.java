package org.umg.sistemamedicoii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.RecetaMedica;

public interface RecetaMedicaRepository extends JpaRepository<RecetaMedica, Integer> {
}