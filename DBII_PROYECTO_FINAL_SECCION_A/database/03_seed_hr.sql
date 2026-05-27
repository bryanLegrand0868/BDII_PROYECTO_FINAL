-- ============================================================
--  SEED — Catálogo de permisos sobre HR + roles de la demo.
--
--  Ejecutar conectado como proyectoFinal:
--     sqlplus "proyectoFinal/admin123@localhost:1521/orclpdb"
--     SQL> @03_seed_hr.sql
--
--  PRE-REQUISITO: el backend debe haberse iniciado al menos
--  una vez, para que exista el usuario superadmin (user_id=1).
--
--  Para resetear:
--     DELETE FROM APP_PERMISSIONS;
--     DELETE FROM APP_ROLES;
--     -- y en Oracle, si quedaron:
--     DROP ROLE ROL_LECTURA;
--     DROP ROLE ROL_ADMIN;
--     COMMIT;
-- ============================================================


-- ------------------------------------------------------------
--  1) Crear roles REALES en Oracle (ROL_LECTURA, ROL_ADMIN)
--     Los envolvemos en bloques para que no fallen si ya existen.
-- ------------------------------------------------------------

BEGIN
   EXECUTE IMMEDIATE 'CREATE ROLE ROL_LECTURA';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE = -1921 THEN NULL;        -- ORA-01921: rol ya existe
      ELSE RAISE; END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'CREATE ROLE ROL_ADMIN';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE = -1921 THEN NULL;
      ELSE RAISE; END IF;
END;
/


-- ------------------------------------------------------------
--  2) Catalogar roles built-in de Oracle + los recién creados.
--     is_oracle_role='S' porque YA existen en Oracle.
-- ------------------------------------------------------------

INSERT INTO APP_ROLES (role_name, description, parent_role_id, is_oracle_role, created_by)
VALUES ('CONNECT',  'Built-in Oracle: rol básico de conexión',          NULL, 'S', 1);

INSERT INTO APP_ROLES (role_name, description, parent_role_id, is_oracle_role, created_by)
VALUES ('RESOURCE', 'Built-in Oracle: crear objetos propios',           NULL, 'S', 1);

INSERT INTO APP_ROLES (role_name, description, parent_role_id, is_oracle_role, created_by)
VALUES ('DBA',      'Built-in Oracle: administrador (usar con cuidado)', NULL, 'S', 1);

INSERT INTO APP_ROLES (role_name, description, parent_role_id, is_oracle_role, created_by)
VALUES ('SELECT_CATALOG_ROLE',
        'Built-in Oracle: lectura del diccionario',                      NULL, 'S', 1);

INSERT INTO APP_ROLES (role_name, description, parent_role_id, is_oracle_role, created_by)
VALUES ('ROL_LECTURA',
        'Sólo lectura sobre el esquema HR (rúbrica Fase 3)', NULL, 'S', 1);

INSERT INTO APP_ROLES (role_name, description, parent_role_id, is_oracle_role, created_by)
VALUES ('ROL_ADMIN',
        'Lectura y escritura sobre el esquema HR (rúbrica Fase 3)', NULL, 'S', 1);


-- ------------------------------------------------------------
--  3) Catálogo de permisos sobre HR.
-- ------------------------------------------------------------

INSERT INTO APP_PERMISSIONS (object_name, object_type, privilege_type, schema_name, description)
VALUES ('EMPLOYEES', 'TABLE', 'SELECT', 'HR', 'Lectura de empleados');
INSERT INTO APP_PERMISSIONS (object_name, object_type, privilege_type, schema_name, description)
VALUES ('EMPLOYEES', 'TABLE', 'INSERT', 'HR', 'Insertar empleados');
INSERT INTO APP_PERMISSIONS (object_name, object_type, privilege_type, schema_name, description)
VALUES ('EMPLOYEES', 'TABLE', 'UPDATE', 'HR', 'Actualizar empleados');
INSERT INTO APP_PERMISSIONS (object_name, object_type, privilege_type, schema_name, description)
VALUES ('EMPLOYEES', 'TABLE', 'DELETE', 'HR', 'Eliminar empleados');

INSERT INTO APP_PERMISSIONS (object_name, object_type, privilege_type, schema_name, description)
VALUES ('DEPARTMENTS', 'TABLE', 'SELECT', 'HR', 'Lectura de departamentos');
INSERT INTO APP_PERMISSIONS (object_name, object_type, privilege_type, schema_name, description)
VALUES ('DEPARTMENTS', 'TABLE', 'INSERT', 'HR', 'Insertar departamentos');
INSERT INTO APP_PERMISSIONS (object_name, object_type, privilege_type, schema_name, description)
VALUES ('DEPARTMENTS', 'TABLE', 'UPDATE', 'HR', 'Actualizar departamentos');
INSERT INTO APP_PERMISSIONS (object_name, object_type, privilege_type, schema_name, description)
VALUES ('DEPARTMENTS', 'TABLE', 'DELETE', 'HR', 'Eliminar departamentos');

INSERT INTO APP_PERMISSIONS (object_name, object_type, privilege_type, schema_name, description)
VALUES ('JOBS', 'TABLE', 'SELECT', 'HR', 'Lectura de puestos');
INSERT INTO APP_PERMISSIONS (object_name, object_type, privilege_type, schema_name, description)
VALUES ('JOBS', 'TABLE', 'INSERT', 'HR', 'Insertar puestos');
INSERT INTO APP_PERMISSIONS (object_name, object_type, privilege_type, schema_name, description)
VALUES ('JOBS', 'TABLE', 'UPDATE', 'HR', 'Actualizar puestos');

INSERT INTO APP_PERMISSIONS (object_name, object_type, privilege_type, schema_name, description)
VALUES ('JOB_HISTORY', 'TABLE', 'SELECT', 'HR', 'Lectura de historial');

INSERT INTO APP_PERMISSIONS (object_name, object_type, privilege_type, schema_name, description)
VALUES ('LOCATIONS', 'TABLE', 'SELECT', 'HR', 'Lectura de ubicaciones');
INSERT INTO APP_PERMISSIONS (object_name, object_type, privilege_type, schema_name, description)
VALUES ('LOCATIONS', 'TABLE', 'UPDATE', 'HR', 'Actualizar ubicaciones');

INSERT INTO APP_PERMISSIONS (object_name, object_type, privilege_type, schema_name, description)
VALUES ('COUNTRIES', 'TABLE', 'SELECT', 'HR', 'Lectura de países');

INSERT INTO APP_PERMISSIONS (object_name, object_type, privilege_type, schema_name, description)
VALUES ('REGIONS', 'TABLE', 'SELECT', 'HR', 'Lectura de regiones');

COMMIT;
