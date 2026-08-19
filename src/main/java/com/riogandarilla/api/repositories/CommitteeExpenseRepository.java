package com.riogandarilla.api.repositories;

import com.riogandarilla.api.dto.request.CreateCommitteeExpenseRequest;
import com.riogandarilla.api.entities.CommitteeExpense;
import com.riogandarilla.api.dto.response.MonthlyExpenseSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public class CommitteeExpenseRepository {
    private final JdbcTemplate jdbcTemplate;

    public CommitteeExpenseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CommitteeExpense create(CreateCommitteeExpenseRequest request) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO gastos_comite (concepto, proveedor, monto, fecha_gasto, mes, anio, estatus)
                VALUES (?, NULLIF(BTRIM(?), ''), ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, request.concepto().trim(), request.proveedor(), request.monto(),
                request.fechaGasto(), request.mes(), request.anio(), request.estatus().trim().toUpperCase());
        return findById(id);
    }

    public CommitteeExpense findById(Long id) {
        return jdbcTemplate.queryForObject("""
                SELECT id, concepto, proveedor, monto, fecha_gasto, mes, anio, estatus
                FROM gastos_comite WHERE id = ?
                """, (rs, rowNum) -> new CommitteeExpense(
                rs.getLong("id"), rs.getString("concepto"), rs.getString("proveedor"),
                rs.getBigDecimal("monto"), rs.getObject("fecha_gasto", LocalDate.class),
                rs.getInt("mes"), rs.getInt("anio"), rs.getString("estatus")
        ), id);
    }

    public List<CommitteeExpense> findByPeriod(int month, int year) {
        return jdbcTemplate.query("""
                SELECT id, concepto, proveedor, monto, fecha_gasto, mes, anio, estatus
                FROM gastos_comite WHERE mes = ? AND anio = ?
                ORDER BY fecha_gasto DESC, id DESC
                """, (rs, rowNum) -> new CommitteeExpense(
                rs.getLong("id"), rs.getString("concepto"), rs.getString("proveedor"),
                rs.getBigDecimal("monto"), rs.getObject("fecha_gasto", LocalDate.class),
                rs.getInt("mes"), rs.getInt("anio"), rs.getString("estatus")
        ), month, year);
    }

    public BigDecimal totalByPeriodAndStatus(int month, int year, String status) {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(monto), 0) FROM gastos_comite
                WHERE mes = ? AND anio = ? AND estatus = ?
                """, BigDecimal.class, month, year, status);
    }

    public List<MonthlyExpenseSummary> annualSummary(int year) {
        return jdbcTemplate.query("""
                SELECT month_number,
                       COALESCE(SUM(monto) FILTER (WHERE estatus = 'PAGADO'), 0) AS covered,
                       COALESCE(SUM(monto) FILTER (WHERE estatus = 'PENDIENTE'), 0) AS pending
                FROM generate_series(1, 12) AS month_number
                LEFT JOIN gastos_comite ON gastos_comite.mes = month_number AND gastos_comite.anio = ?
                GROUP BY month_number
                ORDER BY month_number
                """, (rs, rowNum) -> new MonthlyExpenseSummary(
                rs.getInt("month_number"), rs.getBigDecimal("covered"), rs.getBigDecimal("pending")
        ), year);
    }
}
