package com.riogandarilla.api.repositories;

import com.riogandarilla.api.dto.request.CreatePaymentMovementRequest;
import com.riogandarilla.api.configs.properties.AppProperties;
import com.riogandarilla.api.entities.PaymentMovement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public class PaymentMovementRepository {

    private static final String DETAIL_SELECT = """
            SELECT
                mp.id,
                mp.id_residente,
                r.nombre AS residente_nombre,
                r.telefono AS residente_telefono,
                r.num_casa,
                r.activo AS residente_activo,
                mp.monto,
                mp.observaciones,
                mp.forma_pago,
                mp.fecha_pago,
                mp.mes,
                mp.anio,
                mp.folio,
                mp.estatus,
                mp.whatsapp_enviado,
                mp.fecha_envio_whatsapp,
                mp.whatsapp_message_id,
                mp.fecha_creacion,
                mp.fecha_actualizacion
            FROM movimientos_pago mp
            JOIN residentes r ON r.id = mp.id_residente
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AppProperties properties;

    public PaymentMovementRepository(JdbcTemplate jdbcTemplate, AppProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    public PaymentMovement create(Long residentId, CreatePaymentMovementRequest request) {
        String sql = """
                INSERT INTO movimientos_pago (
                    id_residente,
                    monto,
                    observaciones,
                    forma_pago,
                    fecha_pago,
                    mes,
                    anio,
                    folio
                ) VALUES (
                    ?,
                    ?,
                    NULLIF(BTRIM(CAST(? AS text)), ''),
                    ?,
                    ?,
                    ?,
                    ?,
                    'GAN-' || ? || '-' || LPAD(nextval('movimientos_pago_folio_seq')::text, 6, '0')
                )
                RETURNING id
                """;

        Long id = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                residentId,
                request.monto(),
                normalizeObservations(request.observaciones()),
                request.formaPago(),
                request.fechaPago(),
                request.mes(),
                request.anio(),
                request.anio()
        );

        if (id == null) {
            throw new IllegalStateException("La base de datos no devolvió el movimiento creado");
        }
        return findById(id).orElseThrow(() ->
                new IllegalStateException("No fue posible recuperar el movimiento creado"));
    }

    public Optional<PaymentMovement> findById(long id) {
        String sql = DETAIL_SELECT + " WHERE mp.id = ?";
        return jdbcTemplate.query(sql, rowMapper(), id).stream().findFirst();
    }

    public Optional<PaymentMovement> findLatestRegisteredByHouseAndPeriod(
            int houseNumber,
            int month,
            int year
    ) {
        String sql = DETAIL_SELECT + """
                WHERE r.num_casa = ?
                  AND r.activo = TRUE
                  AND mp.mes = ?
                  AND mp.anio = ?
                  AND mp.estatus = 'REGISTRADO'
                ORDER BY mp.fecha_pago DESC, mp.id DESC
                LIMIT 1
                """;
        return jdbcTemplate.query(sql, rowMapper(), houseNumber, month, year)
                .stream()
                .findFirst();
    }

    public List<PaymentMovement> findPaidResidentsByPeriod(int month, int year) {
        String sql = DETAIL_SELECT + """
                WHERE mp.id IN (
                    SELECT DISTINCT ON (candidate.id_residente) candidate.id
                    FROM movimientos_pago candidate
                    WHERE candidate.mes = ?
                      AND candidate.anio = ?
                      AND candidate.estatus = 'REGISTRADO'
                    ORDER BY candidate.id_residente, candidate.fecha_pago DESC, candidate.id DESC
                )
                ORDER BY r.num_casa, mp.id
                """;
        return jdbcTemplate.query(sql, rowMapper(), month, year);
    }

    public boolean reserveWhatsAppSend(long id, UUID reservationId, OffsetDateTime reservedAt) {
        String sql = """
                UPDATE movimientos_pago
                SET whatsapp_envio_reserva = ?,
                    fecha_reserva_whatsapp = ?,
                    fecha_actualizacion = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND estatus = 'REGISTRADO'
                  AND whatsapp_enviado = FALSE
                  AND (
                      whatsapp_envio_reserva IS NULL
                      OR fecha_reserva_whatsapp < ?
                  )
                """;
        return jdbcTemplate.update(
                sql,
                reservationId,
                reservedAt,
                id,
                reservedAt.minus(properties.duplicateWindow())
        ) == 1;
    }

    public void releaseWhatsAppSend(long id, UUID reservationId) {
        String sql = """
                UPDATE movimientos_pago
                SET whatsapp_envio_reserva = NULL,
                    fecha_reserva_whatsapp = NULL,
                    fecha_actualizacion = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND whatsapp_envio_reserva = ?
                  AND whatsapp_enviado = FALSE
                """;
        jdbcTemplate.update(sql, id, reservationId);
    }

    public boolean markWhatsAppSent(
            long id,
            UUID reservationId,
            String messageId,
            OffsetDateTime sentAt
    ) {
        String sql = """
                UPDATE movimientos_pago
                SET whatsapp_enviado = TRUE,
                    fecha_envio_whatsapp = ?,
                    whatsapp_message_id = ?,
                    whatsapp_envio_reserva = NULL,
                    fecha_reserva_whatsapp = NULL,
                    fecha_actualizacion = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND estatus = 'REGISTRADO'
                  AND whatsapp_enviado = FALSE
                  AND whatsapp_envio_reserva = ?
                """;
        return jdbcTemplate.update(sql, sentAt, messageId, id, reservationId) == 1;
    }

    private String normalizeObservations(String value) {
        return value == null ? null : value.replace("\u0000", "").trim();
    }

    private RowMapper<PaymentMovement> rowMapper() {
        return (rs, rowNum) -> new PaymentMovement(
                rs.getLong("id"),
                rs.getLong("id_residente"),
                rs.getString("residente_nombre"),
                rs.getString("residente_telefono"),
                rs.getInt("num_casa"),
                rs.getBoolean("residente_activo"),
                rs.getBigDecimal("monto"),
                rs.getString("observaciones"),
                rs.getInt("forma_pago"),
                rs.getObject("fecha_pago", LocalDate.class),
                rs.getInt("mes"),
                rs.getInt("anio"),
                rs.getString("folio"),
                rs.getString("estatus"),
                rs.getBoolean("whatsapp_enviado"),
                rs.getObject("fecha_envio_whatsapp", OffsetDateTime.class),
                rs.getString("whatsapp_message_id"),
                rs.getObject("fecha_creacion", OffsetDateTime.class),
                rs.getObject("fecha_actualizacion", OffsetDateTime.class)
        );
    }
}
