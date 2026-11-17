package com.helpdesk.identidad.service;

import com.helpdesk.identidad.model.Rol;
import com.helpdesk.identidad.model.Usuario;
import com.helpdesk.identidad.repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsuarioServiceTest {

    private final Map<Long, Usuario> baseDeDatos = new HashMap<>();
    private final UsuarioRepository repositorio = mock(UsuarioRepository.class);
    private final PasswordEncoder codificador = new BCryptPasswordEncoder();
    private UsuarioService service;

    @BeforeEach
    void preparar() {
        when(repositorio.save(any(Usuario.class))).thenAnswer(llamada -> {
            Usuario usuario = llamada.getArgument(0);
            if (usuario.getId() == null) {
                usuario.setId((long) baseDeDatos.size() + 1);
            }
            baseDeDatos.put(usuario.getId(), usuario);
            return usuario;
        });
        when(repositorio.findById(anyLong()))
                .thenAnswer(llamada -> Optional.ofNullable(baseDeDatos.get(llamada.<Long>getArgument(0))));
        when(repositorio.findAll(any(Sort.class))).thenAnswer(llamada -> new ArrayList<>(baseDeDatos.values()));
        when(repositorio.findByUsername(anyString())).thenAnswer(llamada -> baseDeDatos.values().stream()
                .filter(u -> llamada.<String>getArgument(0).equals(u.getUsername()))
                .findFirst());
        when(repositorio.countByRol(any(Rol.class))).thenAnswer(llamada -> baseDeDatos.values().stream()
                .filter(u -> u.getRol() == llamada.<Rol>getArgument(0))
                .count());
        doAnswer(llamada -> {
            Usuario borrado = llamada.getArgument(0);
            baseDeDatos.remove(borrado.getId());
            return null;
        }).when(repositorio).delete(any(Usuario.class));
        service = new UsuarioService(repositorio, codificador);
    }

    private Usuario usuario(String nombre, Rol rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setRol(rol);
        return u;
    }

    private Usuario conCredenciales(String nombre, Rol rol, String username, String clave) {
        Usuario u = usuario(nombre, rol);
        u.setUsername(username);
        u.setClave(clave);
        return u;
    }

    private HttpStatus estadoDe(ResponseStatusException e) {
        return HttpStatus.valueOf(e.getStatusCode().value());
    }

    @Test
    void crearAsignaIdYSePuedeConsultar() {
        Usuario creado = service.crear(usuario("Ana", Rol.SOLICITANTE));

        assertEquals(1L, creado.getId());
        assertEquals("Ana", service.obtenerPorId(1L).getNombre());
        assertEquals(1, service.obtenerTodos().size());
    }

    @Test
    void rechazaNombreVacioORolNulo() {
        assertThrows(ResponseStatusException.class, () -> service.crear(usuario(" ", Rol.TECNICO)));
        assertThrows(ResponseStatusException.class, () -> service.crear(usuario("Luis", null)));
    }

    @Test
    void obtenerUnIdInexistenteDa404() {
        assertThrows(ResponseStatusException.class, () -> service.obtenerPorId(99L));
    }

    @Test
    void crearConClaveGuardaElHashYNuncaLaClaveEnClaro() {
        Usuario creado = service.crear(conCredenciales("Ana", Rol.SOLICITANTE, "Ana.Lopez", "secreta123"));

        assertEquals("ana.lopez", creado.getUsername());
        assertNotEquals("secreta123", creado.getPasswordHash());
        assertTrue(codificador.matches("secreta123", creado.getPasswordHash()));
        assertNull(creado.getClave());
        assertTrue(creado.estaActivo());
    }

    @Test
    void crearSinCredencialesDejaAlUsuarioSinAcceso() {
        Usuario creado = service.crear(usuario("Luis", Rol.TECNICO));

        assertNull(creado.getUsername());
        assertNull(creado.getPasswordHash());
    }

    @Test
    void claveCortaDa400() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.crear(conCredenciales("Ana", Rol.SOLICITANTE, "ana", "corta")));

        assertEquals(HttpStatus.BAD_REQUEST, estadoDe(e));
    }

    @Test
    void claveSinUsuarioDa400() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.crear(conCredenciales("Ana", Rol.SOLICITANTE, null, "secreta123")));

        assertEquals(HttpStatus.BAD_REQUEST, estadoDe(e));
    }

    @Test
    void usuarioConFormatoInvalidoDa400() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.crear(conCredenciales("Ana", Rol.SOLICITANTE, "a b", "secreta123")));

        assertEquals(HttpStatus.BAD_REQUEST, estadoDe(e));
    }

    @Test
    void usuarioRepetidoDa409() {
        service.crear(conCredenciales("Ana", Rol.SOLICITANTE, "ana", "secreta123"));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.crear(conCredenciales("Otra Ana", Rol.SOLICITANTE, "ANA", "secreta456")));

        assertEquals(HttpStatus.CONFLICT, estadoDe(e));
    }

    @Test
    void actualizarPermiteCambiarRolActivoYClave() {
        Usuario creado = service.crear(conCredenciales("Ana", Rol.SOLICITANTE, "ana", "secreta123"));
        String hashAnterior = creado.getPasswordHash();

        Usuario datos = new Usuario();
        datos.setRol(Rol.TECNICO);
        datos.setActivo(false);
        datos.setClave("otraClave99");
        Usuario actualizado = service.actualizar(creado.getId(), datos);

        assertEquals(Rol.TECNICO, actualizado.getRol());
        assertFalse(actualizado.estaActivo());
        assertNotEquals(hashAnterior, actualizado.getPasswordHash());
        assertTrue(codificador.matches("otraClave99", actualizado.getPasswordHash()));
    }

    @Test
    void actualizarPuedeConservarSuPropioUsuarioSinDarConflicto() {
        Usuario creado = service.crear(conCredenciales("Ana", Rol.SOLICITANTE, "ana", "secreta123"));

        Usuario datos = new Usuario();
        datos.setUsername("ana");
        datos.setNombre("Ana Maria");
        Usuario actualizado = service.actualizar(creado.getId(), datos);

        assertEquals("Ana Maria", actualizado.getNombre());
        assertEquals("ana", actualizado.getUsername());
    }

    @Test
    void crearRegistraLaFechaDeIngreso() {
        Usuario creado = service.crear(usuario("Ana", Rol.SOLICITANTE));

        assertNotNull(creado.getFechaIngreso());
    }

    @Test
    void eliminarBorraAlUsuarioYNoPermiteEliminarseASiMismo() {
        service.crear(usuario("Admin", Rol.ADMINISTRADOR));
        service.crear(usuario("Admin dos", Rol.ADMINISTRADOR));
        Usuario tecnico = service.crear(usuario("Tec", Rol.TECNICO));

        ResponseStatusException propio = assertThrows(ResponseStatusException.class,
                () -> service.eliminar(tecnico.getId(), tecnico.getId()));
        assertEquals(HttpStatus.CONFLICT, estadoDe(propio));

        service.eliminar(tecnico.getId(), 1L);
        assertFalse(baseDeDatos.containsKey(tecnico.getId()));
    }

    @Test
    void noSePuedeEliminarNiQuitarElRolAlUnicoAdministrador() {
        Usuario admin = service.crear(usuario("Admin", Rol.ADMINISTRADOR));

        ResponseStatusException alEliminar = assertThrows(ResponseStatusException.class,
                () -> service.eliminar(admin.getId(), null));
        assertEquals(HttpStatus.CONFLICT, estadoDe(alEliminar));

        Usuario datos = new Usuario();
        datos.setRol(Rol.TECNICO);
        ResponseStatusException alDegradar = assertThrows(ResponseStatusException.class,
                () -> service.actualizar(admin.getId(), datos));
        assertEquals(HttpStatus.CONFLICT, estadoDe(alDegradar));
        assertEquals(Rol.ADMINISTRADOR, service.obtenerPorId(admin.getId()).getRol());
    }

    @Test
    void conVariosAdministradoresSiSePuedeEliminarUnoDeEllos() {
        service.crear(usuario("Admin", Rol.ADMINISTRADOR));
        Usuario otro = service.crear(usuario("Otro admin", Rol.ADMINISTRADOR));

        service.eliminar(otro.getId(), 1L);

        assertFalse(baseDeDatos.containsKey(otro.getId()));
    }
}
