package com.umg.dclmanager.controller;

import com.umg.dclmanager.dto.Requests.DbUserActionReq;
import com.umg.dclmanager.dto.Requests.DbUserCreateReq;
import com.umg.dclmanager.dto.Requests.DbUserPasswordReq;
import com.umg.dclmanager.service.DbService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Gestión REAL de usuarios Oracle (CREATE USER / ALTER USER / DROP USER).
 * Fase 2 de la rúbrica: todo se ejecuta en vivo contra el DBMS.
 */
@RestController
@CrossOrigin
@RequestMapping("/api/db-users")
public class DbUserController {

    private final DbService db;

    public DbUserController(DbService db) {
        this.db = db;
    }

    /**
     * Listar usuarios reales de Oracle (Fase 1 paso 4).
     * Filtra los usuarios "mantenidos por Oracle" (SYS, SYSTEM, APEX_*,
     * GSMADMIN_INTERNAL, etc.) para que el admin no pueda dropearlos
     * por accidente desde la UI.
     */
    @GetMapping
    public List<Map<String, Object>> list() {
        return db.query("""
                SELECT username, account_status, created,
                       lock_date, expiry_date,
                       default_tablespace, temporary_tablespace,
                       oracle_maintained
                FROM DBA_USERS
                WHERE oracle_maintained = 'N'
                   OR username IN ('HR','PROYECTOFINAL','SUPERADMIN')
                ORDER BY username
                """);
    }

    /** Listado completo (incluye usuarios del sistema), solo lectura. */
    @GetMapping("/all")
    public List<Map<String, Object>> listAll() {
        return db.query("""
                SELECT username, account_status, created,
                       lock_date, expiry_date,
                       default_tablespace, temporary_tablespace,
                       oracle_maintained
                FROM DBA_USERS
                ORDER BY oracle_maintained, username
                """);
    }

    @GetMapping("/{username}")
    public Map<String, Object> details(@PathVariable String username) {
        String u = db.q(username);
        return db.one("""
                SELECT username, account_status, created,
                       lock_date, expiry_date, profile,
                       default_tablespace, temporary_tablespace
                FROM DBA_USERS
                WHERE username = ?
                """, u);
    }

    /** Roles concedidos al usuario en Oracle. */
    @GetMapping("/{username}/roles")
    public List<Map<String, Object>> userRoles(@PathVariable String username) {
        String u = db.q(username);
        return db.query("""
                SELECT granted_role, admin_option, default_role
                FROM DBA_ROLE_PRIVS
                WHERE grantee = ?
                ORDER BY granted_role
                """, u);
    }

    /** Privilegios sobre objetos otorgados directamente. */
    @GetMapping("/{username}/object-privs")
    public List<Map<String, Object>> objectPrivs(@PathVariable String username) {
        String u = db.q(username);
        return db.query("""
                SELECT owner, table_name, privilege, grantable
                FROM DBA_TAB_PRIVS
                WHERE grantee = ?
                ORDER BY owner, table_name, privilege
                """, u);
    }

    /** Privilegios del sistema. */
    @GetMapping("/{username}/system-privs")
    public List<Map<String, Object>> systemPrivs(@PathVariable String username) {
        String u = db.q(username);
        return db.query("""
                SELECT privilege, admin_option
                FROM DBA_SYS_PRIVS
                WHERE grantee = ?
                ORDER BY privilege
                """, u);
    }

