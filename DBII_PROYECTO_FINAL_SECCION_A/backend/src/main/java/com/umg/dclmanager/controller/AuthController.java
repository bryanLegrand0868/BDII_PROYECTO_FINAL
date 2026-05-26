package com.umg.dclmanager.controller;

import com.umg.dclmanager.dto.Requests.Login;
import com.umg.dclmanager.security.JwtService;
import com.umg.dclmanager.service.DbService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/auth")
public class AuthController {

    private final DbService db;
    private final PasswordEncoder encoder;
    private final JwtService jwt;



    public AuthController(DbService db, PasswordEncoder encoder, JwtService jwt) {
        this.db = db;
        this.encoder = encoder;
        this.jwt = jwt;
    }


@GetMapping("/test-hash")
public String testHash() {
    return encoder.encode("admin123");
}


   @PostMapping("/login")
public Map<String, Object> login(@RequestBody Login r, HttpServletRequest req) {

    Map<String, Object> user =
            db.one("SELECT * FROM APP_USERS WHERE username = ?", r.username());

    // ========== AGREGA ESTO AQUI ==========
    System.out.println("=== DEBUG LOGIN ===");
    System.out.println("Usuario: " + r.username());
    System.out.println("Password: " + r.password());
    System.out.println("User map: " + user);
    if (user != null) {
        System.out.println("Hash en BD: " + user.get("PASSWORD_HASH"));
        System.out.println("¿Match?: " + encoder.matches(r.password(), String.valueOf(user.get("PASSWORD_HASH"))));
    }
    System.out.println("==================");
    // =====================================

    if (user == null) {
        throw new RuntimeException("Usuario o contraseña incorrectos");
    }
    // ... resto igual

        String status = String.valueOf(user.get("status"));

        if (!"ACTIVO".equals(status)) {
            throw new RuntimeException("Usuario inactivo o bloqueado");
        }

        String hash = String.valueOf(user.get("password_hash"));

        if (!encoder.matches(r.password(), hash)) {

            try {
                db.audit(
                        ((Number) user.get("USER_ID")).longValue(),
                        "LOGIN_FAILED",
                        "SYSTEM",
                        null,
                        r.username(),
                        null,
                        req.getRemoteAddr(),
                        "ERROR",
                        "Credenciales inválidas"
                );
            } catch (Exception ignored) {
            }

            throw new RuntimeException("Usuario o contraseña incorrectos");
        }

        Long userId = ((Number) user.get("USER_ID")).longValue();

        db.update(
                "UPDATE APP_USERS SET last_login = SYSTIMESTAMP WHERE user_id = ?",
                userId
        );

        String token = jwt.token(
                userId,
                String.valueOf(user.get("USERNAME")),
                String.valueOf(user.get("APP_ROLE"))
        );

        db.audit(
                userId,
                "LOGIN",
                "SYSTEM",
                null,
                r.username(),
                null,
                req.getRemoteAddr(),
                "OK",
                null
        );

        return Map.of(
                "token", token,
                "user", user
        );
    }
}