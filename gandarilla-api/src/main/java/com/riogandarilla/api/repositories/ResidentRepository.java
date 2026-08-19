package com.riogandarilla.api.repositories;

import com.riogandarilla.api.entities.Resident;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public class ResidentRepository {

    private final JdbcTemplate jdbcTemplate;

    public ResidentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Resident> findActiveByHouseNumber(int houseNumber) {
        String sql = """
                SELECT id, nombre, telefono, num_casa, activo,
                       fecha_creacion, fecha_actualizacion
                FROM residentes
                WHERE num_casa = ? AND activo = TRUE
                LIMIT 1
                """;
        return jdbcTemplate.query(sql, rowMapper(), houseNumber).stream().findFirst();
    }

    public List<Resident> findAllActive() {
        String sql = """
                SELECT id, nombre, telefono, num_casa, activo,
                       fecha_creacion, fecha_actualizacion
                FROM residentes
                WHERE activo = TRUE
                ORDER BY num_casa
                """;
        return jdbcTemplate.query(sql, rowMapper());
    }

    private RowMapper<Resident> rowMapper() {
        return (rs, rowNum) -> new Resident(
                rs.getLong("id"),
                rs.getString("nombre"),
                rs.getString("telefono"),
                rs.getInt("num_casa"),
                rs.getBoolean("activo"),
                rs.getObject("fecha_creacion", OffsetDateTime.class),
                rs.getObject("fecha_actualizacion", OffsetDateTime.class)
        );
    }
}
