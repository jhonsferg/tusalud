INSERT INTO usuarios (nombre, apellido, email, password, telefono, estado, created_at) VALUES
    ('Mariana', 'Ríos', 'gerencia@tusalud.com', '$2a$10$eKaIoJt42G.9.LoLfVjT5O46fIVxZ5M.FRuePYPp.2s3Lyal0IEZS', '907778899', 'ACTIVO', NOW() - INTERVAL '40 days'),
    ('Esteban', 'Mora', 'finanzas@tusalud.com', '$2a$10$eKaIoJt42G.9.LoLfVjT5O46fIVxZ5M.FRuePYPp.2s3Lyal0IEZS', '908889900', 'ACTIVO', NOW() - INTERVAL '40 days'),
    ('Valentina', 'Cruz', 'operaciones@tusalud.com', '$2a$10$eKaIoJt42G.9.LoLfVjT5O46fIVxZ5M.FRuePYPp.2s3Lyal0IEZS', '909990011', 'ACTIVO', NOW() - INTERVAL '40 days'),
    ('Ximena', 'Solís', 'rrhh@tusalud.com', '$2a$10$eKaIoJt42G.9.LoLfVjT5O46fIVxZ5M.FRuePYPp.2s3Lyal0IEZS', '910001122', 'ACTIVO', NOW() - INTERVAL '40 days'),
    ('Hugo', 'Benavides', 'calidad@tusalud.com', '$2a$10$eKaIoJt42G.9.LoLfVjT5O46fIVxZ5M.FRuePYPp.2s3Lyal0IEZS', '911112233', 'ACTIVO', NOW() - INTERVAL '40 days');

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id
FROM (VALUES
    ('gerencia@tusalud.com', 'ROLE_GERENCIA'),
    ('finanzas@tusalud.com', 'ROLE_FINANZAS'),
    ('operaciones@tusalud.com', 'ROLE_OPERACIONES'),
    ('rrhh@tusalud.com', 'ROLE_RRHH'),
    ('calidad@tusalud.com', 'ROLE_CALIDAD')) AS v (email, rol)
JOIN usuarios u ON u.email = v.email
JOIN roles r ON r.nombre = v.rol;
