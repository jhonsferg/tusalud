INSERT INTO usuarios (nombre, apellido, email, password, telefono, estado, created_at) VALUES
    ('Laura', 'Quispe', 'lab@tusalud.com', '$2a$10$eKaIoJt42G.9.LoLfVjT5O46fIVxZ5M.FRuePYPp.2s3Lyal0IEZS', '901112233', 'ACTIVO', NOW() - INTERVAL '45 days'),
    ('Rosa', 'Medina', 'farmacia@tusalud.com', '$2a$10$eKaIoJt42G.9.LoLfVjT5O46fIVxZ5M.FRuePYPp.2s3Lyal0IEZS', '902223344', 'ACTIVO', NOW() - INTERVAL '45 days'),
    ('Pedro', 'Ramos', 'almacen@tusalud.com', '$2a$10$eKaIoJt42G.9.LoLfVjT5O46fIVxZ5M.FRuePYPp.2s3Lyal0IEZS', '903334455', 'ACTIVO', NOW() - INTERVAL '45 days');

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u JOIN roles r ON r.nombre = 'ROLE_LABORATORIO' WHERE u.email = 'lab@tusalud.com';

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u JOIN roles r ON r.nombre = 'ROLE_FARMACIA' WHERE u.email = 'farmacia@tusalud.com';

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u JOIN roles r ON r.nombre = 'ROLE_ALMACEN' WHERE u.email = 'almacen@tusalud.com';
