package com.umg.dclmanager.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

@Service
public class DbService {

    private final JdbcTemplate jdbc;

    public DbService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> query(String sql, Object... args) {
        return jdbc.queryForList(sql, args);
    }

    public Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> list = query(sql, args);
        return list.isEmpty() ? null : list.get(0);
    }

    public int update(String sql, Object... args) {
        return jdbc.update(sql, args);
    }

    public Long insert(String idColumn, String sql, Object... args) {

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{idColumn});

            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }

            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();

        if (key == null) {
            throw new RuntimeException("No se pudo obtener el ID generado");
        }

        return key.longValue();
    }

    public void audit(
            Long actor,
            String action,
            String target,
            Long targetId,
            String targetName,
            String script,
            String ip,
            String result,
            String error
    ) {

        jdbc.update("""
                INSERT INTO AUDIT_LOG(
                    actor_user_id,
                    action_type,
                    target_type,
                    target_id,
                    target_name,
                    sql_generated,
                    ip_address,
                    result,
                    error_detail
                )
                VALUES(?,?,?,?,?,?,?,?,?)
                """,
                actor,
                action,
                target,
                targetId,
                targetName,
                script,
                ip,
                result,
                error
        );
    }

    public String q(String name) {

        if (name == null || name.isBlank()) {
            throw new RuntimeException("Nombre SQL vacío");
        }

        String clean = name.trim().replace("\"", "").toUpperCase();

        if (!clean.matches("[A-Z0-9_#$]+")) {
            throw new RuntimeException("Nombre SQL inválido: " + name);
        }

        return clean;
    }

    public Long valActor(Long actor) {
    return actor == null ? 1L : actor;
}

    public String val(String value, String def) {
        return value == null || value.isBlank() ? def : value;
    }
}