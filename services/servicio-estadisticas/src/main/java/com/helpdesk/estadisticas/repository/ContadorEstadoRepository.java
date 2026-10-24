package com.helpdesk.estadisticas.repository;

import com.helpdesk.estadisticas.model.ContadorEstado;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContadorEstadoRepository extends JpaRepository<ContadorEstado, String> {
}
