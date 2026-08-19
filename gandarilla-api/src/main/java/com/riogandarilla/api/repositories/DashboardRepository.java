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
        String sql = """
                SELECT
                    COALESCE(SUM(monto) FILTER (WHERE estatus = 'REGISTRADO'), 0) AS total_recaudado,
                    COUNT(DISTINCT r.num_casa) FILTER (WHERE mp.estatus = 'REGISTRADO') AS casas_pagadas,
                    COUNT(*) FILTER (WHERE mp.estatus = 'REGISTRADO') AS registrados,
                    COUNT(*) FILTER (WHERE mp.estatus = 'CANCELADO') AS cancelados,
                    COUNT(*) FILTER (WHERE mp.estatus = 'REGISTRADO' AND mp.whatsapp_enviado) AS enviados,
                    COUNT(*) FILTER (WHERE mp.estatus = 'REGISTRADO' AND NOT mp.whatsapp_enviado) AS pendientes
                FROM movimientos_pago mp
                JOIN residentes r ON r.id = mp.id_residente
                WHERE mp.mes = ? AND mp.anio = ?
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            int paid = rs.getInt("casas_pagadas");
            return new DashboardSummary(
                    month, year, rs.getBigDecimal("total_recaudado"), paid,
                    Math.max(0, 50 - paid), rs.getInt("registrados"),
                    rs.getInt("cancelados"), rs.getInt("enviados"), rs.getInt("pendientes")
            );
        }, month, year);
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
