package com.umg.dclmanager.controller;

import com.umg.dclmanager.dto.Requests.AssignRoleReq;
import com.umg.dclmanager.dto.Requests.GrantRoleToDbUserReq;
import com.umg.dclmanager.dto.Requests.RoleReq;
import com.umg.dclmanager.service.DbService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/roles")
public class RoleController {

    private final DbService db;

    public RoleController(DbService db) {
        this.db = db;
    }

    @GetMapping
    public List<Map<String, Object>> all() {
        return db.query("SELECT * FROM APP_ROLES ORDER BY role_id");
    }

    @GetMapping("/hierarchy")
    public List<Map<String, Object>> hierarchy() {
        return db.query("""
                SELECT *
                FROM VW_ROLE_HIERARCHY
                ORDER BY parent_role_id NULLS FIRST, role_id
                """);
    }

    /** Lista los roles Oracle reales (catálogo + DBA_ROLES). */
    @GetMapping("/db")
    public List<Map<String, Object>> dbRoles() {
        return db.query("""
                SELECT role, oracle_maintained, common
                FROM DBA_ROLES
                ORDER BY role
                """);
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody RoleReq r, HttpServletRequest req) {

        String role = db.q(r.roleName());
        Long actor = db.valActor(r.createdBy());

        Long id = db.insert(
                "ROLE_ID",
                """
                INSERT INTO APP_ROLES(
                    role_name, description, parent_role_id,
                    is_oracle_role, created_by
                ) VALUES(?,?,?,?,?)
                """,
                role, r.description(), r.parentRoleId(), "S", actor
        );

        String sql = "CREATE ROLE " + role;

        try {
            db.executeDCL(
                    sql, "CREATE_ROLE",
                    actor, "CREATE_ROLE", "ROLE",
                    id, role,
                    "Crear rol Oracle " + role,
                    req.getRemoteAddr()
            );
        } catch (RuntimeException ex) {
            // Rollback del registro en APP_ROLES si Oracle rechazó (p.ej. ya existe).
            db.update("DELETE FROM APP_ROLES WHERE role_id = ?", id);
            throw ex;
        }

        return Map.of(
                "message", "Rol creado en Oracle",
                "roleId", id,
                "sql", sql
        );
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody RoleReq r) {

        db.update("""
                UPDATE APP_ROLES
                SET description = ?, parent_role_id = ?
                WHERE role_id = ?
                """,
                r.description(), r.parentRoleId(), id
        );

        return Map.of("message", "Rol actualizado (sólo metadatos en catálogo)");
    }

    @DeleteMapping("/{id}/{actor}")
    public Map<String, Object> delete(
            @PathVariable Long id,
            @PathVariable Long actor,
            HttpServletRequest req
    ) {

        Map<String, Object> role = db.one(
                "SELECT role_name FROM APP_ROLES WHERE role_id = ?", id);

        if (role == null) throw new RuntimeException("Rol no encontrado");

        String roleName = db.q(String.valueOf(role.get("ROLE_NAME")));
        String sql = "DROP ROLE " + roleName;

        db.executeDCL(
                sql, "DROP_ROLE",
                actor, "DROP_ROLE", "ROLE",
                id, roleName,
                "Eliminar rol Oracle " + roleName,
                req.getRemoteAddr()
        );

        db.update("DELETE FROM APP_ROLES WHERE role_id = ?", id);

        return Map.of("message", "Rol eliminado en Oracle", "sql", sql);
    }

    /** Asigna un rol del catálogo a un APP_USER (no ejecuta DCL Oracle). */
    @PostMapping("/assign")
    public Map<String, Object> assign(@RequestBody AssignRoleReq r, HttpServletRequest req) {

        db.update("""
                INSERT INTO USER_ROLES(
                    user_id, role_id, assigned_by, expires_at, is_active
                ) VALUES(?,?,?,NULL,'S')
                """,
                r.userId(), r.roleId(), r.assignedBy()
        );

        return Map.of("message", "Rol asignado al usuario de la app");
    }

    @DeleteMapping("/assign/{userId}/{roleId}/{actor}")
    public Map<String, Object> revoke(
            @PathVariable Long userId,
            @PathVariable Long roleId,
            @PathVariable Long actor
    ) {

        db.update("DELETE FROM USER_ROLES WHERE user_id = ? AND role_id = ?",
                userId, roleId);

        return Map.of("message", "Rol revocado del usuario de la app");
    }

    /** Asigna un rol del catálogo a un usuario Oracle REAL: GRANT rol TO oracleUser. */
    @PostMapping("/grant-to-db-user")
    public Map<String, Object> grantToDbUser(
            @RequestBody GrantRoleToDbUserReq r,
            HttpServletRequest req
    ) {

        Map<String, Object> role = db.one(
                "SELECT role_name FROM APP_ROLES WHERE role_id = ?", r.roleId());

        if (role == null) throw new RuntimeException("Rol no encontrado");

        String roleName = db.q(String.valueOf(role.get("ROLE_NAME")));
        String oracleUser = db.q(r.oracleUsername());
        boolean withAdmin = "S".equalsIgnoreCase(r.adminOption());

        String sql = "GRANT " + roleName + " TO " + oracleUser +
                (withAdmin ? " WITH ADMIN OPTION" : "");

        db.executeDCL(
                sql, "GRANT",
                db.valActor(r.actorId()),
                "GRANT", "ROLE",
                r.roleId(),
                "ROLE=" + roleName + " USER=" + oracleUser,
                "GRANT rol " + roleName + " a " + oracleUser,
                req.getRemoteAddr()
        );

        return Map.of("message", "Rol concedido al usuario Oracle", "sql", sql);
    }

    @DeleteMapping("/grant-to-db-user/{roleId}/{oracleUser}/{actor}")
    public Map<String, Object> revokeFromDbUser(
            @PathVariable Long roleId,
            @PathVariable String oracleUser,
            @PathVariable Long actor,
            HttpServletRequest req
    ) {

        Map<String, Object> role = db.one(
                "SELECT role_name FROM APP_ROLES WHERE role_id = ?", roleId);

        if (role == null) throw new RuntimeException("Rol no encontrado");

        String roleName = db.q(String.valueOf(role.get("ROLE_NAME")));
        String safeUser = db.q(oracleUser);

        String sql = "REVOKE " + roleName + " FROM " + safeUser;

        db.executeDCL(
                sql, "REVOKE",
                actor, "REVOKE", "ROLE",
                roleId,
                "ROLE=" + roleName + " USER=" + safeUser,
                "REVOKE rol " + roleName + " de " + safeUser,
                req.getRemoteAddr()
        );

        return Map.of("message", "Rol revocado del usuario Oracle", "sql", sql);
    }

    /** Quién tiene este rol concedido en Oracle. */
    @GetMapping("/{id}/grantees")
    public List<Map<String, Object>> grantees(@PathVariable Long id) {

        Map<String, Object> role = db.one(
                "SELECT role_name FROM APP_ROLES WHERE role_id = ?", id);

        if (role == null) return List.of();

        String roleName = db.q(String.valueOf(role.get("ROLE_NAME")));

        return db.query("""
                SELECT grantee, granted_role, admin_option, default_role
                FROM DBA_ROLE_PRIVS
                WHERE granted_role = ?
                ORDER BY grantee
                """, roleName);
    }
}
