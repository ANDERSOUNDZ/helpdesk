/**
 * Capa de INFRAESTRUCTURA: todo lo que habla con el exterior. Controllers REST (en un
 * subpaquete web), base de datos, cifrado de claves y configuracion de Spring.
 *
 * Aqui si se usa Spring. Depende de application y domain, nunca al reves: asi cambiar
 * de base de datos o de framework web no toca las reglas de negocio.
 */
package com.helpdesk.identity.infrastructure;
