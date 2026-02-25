BEGIN;

CREATE TYPE app_client_status AS ENUM (
  'active',
  'inactive',
  'archived'
);

CREATE TABLE app_clients (
  id BIGSERIAL PRIMARY KEY,
  app_client_id UUID NOT NULL DEFAULT gen_random_uuid(),
  client_id TEXT NOT NULL UNIQUE,
  name TEXT,
  secret_hash bytea NOT NULL,
  salt bytea NOT NULL,
  iterations int NOT NULL,
  roles TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
  status app_client_status NOT NULL DEFAULT 'active',
  last_used_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_app_clients_app_client_id
ON app_clients(app_client_id);

CREATE INDEX idx_app_clients_status
ON app_clients(status);

CREATE INDEX idx_app_clients_roles
ON app_clients
USING GIN (roles);

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.app_clients TO kiwi_app;
GRANT USAGE, SELECT ON SEQUENCE public.app_clients_id_seq TO kiwi_app;

COMMIT;
