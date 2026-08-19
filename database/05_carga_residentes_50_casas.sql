-- ============================================================
-- Privada Río Gandarilla
-- Carga inicial de residentes para las casas 1 a 50.
--
-- INSTRUCCIONES:
-- 1. Sustituye cada NULL de la columna telefono por el teléfono internacional.
-- 2. Usa únicamente dígitos: México = 52 + número de 10 dígitos.
-- 3. Ejecuta el script conectado a la base de datos "Gandarilla".
--
-- El script se detiene sin insertar filas si falta algún teléfono o si ya
-- existe un residente activo en cualquiera de las casas indicadas.
-- ============================================================

BEGIN;

CREATE TEMP TABLE carga_residentes (
    num_casa SMALLINT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    telefono VARCHAR(20)
) ON COMMIT DROP;

INSERT INTO carga_residentes (num_casa, nombre, telefono) VALUES
    (1,  'Lorena Salazar Vega', NULL),
    (2,  'Citlali Madueño', NULL),
    (3,  'Clemente Valdez', NULL),
    (4,  'Ricardo Montoya', NULL),
    (5,  'Julio Gonzalez', NULL),
    (6,  'Alma Ochoa', NULL),
    (7,  'Andres Najar Soto', NULL),
    (8,  'Orlando Cruz', NULL),
    (9,  'Nerea Anabel Robledo Ahumada', NULL),
    (10, 'Daniel Gastelum', NULL),
    (11, 'Edna Quintero', NULL),
    (12, 'Carolina Gonzalez', NULL),
    (13, 'Gustavo Quintero', NULL),
    (14, 'Hector Camacho', NULL),
    (15, 'Lidia Quintero', NULL),
    (16, 'Mario Adolfo Hernandez Rodriguez', NULL),
    (17, 'Brian Rivera', NULL),
    (18, 'Ana Lucina Rocha Vargas', NULL),
    (19, 'Aracely Solis', NULL),
    (20, 'Chistian René Rocha', NULL),
    (21, 'Daniel Gradilla', NULL),
    (22, 'Gamaliel Sarabia', NULL),
    (23, 'Jose Antonio Rojas Arenas', NULL),
    (24, 'Eduardo Garrido', NULL),
    (25, 'Andres Gonzalez Gamez', NULL),
    (26, 'Ana Karen Soto felix', NULL),
    (27, 'Nestor Campos', NULL),
    (28, 'Emili Sujey Vega Gonzalez', NULL),
    (29, 'Brenda Yeneli Baez Manjarrez', NULL),
    (30, 'Mara Osuna', NULL),
    (31, 'José Pablo Melendrez Gonzalez', NULL),
    (32, 'Ana Nevarez', NULL),
    (33, 'Luis Miguel Flores', NULL),
    (34, 'Sandra Barraza', NULL),
    (35, 'Xochilt Margoth Cruz Montoya', NULL),
    (36, 'Olivia Romero', NULL),
    (37, 'Cinthia Jimenez', NULL),
    (38, 'Yoseline Rios', NULL),
    (39, 'Ruth Vanessa Chaidez Gil', NULL),
    (40, 'Yarline', NULL),
    (41, 'Andres Najar Ponce', NULL),
    (42, 'Daniel Salazar Sanchez', NULL),
    (43, 'Jose Rodriguez', NULL),
    (44, 'Yadin Esau Orozco Lomas', NULL),
    (45, 'Sandra Becerra Maldonado', NULL),
    (46, 'Eduardo Mascareño', NULL),
    (47, 'Yesenia Herrera Velarde', NULL),
    (48, 'Ana Hilda Muñoz', NULL),
    (49, 'Karen Yishell Tiznado Garcia', NULL),
    (50, 'Maria Elizabeth Lugo Angulo', NULL);

DO $$
DECLARE
    casas_sin_telefono TEXT;
    casas_ocupadas TEXT;
BEGIN
    SELECT STRING_AGG(num_casa::TEXT, ', ' ORDER BY num_casa)
    INTO casas_sin_telefono
    FROM carga_residentes
    WHERE telefono IS NULL
       OR telefono !~ '^[0-9]{10,15}$';

    IF casas_sin_telefono IS NOT NULL THEN
        RAISE EXCEPTION
            'Completa teléfonos internacionales válidos para las casas: %',
            casas_sin_telefono;
    END IF;

    SELECT STRING_AGG(r.num_casa::TEXT, ', ' ORDER BY r.num_casa)
    INTO casas_ocupadas
    FROM residentes r
    JOIN carga_residentes c ON c.num_casa = r.num_casa
    WHERE r.activo = TRUE;

    IF casas_ocupadas IS NOT NULL THEN
        RAISE EXCEPTION
            'Ya existen residentes activos en las casas: %. Desactívalos explícitamente antes de cargar.',
            casas_ocupadas;
    END IF;
END $$;

INSERT INTO residentes (nombre, telefono, num_casa, activo)
SELECT nombre, telefono, num_casa, TRUE
FROM carga_residentes
ORDER BY num_casa;

COMMIT;

SELECT id, num_casa, nombre, telefono, activo
FROM residentes
WHERE num_casa BETWEEN 1 AND 50
ORDER BY num_casa, activo DESC, id DESC;
