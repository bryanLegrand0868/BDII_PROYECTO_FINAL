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

    public JdbcTemplate jdbc() {
        return jdbc;
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
            Long actor, String action, String target,
            Long targetId, String targetName,
            String script, String ip,
            String result, String error
    ) {
        jdbc.update("""
                INSERT INTO AUDIT_LOG(
                    actor_user_id, action_type, target_type,
                    target_id, target_name, sql_generated,
                    ip_address, result, error_detail
                ) VALUES(?,?,?,?,?,?,?,?,?)
                """,
                actor, action, target, targetId, targetName,
                script, ip, result, error
        );
    }

    /**
     * Guarda el SQL generado en SQL_SCRIPTS para conservar evidencia
     * y poder mostrarlo como "script generado" en la pantalla.
     */
    public void saveScript(Long actor, String type, String sql, String description) {
        jdbc.update("""
                INSERT INTO SQL_SCRIPTS(
                    generated_by, script_type, script_content,
                    description, applied, applied_at, applied_by
                ) VALUES(?,?,?,?, 'S', SYSTIMESTAMP, ?)
                """,
                actor, type, sql, description, actor
        );
    }

    /**
     * Núcleo del sistema: ejecuta un DCL real contra Oracle, registra
     * el script y deja auditoría. Si Oracle rechaza el SQL, audita
     * el error y lanza excepción para que el endpoint devuelva 400.
     */
    public void executeDCL(
            String sql, String scriptType,
            Long actor, String actionType, String targetType,
            Long targetId, String targetName,
            String description, String ip
    ) {
        try {
            jdbc.execute(sql);
            saveScript(actor, scriptType, sql, description);
            audit(actor, actionType, targetType, targetId, targetName,
                    sql, ip, "OK", null);
        } catch (Exception e) {
            audit(actor, actionType, targetType, targetId, targetName,
                    sql, ip, "ERROR", e.getMessage());
            throw new RuntimeException("Oracle rechazó el DCL: " + e.getMessage(), e);
        }
    }

    /** Valida que un identificador Oracle sea seguro para concatenar. */
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

    /** Sanitiza un password Oracle: solo permite alfanuméricos y algunos especiales. */
    public String qPassword(String pwd) {
        if (pwd == null || pwd.isBlank()) {
            throw new RuntimeException("Password vacío");
        }
        if (!pwd.matches("[A-Za-z0-9_@#\\$\\.\\-]+")) {
            throw new RuntimeException("Password contiene caracteres no permitidos");
        }
        return pwd;
    }

    public Long valActor(Long actor) {
        return actor == null ? 1L : actor;
    }

    public String val(String value, String def) {
        return value == null || value.isBlank() ? def : value;
    }
}
