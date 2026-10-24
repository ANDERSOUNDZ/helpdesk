package com.helpdesk.identity.domain;

import com.helpdesk.identity.domain.model.Rol;
import com.helpdesk.identity.domain.model.Usuario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test del dominio SIN Spring: arranca en milisegundos.
class UsuarioTest {

    @Test
    void usuarioNuevoGuardaNombreSinEspaciosYRol() {
        Usuario usuario = Usuario.nuevo("  Ana  ", Rol.ESTUDIANTE);

        assertEquals("Ana", usuario.getNombre());
        assertEquals(Rol.ESTUDIANTE, usuario.getRol());
    }

    @Test
    void soloTecnicoYAdministradorPuedenAtenderTickets() {
        assertTrue(Usuario.nuevo("Luis", Rol.TECNICO).puedeAtenderTickets());
        assertTrue(Usuario.nuevo("Eva", Rol.ADMINISTRADOR).puedeAtenderTickets());
        assertFalse(Usuario.nuevo("Ana", Rol.ESTUDIANTE).puedeAtenderTickets());
    }

    @Test
    void rechazaNombreVacio() {
        assertThrows(IllegalArgumentException.class, () -> Usuario.nuevo("  ", Rol.TECNICO));
    }

    @Test
    void rechazaRolNulo() {
        assertThrows(IllegalArgumentException.class, () -> Usuario.nuevo("Ana", null));
    }
}
