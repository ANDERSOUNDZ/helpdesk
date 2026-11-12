package com.helpdesk.comunicacion.service;

import com.helpdesk.comunicacion.model.Adjunto;
import com.helpdesk.comunicacion.repository.AdjuntoRepository;
import com.helpdesk.comunicacion.repository.ComentarioRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdjuntoService {

    public record ArchivoAdjunto(Adjunto adjunto, Path ruta) {
    }

    private final AdjuntoRepository adjuntos;
    private final ComentarioRepository comentarios;
    private final Path directorio;
    private final long maxTamanoBytes;
    private final int maxArchivos;
    private final Set<String> tiposPermitidos;

    public AdjuntoService(
            AdjuntoRepository adjuntos,
            ComentarioRepository comentarios,
            @Value("${app.adjuntos.directorio:./datos/adjuntos}") String directorio,
            @Value("${app.adjuntos.max-tamano-bytes:5242880}") long maxTamanoBytes,
            @Value("${app.adjuntos.max-archivos:3}") int maxArchivos,
            @Value("${app.adjuntos.tipos-permitidos:image/png,image/jpeg,application/pdf}") String tiposPermitidos) {
        this.adjuntos = adjuntos;
        this.comentarios = comentarios;
        this.directorio = Paths.get(directorio).toAbsolutePath().normalize();
        this.maxTamanoBytes = maxTamanoBytes;
        this.maxArchivos = maxArchivos;
        this.tiposPermitidos = Arrays.stream(tiposPermitidos.split(","))
                .map(String::trim)
                .map(tipo -> tipo.toLowerCase(Locale.ROOT))
                .filter(tipo -> !tipo.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        crearDirectorio();
    }

    private void crearDirectorio() {
        try {
            Files.createDirectories(directorio);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el directorio de adjuntos: " + directorio, e);
        }
    }

    @Transactional
    public List<Adjunto> guardar(Long comentarioId, List<MultipartFile> archivos) {
        if (archivos == null || archivos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes enviar al menos un archivo");
        }
        if (archivos.size() > maxArchivos) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo se permiten " + maxArchivos + " archivos por comentario");
        }
        if (!comentarios.existsById(comentarioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No existe el comentario con id " + comentarioId);
        }

        List<Path> archivosEscritos = new ArrayList<>();
        List<Adjunto> guardados = new ArrayList<>();
        try {
            for (MultipartFile archivo : archivos) {
                validar(archivo);
                String nombreOriginal = sanitizarNombre(archivo.getOriginalFilename());
                String nombreAlmacenado = UUID.randomUUID() + extension(nombreOriginal);
                Path destino = directorio.resolve(nombreAlmacenado).normalize();
                if (!destino.startsWith(directorio)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de archivo invalido");
                }

                try (InputStream entrada = archivo.getInputStream()) {
                    Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
                }
                archivosEscritos.add(destino);

                Adjunto adjunto = new Adjunto();
                adjunto.setComentarioId(comentarioId);
                adjunto.setNombreOriginal(nombreOriginal);
                adjunto.setNombreAlmacenado(nombreAlmacenado);
                adjunto.setContentType(archivo.getContentType());
                adjunto.setTamanoBytes(archivo.getSize());
                adjunto.setCreadoEn(Instant.now());
                guardados.add(adjuntos.save(adjunto));
            }
            return guardados;
        } catch (IOException e) {
            limpiarArchivos(archivosEscritos);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo guardar el adjunto", e);
        } catch (RuntimeException e) {
            limpiarArchivos(archivosEscritos);
            throw e;
        }
    }

    public List<Adjunto> listarPorComentario(Long comentarioId) {
        if (!comentarios.existsById(comentarioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No existe el comentario con id " + comentarioId);
        }
        return adjuntos.findByComentarioIdOrderByIdAsc(comentarioId);
    }

    public ArchivoAdjunto obtener(Long id) {
        Adjunto adjunto = adjuntos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe el adjunto con id " + id));
        Path ruta = directorio.resolve(adjunto.getNombreAlmacenado()).normalize();
        if (!ruta.startsWith(directorio) || !Files.exists(ruta)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El archivo del adjunto no existe");
        }
        return new ArchivoAdjunto(adjunto, ruta);
    }

    @Transactional
    public void eliminar(Long id) {
        Adjunto adjunto = adjuntos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe el adjunto con id " + id));
        borrarArchivo(adjunto.getNombreAlmacenado());
        adjuntos.delete(adjunto);
    }

    @Transactional
    public void eliminarPorComentario(Long comentarioId) {
        List<Adjunto> lista = adjuntos.findByComentarioIdOrderByIdAsc(comentarioId);
        for (Adjunto adjunto : lista) {
            borrarArchivo(adjunto.getNombreAlmacenado());
        }
        adjuntos.deleteByComentarioId(comentarioId);
    }

    private void validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo esta vacio");
        }
        if (archivo.getSize() > maxTamanoBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "El archivo supera el limite de " + (maxTamanoBytes / 1024 / 1024) + " MB");
        }
        String contentType = archivo.getContentType();
        if (contentType == null || !tiposPermitidos.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Solo se permiten imagenes PNG/JPEG y archivos PDF");
        }
    }

    private String sanitizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "archivo";
        }
        String normalizado = nombre.replace('\\', '/');
        int separador = normalizado.lastIndexOf('/');
        if (separador >= 0) {
            normalizado = normalizado.substring(separador + 1);
        }
        normalizado = normalizado.replaceAll("[\\r\\n\\t\\u0000]", "_").trim();
        if (normalizado.isBlank() || normalizado.equals(".") || normalizado.equals("..")) {
            normalizado = "archivo";
        }
        return normalizado.length() > 255 ? normalizado.substring(0, 255) : normalizado;
    }

    private String extension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        if (punto < 0 || punto == nombre.length() - 1) {
            return "";
        }
        String extension = nombre.substring(punto).toLowerCase(Locale.ROOT);
        return extension.length() <= 10 ? extension : "";
    }

    private void borrarArchivo(String nombreAlmacenado) {
        try {
            Files.deleteIfExists(directorio.resolve(nombreAlmacenado).normalize());
        } catch (IOException e) {
            // El archivo puede haber sido borrado manualmente. No bloqueamos la operacion.
        }
    }

    private void limpiarArchivos(List<Path> archivos) {
        for (Path archivo : archivos) {
            try {
                Files.deleteIfExists(archivo);
            } catch (IOException e) {
                // Limpieza best effort.
            }
        }
    }
}
