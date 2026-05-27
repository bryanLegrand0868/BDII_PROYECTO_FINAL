package com.umg.dclmanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Abre una conexión Oracle puntual usando credenciales arbitrarias
 * (las del usuario a probar) y ejecuta una consulta. Si Oracle
 * responde ORA-01017 (bad credentials), ORA-28000 (account locked),
 * ORA-01031 (insufficient privileges), etc., devolvemos el mensaje
 * tal cual — eso es exactamente lo que el catedrático quiere ver
 * en la Fase 5.
 */
@Service
public class OracleTestService {

    private final String url;

    public OracleTestService(@Value("${spring.datasource.url}") String url) {
        this.url = url;
    }

    public Result tryAs(String username, String password, String sql) {

        if (username == null || username.isBlank())
            return Result.fail(null, "Usuario vacío");
        if (password == null || password.isBlank())
            return Result.fail(null, "Password vacío");
        if (sql == null || sql.isBlank())
            sql = "SELECT 1 AS ok FROM DUAL";

        String upper = sql.trim().toUpperCase();
        if (!upper.startsWith("SELECT") && !upper.startsWith("WITH")) {
            return Result.fail(null, "Sólo se permiten SELECT/WITH en /test-access");
        }

        try (Connection c = DriverManager.getConnection(url, username, password);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            List<Map<String, Object>> rows = new ArrayList<>();
            int max = 100;
            while (rs.next() && rows.size() < max) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    row.put(md.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
            return new Result(true, rows, null, null);

        } catch (SQLException ex) {
            return Result.fail(String.valueOf(ex.getErrorCode()), ex.getMessage());
        } catch (Exception ex) {
            return Result.fail(null, ex.getMessage());
        }
    }

    public record Result(boolean ok, List<Map<String, Object>> rows,
                         String errorCode, String error) {

        public static Result fail(String code, String msg) {
            return new Result(false, List.of(), code, msg);
        }
    }
}
