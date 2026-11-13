package com.helpdesk.identidad.config;

import com.helpdesk.identidad.controller.AuthController;
import com.helpdesk.identidad.controller.UsuarioController;
import com.helpdesk.identidad.dto.LoginResponse;
import com.helpdesk.identidad.dto.UsuarioSesion;
import com.helpdesk.identidad.model.Rol;
import com.helpdesk.identidad.model.Usuario;
import com.helpdesk.identidad.service.AuthService;
import com.helpdesk.identidad.service.JwtService;
import com.helpdesk.identidad.service.UsuarioService;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({UsuarioController.class, AuthController.class})
@Import(SeguridadConfig.class)
@TestPropertySource(properties = {
        "app.auth.enabled=true",
        "app.jwt.secret=una-clave-de-prueba-de-al-menos-32-caracteres"
})
class SeguridadConfigTest {

    private static final String SECRETO = "una-clave-de-prueba-de-al-menos-32-caracteres";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private AuthService authService;

    private RequestPostProcessor como(String rol) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNombre("Ana");
        u.setRol(Rol.TECNICO);
        u.setUsername("ana");
        u.setPasswordHash("$2a$10$hashquenuncadebesalir");
        return u;
    }

    @Test
    void sinTokenSePuedeHacerLoginPeroNoConsultarUsuarios() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse("t", "Bearer", 1L,
                new UsuarioSesion(1L, "Ana", "ana", Rol.TECNICO)));

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"clave\":\"secreta123\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/usuarios/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void cualquierRolAutenticadoPuedeConsultarUsuarios() throws Exception {
        when(usuarioService.obtenerTodos()).thenReturn(List.of(usuario()));
        when(usuarioService.obtenerPorId(anyLong())).thenReturn(usuario());

        for (String rol : List.of("SOLICITANTE", "TECNICO", "ADMINISTRADOR")) {
            mvc.perform(get("/usuarios").with(como(rol))).andExpect(status().isOk());
            mvc.perform(get("/usuarios/1").with(como(rol))).andExpect(status().isOk());
        }
    }

    @Test
    void soloElAdministradorPuedeCrearEditarOEliminarUsuarios() throws Exception {
        when(usuarioService.crear(any())).thenReturn(usuario());
        when(usuarioService.actualizar(anyLong(), any())).thenReturn(usuario());
        String cuerpo = "{\"nombre\":\"Ana\",\"rol\":\"TECNICO\"}";

        for (String rol : List.of("SOLICITANTE", "TECNICO")) {
            mvc.perform(post("/usuarios").with(como(rol)).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/usuarios/1").with(como(rol)).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/usuarios/1").with(como(rol))).andExpect(status().isForbidden());
        }

        mvc.perform(post("/usuarios").with(como("ADMINISTRADOR")).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated());
        mvc.perform(put("/usuarios/1").with(como("ADMINISTRADOR")).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk());
        mvc.perform(delete("/usuarios/1").with(como("ADMINISTRADOR"))).andExpect(status().isNoContent());
    }

    @Test
    void laRespuestaNuncaIncluyeElHashNiLaClave() throws Exception {
        Usuario conClave = usuario();
        conClave.setClave("secreta123");
        when(usuarioService.obtenerTodos()).thenReturn(List.of(conClave));

        mvc.perform(get("/usuarios").with(como("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("ana"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].clave").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("hashquenuncadebesalir"))));
    }

    @Test
    void laClaveEnviadaAlCrearUnUsuarioLlegaAlServicio() throws Exception {
        when(usuarioService.crear(any())).thenReturn(usuario());

        mvc.perform(post("/usuarios").with(como("ADMINISTRADOR")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"rol\":\"TECNICO\",\"username\":\"ana\",\"clave\":\"secreta123\"}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<Usuario> captura = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioService).crear(captura.capture());
        assertEquals("secreta123", captura.getValue().getClave());
        assertEquals("ana", captura.getValue().getUsername());
    }

    @Test
    void unTokenRealFirmadoConLaClaveEsAceptado() throws Exception {
        when(usuarioService.obtenerTodos()).thenReturn(List.of(usuario()));
        when(usuarioService.crear(any())).thenReturn(usuario());
        Usuario admin = usuario();
        admin.setRol(Rol.ADMINISTRADOR);
        String token = new JwtService(SECRETO, 60).emitir(admin).token();

        mvc.perform(get("/usuarios").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(post("/usuarios").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"X\",\"rol\":\"TECNICO\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void unTokenFirmadoConOtraClaveOMalFormadoEsRechazado() throws Exception {
        Usuario admin = usuario();
        admin.setRol(Rol.ADMINISTRADOR);
        String ajeno = new JwtService("otra-clave-distinta-de-al-menos-32-caracteres!", 60).emitir(admin).token();

        mvc.perform(get("/usuarios").header("Authorization", "Bearer " + ajeno)).andExpect(status().isUnauthorized());
        mvc.perform(get("/usuarios").header("Authorization", "Bearer basura")).andExpect(status().isUnauthorized());
    }

    @Test
    void elChequeoDeSaludEsPublico() throws Exception {
        int estado = mvc.perform(get("/actuator/health")).andReturn().getResponse().getStatus();

        org.junit.jupiter.api.Assertions.assertNotEquals(401, estado);
        org.junit.jupiter.api.Assertions.assertNotEquals(403, estado);
    }
}
