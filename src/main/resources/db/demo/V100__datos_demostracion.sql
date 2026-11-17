INSERT INTO usuarios (nombre, apellido, email, password, telefono, estado, created_at) VALUES
    ('Administrador', 'General', 'admin@tusalud.com', '$2a$10$CZxj2GbX38cheEgkl9vloeFJ4liISB0ZjYEEQOj.V3qdqtfniP1ZO', '999888777', 'ACTIVO', NOW() - INTERVAL '60 days'),
    ('Luis', 'García', 'dr.garcia@tusalud.com', '$2a$10$LnkKfNnzSze9qUa3qoZp0OyVBKApVqMBHV5vY1k0YhvEC5pGtJqKm', '912345678', 'ACTIVO', NOW() - INTERVAL '60 days'),
    ('Carmen', 'López', 'dra.lopez@tusalud.com', '$2a$10$LnkKfNnzSze9qUa3qoZp0OyVBKApVqMBHV5vY1k0YhvEC5pGtJqKm', '923456789', 'ACTIVO', NOW() - INTERVAL '60 days'),
    ('Ricardo', 'Salazar', 'dr.salazar@tusalud.com', '$2a$10$LnkKfNnzSze9qUa3qoZp0OyVBKApVqMBHV5vY1k0YhvEC5pGtJqKm', '934567890', 'ACTIVO', NOW() - INTERVAL '60 days'),
    ('Patricia', 'Vega', 'dra.vega@tusalud.com', '$2a$10$LnkKfNnzSze9qUa3qoZp0OyVBKApVqMBHV5vY1k0YhvEC5pGtJqKm', '945678901', 'ACTIVO', NOW() - INTERVAL '60 days'),
    ('Andrés', 'Quispe', 'dr.quispe@tusalud.com', '$2a$10$LnkKfNnzSze9qUa3qoZp0OyVBKApVqMBHV5vY1k0YhvEC5pGtJqKm', '956789012', 'ACTIVO', NOW() - INTERVAL '60 days'),
    ('Lucía', 'Fernández', 'dra.fernandez@tusalud.com', '$2a$10$LnkKfNnzSze9qUa3qoZp0OyVBKApVqMBHV5vY1k0YhvEC5pGtJqKm', '967890123', 'ACTIVO', NOW() - INTERVAL '60 days'),
    ('Marcos', 'Delgado', 'dr.delgado@tusalud.com', '$2a$10$LnkKfNnzSze9qUa3qoZp0OyVBKApVqMBHV5vY1k0YhvEC5pGtJqKm', '978901234', 'ACTIVO', NOW() - INTERVAL '60 days'),
    ('Juan', 'Pérez', 'juan.perez@correo.com', '$2a$10$2RaRGJ5Ph7t.ev4VxgQcMuSrWKnv7KcO4XRmGhuTV9jmm81Wq6OjK', '987654321', 'ACTIVO', NOW() - INTERVAL '60 days'),
    ('María', 'Rodríguez', 'maria.rodriguez@correo.com', '$2a$10$2RaRGJ5Ph7t.ev4VxgQcMuSrWKnv7KcO4XRmGhuTV9jmm81Wq6OjK', '976543210', 'ACTIVO', NOW() - INTERVAL '60 days'),
    ('Sofía', 'Ramírez', 'sofia.ramirez@correo.com', '$2a$10$2RaRGJ5Ph7t.ev4VxgQcMuSrWKnv7KcO4XRmGhuTV9jmm81Wq6OjK', '911223344', 'ACTIVO', NOW() - INTERVAL '60 days'),
    ('Carlos', 'Mendoza', 'carlos.mendoza@correo.com', '$2a$10$2RaRGJ5Ph7t.ev4VxgQcMuSrWKnv7KcO4XRmGhuTV9jmm81Wq6OjK', '922334455', 'ACTIVO', NOW() - INTERVAL '60 days'),
    ('Valeria', 'Castillo', 'valeria.castillo@correo.com', '$2a$10$2RaRGJ5Ph7t.ev4VxgQcMuSrWKnv7KcO4XRmGhuTV9jmm81Wq6OjK', '933445566', 'ACTIVO', NOW() - INTERVAL '60 days'),
    ('Diego', 'Herrera', 'diego.herrera@correo.com', '$2a$10$2RaRGJ5Ph7t.ev4VxgQcMuSrWKnv7KcO4XRmGhuTV9jmm81Wq6OjK', '944556677', 'ACTIVO', NOW() - INTERVAL '60 days'),
    ('Camila', 'Torres', 'camila.torres@correo.com', '$2a$10$2RaRGJ5Ph7t.ev4VxgQcMuSrWKnv7KcO4XRmGhuTV9jmm81Wq6OjK', '955667788', 'ACTIVO', NOW() - INTERVAL '60 days'),
    ('Luis Alberto', 'Vargas', 'luis.vargas@correo.com', '$2a$10$2RaRGJ5Ph7t.ev4VxgQcMuSrWKnv7KcO4XRmGhuTV9jmm81Wq6OjK', '966778899', 'ACTIVO', NOW() - INTERVAL '60 days'),
    ('Ana Lucía', 'Paredes', 'ana.paredes@correo.com', '$2a$10$2RaRGJ5Ph7t.ev4VxgQcMuSrWKnv7KcO4XRmGhuTV9jmm81Wq6OjK', '977889900', 'ACTIVO', NOW() - INTERVAL '60 days'),
    ('Jorge', 'Medina', 'jorge.medina@correo.com', '$2a$10$2RaRGJ5Ph7t.ev4VxgQcMuSrWKnv7KcO4XRmGhuTV9jmm81Wq6OjK', '988990011', 'ACTIVO', NOW() - INTERVAL '60 days');

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u JOIN roles r ON r.nombre = 'ROLE_ADMIN'
WHERE u.email IN ('admin@tusalud.com');

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u JOIN roles r ON r.nombre = 'ROLE_MEDICO'
WHERE u.email IN ('dr.garcia@tusalud.com', 'dra.lopez@tusalud.com', 'dr.salazar@tusalud.com', 'dra.vega@tusalud.com', 'dr.quispe@tusalud.com', 'dra.fernandez@tusalud.com', 'dr.delgado@tusalud.com');

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u JOIN roles r ON r.nombre = 'ROLE_PACIENTE'
WHERE u.email IN ('juan.perez@correo.com', 'maria.rodriguez@correo.com', 'sofia.ramirez@correo.com', 'carlos.mendoza@correo.com', 'valeria.castillo@correo.com', 'diego.herrera@correo.com', 'camila.torres@correo.com', 'luis.vargas@correo.com', 'ana.paredes@correo.com', 'jorge.medina@correo.com');

