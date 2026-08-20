package com.riogandarilla.api.repositories;

import com.riogandarilla.api.dto.response.DashboardSummary;
import com.riogandarilla.api.entities.PaymentMovement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbcTemplate;

    public DashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardSummary summary(int month, int year) {
        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
        String sql = """
                WITH active_houses AS (
                    SELECT num_casa
                    FROM residentes
                    WHERE activo = TRUE
                ),
                committee AS (
                    SELECT DISTINCT num_casa
                    FROM comite_casas
                    WHERE fecha_inicio <= ?
                      AND (fecha_fin IS NULL OR fecha_fin >= ?)
                ),
                paid_houses AS (
                    SELECT DISTINCT r.num_casa
                    FROM movimientos_pago mp
                    JOIN residentes r ON r.id = mp.id_residente
                    WHERE mp.mes = ? AND mp.anio = ? AND mp.estatus = 'REGISTRADO'
                ),
                movement_totals AS (
                    SELECT
                        COALESCE(SUM(monto) FILTER (WHERE estatus = 'REGISTRADO'), 0) AS total_recaudado,
                        COUNT(*) FILTER (WHERE estatus = 'REGISTRADO') AS registrados,
                        COUNT(*) FILTER (WHERE estatus = 'CANCELADO') AS cancelados,
                        COUNT(*) FILTER (WHERE estatus = 'REGISTRADO' AND whatsapp_enviado) AS enviados,
                        COUNT(*) FILTER (WHERE estatus = 'REGISTRADO' AND NOT whatsapp_enviado) AS pendientes
                    FROM movimientos_pago
                    WHERE mes = ? AND anio = ?
                )
                SELECT
                    mt.total_recaudado,
                    (SELECT COUNT(*) FROM paid_houses p WHERE NOT EXISTS (
                        SELECT 1 FROM committee c WHERE c.num_casa = p.num_casa
                    )) AS casas_pagadas,
                    (SELECT COUNT(*) FROM active_houses a
                        WHERE NOT EXISTS (SELECT 1 FROM committee c WHERE c.num_casa = a.num_casa)
                          AND NOT EXISTS (SELECT 1 FROM paid_houses p WHERE p.num_casa = a.num_casa)
                    ) AS casas_pendientes,
                    mt.registrados,
                    mt.cancelados,
                    mt.enviados,
                    mt.pendientes
                FROM movement_totals mt
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new DashboardSummary(
                month,
                year,
                rs.getBigDecimal("total_recaudado"),
                rs.getInt("casas_pagadas"),
                rs.getInt("casas_pendientes"),
                rs.getInt("registrados"),
                rs.getInt("cancelados"),
                rs.getInt("enviados"),
                rs.getInt("pendientes")
        ), periodEnd, periodStart, month, year, month, year);
    }

    public PaymentStatusCounts paymentStatusCounts(int month, int year, int lateFromDay) {
        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
        String sql = """
                WITH active_houses AS (
                    SELECT num_casa
                    FROM residentes
                    WHERE activo = TRUE
                ),
                committee AS (
                    SELECT DISTINCT num_casa
                    FROM comite_casas
                    WHERE fecha_inicio <= ?
                      AND (fecha_fin IS NULL OR fecha_fin >= ?)
                ),
                first_payment AS (
                    SELECT r.num_casa, MIN(mp.fecha_pago) AS fecha_pago
                    FROM movimientos_pago mp
                    JOIN residentes r ON r.id = mp.id_residente
                    WHERE mp.mes = ?
                      AND mp.anio = ?
                      AND mp.estatus = 'REGISTRADO'
                    GROUP BY r.num_casa
                )
                SELECT
                    COUNT(*) FILTER (
                        WHERE c.num_casa IS NULL AND fp.fecha_pago IS NOT NULL AND EXTRACT(DAY FROM fp.fecha_pago) < ?
                    ) AS pagadas_tiempo,
                    COUNT(*) FILTER (
                        WHERE c.num_casa IS NULL AND fp.fecha_pago IS NOT NULL AND EXTRACT(DAY FROM fp.fecha_pago) >= ?
                    ) AS pagadas_atraso,
                    COUNT(*) FILTER (
                        WHERE c.num_casa IS NULL AND fp.fecha_pago IS NULL
                    ) AS sin_pago,
                    COUNT(*) FILTER (
                        WHERE c.num_casa IS NOT NULL
                    ) AS comite_exento,
                    COUNT(*) AS total_casas
                FROM active_houses a
                LEFT JOIN committee c ON c.num_casa = a.num_casa
                LEFT JOIN first_payment fp ON fp.num_casa = a.num_casa
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new PaymentStatusCounts(
                rs.getInt("pagadas_tiempo"),
                rs.getInt("pagadas_atraso"),
                rs.getInt("sin_pago"),
                rs.getInt("comite_exento"),
                rs.getInt("total_casas")
        ), periodEnd, periodStart, month, year, lateFromDay, lateFromDay);
    }

    public List<PaymentMovement> recent(int month, int year) {
        String sql = """
                SELECT mp.id, mp.id_residente, r.nombre AS residente_nombre,
                       r.telefono AS residente_telefono, r.num_casa,
                       r.activo AS residente_activo, mp.monto, mp.observaciones,
                       mp.forma_pago, mp.fecha_pago, mp.mes, mp.anio, mp.folio,
                       mp.estatus, mp.whatsapp_enviado, mp.fecha_envio_whatsapp,
                       mp.whatsapp_message_id, mp.fecha_creacion, mp.fecha_actualizacion
                FROM movimientos_pago mp
                JOIN residentes r ON r.id = mp.id_residente
                WHERE mp.mes = ? AND mp.anio = ?
                ORDER BY mp.fecha_pago DESC, mp.id DESC
                LIMIT 10
                """;
        return jdbcTemplate.query(sql, rowMapper(), month, year);
    }

    public record PaymentStatusCounts(
            int pagadasATiempo,
            int pagadasConAtraso,
            int sinPago,
            int comiteExento,
            int totalCasas
    ) {
    }

    private RowMapper<PaymentMovement> rowMapper() {
        return (rs, rowNum) -> new PaymentMovement(
                rs.getLong("id"), rs.getLong("id_residente"),
                rs.getString("residente_nombre"), rs.getString("residente_telefono"),
                rs.getInt("num_casa"), rs.getBoolean("residente_activo"),
                rs.getBigDecimal("monto"), rs.getString("observaciones"),
                rs.getInt("forma_pago"), rs.getObject("fecha_pago", LocalDate.class),
                rs.getInt("mes"), rs.getInt("anio"), rs.getString("folio"),
                rs.getString("estatus"), rs.getBoolean("whatsapp_enviado"),
                rs.getObject("fecha_envio_whatsapp", OffsetDateTime.class),
                rs.getString("whatsapp_message_id"),
                rs.getObject("fecha_creacion", OffsetDateTime.class),
                rs.getObject("fecha_actualizacion", OffsetDateTime.class)
        );
    }
}
