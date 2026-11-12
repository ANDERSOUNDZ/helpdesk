package com.helpdesk.comunicacion.service;

import com.helpdesk.comunicacion.model.Adjunto;
import com.helpdesk.comunicacion.repository.AdjuntoRepository;
import com.helpdesk.comunicacion.repository.ComentarioRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdjuntoServiceTest {

    @TempDir
    Path tempDir;

    private final AdjuntoRepository adjuntos = mock(AdjuntoRepository.class);
    private final ComentarioRepository comentarios = mock(ComentarioRepository.class);
    private final AtomicLong ids = new AtomicLong(1);
    private AdjuntoService service;

    @BeforeEach
    void preparar() {
        when(comentarios.existsById(1L)).thenReturn(true);
        when(adjuntos.save(any(Adjunto.class))).thenAnswer(i -> {
            Adjunto a = i.getArgument(0);
            if (a.getId() == null) {
                a.setId(ids.getAndIncrement());
            }
            return a;
        });
        service = new AdjuntoService(adjuntos, comentarios, tempDir.toString(),
                5 * 1024 * 1024, 3, "image/png,image/jpeg,application/pdf");
    }

    @Test
    void guardaArchivoValidoEnDiscoYMetadata() throws Exception {
        byte[] contenido = "PNG simulado".getBytes();
        MultipartFile archivo = new MockMultipartFile(
                "archivos", "captura.png", "image/png", contenido);

        List<Adjunto> guardados = service.guardar(1L, List.of(archivo));

        assertEquals(1, guardados.size());
        assertEquals("captura.png", guardados.get(0).getNombreOriginal());
        Path almacenado = tempDir.resolve(guardados.get(0).getNombreAlmacenado());
        assertTrue(Files.exists(almacenado));
        assertEquals(contenido.length, Files.size(almacenado));
    }

    @Test
    void rechazaTipoNoPermitido() {
        MultipartFile archivo = new MockMultipartFile(
                "archivos", "notas.txt", "text/plain", "hola".getBytes());

        assertThrows(ResponseStatusException.class, () -> service.guardar(1L, List.of(archivo)));
    }

    @Test
    void rechazaArchivoDemasiadoGrande() {
        byte[] grande = new byte[6 * 1024 * 1024];
        MultipartFile archivo = new MockMultipartFile(
                "archivos", "grande.pdf", "application/pdf", grande);

        assertThrows(ResponseStatusException.class, () -> service.guardar(1L, List.of(archivo)));
    }

    @Test
    void rechazaMasDeTresArchivos() {
        MultipartFile archivo = new MockMultipartFile(
                "archivos", "ok.png", "image/png", "ok".getBytes());

        assertThrows(ResponseStatusException.class,
                () -> service.guardar(1L, List.of(archivo, archivo, archivo, archivo)));
    }

    @Test
    void obtieneYEliminaAdjunto() throws Exception {
        MultipartFile archivo = new MockMultipartFile(
                "archivos", "evidencia.pdf", "application/pdf", "pdf".getBytes());
        Adjunto guardado = service.guardar(1L, List.of(archivo)).get(0);
        when(adjuntos.findById(guardado.getId())).thenReturn(Optional.of(guardado));

        AdjuntoService.ArchivoAdjunto obtenido = service.obtener(guardado.getId());
        assertTrue(Files.exists(obtenido.ruta()));

        service.eliminar(guardado.getId());
        assertFalse(Files.exists(obtenido.ruta()));
    }

    @Test
    void rechazaComentarioInexistente() {
        when(comentarios.existsById(99L)).thenReturn(false);
        MultipartFile archivo = new MockMultipartFile(
                "archivos", "ok.png", "image/png", "ok".getBytes());

        assertThrows(ResponseStatusException.class, () -> service.guardar(99L, List.of(archivo)));
    }
}