INSERT INTO medicos (colegiatura, consultorio, usuario_id)
SELECT 'CMP-12345', 'Consultorio 101', u.id FROM usuarios u
WHERE u.email = 'dr.garcia@tusalud.com';
INSERT INTO horarios_atencion (dia_semana, duracion_cita_minutos, hora_fin, hora_inicio, medico_id)
SELECT d.dia, 30, TIME '13:00', TIME '08:00', m.id FROM medicos m
CROSS JOIN (VALUES ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY')) AS d(dia) WHERE m.colegiatura = 'CMP-12345';

INSERT INTO medicos (colegiatura, consultorio, usuario_id)
SELECT 'CMP-67890', 'Consultorio 205', u.id FROM usuarios u
WHERE u.email = 'dra.lopez@tusalud.com';
INSERT INTO horarios_atencion (dia_semana, duracion_cita_minutos, hora_fin, hora_inicio, medico_id)
SELECT d.dia, 30, TIME '19:00', TIME '14:00', m.id FROM medicos m
CROSS JOIN (VALUES ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY')) AS d(dia) WHERE m.colegiatura = 'CMP-67890';

INSERT INTO medicos (colegiatura, consultorio, usuario_id)
SELECT 'CMP-23456', 'Consultorio 102', u.id FROM usuarios u
WHERE u.email = 'dr.salazar@tusalud.com';
INSERT INTO horarios_atencion (dia_semana, duracion_cita_minutos, hora_fin, hora_inicio, medico_id)
SELECT d.dia, 30, TIME '12:00', TIME '08:00', m.id FROM medicos m
CROSS JOIN (VALUES ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY')) AS d(dia) WHERE m.colegiatura = 'CMP-23456';
INSERT INTO horarios_atencion (dia_semana, duracion_cita_minutos, hora_fin, hora_inicio, medico_id)
SELECT d.dia, 30, TIME '18:00', TIME '15:00', m.id FROM medicos m
CROSS JOIN (VALUES ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY')) AS d(dia) WHERE m.colegiatura = 'CMP-23456';

INSERT INTO medicos (colegiatura, consultorio, usuario_id)
SELECT 'CMP-34567', 'Consultorio 310', u.id FROM usuarios u
WHERE u.email = 'dra.vega@tusalud.com';
INSERT INTO horarios_atencion (dia_semana, duracion_cita_minutos, hora_fin, hora_inicio, medico_id)
SELECT d.dia, 30, TIME '14:00', TIME '09:00', m.id FROM medicos m
CROSS JOIN (VALUES ('TUESDAY'), ('THURSDAY')) AS d(dia) WHERE m.colegiatura = 'CMP-34567';

INSERT INTO medicos (colegiatura, consultorio, usuario_id)
SELECT 'CMP-45678', 'Consultorio 215', u.id FROM usuarios u
WHERE u.email = 'dr.quispe@tusalud.com';
INSERT INTO horarios_atencion (dia_semana, duracion_cita_minutos, hora_fin, hora_inicio, medico_id)
SELECT d.dia, 30, TIME '16:00', TIME '10:00', m.id FROM medicos m
CROSS JOIN (VALUES ('MONDAY'), ('WEDNESDAY'), ('FRIDAY')) AS d(dia) WHERE m.colegiatura = 'CMP-45678';

INSERT INTO medicos (colegiatura, consultorio, usuario_id)
SELECT 'CMP-56789', 'Consultorio 402', u.id FROM usuarios u
WHERE u.email = 'dra.fernandez@tusalud.com';
INSERT INTO horarios_atencion (dia_semana, duracion_cita_minutos, hora_fin, hora_inicio, medico_id)
SELECT d.dia, 30, TIME '19:00', TIME '14:00', m.id FROM medicos m
CROSS JOIN (VALUES ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY')) AS d(dia) WHERE m.colegiatura = 'CMP-56789';

INSERT INTO medicos (colegiatura, consultorio, usuario_id)
SELECT 'CMP-67801', 'Consultorio 118', u.id FROM usuarios u
WHERE u.email = 'dr.delgado@tusalud.com';
INSERT INTO horarios_atencion (dia_semana, duracion_cita_minutos, hora_fin, hora_inicio, medico_id)
SELECT d.dia, 30, TIME '13:00', TIME '08:00', m.id FROM medicos m
CROSS JOIN (VALUES ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY'), ('SATURDAY')) AS d(dia) WHERE m.colegiatura = 'CMP-67801';

INSERT INTO horarios_atencion (dia_semana, duracion_cita_minutos, hora_fin, hora_inicio, medico_id)
SELECT 'SATURDAY', 30, TIME '12:00', TIME '08:00', id FROM medicos WHERE colegiatura = 'CMP-67890';

INSERT INTO medico_especialidades (medico_id, especialidad_id)
SELECT m.id, e.id FROM medicos m
JOIN (VALUES
    ('CMP-12345', 'Cardiología'),
    ('CMP-12345', 'Medicina General'),
    ('CMP-67890', 'Pediatría'),
    ('CMP-23456', 'Medicina General'),
    ('CMP-34567', 'Dermatología'),
    ('CMP-45678', 'Traumatología'),
    ('CMP-56789', 'Ginecología'),
    ('CMP-67801', 'Oftalmología'),
    ('CMP-67801', 'Medicina General')
) AS v(colegiatura, especialidad) ON v.colegiatura = m.colegiatura
JOIN especialidades e ON e.nombre = v.especialidad;

INSERT INTO pacientes (dni, fecha_nacimiento, historial_clinico_numero, grupo_sanguineo, alergias, antecedentes, usuario_id) VALUES
    ('12345678', DATE '1990-05-15', 'HC-000001', 'O+', 'Penicilina', 'Hipertensión leve diagnosticada en 2022', (SELECT id FROM usuarios WHERE email = 'juan.perez@correo.com')),
    ('87654321', DATE '1985-11-20', 'HC-000002', 'A+', 'Ninguna conocida', 'Asma estacional', (SELECT id FROM usuarios WHERE email = 'maria.rodriguez@correo.com')),
    ('45123678', DATE '1994-03-08', 'HC-000003', 'B+', 'Polen y ácaros', 'Sin antecedentes relevantes', (SELECT id FROM usuarios WHERE email = 'sofia.ramirez@correo.com')),
    ('40213456', DATE '1978-09-21', 'HC-000004', 'O-', 'Ibuprofeno', 'Hipertensión arterial en tratamiento', (SELECT id FROM usuarios WHERE email = 'carlos.mendoza@correo.com')),
    ('71234509', DATE '1998-12-02', 'HC-000005', NULL, NULL, NULL, (SELECT id FROM usuarios WHERE email = 'valeria.castillo@correo.com')),
    ('46789012', DATE '1989-06-17', 'HC-000006', 'A-', 'Ninguna conocida', 'Lesión de rodilla en 2024', (SELECT id FROM usuarios WHERE email = 'diego.herrera@correo.com')),
    ('72345611', DATE '2001-01-30', 'HC-000007', NULL, NULL, NULL, (SELECT id FROM usuarios WHERE email = 'camila.torres@correo.com')),
    ('43219876', DATE '1972-11-05', 'HC-000008', NULL, NULL, NULL, (SELECT id FROM usuarios WHERE email = 'luis.vargas@correo.com')),
    ('70123987', DATE '1992-07-14', 'HC-000009', NULL, NULL, NULL, (SELECT id FROM usuarios WHERE email = 'ana.paredes@correo.com')),
    ('41567890', DATE '1965-04-25', 'HC-000010', 'AB+', 'Mariscos', 'Colesterol alto y antecedentes familiares cardíacos', (SELECT id FROM usuarios WHERE email = 'jorge.medina@correo.com'));

INSERT INTO cupones (activo, codigo, created_at, descripcion, fecha_fin, fecha_inicio, tipo, usos_actuales, usos_maximos, valor) VALUES
    (true, 'BIENVENIDA10', NOW(), '10 % de descuento en tu primera consulta', CURRENT_DATE + 365, CURRENT_DATE - 30, 'PORCENTAJE', 0, NULL, 10.00),
    (true, 'SALUD20', NOW(), 'Campaña de salud preventiva', CURRENT_DATE + 90, CURRENT_DATE - 7, 'PORCENTAJE', 0, 100, 20.00),
    (true, 'VERANO50', NOW(), 'Descuento fijo de verano, ya finalizado', CURRENT_DATE - 30, CURRENT_DATE - 120, 'MONTO_FIJO', 0, NULL, 50.00);

INSERT INTO metodos_pago (created_at, marca, predeterminado, tipo, titular, ultimos4, vencimiento_anio, vencimiento_mes, usuario_id) VALUES
    (NOW(), 'VISA', true, 'TARJETA_CREDITO', 'JUAN PEREZ', '4242', EXTRACT(YEAR FROM CURRENT_DATE)::integer + 3, 12, (SELECT id FROM usuarios WHERE email = 'juan.perez@correo.com')),
    (NOW(), 'MASTERCARD', true, 'TARJETA_CREDITO', 'MARIA RODRIGUEZ', '4444', EXTRACT(YEAR FROM CURRENT_DATE)::integer + 3, 12, (SELECT id FROM usuarios WHERE email = 'maria.rodriguez@correo.com')),
    (NOW(), 'VISA', true, 'TARJETA_CREDITO', 'SOFIA RAMIREZ', '1881', EXTRACT(YEAR FROM CURRENT_DATE)::integer + 3, 12, (SELECT id FROM usuarios WHERE email = 'sofia.ramirez@correo.com'));

CREATE FUNCTION pg_temp.fecha_atencion(dias integer, dias_semana integer[]) RETURNS date AS $$
DECLARE
    fecha date := CURRENT_DATE + dias;
    paso integer := CASE WHEN dias < 0 THEN -1 ELSE 1 END;
BEGIN
    WHILE NOT (EXTRACT(ISODOW FROM fecha)::integer = ANY (dias_semana)) LOOP
        fecha := fecha + paso;
    END LOOP;
    RETURN fecha;
END;
$$ LANGUAGE plpgsql;

INSERT INTO citas (created_at, estado, fecha_hora, monto, motivo, recordatorio_enviado, updated_at, version, medico_id, paciente_id, especialidad_id) VALUES
    (NOW(), 'CONFIRMADA', pg_temp.fecha_atencion(1, ARRAY[1, 2, 3, 4, 5, 6]) + TIME '15:00', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Pediatría'), 'Control pediátrico de su hijo', false, NOW(), 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-67890'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'maria.rodriguez@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Pediatría')),
    (NOW(), 'PENDIENTE', pg_temp.fecha_atencion(1, ARRAY[1, 2, 3, 4, 5]) + TIME '09:00', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Cardiología'), 'Control de presión arterial', false, NOW(), 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-12345'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'juan.perez@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Cardiología')),
    (NOW() - INTERVAL '30 days', 'ATENDIDA', pg_temp.fecha_atencion(-21, ARRAY[1, 2, 3, 4, 5]) + TIME '08:30', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Medicina General'), 'Chequeo general anual', false, NOW() - INTERVAL '30 days', 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-23456'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'sofia.ramirez@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Medicina General')),
    (NOW() - INTERVAL '30 days', 'ATENDIDA', pg_temp.fecha_atencion(-14, ARRAY[1, 2, 3, 4, 5]) + TIME '09:00', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Cardiología'), 'Seguimiento de hipertensión', false, NOW() - INTERVAL '30 days', 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-12345'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'carlos.mendoza@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Cardiología')),
    (NOW() - INTERVAL '30 days', 'ATENDIDA', pg_temp.fecha_atencion(-10, ARRAY[2, 4]) + TIME '10:00', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Dermatología'), 'Control de acné', false, NOW() - INTERVAL '30 days', 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-34567'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'valeria.castillo@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Dermatología')),
    (NOW() - INTERVAL '30 days', 'ATENDIDA', pg_temp.fecha_atencion(-9, ARRAY[1, 3, 5]) + TIME '11:00', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Traumatología'), 'Dolor de rodilla tras una lesión deportiva', false, NOW() - INTERVAL '30 days', 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-45678'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'diego.herrera@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Traumatología')),
    (NOW() - INTERVAL '30 days', 'ATENDIDA', pg_temp.fecha_atencion(-7, ARRAY[1, 2, 3, 4, 5]) + TIME '15:00', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Ginecología'), 'Control ginecológico anual', false, NOW() - INTERVAL '30 days', 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-56789'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'camila.torres@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Ginecología')),
    (NOW() - INTERVAL '30 days', 'ATENDIDA', pg_temp.fecha_atencion(-6, ARRAY[3, 4, 5, 6]) + TIME '09:30', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Oftalmología'), 'Revisión de la vista por fatiga visual', false, NOW() - INTERVAL '30 days', 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-67801'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'luis.vargas@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Oftalmología')),
    (NOW() - INTERVAL '30 days', 'CANCELADA', pg_temp.fecha_atencion(-5, ARRAY[1, 2, 3, 4, 5, 6]) + TIME '14:30', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Pediatría'), 'Control de crecimiento de mi hija', false, NOW() - INTERVAL '30 days', 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-67890'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'ana.paredes@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Pediatría')),
    (NOW() - INTERVAL '30 days', 'CANCELADA', pg_temp.fecha_atencion(-4, ARRAY[1, 2, 3, 4, 5]) + TIME '16:00', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Medicina General'), 'Certificado médico laboral', false, NOW() - INTERVAL '30 days', 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-23456'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'jorge.medina@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Medicina General')),
    (NOW() - INTERVAL '30 days', 'ATENDIDA', pg_temp.fecha_atencion(-3, ARRAY[2, 4]) + TIME '11:30', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Dermatología'), 'Revisión de manchas en la piel', false, NOW() - INTERVAL '30 days', 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-34567'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'sofia.ramirez@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Dermatología')),
    (NOW(), 'CONFIRMADA', pg_temp.fecha_atencion(2, ARRAY[1, 2, 3, 4, 5]) + TIME '10:00', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Cardiología'), 'Palpitaciones ocasionales', false, NOW(), 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-12345'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'sofia.ramirez@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Cardiología')),
    (NOW(), 'PENDIENTE', pg_temp.fecha_atencion(2, ARRAY[1, 2, 3, 4, 5]) + TIME '11:00', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Cardiología'), 'Dolor en el pecho al caminar rápido', false, NOW(), 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-12345'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'camila.torres@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Cardiología')),
    (NOW(), 'CONFIRMADA', pg_temp.fecha_atencion(2, ARRAY[1, 2, 3, 4, 5]) + TIME '12:00', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Cardiología'), 'Chequeo cardiológico preventivo', false, NOW(), 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-12345'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'diego.herrera@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Cardiología')),
    (NOW(), 'PENDIENTE', pg_temp.fecha_atencion(2, ARRAY[1, 2, 3, 4, 5]) + TIME '09:00', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Medicina General'), 'Resfriado persistente', false, NOW(), 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-23456'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'carlos.mendoza@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Medicina General')),
    (NOW(), 'CONFIRMADA', pg_temp.fecha_atencion(3, ARRAY[1, 2, 3, 4, 5]) + TIME '16:00', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Ginecología'), 'Control prenatal', false, NOW(), 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-56789'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'valeria.castillo@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Ginecología')),
    (NOW(), 'PENDIENTE', pg_temp.fecha_atencion(4, ARRAY[3, 4, 5, 6]) + TIME '10:00', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Oftalmología'), 'Cambio de lentes', false, NOW(), 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-67801'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'diego.herrera@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Oftalmología')),
    (NOW(), 'CONFIRMADA', pg_temp.fecha_atencion(5, ARRAY[2, 4]) + TIME '10:30', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Dermatología'), 'Revisión de lunares', false, NOW(), 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-34567'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'camila.torres@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Dermatología')),
    (NOW(), 'PENDIENTE', pg_temp.fecha_atencion(5, ARRAY[1, 3, 5]) + TIME '12:00', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Traumatología'), 'Dolor lumbar persistente', false, NOW(), 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-45678'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'luis.vargas@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Traumatología')),
    (NOW(), 'CONFIRMADA', pg_temp.fecha_atencion(6, ARRAY[1, 2, 3, 4, 5, 6]) + TIME '16:00', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Pediatría'), 'Vacunación de refuerzo', false, NOW(), 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-67890'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'ana.paredes@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Pediatría')),
    (NOW(), 'PENDIENTE', pg_temp.fecha_atencion(7, ARRAY[1, 2, 3, 4, 5]) + TIME '11:00', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Cardiología'), 'Control de colesterol', false, NOW(), 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-12345'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'jorge.medina@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Cardiología')),
    (NOW(), 'PENDIENTE', pg_temp.fecha_atencion(8, ARRAY[1, 2, 3, 4, 5]) + TIME '15:30', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Medicina General'), 'Dolor de garganta', false, NOW(), 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-23456'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'juan.perez@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Medicina General')),
    (NOW(), 'CONFIRMADA', pg_temp.fecha_atencion(9, ARRAY[2, 4]) + TIME '12:00', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Dermatología'), 'Dermatitis en las manos', false, NOW(), 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-34567'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'maria.rodriguez@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Dermatología'));

