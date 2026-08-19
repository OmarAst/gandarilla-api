-- ============================================================
-- Privada Río Gandarilla
-- Script 03: ejemplos para cargar residentes
-- Reemplaza los datos antes de ejecutar.
-- El teléfono debe incluir código de país, solo dígitos.
-- México: 52 + número de 10 dígitos, por ejemplo 526671234567.
-- ============================================================

-- Ejemplo individual:
-- INSERT INTO residentes (nombre, telefono, num_casa, activo)
-- VALUES ('NOMBRE DEL TITULAR', '526671234567', 1, TRUE);

-- Ejemplo para desactivar al residente anterior de una casa y registrar uno nuevo:
-- BEGIN;
-- UPDATE residentes
-- SET activo = FALSE, fecha_actualizacion = CURRENT_TIMESTAMP
-- WHERE num_casa = 1 AND activo = TRUE;
--
-- INSERT INTO residentes (nombre, telefono, num_casa, activo)
-- VALUES ('NUEVO TITULAR', '526671234567', 1, TRUE);
-- COMMIT;

-- Consulta de validación:
SELECT id, nombre, telefono, num_casa, activo
FROM residentes
ORDER BY num_casa, activo DESC, id;
