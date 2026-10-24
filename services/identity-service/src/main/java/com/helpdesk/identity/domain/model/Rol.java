package com.helpdesk.identity.domain.model;

// Los tres perfiles del HelpDesk. Enum en vez de String para que no existan roles
// inventados. Los demas servicios NO importan esta clase: cuando necesiten saber el rol
// lo leen como texto ("TECNICO") desde el token o la API, y tienen su propia copia.
public enum Rol {
    ESTUDIANTE,
    TECNICO,
    ADMINISTRADOR
}