INSERT INTO atenciones (diagnostico, fecha, frecuencia_cardiaca, indicaciones, peso_kg, presion_arterial, talla_cm, temperatura_c, tratamiento, cita_id, medico_id, paciente_id)
SELECT 'Paciente sana, sin hallazgos patológicos', c.fecha_hora + INTERVAL '25 minutes', 72, 'Mantener actividad física regular y control anual', 62.00, '110/70', 165.0, 36.6, 'No requiere tratamiento', c.id, c.medico_id, c.paciente_id
FROM citas c WHERE c.motivo = 'Chequeo general anual' AND c.estado = 'ATENDIDA';
INSERT INTO atencion_receta (atencion_id, dosis, duracion, frecuencia, medicamento, posicion)
SELECT a.id, '1000 UI', '60 días', '1 vez al día', 'Vitamina D', 0 FROM atenciones a JOIN citas c ON c.id = a.cita_id WHERE c.motivo = 'Chequeo general anual';

INSERT INTO atenciones (diagnostico, fecha, frecuencia_cardiaca, indicaciones, peso_kg, presion_arterial, talla_cm, temperatura_c, tratamiento, cita_id, medico_id, paciente_id)
SELECT 'Hipertensión arterial controlada', c.fecha_hora + INTERVAL '25 minutes', 78, 'Controlar la presión en casa dos veces por semana', 84.50, '130/85', 174.0, 36.5, 'Continuar con enalapril y dieta baja en sodio', c.id, c.medico_id, c.paciente_id
FROM citas c WHERE c.motivo = 'Seguimiento de hipertensión' AND c.estado = 'ATENDIDA';
INSERT INTO atencion_receta (atencion_id, dosis, duracion, frecuencia, medicamento, posicion)
SELECT a.id, '10 mg', '30 días', '1 vez al día', 'Enalapril', 0 FROM atenciones a JOIN citas c ON c.id = a.cita_id WHERE c.motivo = 'Seguimiento de hipertensión';

