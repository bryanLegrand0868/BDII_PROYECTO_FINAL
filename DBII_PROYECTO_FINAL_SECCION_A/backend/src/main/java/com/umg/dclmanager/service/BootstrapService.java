package com.umg.dclmanager.service;

import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Al arrancar el backend:
 *   - Crea el usuario superadmin del web app si no existe
 *     (con hash bcrypt real de "admin123").
 *
 * Nota: el catálogo de permisos (sobre HR) y los roles built-in los
 * inserta el script SQL 03_seed_hr.sql, NO el backend. Mantener
 * separadas las responsabilidades hace que el reseteo del catálogo
 * (DELETE FROM APP_PERMISSIONS) sea trivial sin afectar al login.
 */
@Service
public class BootstrapService {

    private final DbService db;
    private final PasswordEncoder encoder;

    public BootstrapService(DbService db, PasswordEncoder encoder) {
        this.db = db;
        this.encoder = encoder;
    }

    @PostConstruct
    public void init() {
        try {
            ensureSuperadmin();
        } catch (Exception e) {
            System.err.println("[Bootstrap] aviso: " + e.getMessage());
        }
    }

    private void ensureSuperadmin() {
        Map<String, Object> existing = db.one(
                "SELECT user_id FROM APP_USERS WHERE username = 'superadmin'");

        if (existing != null) return;

        db.update("""
                INSERT INTO APP_USERS(
                    username, password_hash, email,
                    full_name, status, app_role
                ) VALUES(?,?,?,?,?,?)
                """,
                "superadmin",
                encoder.encode("admin123"),
                "admin@dcl-manager.local",
                "Administrador Principal",
                "ACTIVO",
                "SUPERADMIN"
        );

        System.out.println("[Bootstrap] usuario superadmin/admin123 creado (user_id=1)");
    }
}
