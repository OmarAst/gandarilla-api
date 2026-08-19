CREATE TABLE gastos_comite (
    id BIGSERIAL PRIMARY KEY,
    concepto VARCHAR(150) NOT NULL,
    proveedor VARCHAR(150),
    monto NUMERIC(10, 2) NOT NULL,
    fecha_gasto DATE NOT NULL,
    mes SMALLINT NOT NULL,
    anio SMALLINT NOT NULL,
    estatus VARCHAR(20) NOT NULL DEFAULT 'PAGADO',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_gastos_concepto_no_vacio CHECK (BTRIM(concepto) <> ''),
    CONSTRAINT chk_gastos_monto CHECK (monto > 0),
    CONSTRAINT chk_gastos_mes CHECK (mes BETWEEN 1 AND 12),
    CONSTRAINT chk_gastos_anio CHECK (anio BETWEEN 2020 AND 2100),
    CONSTRAINT chk_gastos_estatus CHECK (estatus IN ('PAGADO', 'PENDIENTE'))
);

CREATE INDEX idx_gastos_periodo ON gastos_comite (anio, mes);
CREATE INDEX idx_gastos_estatus ON gastos_comite (estatus);