INSERT INTO atenciones (diagnostico, fecha, frecuencia_cardiaca, indicaciones, peso_kg, presion_arterial, talla_cm, temperatura_c, tratamiento, cita_id, medico_id, paciente_id)
SELECT 'Acné vulgar moderado', c.fecha_hora + INTERVAL '25 minutes', 70, 'Evitar manipular las lesiones y usar protector solar', 58.00, '100/65', 160.0, 36.4, 'Tratamiento tópico y limpieza facial suave', c.id, c.medico_id, c.paciente_id
FROM citas c WHERE c.motivo = 'Control de acné' AND c.estado = 'ATENDIDA';
INSERT INTO atencion_receta (atencion_id, dosis, duracion, frecuencia, medicamento, posicion)
SELECT a.id, 'Capa fina', '8 semanas', 'Por la noche', 'Adapaleno gel 0.1 %', 0 FROM atenciones a JOIN citas c ON c.id = a.cita_id WHERE c.motivo = 'Control de acné';
INSERT INTO atencion_receta (atencion_id, dosis, duracion, frecuencia, medicamento, posicion)
SELECT a.id, 'Aplicar', 'Continuo', '2 veces al día', 'Gel limpiador facial', 1 FROM atenciones a JOIN citas c ON c.id = a.cita_id WHERE c.motivo = 'Control de acné';

