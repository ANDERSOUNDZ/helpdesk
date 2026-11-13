package com.helpdesk.identidad.dto;

import com.helpdesk.identidad.model.Rol;

public record UsuarioSesion(Long id, String nombre, String username, Rol rol) {
}
