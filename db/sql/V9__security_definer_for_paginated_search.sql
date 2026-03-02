ALTER FUNCTION public.api_search_objects(text, text[], uuid, int, int)
  OWNER TO flyway_migrator;

ALTER FUNCTION public.api_fuzzy_search(text, int, int)
  OWNER TO flyway_migrator;

ALTER FUNCTION public.api_search_objects(text, text[], uuid, int, int)
  SECURITY DEFINER
  SET search_path = public;

ALTER FUNCTION public.api_fuzzy_search(text, int, int)
  SECURITY DEFINER
  SET search_path = public;

GRANT EXECUTE ON FUNCTION public.api_search_objects(text, text[], uuid, int, int) TO kiwi_app;
GRANT EXECUTE ON FUNCTION public.api_fuzzy_search(text, int, int) TO kiwi_app;
