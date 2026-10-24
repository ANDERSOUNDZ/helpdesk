package com.helpdesk.identity.model;

// Los tres perfiles del HelpDesk. Los demas servicios NO importan esta clase: cuando
// necesiten el rol lo reciben como texto ("TECNICO").
public enum Rol {
    ESTUDIANTE,
    TECNICO,
    ADMINISTRADOR
}