INSERT INTO atenciones (diagnostico, fecha, frecuencia_cardiaca, indicaciones, peso_kg, presion_arterial, talla_cm, temperatura_c, tratamiento, cita_id, medico_id, paciente_id)
SELECT 'Esguince de rodilla grado I', c.fecha_hora + INTERVAL '25 minutes', 68, 'Evitar deportes de impacto por 3 semanas', 76.00, '120/78', 178.0, 36.6, 'Reposo relativo, hielo local y fisioterapia', c.id, c.medico_id, c.paciente_id
FROM citas c WHERE c.motivo = 'Dolor de rodilla tras una lesión deportiva' AND c.estado = 'ATENDIDA';
INSERT INTO atencion_receta (atencion_id, dosis, duracion, frecuencia, medicamento, posicion)
SELECT a.id, '550 mg', '5 días', 'Cada 12 horas', 'Naproxeno', 0 FROM atenciones a JOIN citas c ON c.id = a.cita_id WHERE c.motivo = 'Dolor de rodilla tras una lesión deportiva';
INSERT INTO atencion_receta (atencion_id, dosis, duracion, frecuencia, medicamento, posicion)
SELECT a.id, 'Aplicar', '7 días', '3 veces al día', 'Diclofenaco gel', 1 FROM atenciones a JOIN citas c ON c.id = a.cita_id WHERE c.motivo = 'Dolor de rodilla tras una lesión deportiva';

