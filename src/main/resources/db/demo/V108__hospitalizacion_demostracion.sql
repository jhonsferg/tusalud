INSERT INTO camas (codigo, area, piso, descripcion, estado, precio_dia, activa) VALUES
    ('H-101', 'HOSPITALIZACION', 1, 'Habitación compartida con dos camas', 'LIBRE', 150.00, true),
    ('H-102', 'HOSPITALIZACION', 1, 'Habitación compartida con dos camas', 'LIMPIEZA', 150.00, true),
    ('H-103', 'HOSPITALIZACION', 1, 'Habitación individual en revisión de instalaciones', 'MANTENIMIENTO', 170.00, true),
    ('H-104', 'HOSPITALIZACION', 1, 'Habitación individual con baño', 'OCUPADA', 170.00, true),
    ('H-201', 'HOSPITALIZACION', 2, 'Habitación individual con monitor cardiaco', 'OCUPADA', 180.00, true),
    ('H-202', 'HOSPITALIZACION', 2, 'Habitación individual con baño', 'LIBRE', 180.00, true),
    ('H-203', 'HOSPITALIZACION', 2, 'Suite con sala de visitas', 'LIBRE', 260.00, true),
    ('UCI-01', 'UCI', 3, 'Unidad de cuidados intensivos con ventilador', 'LIBRE', 650.00, true),
    ('UCI-02', 'UCI', 3, 'Unidad de cuidados intensivos con ventilador', 'LIBRE', 650.00, true),
    ('EMG-01', 'EMERGENCIA', 1, 'Box de reanimación', 'LIBRE', 250.00, true),
    ('EMG-02', 'EMERGENCIA', 1, 'Box de atención', 'LIBRE', 250.00, true),
    ('EMG-03', 'EMERGENCIA', 1, 'Box de atención', 'LIBRE', 250.00, true),
    ('OBS-01', 'OBSERVACION', 1, 'Camilla de observación', 'LIBRE', 120.00, true),
    ('OBS-02', 'OBSERVACION', 1, 'Camilla de observación', 'LIBRE', 120.00, true);

INSERT INTO ingresos_hospitalarios (codigo, tipo, estado, paciente_id, cama_id, medico_id, motivo, diagnostico_ingreso, fecha_ingreso, fecha_alta, diagnostico_alta, indicaciones_alta, cargo_acumulado, desde_en_cama, created_at, updated_at)
SELECT 'HOS-000001', 'PROGRAMADO', 'ACTIVO', p.id, c.id, m.id,
       'Descompensación de insuficiencia cardiaca con disnea y edema en piernas',
       'Insuficiencia cardiaca congestiva',
       NOW() - INTERVAL '2 days', NULL, NULL, NULL, 0.00, NOW() - INTERVAL '2 days', NOW(), NOW()
FROM pacientes p JOIN usuarios pu ON pu.id = p.usuario_id, camas c, medicos m JOIN usuarios mu ON mu.id = m.usuario_id
WHERE pu.email = 'jorge.medina@correo.com' AND c.codigo = 'H-201' AND mu.email = 'dr.garcia@tusalud.com';

INSERT INTO ingresos_hospitalarios (codigo, tipo, estado, paciente_id, cama_id, medico_id, motivo, diagnostico_ingreso, fecha_ingreso, fecha_alta, diagnostico_alta, indicaciones_alta, cargo_acumulado, desde_en_cama, created_at, updated_at)
SELECT 'HOS-000002', 'PROGRAMADO', 'ACTIVO', p.id, c.id, m.id,
       'Cirugía programada de fractura de tibia izquierda',
       'Fractura diafisaria de tibia, postoperatorio inmediato',
       NOW() - INTERVAL '1 day', NULL, NULL, NULL, 0.00, NOW() - INTERVAL '1 day', NOW(), NOW()
FROM pacientes p JOIN usuarios pu ON pu.id = p.usuario_id, camas c, medicos m JOIN usuarios mu ON mu.id = m.usuario_id
WHERE pu.email = 'luis.vargas@correo.com' AND c.codigo = 'H-104' AND mu.email = 'dr.quispe@tusalud.com';

INSERT INTO ingresos_hospitalarios (codigo, tipo, estado, paciente_id, cama_id, medico_id, motivo, diagnostico_ingreso, fecha_ingreso, fecha_alta, diagnostico_alta, indicaciones_alta, cargo_acumulado, desde_en_cama, created_at, updated_at,
    triaje_nivel, triaje_presion_arterial, triaje_frecuencia_cardiaca, triaje_frecuencia_respiratoria, triaje_temperatura, triaje_saturacion_o2, triaje_glasgow, triaje_motivo_consulta, triaje_registrado_por_id, triaje_fecha)
SELECT 'HOS-000003', 'EMERGENCIA', 'ACTIVO', p.id, NULL, NULL,
       'Dolor torácico opresivo de inicio súbito', NULL,
       NOW() - INTERVAL '15 minutes', NULL, NULL, NULL, 0.00, NULL, NOW(), NOW(),
       2, '150/95', 108, 22, 37.2, 94, 15, 'Dolor torácico opresivo de inicio súbito', e.id, NOW() - INTERVAL '15 minutes'
FROM pacientes p JOIN usuarios pu ON pu.id = p.usuario_id, usuarios e
WHERE pu.email = 'diego.herrera@correo.com' AND e.email = 'enfermeria@tusalud.com';

INSERT INTO ingresos_hospitalarios (codigo, tipo, estado, paciente_id, cama_id, medico_id, motivo, diagnostico_ingreso, fecha_ingreso, fecha_alta, diagnostico_alta, indicaciones_alta, cargo_acumulado, desde_en_cama, created_at, updated_at,
    triaje_nivel, triaje_presion_arterial, triaje_frecuencia_cardiaca, triaje_frecuencia_respiratoria, triaje_temperatura, triaje_saturacion_o2, triaje_glasgow, triaje_motivo_consulta, triaje_registrado_por_id, triaje_fecha)
