ALTER TABLE movimientos_pago
    ADD COLUMN whatsapp_envio_reserva UUID,
    ADD COLUMN fecha_reserva_whatsapp TIMESTAMPTZ;

ALTER TABLE movimientos_pago
    ADD CONSTRAINT chk_movimientos_pago_reserva_consistente
        CHECK (
            (whatsapp_envio_reserva IS NULL AND fecha_reserva_whatsapp IS NULL)
            OR
            (whatsapp_envio_reserva IS NOT NULL AND fecha_reserva_whatsapp IS NOT NULL)
        );

CREATE INDEX idx_movimientos_pago_reserva_whatsapp
    ON movimientos_pago (fecha_reserva_whatsapp)
    WHERE whatsapp_envio_reserva IS NOT NULL
      AND whatsapp_enviado = FALSE;
