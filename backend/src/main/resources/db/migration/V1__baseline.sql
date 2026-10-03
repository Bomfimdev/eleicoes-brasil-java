-- Baseline do schema Eleições Brasil (Java/Angular).
-- Tabelas de domínio entram nas próximas migrações (Fase 1).

CREATE TABLE IF NOT EXISTS schema_meta (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO schema_meta (key, value)
VALUES ('project', 'eleicoes-brasil-java')
ON CONFLICT (key) DO NOTHING;
