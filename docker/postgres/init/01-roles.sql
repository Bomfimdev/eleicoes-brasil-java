-- Usuário de migrations (DDL) e usuário da aplicação (DML mínimo).
-- Roda só na primeira criação do volume.

CREATE USER eleicoes_migrator WITH PASSWORD 'eleicoes_migrator';
CREATE USER eleicoes_app WITH PASSWORD 'eleicoes_app';

GRANT CONNECT ON DATABASE eleicoes TO eleicoes_migrator;
GRANT CONNECT ON DATABASE eleicoes TO eleicoes_app;

GRANT USAGE, CREATE ON SCHEMA public TO eleicoes_migrator;
GRANT USAGE ON SCHEMA public TO eleicoes_app;

ALTER DEFAULT PRIVILEGES FOR ROLE eleicoes_migrator IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO eleicoes_app;

ALTER DEFAULT PRIVILEGES FOR ROLE eleicoes_migrator IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO eleicoes_app;
