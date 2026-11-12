package com.helpdesk.comunicacion.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail archivoDemasiadoGrande(MaxUploadSizeExceededException ex) {
        ProblemDetail detalle = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "El archivo supera el tamano maximo permitido");
        detalle.setTitle("Archivo demasiado grande");
        return detalle;
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail parteFaltante(MissingServletRequestPartException ex) {
        ProblemDetail detalle = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Debes enviar el campo multipart 'archivos'");
        detalle.setTitle("Solicitud multipart invalida");
        return detalle;
    }
}
