package com.riogandarilla.api.repositories;

import com.riogandarilla.api.entities.CommitteeAssignment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class CommitteeRepository {

    private final JdbcTemplate jdbcTemplate;

    public CommitteeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Set<Integer> findHouseNumbersForDate(LocalDate date) {
        String sql = """
                SELECT DISTINCT num_casa
                FROM comite_casas
                WHERE fecha_inicio <= ?
                  AND (fecha_fin IS NULL OR fecha_fin >= ?)
                ORDER BY num_casa
                """;
        return jdbcTemplate.queryForList(sql, Integer.class, date, date)
                .stream()
                .collect(Collectors.toSet());
    }

    public List<CommitteeAssignment> findAll() {
        String sql = """
                SELECT id, num_casa, fecha_inicio, fecha_fin, fecha_creacion, fecha_actualizacion
                FROM comite_casas
                ORDER BY fecha_inicio DESC, num_casa
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new CommitteeAssignment(
                rs.getLong("id"),
                rs.getInt("num_casa"),
                rs.getObject("fecha_inicio", LocalDate.class),
                rs.getObject("fecha_fin", LocalDate.class),
                rs.getObject("fecha_creacion", OffsetDateTime.class),
                rs.getObject("fecha_actualizacion", OffsetDateTime.class)
        ));
    }

    public void replaceFrom(LocalDate startDate, List<Integer> houseNumbers) {
        LocalDate previousDay = startDate.minusDays(1);

        jdbcTemplate.update("""
                UPDATE comite_casas
                SET fecha_fin = ?
                WHERE fecha_inicio < ?
                  AND (fecha_fin IS NULL OR fecha_fin >= ?)
                """, previousDay, startDate, startDate);

        jdbcTemplate.update("DELETE FROM comite_casas WHERE fecha_inicio >= ?", startDate);

        String insert = """
                INSERT INTO comite_casas (num_casa, fecha_inicio)
                VALUES (?, ?)
                """;
        for (Integer houseNumber : houseNumbers) {
            jdbcTemplate.update(insert, houseNumber, startDate);
        }
    }
}
