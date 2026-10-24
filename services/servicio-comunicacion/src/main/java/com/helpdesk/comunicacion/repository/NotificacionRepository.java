package com.helpdesk.comunicacion.repository;

import com.helpdesk.comunicacion.model.Notificacion;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
}
