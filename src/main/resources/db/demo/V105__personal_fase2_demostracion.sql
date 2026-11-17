INSERT INTO usuarios (nombre, apellido, email, password, telefono, estado, created_at) VALUES
    ('Carla', 'Núñez', 'recepcion@tusalud.com', '$2a$10$eKaIoJt42G.9.LoLfVjT5O46fIVxZ5M.FRuePYPp.2s3Lyal0IEZS', '904445566', 'ACTIVO', NOW() - INTERVAL '40 days'),
    ('Jorge', 'Paredes', 'caja@tusalud.com', '$2a$10$eKaIoJt42G.9.LoLfVjT5O46fIVxZ5M.FRuePYPp.2s3Lyal0IEZS', '905556677', 'ACTIVO', NOW() - INTERVAL '40 days'),
    ('Lucía', 'Campos', 'enfermeria@tusalud.com', '$2a$10$eKaIoJt42G.9.LoLfVjT5O46fIVxZ5M.FRuePYPp.2s3Lyal0IEZS', '906667788', 'ACTIVO', NOW() - INTERVAL '40 days');

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u JOIN roles r ON r.nombre = 'ROLE_RECEPCION' WHERE u.email = 'recepcion@tusalud.com';

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u JOIN roles r ON r.nombre = 'ROLE_CAJA' WHERE u.email = 'caja@tusalud.com';

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u JOIN roles r ON r.nombre = 'ROLE_ENFERMERIA' WHERE u.email = 'enfermeria@tusalud.com';