SELECT 'HOS-000004', 'EMERGENCIA', 'ACTIVO', p.id, NULL, NULL,
       'Esguince de tobillo derecho tras una caída', NULL,
       NOW() - INTERVAL '40 minutes', NULL, NULL, NULL, 0.00, NULL, NOW(), NOW(),
       4, '120/78', 82, 16, 36.6, 99, 15, 'Esguince de tobillo derecho tras una caída', e.id, NOW() - INTERVAL '40 minutes'
FROM pacientes p JOIN usuarios pu ON pu.id = p.usuario_id, usuarios e
WHERE pu.email = 'camila.torres@correo.com' AND e.email = 'enfermeria@tusalud.com';

INSERT INTO ingresos_hospitalarios (codigo, tipo, estado, paciente_id, cama_id, medico_id, motivo, diagnostico_ingreso, fecha_ingreso, fecha_alta, diagnostico_alta, indicaciones_alta, cargo_acumulado, desde_en_cama, created_at, updated_at)
SELECT 'HOS-000005', 'PROGRAMADO', 'ALTA', p.id, c.id, m.id,
       'Observación por deshidratación y gastroenteritis aguda',
       'Gastroenteritis aguda con deshidratación moderada',
       NOW() - INTERVAL '8 days', NOW() - INTERVAL '5 days',
       'Gastroenteritis resuelta',
       'Dieta blanda por 5 días, hidratación oral abundante y control en consulta externa en 7 días',
       450.00, NULL, NOW() - INTERVAL '8 days', NOW() - INTERVAL '5 days'
FROM pacientes p JOIN usuarios pu ON pu.id = p.usuario_id, camas c, medicos m JOIN usuarios mu ON mu.id = m.usuario_id
WHERE pu.email = 'sofia.ramirez@correo.com' AND c.codigo = 'H-102' AND mu.email = 'dr.salazar@tusalud.com';

INSERT INTO evoluciones_hospitalarias (ingreso_id, tipo, nota, presion_arterial, frecuencia_cardiaca, temperatura, saturacion_o2, autor_id, fecha)
SELECT i.id, 'MEDICA', 'Ingresa con disnea de esfuerzo y edema en miembros inferiores. Se inicia diurético endovenoso y restricción de sal.', '135/85', 92, 36.7, 93, u.id, NOW() - INTERVAL '2 days' + INTERVAL '30 minutes'
FROM ingresos_hospitalarios i, usuarios u WHERE i.codigo = 'HOS-000001' AND u.email = 'dr.garcia@tusalud.com';

INSERT INTO evoluciones_hospitalarias (ingreso_id, tipo, nota, presion_arterial, frecuencia_cardiaca, temperatura, saturacion_o2, autor_id, fecha)
SELECT i.id, 'ENFERMERIA', 'Control de signos vitales sin cambios. Balance hídrico negativo, diuresis adecuada. Paciente tolera la dieta.', '130/82', 84, 36.5, 95, u.id, NOW() - INTERVAL '1 day'
FROM ingresos_hospitalarios i, usuarios u WHERE i.codigo = 'HOS-000001' AND u.email = 'enfermeria@tusalud.com';

INSERT INTO evoluciones_hospitalarias (ingreso_id, tipo, nota, presion_arterial, frecuencia_cardiaca, temperatura, saturacion_o2, autor_id, fecha)
SELECT i.id, 'MEDICA', 'Mejoría clínica, disnea ausente en reposo y edema en regresión. Se continúa tratamiento y se evalúa alta en 48 horas.', '125/80', 78, 36.6, 97, u.id, NOW() - INTERVAL '4 hours'
FROM ingresos_hospitalarios i, usuarios u WHERE i.codigo = 'HOS-000001' AND u.email = 'dr.garcia@tusalud.com';

INSERT INTO evoluciones_hospitalarias (ingreso_id, tipo, nota, presion_arterial, frecuencia_cardiaca, temperatura, saturacion_o2, autor_id, fecha)
SELECT i.id, 'ENFERMERIA', 'Postoperatorio sin sangrado, dolor controlado con analgesia programada. Pulsos distales presentes.', '118/74', 76, 36.4, 98, u.id, NOW() - INTERVAL '6 hours'
FROM ingresos_hospitalarios i, usuarios u WHERE i.codigo = 'HOS-000002' AND u.email = 'enfermeria@tusalud.com';

INSERT INTO evoluciones_hospitalarias (ingreso_id, tipo, nota, presion_arterial, frecuencia_cardiaca, temperatura, saturacion_o2, autor_id, fecha)
SELECT i.id, 'MEDICA', 'Cirugía sin complicaciones con osteosíntesis de tibia. Se indica movilización asistida y profilaxis antitrombótica.', '120/76', 74, 36.5, 98, u.id, NOW() - INTERVAL '20 hours'
FROM ingresos_hospitalarios i, usuarios u WHERE i.codigo = 'HOS-000002' AND u.email = 'dr.quispe@tusalud.com';

INSERT INTO evoluciones_hospitalarias (ingreso_id, tipo, nota, presion_arterial, frecuencia_cardiaca, temperatura, saturacion_o2, autor_id, fecha)
SELECT i.id, 'MEDICA', 'Tolera la vía oral y la diarrea cesó. Se suspende la hidratación endovenosa y se programa el alta.', '110/70', 72, 36.6, 99, u.id, NOW() - INTERVAL '6 days'
FROM ingresos_hospitalarios i, usuarios u WHERE i.codigo = 'HOS-000005' AND u.email = 'dr.salazar@tusalud.com';
