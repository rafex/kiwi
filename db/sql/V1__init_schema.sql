BEGIN;

-- =========================
-- Extensiones necesarias
-- =========================
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- =========================
-- Tipos auxiliares
-- =========================
CREATE TYPE object_status AS ENUM (
  'active',
  'inactive',
  'lost',
  'archived'
);

CREATE TYPE event_type AS ENUM (
  'CREATED',
  'UPDATED',
  'MOVED',
  'TAGGED',
  'STATUS_CHANGED'
);

-- =========================
-- Ubicaciones (jerárquicas)
-- =========================
CREATE TABLE locations (
  location_id UUID PRIMARY KEY,
  name TEXT NOT NULL,
  parent_location_id UUID REFERENCES locations(location_id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_locations_parent
ON locations(parent_location_id);

-- =========================
-- Objetos
-- =========================
CREATE TABLE objects (
  object_id UUID PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT,
  type TEXT,
  status object_status NOT NULL DEFAULT 'active',

  current_location_id UUID REFERENCES locations(location_id),

  tags TEXT[],

  metadata JSONB,

  search_vector TSVECTOR,

  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

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
ON objects(current_location_id);

-- =========================
-- Histórico de eventos
-- =========================
CREATE TABLE object_events (
  event_id UUID PRIMARY KEY,
  object_id UUID NOT NULL REFERENCES objects(object_id),

  event_type event_type NOT NULL,

  from_location_id UUID,
  to_location_id UUID,

  payload JSONB,

  occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_object
ON object_events(object_id);

CREATE INDEX idx_events_time
ON object_events(occurred_at);

COMMIT;
