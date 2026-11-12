package com.helpdesk.comunicacion.repository;

import com.helpdesk.comunicacion.model.Adjunto;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdjuntoRepository extends JpaRepository<Adjunto, Long> {

    List<Adjunto> findByComentarioIdOrderByIdAsc(Long comentarioId);

    void deleteByComentarioId(Long comentarioId);
}
