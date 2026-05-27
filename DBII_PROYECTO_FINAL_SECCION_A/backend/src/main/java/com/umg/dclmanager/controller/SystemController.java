package com.umg.dclmanager.controller;

import com.umg.dclmanager.dto.Requests.TestAccessReq;
import com.umg.dclmanager.service.DbService;
import com.umg.dclmanager.service.OracleTestService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fase 1 (info del servidor, usuario conectado, lista DBA_USERS) y
 * Fase 5 (validación de acceso).
 *
 * Cada query del /info va envuelta en try/catch para que si una falla
 * (ej. el rol del usuario no tiene SELECT sobre V$INSTANCE en este PDB),
 * la pantalla muestre el resto de la información en vez de quedarse
 * bloqueada esperando.
 */
@RestController
@CrossOrigin
@RequestMapping("/api/system")
public class SystemController {

    private final DbService db;
    private final OracleTestService oracleTest;

    public SystemController(DbService db, OracleTestService oracleTest) {
        this.db = db;
        this.oracleTest = oracleTest;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {

        Map<String, Object> out = new HashMap<>();

        out.put("connected", true);

        try {
            out.put("version", db.query("SELECT banner FROM V$VERSION"));
        } catch (Exception e) {
            out.put("version", List.of(Map.of("BANNER", "(sin permisos para V$VERSION)")));
        }

        try {
            out.put("instance", db.one("""
                    SELECT instance_name, host_name, version, status, startup_time
                    FROM V$INSTANCE
                    """));
        } catch (Exception e) {
            out.put("instance", Map.of(
                    "INSTANCE_NAME", "(sin permisos para V$INSTANCE)",
                    "HOST_NAME", "—", "VERSION", "—",
                    "STATUS", "—", "STARTUP_TIME", null));
        }

        try {
            out.put("session", db.one("""
                    SELECT
                        SYS_CONTEXT('USERENV','SESSION_USER')      AS session_user,
                        SYS_CONTEXT('USERENV','CURRENT_SCHEMA')    AS current_schema,
                        SYS_CONTEXT('USERENV','DB_NAME')           AS db_name,
                        SYS_CONTEXT('USERENV','SERVER_HOST')       AS server_host,
                        SYS_CONTEXT('USERENV','IP_ADDRESS')        AS ip_address,
                        SYS_CONTEXT('USERENV','SESSIONID')         AS session_id
                    FROM DUAL
                    """));
        } catch (Exception e) {
            out.put("session", Map.of("SESSION_USER", "(error)"));
        }

        return out;
    }

    @GetMapping("/db-users")
    public List<Map<String, Object>> dbUsers() {
        try {
            return db.query("""
                    SELECT username, account_status, created, lock_date, expiry_date
                    FROM DBA_USERS
                    WHERE oracle_maintained = 'N'
                       OR username IN ('HR','PROYECTOFINAL','SYS','SYSTEM')
                    ORDER BY username
                    """);
        } catch (Exception e) {
            return List.of();
        }
    }

    @PostMapping("/test-access")
    public Map<String, Object> testAccess(
            @RequestBody TestAccessReq r,
            HttpServletRequest req
    ) {
        String ip = req.getRemoteAddr();
        Long actor = db.valActor(r.actorId());

        OracleTestService.Result result = oracleTest.tryAs(
                r.username(), r.password(), r.sql());

        try {
            db.audit(actor, "TEST_ACCESS", "SYSTEM",
                    null, r.username(), r.sql(), ip,
                    result.ok() ? "OK" : "ERROR", result.error());
        } catch (Exception ignored) {}

        Map<String, Object> out = new HashMap<>();
        out.put("ok", result.ok());
        out.put("username", r.username());
        out.put("sql", r.sql());
        out.put("rows", result.rows());
        out.put("error", result.error());
        out.put("errorCode", result.errorCode());
        return out;
    }
}
