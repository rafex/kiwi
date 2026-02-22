DO $$
DECLARE r record;
BEGIN
  FOR r IN
    SELECT n.nspname AS schema_name,
           p.proname AS func_name,
           pg_get_function_identity_arguments(p.oid) AS args
    FROM pg_proc p
    JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'public'
      AND p.proname LIKE 'api\_%' ESCAPE '\'
  LOOP
    EXECUTE format('DROP FUNCTION IF EXISTS %I.%I(%s) CASCADE;',
                   r.schema_name, r.func_name, r.args);
  END LOOP;
END $$;

BEGIN;

DROP TABLE IF EXISTS public.user_roles CASCADE;
DROP TABLE IF EXISTS public.users CASCADE;
DROP TABLE IF EXISTS public.roles CASCADE;
DROP TABLE IF EXISTS public.object_events CASCADE;
DROP TABLE IF EXISTS public.objects CASCADE;
DROP TABLE IF EXISTS public.locations CASCADE;

-- opcional: si vas a correr Flyway desde cero
DROP TABLE IF EXISTS public.flyway_schema_history CASCADE;

COMMIT;