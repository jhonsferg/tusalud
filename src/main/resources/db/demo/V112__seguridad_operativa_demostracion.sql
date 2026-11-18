INSERT INTO eventos_seguridad (tipo, severidad, usuario_id, actor_id, ip_enmascarada, dispositivo, detalle, creado_en)
SELECT v.tipo, v.severidad, u.id, a.id, v.ip, v.dispositivo, v.detalle, NOW() - v.hace
FROM (VALUES
    ('LOGIN_FALLIDO', 'AVISO', 'juan.perez@correo.com', NULL, '190.236.14.x', 'Chrome en Windows', 'Credenciales inválidas', INTERVAL '3 hours'),
    ('LOGIN_FALLIDO', 'AVISO', 'juan.perez@correo.com', NULL, '190.236.14.x', 'Chrome en Windows', 'Credenciales inválidas', INTERVAL '2 hours 50 minutes'),
    ('DISPOSITIVO_NUEVO', 'INFO', 'maria.rodriguez@correo.com', NULL, '181.65.201.x', 'Safari en iPhone', 'Inicio de sesión desde un dispositivo o una dirección no usados antes', INTERVAL '5 hours'),
    ('CONTRASENA_CAMBIADA', 'INFO', 'maria.rodriguez@correo.com', 'maria.rodriguez@correo.com', '181.65.201.x', 'Safari en iPhone', NULL, INTERVAL '4 hours 55 minutes'),
    ('MFA_ACTIVADO', 'INFO', 'dr.garcia@tusalud.com', 'dr.garcia@tusalud.com', '200.48.97.x', 'Chrome en Windows', 'Verificación en dos pasos activada', INTERVAL '1 day 2 hours'),
    ('ROLES_CAMBIADOS', 'AVISO', 'caja@tusalud.com', 'admin@tusalud.com', '200.48.97.x', 'Edge en Windows', 'Roles asignados: ROLE_CAJA', INTERVAL '1 day 6 hours'),
    ('CUENTA_DESACTIVADA', 'AVISO', 'almacen@tusalud.com', 'admin@tusalud.com', '200.48.97.x', 'Edge en Windows', NULL, INTERVAL '2 days'),
    ('CUENTA_ACTIVADA', 'INFO', 'almacen@tusalud.com', 'admin@tusalud.com', '200.48.97.x', 'Edge en Windows', NULL, INTERVAL '1 day 23 hours'),
    ('ACCESO_DENEGADO', 'INFO', 'juan.perez@correo.com', NULL, '190.236.14.x', 'Chrome en Windows', 'GET /api/v1/admin/usuarios', INTERVAL '6 hours'),
    ('LOGIN_FALLIDOS_REPETIDOS', 'ALTA', 'carlos.mendoza@correo.com', NULL, '45.231.88.x', 'Firefox en Linux', '5 intentos fallidos en 15 minutos', INTERVAL '2 days 3 hours'),
    ('BLOQUEO_INTENTOS', 'ALTA', 'carlos.mendoza@correo.com', NULL, '45.231.88.x', 'Firefox en Linux', 'Verificación bloqueada por intentos fallidos', INTERVAL '2 days 2 hours'),
    ('DESCARGA_MASIVA', 'ALTA', 'lab@tusalud.com', NULL, '200.48.97.x', 'Chrome en Windows', '30 descargas en 5 minutos', INTERVAL '4 days')
) AS v(tipo, severidad, correo, correo_actor, ip, dispositivo, detalle, hace)
JOIN usuarios u ON u.email = v.correo
LEFT JOIN usuarios a ON a.email = v.correo_actor;
