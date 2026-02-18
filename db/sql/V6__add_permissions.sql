-- Permisos de uso del schema (si aplica)
GRANT USAGE ON SCHEMA public TO kiwi_app;

-- Lectura + escritura en tablas de auth
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.users TO kiwi_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.roles TO kiwi_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.user_roles TO kiwi_app;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO kiwi_app;