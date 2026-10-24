package com.helpdesk.identidad.repository;

import com.helpdesk.identidad.model.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}
