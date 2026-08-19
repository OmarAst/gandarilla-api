-- ============================================================
-- Privada Río Gandarilla
-- Script 01: creación de la base de datos
-- Ejecutar conectado a la base postgres con un usuario autorizado.
-- PostgreSQL no permite CREATE DATABASE dentro de una transacción.
-- ============================================================

CREATE DATABASE "Gandarilla"
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    TEMPLATE = template0
    CONNECTION LIMIT = -1;
