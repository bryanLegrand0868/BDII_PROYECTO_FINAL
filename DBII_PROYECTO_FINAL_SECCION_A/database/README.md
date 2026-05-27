# Scripts de base de datos — DCL Manager

Oracle Database con **SID = `orcl`** (instancia clásica), puerto 1521.

## Orden de ejecución

### 1. Crear el usuario de servicio (como SYS)

```bash
sqlplus sys/<password>@localhost:1521:orcl as sysdba
SQL> @01_create_user.sql
```

Crea el usuario Oracle `proyectoFinal / admin123` con los privilegios
DBA necesarios (CREATE USER, CREATE ROLE, GRANT ANY ROLE,
GRANT ANY OBJECT PRIVILEGE, SELECT ANY DICTIONARY, V$VERSION, etc.).

### 2. Crear el esquema de la aplicación (como proyectoFinal)

```bash
sqlplus proyectoFinal/admin123@localhost:1521:orcl
SQL> @02_schema.sql
```

Crea las 8 tablas (APP_USERS, APP_ROLES, APP_PERMISSIONS,
USER_ROLES, ROLE_PERMISSIONS, USER_PERMISSIONS, AUDIT_LOG, SQL_SCRIPTS),
índices, 3 triggers de auditoría y 3 vistas.

### 3. Arrancar el backend una primera vez

```bash
cd ../backend
mvn spring-boot:run
```

Al iniciar verás en consola:

```
[Bootstrap] usuario superadmin/admin123 creado (user_id=1)
```

Esto crea al admin del web app con un hash bcrypt real.
Puedes detener el backend con Ctrl+C después de ver el mensaje.

### 4. Cargar el catálogo HR (como proyectoFinal)

```bash
sqlplus proyectoFinal/admin123@localhost:1521:orcl
SQL> @03_seed_hr.sql
```

Inserta:
- 4 roles built-in (CONNECT, RESOURCE, DBA, SELECT_CATALOG_ROLE),
- 2 roles de demo (ROL_LECTURA, ROL_ADMIN, marcados como `is_oracle_role='N'`
  para que la app los cree con `CREATE ROLE` cuando el usuario lo decida),
- 16 permisos sobre HR.* (EMPLOYEES, DEPARTMENTS, JOBS, etc.)
  que la demo usará para los GRANT/REVOKE de las fases 4 y 5.

### 5. Verificar que HR existe

```sql
SELECT username, account_status FROM dba_users WHERE username='HR';
SELECT COUNT(*) FROM hr.employees;   -- debería devolver ~107
```

Si HR está bloqueado:
```sql
ALTER USER hr ACCOUNT UNLOCK;
ALTER USER hr IDENTIFIED BY hr;
```

## Reset completo

Para reiniciar limpio, conectado como `proyectoFinal`:

```sql
DELETE FROM AUDIT_LOG;
DELETE FROM SQL_SCRIPTS;
DELETE FROM USER_PERMISSIONS;
DELETE FROM ROLE_PERMISSIONS;
DELETE FROM USER_ROLES;
DELETE FROM APP_PERMISSIONS;
DELETE FROM APP_ROLES;
DELETE FROM APP_USERS WHERE username <> 'superadmin';
COMMIT;
-- Re-ejecuta 03_seed_hr.sql para repoblar el catálogo
```

Si en tu Oracle algún `ROL_LECTURA`, `ROL_ADMIN` o usuario demo
quedaron creados de pruebas anteriores:

```sql
DROP ROLE ROL_LECTURA;
DROP ROLE ROL_ADMIN;
DROP USER USUARIO_DEMO CASCADE;
```
