BEGIN;

-- =========================
-- Extensiones necesarias
-- =========================
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================
-- Tipos auxiliares
-- =========================
CREATE TYPE object_status AS ENUM (
  'active',
  'inactive',
  'lost',
  'archived'
);

CREATE TYPE users_status AS ENUM (
  'active',
  'inactive',
  'deleted',
  'archived'
);

CREATE TYPE roles_status AS ENUM (
  'active',
  'inactive',
  'archived'
);

CREATE TYPE event_type AS ENUM (
  'CREATED',
  'UPDATED',
  'MOVED',
  'TAGGED',
  'STATUS_CHANGED',
  'METADATA_UPDATED'
);

CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  user_id UUID NOT NULL DEFAULT gen_random_uuid(),
  username TEXT NOT NULL UNIQUE,
  password_hash bytea not null,
  salt bytea not null,
  iterations int not null,
  status users_status NOT NULL DEFAULT 'active',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_users_user_id
ON users(user_id);

CREATE TABLE roles (
  id BIGSERIAL PRIMARY KEY,
  role_id UUID NOT NULL DEFAULT gen_random_uuid(),
  name TEXT NOT NULL UNIQUE,
  description TEXT,
  status roles_status NOT NULL DEFAULT 'active',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_roles_role_id
ON roles(role_id);

CREATE TABLE user_roles (
  user_fk BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role_fk BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
  assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_fk, role_fk)
);

CREATE INDEX idx_user_roles_user
ON user_roles(user_fk);

CREATE INDEX idx_user_roles_role
ON user_roles(role_fk);

-- =========================
-- Ubicaciones (jerárquicas)
-- =========================
CREATE TABLE locations (
  id BIGSERIAL PRIMARY KEY,
  location_id UUID NOT NULL DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  parent_id BIGINT REFERENCES locations(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_locations_location_id
ON locations(location_id);

CREATE INDEX idx_locations_parent
ON locations(parent_id);

-- =========================
-- Objetos
-- =========================
CREATE TABLE objects (
  id BIGSERIAL PRIMARY KEY,
  object_id UUID NOT NULL DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  description TEXT,
  type TEXT,
  status object_status NOT NULL DEFAULT 'active',

  current_location_fk BIGINT REFERENCES locations(id),

  tags TEXT[],

  metadata JSONB,

  search_vector TSVECTOR,

  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_objects_object_id
ON objects(object_id);

-- =========================
-- Índices de búsqueda
-- =========================
CREATE INDEX idx_objects_search
ON objects
USING GIN (search_vector);

CREATE INDEX idx_objects_tags
ON objects
USING GIN (tags);

CREATE INDEX idx_objects_name_trgm
ON objects
USING GIN (name gin_trgm_ops);

CREATE INDEX idx_objects_type
ON objects(type);

CREATE INDEX idx_objects_location
ON objects(current_location_fk);

-- =========================
-- Histórico de eventos
-- =========================
CREATE TABLE object_events (
  id BIGSERIAL PRIMARY KEY,
  event_id UUID NOT NULL DEFAULT gen_random_uuid(),
  object_fk BIGINT NOT NULL REFERENCES objects(id),

  event_type event_type NOT NULL,

  from_location_fk BIGINT REFERENCES locations(id),
  to_location_fk BIGINT REFERENCES locations(id),

  payload JSONB,

  occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_object_events_event_id
ON object_events(event_id);

CREATE INDEX idx_events_object
ON object_events(object_fk);

CREATE INDEX idx_events_time
ON object_events(occurred_at);

COMMIT;
