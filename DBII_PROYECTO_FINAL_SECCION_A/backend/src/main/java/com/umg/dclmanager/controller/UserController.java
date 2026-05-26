package com.umg.dclmanager.controller;

import com.umg.dclmanager.dto.Requests.AppUser;
import com.umg.dclmanager.service.DbService;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/users")
public class UserController {

    private final DbService db;
    private final PasswordEncoder encoder;

    public UserController(DbService db, PasswordEncoder encoder) {
        this.db = db;
        this.encoder = encoder;
    }

    @GetMapping
    public List<Map<String, Object>> all() {
        return db.query("""
            SELECT user_id, username, email, full_name, status,
             app_role, oracle_username, created_at, last_login
             FROM APP_USERS
                ORDER BY user_id""");
    }

    @GetMapping("/{id}")
    public Map<String, Object> one(@PathVariable Long id) {
        return db.one("""
                SELECT user_id, username, email, full_name, status,
                       app_role, oracle_username, created_at, last_login
                FROM APP_USERS
                WHERE user_id = ?
                """, id);
    }

    @GetMapping("/{id}/roles")
    public List<Map<String, Object>> getUserRoles(@PathVariable Long id) {
        return db.query("""
            SELECT r.ROLE_ID, r.ROLE_NAME, r.DESCRIPTION, r.IS_ORACLE_ROLE
            FROM USER_ROLES ur
            JOIN APP_ROLES r ON r.ROLE_ID = ur.ROLE_ID
            WHERE ur.user_id = ? AND ur.is_active = 'S'
            ORDER BY r.ROLE_ID
            """, id);
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody AppUser r, HttpServletRequest req) {

        Long id = db.insert(
                "USER_ID",
                """
                INSERT INTO APP_USERS(
                    username,
                    password_hash,
                    email,
                    full_name,
                    status,
                    app_role,
                    oracle_username
                )
                VALUES(?,?,?,?,?,?,?)
                """,
                r.username(),
                encoder.encode(r.password()),
                r.email(),
                r.fullName(),
                db.val(r.status(), "ACTIVO"),
                db.val(r.appRole(), "VIEWER"),
                r.oracleUsername() == null ? null : db.q(r.oracleUsername())
        );

        String sql = null;

        if (r.oracleUsername() != null && !r.oracleUsername().isBlank()) {

            String oracleUser = db.q(r.oracleUsername());
            String oraclePass = db.val(r.oraclePassword(), "Oracle123");

            sql = "CREATE USER " + oracleUser +
                    " IDENTIFIED BY \"" + oraclePass + "\";\n" +
                    "GRANT CONNECT TO " + oracleUser;

            db.update("""
                    INSERT INTO SQL_SCRIPTS(
                        generated_by,
                        script_type,
                        script_content,
                        description
                    )
                    VALUES(?,?,?,?)
                    """,
                    db.valActor(r.createdBy()),
                    "MIXED",
                    sql,
                    "Crear usuario Oracle " + oracleUser
            );
        }

        return Map.of(
                "message", "Usuario creado",
                "userId", id,
                "sql", sql == null ? "No se generó CREATE USER porque no se envió oracleUsername" : sql
        );
    }

@GetMapping("/current/permissions")
public List<Map<String, Object>> getCurrentUserPermissions(HttpServletRequest req) {
    // Obtener el username del token (lo guarda el JwtFilter)
    String username = (String) req.getAttribute("username");
    
    if (username == null) {
        // Si no viene del filter, intentar obtener del SecurityContext
        username = SecurityContextHolder.getContext().getAuthentication().getName();
    }
    
    return db.query("""
        SELECT DISTINCT OBJECT_NAME, PRIVILEGE_TYPE, SCHEMA_NAME
        FROM VW_USER_EFFECTIVE_PERMISSIONS
        WHERE USERNAME = ?
        ORDER BY OBJECT_NAME, PRIVILEGE_TYPE
        """, username);
}


    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody AppUser r) {

        if (r.password() != null && !r.password().isBlank()) {

            db.update("""
                    UPDATE APP_USERS
                    SET username = ?,
                        password_hash = ?,
                        email = ?,
                        full_name = ?,
                        status = ?,
                        app_role = ?,
                        oracle_username = ?
                    WHERE user_id = ?
                    """,
                    r.username(),
                    encoder.encode(r.password()),
                    r.email(),
                    r.fullName(),
                    r.status(),
                    r.appRole(),
                    r.oracleUsername() == null ? null : db.q(r.oracleUsername()),
                    id
            );

        } else {

            db.update("""
                    UPDATE APP_USERS
                    SET username = ?,
                        email = ?,
                        full_name = ?,
                        status = ?,
                        app_role = ?,
                        oracle_username = ?
                    WHERE user_id = ?
                    """,
                    r.username(),
                    r.email(),
                    r.fullName(),
                    r.status(),
                    r.appRole(),
                    r.oracleUsername() == null ? null : db.q(r.oracleUsername()),
                    id
            );
        }

        return Map.of("message", "Usuario actualizado");
    }

    @DeleteMapping("/{id}/{actor}")
    public Map<String, Object> delete(
            @PathVariable Long id,
            @PathVariable Long actor,
            HttpServletRequest req
    ) {

        Map<String, Object> user = db.one(
                "SELECT username, oracle_username FROM APP_USERS WHERE user_id = ?",
                id
        );

        if (user == null) {
            throw new RuntimeException("Usuario no encontrado");
        }

        String oracleUser = user.get("ORACLE_USERNAME") == null
                ? null
                : db.q(String.valueOf(user.get("ORACLE_USERNAME")));

        String sql = null;

        if (oracleUser != null && !oracleUser.isBlank()) {

            sql = "DROP USER " + oracleUser + " CASCADE";

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
                    "Eliminar usuario Oracle " + oracleUser
            );
        }

        db.update("DELETE FROM APP_USERS WHERE user_id = ?", id);

        db.audit(
                actor,
                "DROP_USER",
                "USER",
                id,
                String.valueOf(user.get("USERNAME")),
                sql,
                req.getRemoteAddr(),
                "OK",
                null
        );

        return Map.of(
                "message", "Usuario eliminado",
                "sql", sql == null ? "No había usuario Oracle asociado" : sql
        );
    }
}