package com.umg.dclmanager.controller;

import com.umg.dclmanager.service.DbService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api")
public class ScriptAuditController {

    private final DbService db;
    private final JdbcTemplate jdbc;

    public ScriptAuditController(DbService db, JdbcTemplate jdbc) {
        this.db = db;
        this.jdbc = jdbc;
    }

    // ==================== AUDITORÍA ====================

    @GetMapping("/audit")
    public List<Map<String, Object>> getAllAudit() {
        return db.query("""
            SELECT * FROM VW_AUDIT_LOG
            ORDER BY PERFORMED_AT DESC
            FETCH FIRST 500 ROWS ONLY
            """);
    }

    @GetMapping("/audit/filter")
    public List<Map<String, Object>> filterAudit(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM VW_AUDIT_LOG WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (action != null && !action.isBlank()) {
            sql.append(" AND ACTION_TYPE = ? ");
            params.add(action);
        }
        if (user != null && !user.isBlank()) {
            sql.append(" AND UPPER(ACTOR_USERNAME) LIKE UPPER(?) ");
            params.add("%" + user + "%");
        }
        if (startDate != null && !startDate.isBlank()) {
            sql.append(" AND PERFORMED_AT >= TO_DATE(?, 'YYYY-MM-DD') ");
            params.add(startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            sql.append(" AND PERFORMED_AT < TO_DATE(?, 'YYYY-MM-DD') + 1 ");
            params.add(endDate);
        }

        sql.append(" ORDER BY PERFORMED_AT DESC FETCH FIRST 500 ROWS ONLY ");

        return db.query(sql.toString(), params.toArray());
    }

    // ==================== SCRIPTS SQL ====================

    @GetMapping("/scripts")
    public List<Map<String, Object>> getAllScripts() {
        return db.query("""
            SELECT s.script_id, s.script_type, s.script_content,
                   s.description, s.generated_at, s.applied,
                   s.applied_at, u.username AS generated_by_username
            FROM SQL_SCRIPTS s
            JOIN APP_USERS u ON u.user_id = s.generated_by
            ORDER BY s.generated_at DESC
            FETCH FIRST 500 ROWS ONLY
            """);
    }

    @GetMapping("/scripts/pending")
    public List<Map<String, Object>> getPendingScripts() {
        return db.query("""
            SELECT s.script_id, s.script_type, s.script_content,
                   s.description, s.generated_at,
                   u.username AS generated_by_username
            FROM SQL_SCRIPTS s
            JOIN APP_USERS u ON u.user_id = s.generated_by
            WHERE s.applied = 'N'
            ORDER BY s.generated_at
            """);
    }

    @PostMapping("/scripts/{scriptId}/execute")
    public Map<String, Object> executeScript(
            @PathVariable Long scriptId,
            @RequestParam Long actorId) {

        Map<String, Object> script = db.one(
            "SELECT script_content, description FROM SQL_SCRIPTS WHERE script_id = ?",
            scriptId
        );

        if (script == null) {
            throw new RuntimeException("Script no encontrado");
        }

        String sql = (String) script.get("SCRIPT_CONTENT");

        try {
            jdbc.execute(sql);

            db.update("""
                UPDATE SQL_SCRIPTS
                SET applied = 'S', applied_at = SYSTIMESTAMP, applied_by = ?
                WHERE script_id = ?
                """, actorId, scriptId);

            return Map.of(
                "message", "Script ejecutado correctamente",
                "sql", sql
            );
        } catch (Exception e) {
            return Map.of(
                "error", "Error al ejecutar script: " + e.getMessage(),
                "sql", sql
            );
        }
    }
}
