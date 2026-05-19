package com.umg.dclmanager.dto;

public class Requests {

    public record Login(
            String username,
            String password
    ) {}

    public record AppUser(
            String username,
            String password,
            String email,
            String fullName,
            String status,
            String appRole,
            String oracleUsername,
            String oraclePassword,
            Long createdBy
    ) {}

    public record RoleReq(
            String roleName,
            String description,
            Long parentRoleId,
            String isOracleRole,
            Long createdBy
    ) {}

    public record PermissionReq(
            String objectName,
            String objectType,
            String privilegeType,
            String schemaName,
            String description
    ) {}

    public record AssignRoleReq(
            Long userId,
            Long roleId,
            Long assignedBy,
            String expiresAt
    ) {}

    public record GrantRolePermReq(
            Long roleId,
            Long permissionId,
            String grantOption,
            Long grantedBy
    ) {}

    public record GrantUserPermReq(
            Long userId,
            Long permissionId,
            String grantOption,
            Long grantedBy,
            String oracleUsername
    ) {}

    public record ScriptReq(
            Long generatedBy,
            String scriptType,
            String scriptContent,
            String description
    ) {}
}