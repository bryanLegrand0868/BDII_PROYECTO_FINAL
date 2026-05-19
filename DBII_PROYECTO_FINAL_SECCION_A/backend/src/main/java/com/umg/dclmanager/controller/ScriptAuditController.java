package com.umg.dclmanager.controller;

import com.umg.dclmanager.dto.Requests.ScriptReq;
import com.umg.dclmanager.service.DbService;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ScriptAuditController {

    private final DbService db;
    private final JdbcTemplate jdbc;

    public ScriptAuditController(DbService db, JdbcTemplate jdbc) {
        this.db = db;
        this.jdbc = jdbc;
    }

    @GetMapping("/audit")
    public List<Map<String, Object>> audit() {
        return db.query("SELECT * FROM VW_AUDIT_LOG");
    }

    @GetMapping("/scripts")
    public List<Map<String, Object>> scripts() {

        return db.query("""
                SELECT script_id,
                       generated_by,
                       script_type,
                       description,
                       generated_at,
                       applied,
                       applied_at,
                       applied_by,
                       script_content
                FROM SQL_SCRIPTS
                ORDER BY generated_at DESC
                """);
    }

    @GetMapping("/scripts/pending")
    public List<Map<String, Object>> pending() {
        return db.query("SELECT * FROM VW_PENDING_SCRIPTS");
    }

    @PostMapping("/scripts")
    public Map<String, Object> create(@RequestBody ScriptReq r) {

        Long id = db.insert(
                "SCRIPT_ID",
                """
                INSERT INTO SQL_SCRIPTS(
                    generated_by,
                    script_type,
                    script_content,
                    description
                )
                VALUES(?,?,?,?)
                """,
                r.generatedBy(),
                r.scriptType(),
                r.scriptContent(),
                r.description()
        );

        return Map.of(
                "message", "Script guardado",
                "scriptId", id
        );
    }

    @PostMapping("/scripts/{id}/apply/{actor}")
    public Map<String, Object> apply(
            @PathVariable Long id,
            @PathVariable Long actor
    ) {

        Map<String, Object> script = db.one(
                "SELECT script_content FROM SQL_SCRIPTS WHERE script_id = ?",
                id
        );

        if (script == null) {
            throw new RuntimeException("Script no encontrado");
        }

        String content = String.valueOf(script.get("SCRIPT_CONTENT"));

        for (String part : content.split(";")) {

            String sql = part.trim();

            if (!sql.isBlank()) {
                jdbc.execute(sql);
            }
        }

        db.update("""
                UPDATE SQL_SCRIPTS
                SET applied = 'S',
                    applied_at = SYSTIMESTAMP,
                    applied_by = ?
                WHERE script_id = ?
                """,
                actor,
                id
        );

        return Map.of("message", "Script aplicado en Oracle");
    }

    @GetMapping("/effective-permissions")
    public List<Map<String, Object>> effective() {

        return db.query("""
                SELECT *
                FROM VW_USER_EFFECTIVE_PERMISSIONS
                ORDER BY user_id, origin_type
                """);
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {

        return Map.of(
                "usuarios", db.one("SELECT COUNT(*) TOTAL FROM APP_USERS"),
                "roles", db.one("SELECT COUNT(*) TOTAL FROM APP_ROLES"),
                "permisos", db.one("SELECT COUNT(*) TOTAL FROM APP_PERMISSIONS"),
                "scriptsPendientes", db.one("SELECT COUNT(*) TOTAL FROM SQL_SCRIPTS WHERE applied = 'N'")
        );
    }
}