INSERT INTO atenciones (diagnostico, fecha, frecuencia_cardiaca, indicaciones, peso_kg, presion_arterial, talla_cm, temperatura_c, tratamiento, cita_id, medico_id, paciente_id)
SELECT 'Control ginecológico normal', c.fecha_hora + INTERVAL '25 minutes', 74, 'Repetir el Papanicolaou en un año', 55.00, '105/68', 158.0, 36.5, 'No requiere tratamiento', c.id, c.medico_id, c.paciente_id
FROM citas c WHERE c.motivo = 'Control ginecológico anual' AND c.estado = 'ATENDIDA';

INSERT INTO atenciones (diagnostico, fecha, frecuencia_cardiaca, indicaciones, peso_kg, presion_arterial, talla_cm, temperatura_c, tratamiento, cita_id, medico_id, paciente_id)
SELECT 'Fatiga visual por uso prolongado de pantallas', c.fecha_hora + INTERVAL '25 minutes', 75, 'Regla 20-20-20 y revisión de la graduación en 6 meses', 80.00, '125/80', 172.0, 36.6, 'Lágrimas artificiales y pausas visuales', c.id, c.medico_id, c.paciente_id
FROM citas c WHERE c.motivo = 'Revisión de la vista por fatiga visual' AND c.estado = 'ATENDIDA';
INSERT INTO atencion_receta (atencion_id, dosis, duracion, frecuencia, medicamento, posicion)
SELECT a.id, '1 gota por ojo', '15 días', '4 veces al día', 'Lágrimas artificiales', 0 FROM atenciones a JOIN citas c ON c.id = a.cita_id WHERE c.motivo = 'Revisión de la vista por fatiga visual';

