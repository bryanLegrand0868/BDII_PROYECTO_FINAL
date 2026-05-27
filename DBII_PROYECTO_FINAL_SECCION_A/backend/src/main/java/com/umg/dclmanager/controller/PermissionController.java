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
@CrossOrigin
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
                    object_name, object_type, privilege_type,
                    schema_name, description
                ) VALUES(?,?,?,?,?)
                """,
                db.q(r.objectName()),
                db.q(r.objectType()),
                db.q(r.privilegeType()),
                db.q(r.schemaName()),
                r.description()
        );

        return Map.of("message", "Permiso creado en catálogo", "permissionId", id);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody PermissionReq r) {

        db.update("""
                UPDATE APP_PERMISSIONS
                SET object_name = ?, object_type = ?, privilege_type = ?,
                    schema_name = ?, description = ?
                WHERE permission_id = ?
                """,
                db.q(r.objectName()), db.q(r.objectType()),
                db.q(r.privilegeType()), db.q(r.schemaName()),
                r.description(), id
        );

        return Map.of("message", "Permiso actualizado");
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        db.update("DELETE FROM APP_PERMISSIONS WHERE permission_id = ?", id);
        return Map.of("message", "Permiso eliminado del catálogo");
    }

    @GetMapping("/role/{roleId}")
    public List<Map<String, Object>> getRolePermissions(@PathVariable Long roleId) {
        return db.query("""
                SELECT p.PERMISSION_ID, p.OBJECT_NAME, p.OBJECT_TYPE,
                       p.PRIVILEGE_TYPE, p.SCHEMA_NAME, rp.GRANT_OPTION,
                       p.DESCRIPTION
                FROM ROLE_PERMISSIONS rp
                JOIN APP_PERMISSIONS p ON p.PERMISSION_ID = rp.PERMISSION_ID
                WHERE rp.ROLE_ID = ?
                ORDER BY p.PERMISSION_ID
                """, roleId);
    }

    /** GRANT <priv> ON <schema>.<obj> TO <role> — ejecutado en vivo. */
    @PostMapping("/grant-role")
    public Map<String, Object> grantRole(
            @RequestBody GrantRolePermReq r,
            HttpServletRequest req
    ) {

        Map<String, Object> role = db.one(
                "SELECT role_name FROM APP_ROLES WHERE role_id = ?", r.roleId());
        Map<String, Object> p = db.one(
                "SELECT * FROM APP_PERMISSIONS WHERE permission_id = ?", r.permissionId());

        if (role == null || p == null) {
            throw new RuntimeException("Rol o permiso no encontrado");
        }

        boolean withGrant = "S".equalsIgnoreCase(val(r.grantOption()));
        String sql = "GRANT " + db.q(String.valueOf(p.get("PRIVILEGE_TYPE"))) +
                " ON " + db.q(String.valueOf(p.get("SCHEMA_NAME"))) +
                "." + db.q(String.valueOf(p.get("OBJECT_NAME"))) +
                " TO " + db.q(String.valueOf(role.get("ROLE_NAME"))) +
                (withGrant ? " WITH GRANT OPTION" : "");

        db.executeDCL(
                sql, "GRANT",
                db.valActor(r.grantedBy()),
                "GRANT", "PERMISSION",
                r.permissionId(),
                "ROLE_ID=" + r.roleId(),
                "GRANT a rol", req.getRemoteAddr()
        );

        db.update("""
                MERGE INTO ROLE_PERMISSIONS rp
                USING (SELECT ? AS rid, ? AS pid FROM dual) src
                ON (rp.role_id = src.rid AND rp.permission_id = src.pid)
                WHEN NOT MATCHED THEN INSERT(
                    role_id, permission_id, grant_option, granted_by
                ) VALUES(src.rid, src.pid, ?, ?)
                """,
                r.roleId(), r.permissionId(),
                val(r.grantOption()), db.valActor(r.grantedBy())
        );

        return Map.of("message", "Permiso GRANT aplicado en Oracle", "sql", sql);
    }

    @DeleteMapping("/grant-role/{roleId}/{permissionId}/{actor}")
    public Map<String, Object> revokeRole(
            @PathVariable Long roleId,
            @PathVariable Long permissionId,
            @PathVariable Long actor,
            HttpServletRequest req
    ) {

        Map<String, Object> role = db.one(
                "SELECT role_name FROM APP_ROLES WHERE role_id = ?", roleId);
        Map<String, Object> p = db.one(
                "SELECT * FROM APP_PERMISSIONS WHERE permission_id = ?", permissionId);

        if (role == null || p == null) throw new RuntimeException("Rol o permiso no encontrado");

        String sql = "REVOKE " + db.q(String.valueOf(p.get("PRIVILEGE_TYPE"))) +
                " ON " + db.q(String.valueOf(p.get("SCHEMA_NAME"))) +
                "." + db.q(String.valueOf(p.get("OBJECT_NAME"))) +
                " FROM " + db.q(String.valueOf(role.get("ROLE_NAME")));

        db.executeDCL(
                sql, "REVOKE",
                actor, "REVOKE", "PERMISSION",
                permissionId, "ROLE_ID=" + roleId,
                "REVOKE a rol", req.getRemoteAddr()
        );

        db.update("DELETE FROM ROLE_PERMISSIONS WHERE role_id = ? AND permission_id = ?",
                roleId, permissionId);

        return Map.of("message", "Permiso REVOKE aplicado en Oracle", "sql", sql);
    }

    /** GRANT directo sobre un usuario Oracle real. */
    @PostMapping("/grant-user")
    public Map<String, Object> grantUser(
            @RequestBody GrantUserPermReq r,
            HttpServletRequest req
    ) {

        String oracleUser = db.q(r.oracleUsername());

        Map<String, Object> p = db.one(
                "SELECT * FROM APP_PERMISSIONS WHERE permission_id = ?", r.permissionId());

        if (p == null) throw new RuntimeException("Permiso no encontrado");

        boolean withGrant = "S".equalsIgnoreCase(val(r.grantOption()));
        String sql = "GRANT " + db.q(String.valueOf(p.get("PRIVILEGE_TYPE"))) +
                " ON " + db.q(String.valueOf(p.get("SCHEMA_NAME"))) +
                "." + db.q(String.valueOf(p.get("OBJECT_NAME"))) +
                " TO " + oracleUser +
                (withGrant ? " WITH GRANT OPTION" : "");

        db.executeDCL(
                sql, "GRANT",
                db.valActor(r.grantedBy()),
                "GRANT", "PERMISSION",
                r.permissionId(),
                "USER_ID=" + r.userId() + " ORACLE_USER=" + oracleUser,
                "GRANT directo a usuario Oracle", req.getRemoteAddr()
        );

        if (r.userId() != null) {
            db.update("""
                    MERGE INTO USER_PERMISSIONS up
                    USING (SELECT ? AS uid, ? AS pid FROM dual) src
                    ON (up.user_id = src.uid AND up.permission_id = src.pid)
                    WHEN NOT MATCHED THEN INSERT(
                        user_id, permission_id, grant_option, granted_by, oracle_username
                    ) VALUES(src.uid, src.pid, ?, ?, ?)
                    """,
                    r.userId(), r.permissionId(),
                    val(r.grantOption()), db.valActor(r.grantedBy()), oracleUser
            );
        }

        return Map.of("message", "GRANT directo aplicado en Oracle", "sql", sql);
    }

    @DeleteMapping("/grant-user/{oracleUser}/{permissionId}/{actor}")
    public Map<String, Object> revokeUser(
            @PathVariable String oracleUser,
            @PathVariable Long permissionId,
            @PathVariable Long actor,
            HttpServletRequest req
    ) {

        String safeUser = db.q(oracleUser);

        Map<String, Object> p = db.one(
                "SELECT * FROM APP_PERMISSIONS WHERE permission_id = ?", permissionId);

        if (p == null) throw new RuntimeException("Permiso no encontrado");

        String sql = "REVOKE " + db.q(String.valueOf(p.get("PRIVILEGE_TYPE"))) +
                " ON " + db.q(String.valueOf(p.get("SCHEMA_NAME"))) +
                "." + db.q(String.valueOf(p.get("OBJECT_NAME"))) +
                " FROM " + safeUser;

        db.executeDCL(
                sql, "REVOKE",
                actor, "REVOKE", "PERMISSION",
                permissionId,
                "ORACLE_USER=" + safeUser,
                "REVOKE directo a usuario Oracle", req.getRemoteAddr()
        );

        db.update("""
                DELETE FROM USER_PERMISSIONS
                WHERE oracle_username = ? AND permission_id = ?
                """, safeUser, permissionId);

        return Map.of("message", "REVOKE directo aplicado en Oracle", "sql", sql);
    }

    private String val(String value) {
        return value == null || value.isBlank() ? "N" : value;
    }
}
