/**
 * Capa de INFRAESTRUCTURA: todo lo que habla con el exterior. Controllers REST (en un
 * subpaquete web), base de datos y recepcion de avisos de cambio de estado.
 *
 * Aqui si se usa Spring. Depende de application y domain, nunca al reves: asi cambiar
 * de base de datos o de canal de aviso no toca las reglas de negocio.
 */
package com.helpdesk.communication.infrastructure;
