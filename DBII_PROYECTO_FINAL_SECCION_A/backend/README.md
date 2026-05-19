# DCL Manager Backend
Backend Spring Boot para administrar usuarios, roles, permisos, auditoría y scripts DCL en Oracle.

## Requisitos
- Java 17
- Maven
- Oracle XE/Oracle DB
- Esquema ya creado con tus tablas APP_USERS, APP_ROLES, APP_PERMISSIONS, etc.

## Configuración
Editar `src/main/resources/application.properties` si tu servicio Oracle no es `XEPDB1`.

## Importante: contraseña inicial
Tu seed tiene un hash falso: `$2b$12$CHANGE_THIS_HASH_IN_PRODUCTION`.
Genera un hash bcrypt real o crea un usuario por endpoint `/api/users`.

Para generar hash rápido puedes iniciar el backend y usar temporalmente un endpoint de creación de usuario, o reemplazar el password_hash con BCrypt de `admin123`.

## Ejecutar
```bash
mvn spring-boot:run
```

## Endpoints principales
### Auth
POST `/api/auth/login`
```json
{"username":"superadmin","password":"admin123"}
```

### Usuarios
GET `/api/users`
POST `/api/users`
PUT `/api/users/{id}`
DELETE `/api/users/{id}`

### Roles
GET `/api/roles`
GET `/api/roles/hierarchy`
POST `/api/roles`
POST `/api/roles/assign`
DELETE `/api/roles/assign/{userId}/{roleId}/{actor}`

### Permisos
GET `/api/permissions`
POST `/api/permissions`
POST `/api/permissions/grant-role`
POST `/api/permissions/grant-user`
DELETE `/api/permissions/grant-role/{roleId}/{permissionId}/{actor}`
DELETE `/api/permissions/grant-user/{userId}/{permissionId}/{actor}`

### Auditoría y scripts
GET `/api/audit`
GET `/api/scripts`
GET `/api/scripts/pending`
POST `/api/scripts/{id}/apply/{actor}`
GET `/api/effective-permissions`
GET `/api/dashboard`

## Flujo sugerido
1. Login.
2. Crear roles.
3. Crear permisos.
4. Asignar permisos a roles o usuarios.
5. Revisar scripts generados.
6. Aplicar scripts si se desea ejecutar DCL real en Oracle.
