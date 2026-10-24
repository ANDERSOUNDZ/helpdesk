CREATE USER tickets WITH PASSWORD 'tickets';
CREATE DATABASE tickets_db OWNER tickets;
REVOKE CONNECT ON DATABASE tickets_db FROM PUBLIC;

CREATE USER identidad WITH PASSWORD 'identidad';
CREATE DATABASE identidad_db OWNER identidad;
REVOKE CONNECT ON DATABASE identidad_db FROM PUBLIC;

CREATE USER comunicacion WITH PASSWORD 'comunicacion';
CREATE DATABASE comunicacion_db OWNER comunicacion;
REVOKE CONNECT ON DATABASE comunicacion_db FROM PUBLIC;

CREATE USER estadisticas WITH PASSWORD 'estadisticas';
CREATE DATABASE estadisticas_db OWNER estadisticas;
REVOKE CONNECT ON DATABASE estadisticas_db FROM PUBLIC;