    /** CREATE USER + GRANT CREATE SESSION. Fase 2 paso 1. */
    @PostMapping
    public Map<String, Object> create(@RequestBody DbUserCreateReq r, HttpServletRequest req) {

        String username = db.q(r.username());
        String password = db.qPassword(r.password());

        StringBuilder sb = new StringBuilder();
        sb.append("CREATE USER ").append(username)
          .append(" IDENTIFIED BY \"").append(password).append("\"");

        if (r.defaultTablespace() != null && !r.defaultTablespace().isBlank()) {
            sb.append(" DEFAULT TABLESPACE ").append(db.q(r.defaultTablespace()));
        }
        if (r.temporaryTablespace() != null && !r.temporaryTablespace().isBlank()) {
            sb.append(" TEMPORARY TABLESPACE ").append(db.q(r.temporaryTablespace()));
        }

        String createSql = sb.toString();
        String grantSql = "GRANT CREATE SESSION TO " + username;

        Long actor = db.valActor(r.actorId());
        String ip = req.getRemoteAddr();

        db.executeDCL(createSql, "CREATE_USER",
                actor, "CREATE_USER", "USER",
                null, username,
                "Crear usuario Oracle " + username, ip);

        db.executeDCL(grantSql, "GRANT",
                actor, "GRANT", "USER",
                null, username,
                "Permitir login a " + username, ip);

        return Map.of(
                "message", "Usuario Oracle creado",
                "username", username,
                "sql", createSql + ";\n" + grantSql
        );
    }

    /** ALTER USER ... IDENTIFIED BY new_pwd — Fase 2 paso 2. */
    @PutMapping("/{username}/password")
    public Map<String, Object> changePassword(
            @PathVariable String username,
            @RequestBody DbUserPasswordReq r,
            HttpServletRequest req
    ) {
        String u = db.q(username);
        String pwd = db.qPassword(r.newPassword());

        String sql = "ALTER USER " + u + " IDENTIFIED BY \"" + pwd + "\"";

        db.executeDCL(sql, "ALTER_USER",
                db.valActor(r.actorId()),
                "CHANGE_PASSWORD", "USER",
                null, u,
                "Cambiar password de " + u,
                req.getRemoteAddr());

        return Map.of("message", "Password cambiado", "sql", sql);
    }

    /** ALTER USER ... ACCOUNT LOCK — Fase 2 paso 3. */
    @PutMapping("/{username}/lock")
    public Map<String, Object> lock(
            @PathVariable String username,
            @RequestBody DbUserActionReq r,
            HttpServletRequest req
    ) {
        String u = db.q(username);
        String sql = "ALTER USER " + u + " ACCOUNT LOCK";

        db.executeDCL(sql, "ALTER_USER",
                db.valActor(r.actorId()),
                "LOCK_USER", "USER",
                null, u,
                "Bloquear cuenta " + u,
                req.getRemoteAddr());

        return Map.of("message", "Usuario bloqueado", "sql", sql);
    }

    /** ALTER USER ... ACCOUNT UNLOCK — Fase 2 paso 4. */
    @PutMapping("/{username}/unlock")
    public Map<String, Object> unlock(
            @PathVariable String username,
            @RequestBody DbUserActionReq r,
            HttpServletRequest req
    ) {
        String u = db.q(username);
        String sql = "ALTER USER " + u + " ACCOUNT UNLOCK";

        db.executeDCL(sql, "ALTER_USER",
                db.valActor(r.actorId()),
                "UNLOCK_USER", "USER",
                null, u,
                "Desbloquear cuenta " + u,
                req.getRemoteAddr());

        return Map.of("message", "Usuario desbloqueado", "sql", sql);
    }

    /** DROP USER ... CASCADE — Fase 2 paso 5. */
    @DeleteMapping("/{username}/{actor}")
    public Map<String, Object> drop(
            @PathVariable String username,
            @PathVariable Long actor,
            HttpServletRequest req
    ) {
        String u = db.q(username);
        String sql = "DROP USER " + u + " CASCADE";

        db.executeDCL(sql, "DROP_USER",
                actor, "DROP_USER", "USER",
                null, u,
                "Eliminar usuario Oracle " + u,
                req.getRemoteAddr());

        // Limpia referencias en USER_PERMISSIONS
        db.update("DELETE FROM USER_PERMISSIONS WHERE oracle_username = ?", u);

        return Map.of("message", "Usuario Oracle eliminado", "sql", sql);
    }
}
