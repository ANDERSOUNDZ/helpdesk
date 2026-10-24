/**
 * Capa de APLICACION: casos de uso (generar el resumen del dashboard, reporte semanal)
 * y los puertos de entrada y salida.
 *
 * Solo depende del dominio. Los datos de tickets los pide por un puerto de salida; este
 * servicio NO lee la base de datos de tickets-service, para no acoplar los dos.
 */
package com.helpdesk.analytics.application;
