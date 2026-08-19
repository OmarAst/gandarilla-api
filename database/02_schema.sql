-- ============================================================
-- Privada Río Gandarilla
-- Script 02: esquema principal
-- Ejecutar conectado a la base de datos "Gandarilla".
-- ============================================================


CREATE SEQUENCE IF NOT EXISTS movimientos_pago_folio_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;

CREATE TABLE IF NOT EXISTS residentes (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    num_casa SMALLINT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_residentes_nombre_no_vacio
        CHECK (BTRIM(nombre) <> ''),
    CONSTRAINT chk_residentes_telefono_formato
        CHECK (telefono ~ '^[0-9]{10,15}$'),
    CONSTRAINT chk_residentes_num_casa
        CHECK (num_casa BETWEEN 1 AND 50)
);

-- Solo puede existir un residente activo por casa.
CREATE UNIQUE INDEX IF NOT EXISTS uq_residentes_casa_activa
    ON residentes (num_casa)
    WHERE activo = TRUE;

CREATE INDEX IF NOT EXISTS idx_residentes_num_casa
    ON residentes (num_casa);

CREATE INDEX IF NOT EXISTS idx_residentes_activo
    ON residentes (activo);

CREATE TABLE IF NOT EXISTS movimientos_pago (
    id BIGSERIAL PRIMARY KEY,
    id_residente BIGINT NOT NULL,
    monto NUMERIC(10, 2) NOT NULL,
    observaciones VARCHAR(500),
    forma_pago SMALLINT NOT NULL,
    fecha_pago DATE NOT NULL,
    mes SMALLINT NOT NULL,
    anio SMALLINT NOT NULL,
    folio VARCHAR(30) NOT NULL,
    estatus VARCHAR(20) NOT NULL DEFAULT 'REGISTRADO',
    whatsapp_enviado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_envio_whatsapp TIMESTAMPTZ,
    whatsapp_message_id VARCHAR(255),
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_movimientos_pago_residente
        FOREIGN KEY (id_residente)
        REFERENCES residentes (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT uq_movimientos_pago_folio
        UNIQUE (folio),
    CONSTRAINT chk_movimientos_pago_monto
        CHECK (monto > 0),
    CONSTRAINT chk_movimientos_pago_forma
        CHECK (forma_pago BETWEEN 1 AND 4),
    CONSTRAINT chk_movimientos_pago_mes
        CHECK (mes BETWEEN 1 AND 12),
    CONSTRAINT chk_movimientos_pago_anio
        CHECK (anio BETWEEN 2020 AND 2100),
    CONSTRAINT chk_movimientos_pago_estatus
        CHECK (estatus IN ('REGISTRADO', 'CANCELADO')),
    CONSTRAINT chk_movimientos_pago_envio_consistente
        CHECK (
            (whatsapp_enviado = FALSE AND fecha_envio_whatsapp IS NULL)
            OR
            (whatsapp_enviado = TRUE AND fecha_envio_whatsapp IS NOT NULL)
        )
);

CREATE INDEX IF NOT EXISTS idx_movimientos_pago_residente
    ON movimientos_pago (id_residente);

CREATE INDEX IF NOT EXISTS idx_movimientos_pago_periodo
    ON movimientos_pago (anio, mes);

CREATE INDEX IF NOT EXISTS idx_movimientos_pago_residente_periodo
    ON movimientos_pago (id_residente, anio, mes);

CREATE INDEX IF NOT EXISTS idx_movimientos_pago_fecha
    ON movimientos_pago (fecha_pago DESC);

CREATE INDEX IF NOT EXISTS idx_movimientos_pago_estatus
    ON movimientos_pago (estatus);

CREATE INDEX IF NOT EXISTS idx_movimientos_pago_whatsapp_pendiente
    ON movimientos_pago (whatsapp_enviado)
    WHERE whatsapp_enviado = FALSE AND estatus = 'REGISTRADO';


CREATE OR REPLACE FUNCTION actualizar_fecha_actualizacion()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fecha_actualizacion = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_residentes_fecha_actualizacion ON residentes;
CREATE TRIGGER trg_residentes_fecha_actualizacion
    BEFORE UPDATE ON residentes
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_fecha_actualizacion();

DROP TRIGGER IF EXISTS trg_movimientos_pago_fecha_actualizacion ON movimientos_pago;
CREATE TRIGGER trg_movimientos_pago_fecha_actualizacion
    BEFORE UPDATE ON movimientos_pago
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_fecha_actualizacion();

