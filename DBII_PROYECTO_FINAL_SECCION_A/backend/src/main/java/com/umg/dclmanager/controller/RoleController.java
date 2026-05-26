package com.umg.dclmanager.controller;

import com.umg.dclmanager.dto.Requests.AssignRoleReq;
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

    @PostMapping
    public Map<String, Object> create(@RequestBody RoleReq r, HttpServletRequest req) {

        String role = db.q(r.roleName());

        String sql = "CREATE ROLE " + role;

        Long id = db.insert(
                "ROLE_ID",
                """
                INSERT INTO APP_ROLES(
                    role_name,
                    description,
                    parent_role_id,
                    is_oracle_role,
                    created_by
                )
                VALUES(?,?,?,?,?)
                """,
                role,
                r.description(),
                r.parentRoleId(),
                db.val(r.isOracleRole(), "N"),
                r.createdBy()
        );

        db.update("""
                INSERT INTO SQL_SCRIPTS(
                    generated_by,
                    script_type,
                    script_content,
                    description
                )
                VALUES(?,?,?,?)
                """,
                r.createdBy(),
                "CREATE_ROLE",
                sql,
                "Crear rol " + role
        );

        db.audit(
                r.createdBy(),
                "CREATE_ROLE",
                "ROLE",
                id,
                role,
                sql,
                req.getRemoteAddr(),
                "OK",
                null
        );

        return Map.of(
                "message", "Rol creado y script generado",
                "roleId", id,
                "sql", sql
        );
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody RoleReq r) {

        db.update("""
                UPDATE APP_ROLES
                SET role_name = ?,
                    description = ?,
                    parent_role_id = ?,
                    is_oracle_role = ?
                WHERE role_id = ?
                """,
                db.q(r.roleName()),
                r.description(),
                r.parentRoleId(),
                db.val(r.isOracleRole(), "N"),
                id
        );

        return Map.of("message", "Rol actualizado");
    }

    @DeleteMapping("/{id}/{actor}")
    public Map<String, Object> delete(
            @PathVariable Long id,
            @PathVariable Long actor,
            HttpServletRequest req
    ) {

        Map<String, Object> role = db.one(
                "SELECT role_name FROM APP_ROLES WHERE role_id = ?",
                id
        );

        if (role == null) {
            throw new RuntimeException("Rol no encontrado");
        }

        String roleName = db.q(String.valueOf(role.get("ROLE_NAME")));

        String sql = "DROP ROLE " + roleName;

        db.update("""
                INSERT INTO SQL_SCRIPTS(
                    generated_by,
                    script_type,
                    script_content,
                    description
                )
                VALUES(?,?,?,?)
                """,
                actor,
                "DROP_ROLE",
                sql,
                "Eliminar rol " + roleName
        );

        db.update("DELETE FROM APP_ROLES WHERE role_id = ?", id);

        db.audit(
                actor,
                "DROP_ROLE",
                "ROLE",
                id,
                roleName,
                sql,
                req.getRemoteAddr(),
                "OK",
                null
        );

        return Map.of(
                "message", "Rol eliminado y script generado",
                "sql", sql
        );
    }

    @PostMapping("/assign")
    public Map<String, Object> assign(@RequestBody AssignRoleReq r, HttpServletRequest req) {

        db.update("""
                INSERT INTO USER_ROLES(
                    user_id,
                    role_id,
                    assigned_by,
                    expires_at,
                    is_active
                )
                VALUES(?,?,?,NULL,'S')
                """,
                r.userId(),
                r.roleId(),
                r.assignedBy()
        );

        Map<String, Object> role = db.one(
                "SELECT role_name FROM APP_ROLES WHERE role_id = ?",
                r.roleId()
        );

        Map<String, Object> user = db.one(
                "SELECT oracle_username, username FROM APP_USERS WHERE user_id = ?",
                r.userId()
        );

        String destination = user.get("ORACLE_USERNAME") != null
                ? db.q(String.valueOf(user.get("ORACLE_USERNAME")))
                : db.q(String.valueOf(user.get("USERNAME")));

        String sql = "GRANT " + db.q(String.valueOf(role.get("ROLE_NAME"))) +
                " TO " + destination;

        db.update("""
                INSERT INTO SQL_SCRIPTS(
                    generated_by,
                    script_type,
                    script_content,
                    description
                )
                VALUES(?,?,?,?)
                """,
                r.assignedBy(),
                "GRANT",
                sql,
                "Asignar rol a usuario"
        );

        db.audit(
                r.assignedBy(),
                "ASSIGN_ROLE",
                "ROLE",
                r.roleId(),
                "USER_ID=" + r.userId() + " ROLE_ID=" + r.roleId(),
                sql,
                req.getRemoteAddr(),
                "OK",
                null
        );

        return Map.of(
                "message", "Rol asignado",
                "sql", sql
        );
    }

    @DeleteMapping("/assign/{userId}/{roleId}/{actor}")
    public Map<String, Object> revoke(
            @PathVariable Long userId,
            @PathVariable Long roleId,
            @PathVariable Long actor,
            HttpServletRequest req
    ) {

        Map<String, Object> role = db.one(
                "SELECT role_name FROM APP_ROLES WHERE role_id = ?",
                roleId
        );

        Map<String, Object> user = db.one(
                "SELECT oracle_username, username FROM APP_USERS WHERE user_id = ?",
                userId
        );

        String destination = user.get("ORACLE_USERNAME") != null
                ? db.q(String.valueOf(user.get("ORACLE_USERNAME")))
                : db.q(String.valueOf(user.get("USERNAME")));

        String sql = "REVOKE " + db.q(String.valueOf(role.get("ROLE_NAME"))) +
                " FROM " + destination;

        db.update(
                "DELETE FROM USER_ROLES WHERE user_id = ? AND role_id = ?",
                userId,
                roleId
        );

        db.update("""
                INSERT INTO SQL_SCRIPTS(
                    generated_by,
                    script_type,
                    script_content,
                    description
                )
                VALUES(?,?,?,?)
                """,
                actor,
                "REVOKE",
                sql,
                "Revocar rol a usuario"
        );

        db.audit(
                actor,
                "REVOKE_ROLE",
                "ROLE",
                roleId,
                "USER_ID=" + userId + " ROLE_ID=" + roleId,
                sql,
                req.getRemoteAddr(),
                "OK",
                null
        );

        return Map.of(
                "message", "Rol revocado",
                "sql", sql
        );
    }
}