INSERT INTO citas (created_at, estado, fecha_hora, monto, motivo, recordatorio_enviado, updated_at, version, medico_id, paciente_id, especialidad_id) VALUES
    (NOW() - INTERVAL '3 days', 'CONFIRMADA', CURRENT_DATE + TIME '09:00', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Cardiología'), 'Control de presión arterial de rutina', false, NOW(), 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-12345'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'jorge.medina@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Cardiología')),
    (NOW() - INTERVAL '3 days', 'CONFIRMADA', CURRENT_DATE + TIME '10:00', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Pediatría'), 'Control pediátrico de su hija', false, NOW(), 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-67890'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'ana.paredes@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Pediatría')),
    (NOW() - INTERVAL '4 days', 'ATENDIDA', CURRENT_DATE + TIME '08:00', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Ginecología'), 'Control ginecológico', false, NOW(), 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-56789'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'camila.torres@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Ginecología')),
    (NOW() - INTERVAL '4 days', 'CONFIRMADA', CURRENT_DATE + TIME '08:30', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Oftalmología'), 'Cambio de lentes', false, NOW(), 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-67801'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'luis.vargas@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Oftalmología')),
    (NOW() - INTERVAL '2 days', 'PENDIENTE', CURRENT_DATE + TIME '11:00', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Dermatología'), 'Revisión de lunares', false, NOW(), 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-34567'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'valeria.castillo@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Dermatología')),
    (NOW() - INTERVAL '2 days', 'CONFIRMADA', CURRENT_DATE + TIME '12:00', (SELECT precio_consulta FROM especialidades WHERE nombre = 'Medicina General'), 'Resfriado persistente', false, NOW(), 0, (SELECT id FROM medicos WHERE colegiatura = 'CMP-23456'), (SELECT p.id FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'sofia.ramirez@correo.com'), (SELECT id FROM especialidades WHERE nombre = 'Medicina General'));

INSERT INTO turnos (numero, secuencia, fecha, prioridad, estado, motivo, consultorio, paciente_id, cita_id, medico_id, creado_en, llamado_en, inicio_atencion_en, fin_atencion_en)
SELECT v.numero, v.secuencia, CURRENT_DATE, v.prioridad, v.estado, c.motivo, m.consultorio, c.paciente_id, c.id, c.medico_id, (NOW() - v.llegada)::timestamp, v.llamado, v.inicio, v.fin
FROM (VALUES
    ('A-001', 1, 'NORMAL', 'EN_ESPERA', 'jorge.medina@correo.com', INTERVAL '20 minutes', NULL::timestamp, NULL::timestamp, NULL::timestamp),
    ('A-002', 2, 'NORMAL', 'LLAMADO', 'ana.paredes@correo.com', INTERVAL '35 minutes', (NOW() - INTERVAL '5 minutes')::timestamp, NULL::timestamp, NULL::timestamp),
    ('A-003', 3, 'NORMAL', 'ATENDIDO', 'camila.torres@correo.com', INTERVAL '90 minutes', (NOW() - INTERVAL '75 minutes')::timestamp, (NOW() - INTERVAL '70 minutes')::timestamp, (NOW() - INTERVAL '45 minutes')::timestamp),
    ('A-004', 4, 'NORMAL', 'AUSENTE', 'luis.vargas@correo.com', INTERVAL '60 minutes', (NOW() - INTERVAL '50 minutes')::timestamp, NULL::timestamp, NULL::timestamp)
) AS v(numero, secuencia, prioridad, estado, email, llegada, llamado, inicio, fin)
JOIN usuarios u ON u.email = v.email
JOIN pacientes p ON p.usuario_id = u.id
JOIN citas c ON c.paciente_id = p.id AND c.fecha_hora::date = CURRENT_DATE
JOIN medicos m ON m.id = c.medico_id;

INSERT INTO turnos (numero, secuencia, fecha, prioridad, estado, motivo, consultorio, paciente_id, cita_id, medico_id, creado_en, llamado_en, inicio_atencion_en, fin_atencion_en)
SELECT 'P-001', 1, CURRENT_DATE, 'PREFERENTE', 'EN_ESPERA', 'Dolor intenso en la rodilla', m.consultorio, p.id, NULL::uuid,
       m.id, (NOW() - INTERVAL '10 minutes')::timestamp, NULL::timestamp, NULL::timestamp, NULL::timestamp
FROM pacientes p
JOIN usuarios u ON u.id = p.usuario_id
JOIN medicos m ON m.colegiatura = 'CMP-45678'
WHERE u.email = 'diego.herrera@correo.com';
