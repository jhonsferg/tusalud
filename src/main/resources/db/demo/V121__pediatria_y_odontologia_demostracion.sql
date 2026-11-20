INSERT INTO especialidades (nombre, descripcion, precio_consulta) VALUES
    ('Odontología', 'Salud bucal, prevención, restauración, endodoncia y cirugía dental', 100.00);

INSERT INTO usuarios (nombre, apellido, email, password, telefono, estado, created_at) VALUES
    ('Camila', 'Montoya', 'dra.montoya@tusalud.com', '$2a$10$LnkKfNnzSze9qUa3qoZp0OyVBKApVqMBHV5vY1k0YhvEC5pGtJqKm', '989012345', 'ACTIVO', NOW() - INTERVAL '45 days'),
    ('Luciana', 'Ramos', 'luciana.ramos@correo.com', '$2a$10$2RaRGJ5Ph7t.ev4VxgQcMuSrWKnv7KcO4XRmGhuTV9jmm81Wq6OjK', '901234501', 'ACTIVO', NOW() - INTERVAL '30 days'),
    ('Mateo', 'Castro', 'mateo.castro@correo.com', '$2a$10$2RaRGJ5Ph7t.ev4VxgQcMuSrWKnv7KcO4XRmGhuTV9jmm81Wq6OjK', '901234502', 'ACTIVO', NOW() - INTERVAL '30 days'),
    ('Valentina', 'Torres', 'valentina.torres@correo.com', '$2a$10$2RaRGJ5Ph7t.ev4VxgQcMuSrWKnv7KcO4XRmGhuTV9jmm81Wq6OjK', '901234503', 'ACTIVO', NOW() - INTERVAL '30 days');

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u JOIN roles r ON r.nombre = 'ROLE_MEDICO'
WHERE u.email = 'dra.montoya@tusalud.com';

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u JOIN roles r ON r.nombre = 'ROLE_PACIENTE'
WHERE u.email IN ('luciana.ramos@correo.com', 'mateo.castro@correo.com', 'valentina.torres@correo.com');

INSERT INTO medicos (colegiatura, consultorio, usuario_id)
SELECT 'COP-78901', 'Consultorio Odontológico 1', u.id FROM usuarios u
WHERE u.email = 'dra.montoya@tusalud.com';

INSERT INTO medico_especialidades (medico_id, especialidad_id)
SELECT m.id, e.id FROM medicos m, especialidades e
WHERE m.colegiatura = 'COP-78901' AND e.nombre = 'Odontología';

