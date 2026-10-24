package com.helpdesk.comunicacion.repository;

import com.helpdesk.comunicacion.model.Comentario;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
}
