package com.umg.dclmanager.dto;

public class Requests {

    public record Login(String username, String password) {}

    public record AppUser(
            String username, String password, String email,
            String fullName, String status, String appRole,
            String oracleUsername, String oraclePassword,
            Long createdBy
    ) {}

    public record RoleReq(
            String roleName, String description,
            Long parentRoleId, String isOracleRole, Long createdBy
    ) {}

    public record PermissionReq(
            String objectName, String objectType,
            String privilegeType, String schemaName, String description
    ) {}

    public record AssignRoleReq(
            Long userId, Long roleId, Long assignedBy, String expiresAt
    ) {}

    public record GrantRolePermReq(
            Long roleId, Long permissionId, String grantOption, Long grantedBy
    ) {}

    public record GrantUserPermReq(
            Long userId, Long permissionId, String grantOption,
            Long grantedBy, String oracleUsername
    ) {}

    public record ScriptReq(
            Long generatedBy, String scriptType,
            String scriptContent, String description
    ) {}

    // ---------- Oracle DB users (Fase 2) ----------

    public record DbUserCreateReq(
            String username,        // username Oracle, requerido
            String password,        // password Oracle, requerido
            String defaultTablespace,
            String temporaryTablespace,
            Long actorId
    ) {}

    public record DbUserPasswordReq(
            String newPassword,
            Long actorId
    ) {}

    public record DbUserActionReq(
            Long actorId
    ) {}

    // ---------- Fase 5: probar acceso ----------

    public record TestAccessReq(
            String username,
            String password,
            String sql,             // ej: SELECT COUNT(*) FROM PROYECTOFINAL.APP_USERS
            Long actorId
    ) {}

    // ---------- Asignación de rol Oracle ----------

    public record GrantRoleToDbUserReq(
            Long roleId,
            String oracleUsername,
            String adminOption,     // 'S' | 'N'
            Long actorId
    ) {}
}
