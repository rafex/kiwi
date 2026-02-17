-- Asegura que el owner sea el rol de migración (ajusta el nombre si es otro)
-- Ejemplo: flyway_migrator
ALTER FUNCTION public.api_create_object(uuid, text, text, text, text[], jsonb, uuid)
  OWNER TO flyway_migrator;

ALTER FUNCTION public.api_move_object(uuid, uuid)
  OWNER TO flyway_migrator;

ALTER FUNCTION public.api_update_tags(uuid, text[])
  OWNER TO flyway_migrator;

ALTER FUNCTION public.api_update_text(uuid, text, text)
  OWNER TO flyway_migrator;

ALTER FUNCTION public.update_object_search_vector(uuid)
  OWNER TO flyway_migrator;

ALTER FUNCTION public.api_search_objects(text, text[], uuid, int)
  OWNER TO flyway_migrator;

ALTER FUNCTION public.api_fuzzy_search(text, int)
  OWNER TO flyway_migrator;


-- Ahora sí: SECURITY DEFINER + search_path fijo (evita ataques por search_path)
ALTER FUNCTION public.api_create_object(uuid, text, text, text, text[], jsonb, uuid)
  SECURITY DEFINER
  SET search_path = public;

ALTER FUNCTION public.api_move_object(uuid, uuid)
  SECURITY DEFINER
  SET search_path = public;

ALTER FUNCTION public.api_update_tags(uuid, text[])
  SECURITY DEFINER
  SET search_path = public;

ALTER FUNCTION public.api_update_text(uuid, text, text)
  SECURITY DEFINER
  SET search_path = public;

ALTER FUNCTION public.update_object_search_vector(uuid)
  SECURITY DEFINER
  SET search_path = public;

ALTER FUNCTION public.api_search_objects(text, text[], uuid, int)
  SECURITY DEFINER
  SET search_path = public;

ALTER FUNCTION public.api_fuzzy_search(text, int)
  SECURITY DEFINER
  SET search_path = public;

ALTER FUNCTION public.api_create_location(uuid, text, uuid)
  SECURITY DEFINER
  SET search_path = public;


-- Permisos: el usuario app solo ejecuta
GRANT EXECUTE ON FUNCTION public.api_create_object(uuid, text, text, text, text[], jsonb, uuid) TO kiwi_app;
GRANT EXECUTE ON FUNCTION public.api_move_object(uuid, uuid) TO kiwi_app;
GRANT EXECUTE ON FUNCTION public.api_update_tags(uuid, text[]) TO kiwi_app;
GRANT EXECUTE ON FUNCTION public.api_update_text(uuid, text, text) TO kiwi_app;
GRANT EXECUTE ON FUNCTION public.update_object_search_vector(uuid) TO kiwi_app;
GRANT EXECUTE ON FUNCTION public.api_search_objects(text, text[], uuid, int) TO kiwi_app;
GRANT EXECUTE ON FUNCTION public.api_fuzzy_search(text, int) TO kiwi_app;
GRANT EXECUTE ON FUNCTION public.api_create_location(uuid, text, uuid) TO kiwi_app;
 