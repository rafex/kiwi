-- ============================
-- Usuario de aplicación Kiwi
-- ============================

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_roles WHERE rolname = 'kiwi_app'
  ) THEN
    CREATE ROLE kiwi_app
      LOGIN
      PASSWORD 'CHANGE_ME_SECURE_PASSWORD'
      NOSUPERUSER
      NOCREATEDB
      NOCREATEROLE
      NOINHERIT;
  END IF;
END
$$;

-- ============================
-- Permisos básicos
-- ============================

-- Conectar a la BD
GRANT CONNECT ON DATABASE kiwi TO kiwi_app;

-- Usar esquema public
GRANT USAGE ON SCHEMA public TO kiwi_app;

-- ============================
-- Tablas (lectura)
-- ============================

GRANT SELECT ON TABLE
  objects,
  locations,
  object_events
TO kiwi_app;

-- ============================
-- Funciones API (ejecución)
-- ============================

GRANT EXECUTE ON FUNCTION
  api_create_object(
    uuid, text, text, text, text[], jsonb, uuid
  ),
  api_move_object(uuid, uuid),
  api_update_tags(uuid, text[]),
  api_update_text(uuid, text, text),
  api_search_objects(text, text[], uuid, int),
  api_fuzzy_search(text, int)
TO kiwi_app;

-- ============================
-- Secuencias (si existen)
-- ============================

GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO kiwi_app;

-- ============================
-- Default privileges (opcional)
-- Para futuras tablas/funciones
-- ============================

ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT ON TABLES TO kiwi_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT EXECUTE ON FUNCTIONS TO kiwi_app;
