BEGIN;

INSERT INTO roles (role_id, name, description, status)
VALUES
	(gen_random_uuid(), 'ADMIN', 'Administrador del sistema', 'active'),
	(gen_random_uuid(), 'USER', 'Usuario estándar', 'active'),
	(gen_random_uuid(), 'READONLY', 'Usuario de solo lectura', 'active'),
	(gen_random_uuid(), 'AUDITOR', 'Usuario auditor (acceso de revisión)', 'active')
ON CONFLICT (name) DO NOTHING;

COMMIT;
