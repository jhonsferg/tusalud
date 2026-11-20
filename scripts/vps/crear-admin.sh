#!/bin/sh
set -eu

BASE="${BASE:-/root/tusalud}"
. "${BASE}/.env"

docker exec -i databases_postgres sh -c 'psql -U "$POSTGRES_USER" -d "'"${TUSALUD_DB_NAME}"'" -v ON_ERROR_STOP=1 -v email="'"${ADMIN_EMAIL}"'" -v pw="'"${ADMIN_PASSWORD}"'"' <<'SQL'
CREATE EXTENSION IF NOT EXISTS pgcrypto;
INSERT INTO usuarios (nombre, apellido, email, password, telefono, estado, created_at)
SELECT 'Administrador', 'QA', :'email', crypt(:'pw', gen_salt('bf', 10)), NULL, 'ACTIVO', NOW()
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email = :'email');
INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u JOIN roles r ON r.nombre = 'ROLE_ADMIN'
WHERE u.email = :'email'
  AND NOT EXISTS (SELECT 1 FROM usuario_roles ur WHERE ur.usuario_id = u.id AND ur.rol_id = r.id);
DROP EXTENSION pgcrypto;
SQL
echo "administrador listo"
