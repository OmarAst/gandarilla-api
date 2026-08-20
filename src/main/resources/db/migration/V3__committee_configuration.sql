-- Configuración histórica de casas exentas por participación en comité.
CREATE TABLE IF NOT EXISTS comite_casas (
    id BIGSERIAL PRIMARY KEY,
    num_casa SMALLINT NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_comite_casas_num_casa
        CHECK (num_casa BETWEEN 1 AND 50),
    CONSTRAINT chk_comite_casas_periodo
        CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_comite_casa_inicio
    ON comite_casas (num_casa, fecha_inicio);

CREATE UNIQUE INDEX IF NOT EXISTS uq_comite_casa_activa
    ON comite_casas (num_casa)
    WHERE fecha_fin IS NULL;

CREATE INDEX IF NOT EXISTS idx_comite_casas_periodo
    ON comite_casas (fecha_inicio, fecha_fin);

DROP TRIGGER IF EXISTS trg_comite_casas_fecha_actualizacion ON comite_casas;
CREATE TRIGGER trg_comite_casas_fecha_actualizacion
    BEFORE UPDATE ON comite_casas
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_fecha_actualizacion();
