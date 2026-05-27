package com.umg.dclmanager.controller;

import com.umg.dclmanager.dto.Requests.AppUser;
import com.umg.dclmanager.service.DbService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Gestión de usuarios DEL WEB APP (tabla APP_USERS).
 * Los usuarios reales de Oracle se gestionan en DbUserController.
 */
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
                ORDER BY user_id
                """);
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
                    username, password_hash, email,
                    full_name, status, app_role, oracle_username
                ) VALUES(?,?,?,?,?,?,?)
                """,
                r.username(),
                encoder.encode(r.password()),
                r.email(),
                r.fullName(),
                db.val(r.status(), "ACTIVO"),
                db.val(r.appRole(), "VIEWER"),
                r.oracleUsername() == null || r.oracleUsername().isBlank()
                        ? null : db.q(r.oracleUsername())
        );

        return Map.of("message", "Usuario del web app creado", "userId", id);
    }

    @GetMapping("/current/permissions")
    public List<Map<String, Object>> getCurrentUserPermissions(HttpServletRequest req) {
        try {
            String username = (String) req.getAttribute("username");
            if (username == null) {
                username = SecurityContextHolder.getContext().getAuthentication().getName();
            }
            if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
                return List.of();
            }
            return db.query("""
                    SELECT DISTINCT OBJECT_NAME, PRIVILEGE_TYPE, SCHEMA_NAME
                    FROM VW_USER_EFFECTIVE_PERMISSIONS
                    WHERE USERNAME = ?
                    ORDER BY OBJECT_NAME, PRIVILEGE_TYPE
                    """, username);
        } catch (Exception e) {
            // En cualquier fallo, devolver lista vacía en vez de 400
            // para que la UI no se quede sin botones.
            return List.of();
        }
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody AppUser r) {

        if (r.password() != null && !r.password().isBlank()) {
            db.update("""
                    UPDATE APP_USERS
                    SET username = ?, password_hash = ?, email = ?,
                        full_name = ?, status = ?, app_role = ?,
                        oracle_username = ?
                    WHERE user_id = ?
                    """,
                    r.username(), encoder.encode(r.password()), r.email(),
                    r.fullName(), r.status(), r.appRole(),
                    r.oracleUsername() == null || r.oracleUsername().isBlank()
                            ? null : db.q(r.oracleUsername()),
                    id);
        } else {
            db.update("""
                    UPDATE APP_USERS
                    SET username = ?, email = ?, full_name = ?,
                        status = ?, app_role = ?, oracle_username = ?
                    WHERE user_id = ?
                    """,
                    r.username(), r.email(), r.fullName(),
                    r.status(), r.appRole(),
                    r.oracleUsername() == null || r.oracleUsername().isBlank()
                            ? null : db.q(r.oracleUsername()),
                    id);
        }

        return Map.of("message", "Usuario del web app actualizado");
    }

    @DeleteMapping("/{id}/{actor}")
    public Map<String, Object> delete(@PathVariable Long id, @PathVariable Long actor) {
        db.update("DELETE FROM APP_USERS WHERE user_id = ?", id);
        return Map.of("message", "Usuario del web app eliminado");
    }
}
