package com.umg.dclmanager.controller;

import com.umg.dclmanager.dto.Requests.GrantRolePermReq;
import com.umg.dclmanager.dto.Requests.GrantUserPermReq;
import com.umg.dclmanager.dto.Requests.PermissionReq;
import com.umg.dclmanager.service.DbService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final DbService db;

    public PermissionController(DbService db) {
        this.db = db;
    }

    @GetMapping
    public List<Map<String, Object>> all() {
        return db.query("SELECT * FROM APP_PERMISSIONS ORDER BY permission_id");
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody PermissionReq r) {

        Long id = db.insert(
                "PERMISSION_ID",
                """
                INSERT INTO APP_PERMISSIONS(
                    object_name,
                    object_type,
                    privilege_type,
                    schema_name,
                    description
                )
                VALUES(?,?,?,?,?)
                """,
                db.q(r.objectName()),
                db.q(r.objectType()),
                db.q(r.privilegeType()),
                db.q(r.schemaName()),
                r.description()
        );

        return Map.of(
                "message", "Permiso creado",
                "permissionId", id
        );
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(
            @PathVariable Long id,
            @RequestBody PermissionReq r
    ) {

        db.update("""
                UPDATE APP_PERMISSIONS
                SET object_name = ?,
                    object_type = ?,
                    privilege_type = ?,
                    schema_name = ?,
                    description = ?
                WHERE permission_id = ?
                """,
                db.q(r.objectName()),
                db.q(r.objectType()),
                db.q(r.privilegeType()),
                db.q(r.schemaName()),
                r.description(),
                id
        );

        return Map.of("message", "Permiso actualizado");
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {

        db.update("DELETE FROM APP_PERMISSIONS WHERE permission_id = ?", id);

        return Map.of("message", "Permiso eliminado");
    }

    @PostMapping("/grant-role")
    public Map<String, Object> grantRole(
            @RequestBody GrantRolePermReq r,
            HttpServletRequest req
    ) {

        db.update("""
                INSERT INTO ROLE_PERMISSIONS(
                    role_id,
                    permission_id,
                    grant_option,
                    granted_by
                )
                VALUES(?,?,?,?)
                """,
                r.roleId(),
                r.permissionId(),
                val(r.grantOption()),
                r.grantedBy()
        );

        Map<String, Object> role = db.one(
                "SELECT role_name FROM APP_ROLES WHERE role_id = ?",
                r.roleId()
        );

        Map<String, Object> p = db.one(
                "SELECT * FROM APP_PERMISSIONS WHERE permission_id = ?",
                r.permissionId()
        );

        String sql = "GRANT " + p.get("PRIVILEGE_TYPE") +
                " ON " + p.get("SCHEMA_NAME") + "." + p.get("OBJECT_NAME") +
                " TO " + role.get("ROLE_NAME") +
                ("S".equals(val(r.grantOption())) ? " WITH GRANT OPTION" : "");

        db.update("""
                INSERT INTO SQL_SCRIPTS(
                    generated_by,
                    script_type,
                    script_content,
                    description
                )
                VALUES(?,?,?,?)
                """,
                r.grantedBy(),
                "GRANT",
                sql,
                "Permiso a rol"
        );

        db.audit(
                r.grantedBy(),
                "GRANT",
                "PERMISSION",
                r.permissionId(),
                "ROLE_ID=" + r.roleId(),
                sql,
                req.getRemoteAddr(),
                "OK",
                null
        );

        return Map.of(
                "message", "Permiso asignado a rol",
                "sql", sql
        );
    }

    @DeleteMapping("/grant-role/{roleId}/{permissionId}/{actor}")
    public Map<String, Object> revokeRole(
            @PathVariable Long roleId,
            @PathVariable Long permissionId,
            @PathVariable Long actor,
            HttpServletRequest req
    ) {

        Map<String, Object> role = db.one(
                "SELECT role_name FROM APP_ROLES WHERE role_id = ?",
                roleId
        );

        Map<String, Object> p = db.one(
                "SELECT * FROM APP_PERMISSIONS WHERE permission_id = ?",
                permissionId
        );

        String sql = "REVOKE " + p.get("PRIVILEGE_TYPE") +
                " ON " + p.get("SCHEMA_NAME") + "." + p.get("OBJECT_NAME") +
                " FROM " + role.get("ROLE_NAME");

        db.update(
                "DELETE FROM ROLE_PERMISSIONS WHERE role_id = ? AND permission_id = ?",
                roleId,
                permissionId
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
                "Revocar permiso a rol"
        );

        db.audit(
                actor,
                "REVOKE",
                "PERMISSION",
                permissionId,
                "ROLE_ID=" + roleId,
                sql,
                req.getRemoteAddr(),
                "OK",
                null
        );

        return Map.of(
                "message", "Permiso revocado a rol",
                "sql", sql
        );
    }

    @PostMapping("/grant-user")
    public Map<String, Object> grantUser(
            @RequestBody GrantUserPermReq r,
            HttpServletRequest req
    ) {

        String oracleUser = db.q(r.oracleUsername());

        db.update("""
                INSERT INTO USER_PERMISSIONS(
                    user_id,
                    permission_id,
                    grant_option,
                    granted_by,
                    oracle_username
                )
                VALUES(?,?,?,?,?)
                """,
                r.userId(),
                r.permissionId(),
                val(r.grantOption()),
                r.grantedBy(),
                oracleUser
        );

        Map<String, Object> p = db.one(
                "SELECT * FROM APP_PERMISSIONS WHERE permission_id = ?",
                r.permissionId()
        );

        String sql = "GRANT " + p.get("PRIVILEGE_TYPE") +
                " ON " + p.get("SCHEMA_NAME") + "." + p.get("OBJECT_NAME") +
                " TO " + oracleUser +
                ("S".equals(val(r.grantOption())) ? " WITH GRANT OPTION" : "");

        db.update("""
                INSERT INTO SQL_SCRIPTS(
                    generated_by,
                    script_type,
                    script_content,
                    description
                )
                VALUES(?,?,?,?)
                """,
                r.grantedBy(),
                "GRANT",
                sql,
                "Permiso directo a usuario"
        );

        db.audit(
                r.grantedBy(),
                "GRANT",
                "PERMISSION",
                r.permissionId(),
                "USER_ID=" + r.userId(),
                sql,
                req.getRemoteAddr(),
                "OK",
                null
        );

        return Map.of(
                "message", "Permiso directo asignado",
                "sql", sql
        );
    }

    @DeleteMapping("/grant-user/{userId}/{permissionId}/{actor}")
    public Map<String, Object> revokeUser(
            @PathVariable Long userId,
            @PathVariable Long permissionId,
            @PathVariable Long actor,
            HttpServletRequest req
    ) {

        Map<String, Object> up = db.one("""
                SELECT oracle_username
                FROM USER_PERMISSIONS
                WHERE user_id = ? AND permission_id = ?
                """,
                userId,
                permissionId
        );

        Map<String, Object> p = db.one(
                "SELECT * FROM APP_PERMISSIONS WHERE permission_id = ?",
                permissionId
        );

        String sql = "REVOKE " + p.get("PRIVILEGE_TYPE") +
                " ON " + p.get("SCHEMA_NAME") + "." + p.get("OBJECT_NAME") +
                " FROM " + up.get("ORACLE_USERNAME");

        db.update(
                "DELETE FROM USER_PERMISSIONS WHERE user_id = ? AND permission_id = ?",
                userId,
                permissionId
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
                "Revocar permiso directo"
        );

        db.audit(
                actor,
                "REVOKE",
                "PERMISSION",
                permissionId,
                "USER_ID=" + userId,
                sql,
                req.getRemoteAddr(),
                "OK",
                null
        );

        return Map.of(
                "message", "Permiso directo revocado",
                "sql", sql
        );
    }

    private String val(String value) {
        return value == null || value.isBlank() ? "N" : value;
    }
}