INSERT INTO atenciones (diagnostico, fecha, frecuencia_cardiaca, indicaciones, peso_kg, presion_arterial, talla_cm, temperatura_c, tratamiento, cita_id, medico_id, paciente_id)
SELECT 'Léntigos solares benignos', c.fecha_hora + INTERVAL '25 minutes', 71, 'Usar protector solar factor 50 y revisar los lunares cada 6 meses', 62.00, '110/70', 165.0, 36.5, 'Fotoprotección diaria', c.id, c.medico_id, c.paciente_id
FROM citas c WHERE c.motivo = 'Revisión de manchas en la piel' AND c.estado = 'ATENDIDA';
INSERT INTO atencion_receta (atencion_id, dosis, duracion, frecuencia, medicamento, posicion)
SELECT a.id, 'Aplicar', 'Continuo', 'Cada 3 horas al sol', 'Protector solar FPS 50', 0 FROM atenciones a JOIN citas c ON c.id = a.cita_id WHERE c.motivo = 'Revisión de manchas en la piel';

INSERT INTO pagos (codigo_cupon, comprobante, descuento, estado, fecha_pago, metodo_descripcion, monto, total, cita_id, metodo_pago_id)
SELECT b.cupon, 'B001-' || LPAD(b.n::text, 6, '0'), b.descuento, 'PAGADO',
       CASE WHEN b.estado = 'ATENDIDA' THEN b.fecha_hora - INTERVAL '1 day' ELSE NOW() - INTERVAL '2 hours' END,
       COALESCE(mp.marca || ' terminada en ' || mp.ultimos4, 'Efectivo en caja'),
       b.monto, b.monto - b.descuento, b.cita_id, mp.id
FROM (
    SELECT c.id AS cita_id, c.estado, c.monto, c.fecha_hora, c.paciente_id,
           ROW_NUMBER() OVER (ORDER BY c.fecha_hora, c.id) AS n,
           CASE WHEN c.estado = 'ATENDIDA' AND ROW_NUMBER() OVER (PARTITION BY c.estado ORDER BY c.fecha_hora, c.id) <= 2
                THEN 'BIENVENIDA10' END AS cupon,
           CASE WHEN c.estado = 'ATENDIDA' AND ROW_NUMBER() OVER (PARTITION BY c.estado ORDER BY c.fecha_hora, c.id) <= 2
                THEN ROUND(c.monto * 0.10, 2) ELSE 0.00 END AS descuento
    FROM citas c WHERE c.estado IN ('ATENDIDA', 'CONFIRMADA')
) b
JOIN pacientes pa ON pa.id = b.paciente_id
LEFT JOIN metodos_pago mp ON mp.usuario_id = pa.usuario_id AND mp.predeterminado;

INSERT INTO paciente_compartido (paciente_id, medico_origen_id, medico_destino_id, nota, created_at)
SELECT p.id, mo.id, md.id, 'Se solicita valoración complementaria por dolor torácico y control clínico general', NOW() - INTERVAL '2 days'
FROM pacientes p
JOIN usuarios up ON up.id = p.usuario_id AND up.email = 'camila.torres@correo.com'
JOIN medicos mo ON mo.colegiatura = 'CMP-12345'
JOIN medicos md ON md.colegiatura = 'CMP-23456';

