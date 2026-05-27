-- ============================================================
--  Crea el usuario de servicio que usa la aplicación para
--  conectarse a Oracle y ejecutar todos los DCL (CREATE USER,
--  CREATE ROLE, GRANT, REVOKE) en vivo sobre el DBMS.
--
--  Ejecutar como SYS o SYSTEM, conectado al pluggable database
--  (ORCLPDB en Oracle 19c+, XEPDB1 en Oracle XE):
--
--     sqlplus sys/<password>@localhost:1521/ORCLPDB as sysdba
-- ============================================================

-- Limpieza por si ya existe
BEGIN
   EXECUTE IMMEDIATE 'DROP USER proyectoFinal CASCADE';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

CREATE USER proyectoFinal IDENTIFIED BY admin123;

-- Privilegios mínimos para conectar y trabajar
GRANT CONNECT, RESOURCE TO proyectoFinal;
GRANT CREATE VIEW, CREATE TRIGGER, CREATE PROCEDURE,
      CREATE SEQUENCE, CREATE SESSION, UNLIMITED TABLESPACE
    TO proyectoFinal;

-- Privilegios DCL: necesarios para que la aplicación pueda
-- crear/modificar usuarios, roles y permisos en vivo.
GRANT CREATE USER, ALTER USER, DROP USER         TO proyectoFinal;
GRANT CREATE ROLE, DROP ANY ROLE, ALTER ANY ROLE TO proyectoFinal;
GRANT GRANT ANY ROLE, GRANT ANY PRIVILEGE        TO proyectoFinal;
GRANT GRANT ANY OBJECT PRIVILEGE                 TO proyectoFinal;

-- Vistas del diccionario para listar usuarios/roles/privilegios
GRANT SELECT ANY DICTIONARY                       TO proyectoFinal;

-- Vista V$ para info del servidor (V$VERSION, V$INSTANCE)
GRANT SELECT ON V_$VERSION  TO proyectoFinal;
GRANT SELECT ON V_$INSTANCE TO proyectoFinal;

ALTER USER proyectoFinal QUOTA UNLIMITED ON USERS;
