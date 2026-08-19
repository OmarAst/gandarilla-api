# Notas de implementación

- Se sustituyó el directorio JSON de casas por PostgreSQL.
- Se agregaron las tablas `residentes` y `movimientos_pago`.
- El endpoint `POST /api/payment-movements` registra pagos y genera folios únicos.
- El endpoint `POST /api/payment-movements/{id}/send-receipt` genera y envía el recibo del pago exacto.
- Se conserva `POST /api/receipts/send` como compatibilidad para `{casa, mes}` usando el año actual.
- Se permiten múltiples pagos por residente y periodo.
- Las formas de pago se validan como valores 1 a 4 sin tabla de catálogo.
- Los envíos reales actualizan `whatsapp_enviado`, `fecha_envio_whatsapp` y `whatsapp_message_id`.
- Los envíos simulados no alteran el estado de WhatsApp en la base.
