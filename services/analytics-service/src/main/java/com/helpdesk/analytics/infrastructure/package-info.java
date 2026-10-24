/**
 * Capa de INFRAESTRUCTURA: todo lo que habla con el exterior. Controllers REST (en un
 * subpaquete web), cliente que consulta a tickets-service y configuracion de Spring.
 *
 * Aqui si se usa Spring. Depende de application y domain, nunca al reves: asi cambiar
 * la fuente de datos o el framework web no toca las reglas de calculo.
 */
package com.helpdesk.analytics.infrastructure;
