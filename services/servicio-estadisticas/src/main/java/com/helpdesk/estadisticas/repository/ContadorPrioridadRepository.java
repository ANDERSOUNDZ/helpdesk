package com.helpdesk.estadisticas.repository;

import com.helpdesk.estadisticas.model.ContadorPrioridad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContadorPrioridadRepository extends JpaRepository<ContadorPrioridad, String> {
}
