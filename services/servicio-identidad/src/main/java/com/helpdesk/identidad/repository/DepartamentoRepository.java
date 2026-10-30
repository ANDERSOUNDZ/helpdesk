package com.helpdesk.identidad.repository;

import com.helpdesk.identidad.model.Departamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartamentoRepository extends JpaRepository<Departamento, Long> {
}
