package com.umg.dclmanager.controller;

import com.umg.dclmanager.service.DbService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

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
            """);
    }

    @GetMapping("/audit/filter")
    public List<Map<String, Object>> filterAudit(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT * FROM VW_AUDIT_LOG WHERE 1=1
            """);
        
        if (action != null && !action.isEmpty()) {
            sql.append(" AND ACTION_TYPE = '").append(action).append("'");
        }
        if (user != null && !user.isEmpty()) {
            sql.append(" AND ACTOR_USERNAME LIKE '%").append(user).append("%'");
        }
        if (startDate != null && !startDate.isEmpty()) {
            sql.append(" AND PERFORMED_AT >= TO_DATE('").append(startDate).append("', 'YYYY-MM-DD')");
        }
        if (endDate != null && !endDate.isEmpty()) {
            sql.append(" AND PERFORMED_AT <= TO_DATE('").append(endDate).append("', 'YYYY-MM-DD') + 1");
        }
        
        sql.append(" ORDER BY PERFORMED_AT DESC");
        
        return db.query(sql.toString());
    }

    // ==================== SCRIPTS SQL ====================
    
    @GetMapping("/scripts")
    public List<Map<String, Object>> getAllScripts() {
        return db.query("""
            SELECT * FROM SQL_SCRIPTS
            ORDER BY GENERATED_AT DESC
            """);
    }

    @GetMapping("/scripts/pending")
    public List<Map<String, Object>> getPendingScripts() {
        return db.query("""
            SELECT * FROM SQL_SCRIPTS
            WHERE APPLIED = 'N'
            ORDER BY GENERATED_AT
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