INSERT INTO notificaciones (created_at, enlace, leida, mensaje, tipo, titulo, usuario_id)
SELECT NOW() - INTERVAL '2 days', '/pacientes/' || p.id || '/historia-clinica', false,
       'Luis García compartió contigo a Camila Torres.', 'PACIENTE_COMPARTIDO', 'Paciente compartido contigo',
       (SELECT id FROM usuarios WHERE email = 'dr.salazar@tusalud.com')
FROM pacientes p JOIN usuarios up ON up.id = p.usuario_id AND up.email = 'camila.torres@correo.com';

UPDATE cupones SET usos_actuales = (SELECT COUNT(*) FROM pagos WHERE codigo_cupon = 'BIENVENIDA10') WHERE codigo = 'BIENVENIDA10';

INSERT INTO valoraciones (comentario, created_at, puntuacion, cita_id, medico_id, paciente_id)
SELECT 'Muy atento y explicó todo con claridad.', c.fecha_hora + INTERVAL '2 days', 5, c.id, c.medico_id, c.paciente_id
FROM citas c WHERE c.motivo = 'Chequeo general anual' AND c.estado = 'ATENDIDA';
INSERT INTO valoraciones (comentario, created_at, puntuacion, cita_id, medico_id, paciente_id)
SELECT 'Buena atención, aunque esperé un poco.', c.fecha_hora + INTERVAL '2 days', 4, c.id, c.medico_id, c.paciente_id
FROM citas c WHERE c.motivo = 'Seguimiento de hipertensión' AND c.estado = 'ATENDIDA';
INSERT INTO valoraciones (comentario, created_at, puntuacion, cita_id, medico_id, paciente_id)
SELECT 'La doctora fue amable y el tratamiento está funcionando.', c.fecha_hora + INTERVAL '2 days', 5, c.id, c.medico_id, c.paciente_id
FROM citas c WHERE c.motivo = 'Control de acné' AND c.estado = 'ATENDIDA';
INSERT INTO valoraciones (comentario, created_at, puntuacion, cita_id, medico_id, paciente_id)
SELECT 'Excelente diagnóstico y plan de recuperación.', c.fecha_hora + INTERVAL '2 days', 5, c.id, c.medico_id, c.paciente_id
FROM citas c WHERE c.motivo = 'Dolor de rodilla tras una lesión deportiva' AND c.estado = 'ATENDIDA';
INSERT INTO valoraciones (comentario, created_at, puntuacion, cita_id, medico_id, paciente_id)
SELECT NULL, c.fecha_hora + INTERVAL '2 days', 4, c.id, c.medico_id, c.paciente_id
FROM citas c WHERE c.motivo = 'Control ginecológico anual' AND c.estado = 'ATENDIDA';

INSERT INTO notificaciones (created_at, enlace, leida, mensaje, tipo, titulo, usuario_id) VALUES
    (NOW() - INTERVAL '3 hours', '/citas', false, 'Tienes una cita de cardiología pronto. Llega 10 minutos antes.', 'RECORDATORIO_CITA', 'Recordatorio de cita', (SELECT id FROM usuarios WHERE email = 'juan.perez@correo.com')),
    (NOW() - INTERVAL '1 day', '/citas', false, 'Tu cita fue confirmada por el médico.', 'CITA_CONFIRMADA', 'Cita confirmada', (SELECT id FROM usuarios WHERE email = 'juan.perez@correo.com')),
    (NOW() - INTERVAL '2 days', '/perfil', true, 'Se inició sesión desde Chrome en Windows con la IP 192.168.1.20.', 'INICIO_SESION', 'Nuevo inicio de sesión', (SELECT id FROM usuarios WHERE email = 'juan.perez@correo.com')),
    (NOW() - INTERVAL '5 hours', '/agenda', false, 'Un paciente reservó una cita contigo.', 'CITA_CREADA', 'Nueva cita registrada', (SELECT id FROM usuarios WHERE email = 'dr.garcia@tusalud.com')),
    (NOW() - INTERVAL '3 days', '/agenda', true, 'Revisa tu agenda y registra la atención de tus pacientes.', 'SISTEMA', 'Bienvenido a TuSalud', (SELECT id FROM usuarios WHERE email = 'dr.garcia@tusalud.com')),
    (NOW() - INTERVAL '1 hour', '/admin/panel', false, 'Consulta los indicadores de la clínica desde el panel de administración.', 'SISTEMA', 'Panel de control disponible', (SELECT id FROM usuarios WHERE email = 'admin@tusalud.com')),
    (NOW() - INTERVAL '1 day', '/admin/pagos', true, 'Se registraron nuevos pagos de consultas.', 'PAGO_REGISTRADO', 'Pagos del día', (SELECT id FROM usuarios WHERE email = 'admin@tusalud.com'));
