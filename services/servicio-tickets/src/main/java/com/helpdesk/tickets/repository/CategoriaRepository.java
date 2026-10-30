package com.helpdesk.tickets.repository;

import com.helpdesk.tickets.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}