INSERT INTO horarios_atencion (dia_semana, duracion_cita_minutos, hora_fin, hora_inicio, medico_id)
SELECT d.dia, 30, TIME '17:00', TIME '09:00', m.id FROM medicos m
CROSS JOIN (VALUES ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY')) AS d(dia)
WHERE m.colegiatura = 'COP-78901';

INSERT INTO pacientes (alergias, antecedentes, dni, fecha_nacimiento, grupo_sanguineo, historial_clinico_numero, usuario_id, sexo)
SELECT v.alergias, v.antecedentes, v.dni, v.nacimiento, v.grupo, v.historia, u.id, v.sexo
FROM usuarios u
JOIN (VALUES
    ('luciana.ramos@correo.com', '78451236', (CURRENT_DATE - INTERVAL '8 months')::date, 'O+', 'HC-000101', 'F', NULL, 'Nacida a término'),
    ('mateo.castro@correo.com', '79124563', (CURRENT_DATE - INTERVAL '4 years 3 months')::date, 'A+', 'HC-000102', 'M', NULL, NULL),
    ('valentina.torres@correo.com', '80236541', (CURRENT_DATE - INTERVAL '10 years 2 months')::date, 'B+', 'HC-000103', 'F', 'Polen', NULL)
) AS v (email, dni, nacimiento, grupo, historia, sexo, alergias, antecedentes) ON v.email = u.email;

INSERT INTO citas (created_at, estado, fecha_hora, monto, motivo, recordatorio_enviado, updated_at, version, medico_id, paciente_id, especialidad_id)
SELECT NOW() - INTERVAL '15 days', v.estado, (CURRENT_DATE + v.dias) + TIME '15:00',
       (SELECT precio_consulta FROM especialidades WHERE nombre = 'Pediatría'), v.motivo, false, NOW() - INTERVAL '15 days', 0,
       (SELECT id FROM medicos WHERE colegiatura = 'CMP-67890'), p.id,
       (SELECT id FROM especialidades WHERE nombre = 'Pediatría')
FROM pacientes p
JOIN usuarios u ON u.id = p.usuario_id
JOIN (VALUES
    ('luciana.ramos@correo.com', 'ATENDIDA', -12, 'Control de crecimiento y vacunas'),
    ('luciana.ramos@correo.com', 'CONFIRMADA', 5, 'Control de crecimiento y vacunas'),
    ('mateo.castro@correo.com', 'ATENDIDA', -20, 'Control del niño sano'),
    ('mateo.castro@correo.com', 'CONFIRMADA', 7, 'Refuerzo de vacunas'),
    ('valentina.torres@correo.com', 'ATENDIDA', -9, 'Control de peso y crecimiento')
) AS v (email, estado, dias, motivo) ON v.email = u.email;

INSERT INTO registros_vacunacion (paciente_id, vacuna_id, numero_dosis, fecha_aplicacion, lote, establecimiento, responsable_id)
SELECT p.id, va.id, v.dosis, p.fecha_nacimiento + v.dias, 'LV-' || v.codigo || '-' || v.dosis, 'TuSalud',
       (SELECT id FROM usuarios WHERE email = 'enfermeria@tusalud.com')
FROM pacientes p
JOIN usuarios u ON u.id = p.usuario_id
JOIN (VALUES
    ('BCG', 1, 1), ('HVB', 1, 0),
    ('PENTA', 1, 62), ('PENTA', 2, 125), ('IPV', 1, 62), ('IPV', 2, 125),
    ('ROTA', 1, 62), ('ROTA', 2, 125), ('NEUMO', 1, 62), ('NEUMO', 2, 125)
) AS v (codigo, dosis, dias) ON true
JOIN vacunas va ON va.codigo = v.codigo
WHERE u.email = 'luciana.ramos@correo.com';

INSERT INTO registros_vacunacion (paciente_id, vacuna_id, numero_dosis, fecha_aplicacion, lote, establecimiento, responsable_id)
SELECT p.id, va.id, v.dosis, p.fecha_nacimiento + v.dias, 'LV-' || v.codigo || '-' || v.dosis, 'TuSalud',
       (SELECT id FROM usuarios WHERE email = 'enfermeria@tusalud.com')
FROM pacientes p
JOIN usuarios u ON u.id = p.usuario_id
JOIN (VALUES
    ('BCG', 1, 1), ('HVB', 1, 0),
    ('PENTA', 1, 62), ('PENTA', 2, 125), ('PENTA', 3, 187),
    ('IPV', 1, 62), ('IPV', 2, 125), ('APO', 1, 187), ('APO', 2, 545),
    ('ROTA', 1, 62), ('ROTA', 2, 125),
    ('NEUMO', 1, 62), ('NEUMO', 2, 125), ('NEUMO', 3, 370),
    ('INFLU', 1, 185), ('INFLU', 2, 215),
    ('SPR', 1, 372), ('SPR', 2, 548), ('VARICELA', 1, 372), ('AMA', 1, 455), ('DPT', 1, 548)
) AS v (codigo, dosis, dias) ON true
JOIN vacunas va ON va.codigo = v.codigo
WHERE u.email IN ('mateo.castro@correo.com', 'valentina.torres@correo.com');

INSERT INTO mediciones_crecimiento (paciente_id, fecha, peso_kg, talla_cm, perimetro_cefalico_cm, registrado_por_id, observaciones)
SELECT p.id, p.fecha_nacimiento + v.dias, v.peso, v.talla, v.pc,
       (SELECT id FROM usuarios WHERE email = 'dra.lopez@tusalud.com'), v.nota
FROM pacientes p
JOIN usuarios u ON u.id = p.usuario_id
JOIN (VALUES
    ('luciana.ramos@correo.com', 0, 3.20, 49.0, 33.8, 'Al nacer'),
    ('luciana.ramos@correo.com', 60, 5.10, 57.0, 38.2, NULL),
    ('luciana.ramos@correo.com', 122, 6.30, 62.5, 40.4, NULL),
    ('luciana.ramos@correo.com', 183, 7.00, 65.8, 42.0, NULL),
    ('luciana.ramos@correo.com', 240, 7.30, 68.0, 43.0, 'Introducción de alimentación complementaria'),
    ('mateo.castro@correo.com', 365, 9.80, 76.0, 46.3, NULL),
    ('mateo.castro@correo.com', 730, 12.40, 87.5, 48.0, NULL),
    ('mateo.castro@correo.com', 1095, 14.60, 96.0, 49.3, NULL),
    ('mateo.castro@correo.com', 1460, 16.40, 103.0, 50.2, NULL),
    ('valentina.torres@correo.com', 2190, 20.50, 115.0, NULL, NULL),
    ('valentina.torres@correo.com', 2920, 26.00, 127.0, NULL, NULL),
    ('valentina.torres@correo.com', 3650, 42.00, 138.0, NULL, 'Aumento rápido de peso, se indica control nutricional')
) AS v (email, dias, peso, talla, pc, nota) ON v.email = u.email;

INSERT INTO consentimiento_plantillas (codigo, nombre, texto, riesgos, beneficios, alternativas, requiere_testigo, vigencia_dias, version, activa) VALUES
    ('EXODONCIA_DENTAL', 'Extracción dental',
     'Se retirará la pieza dental indicada bajo anestesia local. Usted permanecerá despierto y recibirá indicaciones para el cuidado posterior.',
     'Dolor e inflamación posteriores, sangrado, infección, alveolitis, lesión de piezas o nervios vecinos y fractura de la raíz o del hueso.',
     'Eliminar el foco de dolor o de infección y preparar la boca para una rehabilitación.',
     'Tratamiento de conducto y restauración cuando la pieza es recuperable, o seguimiento con tratamiento médico.',
     false, 30, 1, true),
    ('ENDODONCIA_DENTAL', 'Tratamiento de conducto',
     'Se retirará el tejido inflamado o infectado del interior de la pieza dental, se limpiarán los conductos y se sellarán para conservar la pieza.',
     'Dolor posterior, fractura de instrumentos en el conducto, perforación, persistencia de la infección y necesidad de retratamiento o de extracción.',
     'Conservar la pieza dental natural y eliminar la infección y el dolor.',
     'Extracción de la pieza y reposición con puente, implante o prótesis.',
     false, 60, 1, true),
    ('IMPLANTE_DENTAL', 'Implante dental',
     'Se colocará un implante de titanio en el hueso maxilar para reponer una pieza ausente, con anestesia local y una fase de cicatrización antes de la corona.',
     'Infección, falta de integración del implante, sangrado, lesión de nervios o del seno maxilar y necesidad de injerto óseo.',
     'Reposición fija de la pieza ausente con función y estética cercanas a las de una pieza natural.',
     'Prótesis removible, puente fijo sobre piezas vecinas o no reponer la pieza.',
     true, 90, 1, true);

INSERT INTO consentimientos (plantilla_id, plantilla_version, paciente_id, medico_id, procedimiento, estado, texto_aceptado, requiere_testigo,
                             firmante_tipo, firmante_nombre, firmante_dni, creado_en, firmado_en, vence_en)
SELECT pl.id, pl.version, p.id, m.id, 'Extracción de la pieza 18', 'FIRMADO', pl.texto, false,
       'PACIENTE', u.nombre || ' ' || u.apellido, p.dni, NOW() - INTERVAL '6 days', NOW() - INTERVAL '5 days', NOW() + INTERVAL '25 days'
FROM consentimiento_plantillas pl, medicos m, pacientes p
JOIN usuarios u ON u.id = p.usuario_id
WHERE pl.codigo = 'EXODONCIA_DENTAL' AND m.colegiatura = 'COP-78901' AND u.email = 'juan.perez@correo.com';

INSERT INTO citas (created_at, estado, fecha_hora, monto, motivo, recordatorio_enviado, updated_at, version, medico_id, paciente_id, especialidad_id)
SELECT NOW() - INTERVAL '45 days', v.estado, (CURRENT_DATE + v.dias) + TIME '10:00',
       (SELECT precio_consulta FROM especialidades WHERE nombre = 'Odontología'), v.motivo, false, NOW() - INTERVAL '45 days', 0,
       (SELECT id FROM medicos WHERE colegiatura = 'COP-78901'), p.id,
       (SELECT id FROM especialidades WHERE nombre = 'Odontología')
FROM pacientes p
JOIN usuarios u ON u.id = p.usuario_id
JOIN (VALUES
    ('ATENDIDA', -40, 'Evaluación odontológica y limpieza'),
    ('ATENDIDA', -20, 'Obturación de la pieza 16'),
    ('CONFIRMADA', 7, 'Control y continuación del tratamiento')
) AS v (estado, dias, motivo) ON true
WHERE u.email = 'juan.perez@correo.com';

INSERT INTO odontograma_eventos (paciente_id, pieza, superficie, estado, observacion, registrado_por_id, registrado_en)
SELECT p.id, v.pieza, v.superficie, v.estado, v.observacion, (SELECT id FROM usuarios WHERE email = 'dra.montoya@tusalud.com'),
       NOW() - (v.dias * INTERVAL '1 day')
FROM pacientes p
JOIN usuarios u ON u.id = p.usuario_id
JOIN (VALUES
    (11, NULL, 'CORONA', 'Corona de porcelana antigua', 120),
    (46, 'O', 'OBTURADO', 'Resina previa', 100),
    (36, NULL, 'AUSENTE', 'Extraída hace años', 100),
    (16, 'O', 'CARIES', 'Caries oclusal', 40),
    (26, 'M', 'CARIES', 'Caries proximal mesial', 40),
    (16, 'O', 'OBTURADO', 'Obturación con resina', 20),
    (18, NULL, 'EXTRACCION_INDICADA', 'Tercer molar con pericoronaritis', 20)
) AS v (pieza, superficie, estado, observacion, dias) ON true
WHERE u.email = 'juan.perez@correo.com';

INSERT INTO planes_odontologicos (paciente_id, medico_id, estado, notas, total, creado_en, presentado_en, resuelto_en, resuelto_por_id)
SELECT p.id, m.id, 'EN_CURSO', 'Plan de rehabilitación y control', 320.00, NOW() - INTERVAL '8 days', NOW() - INTERVAL '7 days',
       NOW() - INTERVAL '6 days', u.id
FROM pacientes p
JOIN usuarios u ON u.id = p.usuario_id
CROSS JOIN medicos m
WHERE u.email = 'juan.perez@correo.com' AND m.colegiatura = 'COP-78901';

INSERT INTO sesiones_odontologicas (plan_id, medico_id, fecha, notas)
SELECT pl.id, pl.medico_id, NOW() - INTERVAL '5 days', 'Profilaxis realizada sin incidencias'
FROM planes_odontologicos pl
JOIN pacientes p ON p.id = pl.paciente_id
JOIN usuarios u ON u.id = p.usuario_id
WHERE u.email = 'juan.perez@correo.com';

INSERT INTO plan_odontologico_items (plan_id, procedimiento_id, pieza, superficie, precio, estado, posicion, consentimiento_id, sesion_id, realizado_en)
SELECT pl.id, pr.id, v.pieza, v.superficie, pr.precio, v.estado, v.posicion,
       CASE WHEN v.consentimiento THEN (SELECT c.id FROM consentimientos c WHERE c.paciente_id = pl.paciente_id AND c.estado = 'FIRMADO' LIMIT 1) END,
       CASE WHEN v.estado = 'REALIZADO' THEN (SELECT s.id FROM sesiones_odontologicas s WHERE s.plan_id = pl.id LIMIT 1) END,
       CASE WHEN v.estado = 'REALIZADO' THEN NOW() - INTERVAL '5 days' END
FROM planes_odontologicos pl
JOIN pacientes p ON p.id = pl.paciente_id
JOIN usuarios u ON u.id = p.usuario_id
JOIN (VALUES
    ('PROFILAXIS', NULL, NULL, 'REALIZADO', 0, false),
    ('OBTURACION', 26, 'M', 'PENDIENTE', 1, false),
    ('EXODONCIA', 18, NULL, 'PENDIENTE', 2, true)
) AS v (codigo, pieza, superficie, estado, posicion, consentimiento) ON true
JOIN procedimientos_odontologicos pr ON pr.codigo = v.codigo
WHERE u.email = 'juan.perez@correo.com';

INSERT INTO planes_odontologicos_pagos (plan_id, monto, metodo, referencia, sesion_caja_id, registrado_por_id, registrado_en)
SELECT pl.id, 100.00, 'EFECTIVO', NULL, sc.id, sc.cajero_id, NOW() - INTERVAL '5 days'
FROM planes_odontologicos pl
JOIN pacientes p ON p.id = pl.paciente_id
JOIN usuarios u ON u.id = p.usuario_id
CROSS JOIN (SELECT id, cajero_id FROM sesiones_caja ORDER BY apertura LIMIT 1) sc
WHERE u.email = 'juan.perez@correo.com';
