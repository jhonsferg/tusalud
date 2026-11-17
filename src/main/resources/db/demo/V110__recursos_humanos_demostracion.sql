INSERT INTO empleados (usuario_id, cargo, area, tipo_contrato, fecha_ingreso, fecha_fin_contrato, estado, sueldo,
                       contacto_emergencia_nombre, contacto_emergencia_telefono, dias_vacaciones_anuales, created_at, updated_at)
SELECT u.id, v.cargo, v.area, v.contrato, CURRENT_DATE - v.antiguedad, v.fin, 'ACTIVO', v.sueldo,
       v.contacto, v.telefono, 30, NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days'
FROM (VALUES
    ('admin@tusalud.com', 'Administrador general', 'ADMINISTRACION', 'INDEFINIDO', 1400, NULL::date, 7500.00, 'Rosa Gutiérrez', '987000001'),
    ('gerencia@tusalud.com', 'Gerente general', 'GERENCIA', 'INDEFINIDO', 1800, NULL::date, 14000.00, 'Pedro Ríos', '987000002'),
    ('finanzas@tusalud.com', 'Jefe de finanzas', 'FINANZAS', 'INDEFINIDO', 900, NULL::date, 8200.00, 'Lucía Mora', '987000003'),
    ('operaciones@tusalud.com', 'Jefa de operaciones', 'OPERACIONES', 'INDEFINIDO', 800, NULL::date, 8000.00, 'Carlos Cruz', '987000004'),
    ('rrhh@tusalud.com', 'Jefa de recursos humanos', 'RRHH', 'INDEFINIDO', 700, NULL::date, 7600.00, 'Julio Solís', '987000005'),
    ('calidad@tusalud.com', 'Analista de calidad', 'CALIDAD', 'PLAZO_FIJO', 300, CURRENT_DATE + 40, 5200.00, 'Ana Benavides', '987000006'),
    ('dr.garcia@tusalud.com', 'Cardiólogo', 'MEDICA', 'INDEFINIDO', 1500, NULL::date, 12500.00, 'Elena García', '987000007'),
    ('dra.lopez@tusalud.com', 'Pediatra', 'MEDICA', 'INDEFINIDO', 1200, NULL::date, 11800.00, 'Raúl López', '987000008'),
    ('dr.salazar@tusalud.com', 'Médico general', 'MEDICA', 'PLAZO_FIJO', 400, CURRENT_DATE + 25, 9800.00, 'Marta Salazar', '987000009'),
    ('dra.vega@tusalud.com', 'Dermatóloga', 'MEDICA', 'PART_TIME', 500, NULL::date, 6400.00, 'Iván Vega', '987000010'),
    ('dr.quispe@tusalud.com', 'Traumatólogo', 'MEDICA', 'INDEFINIDO', 650, NULL::date, 11200.00, 'Nora Quispe', '987000011'),
    ('dra.fernandez@tusalud.com', 'Ginecóloga', 'MEDICA', 'INDEFINIDO', 600, NULL::date, 11500.00, 'Saúl Fernández', '987000012'),
    ('dr.delgado@tusalud.com', 'Oftalmólogo', 'MEDICA', 'INDEFINIDO', 550, NULL::date, 11000.00, 'Rita Delgado', '987000013'),
    ('lab@tusalud.com', 'Tecnóloga de laboratorio', 'LABORATORIO', 'INDEFINIDO', 500, NULL::date, 4800.00, 'Diego Quispe', '987000014'),
    ('farmacia@tusalud.com', 'Químico farmacéutico', 'FARMACIA', 'INDEFINIDO', 450, NULL::date, 5400.00, 'Olga Medina', '987000015'),
    ('almacen@tusalud.com', 'Encargado de almacén', 'ALMACEN', 'PLAZO_FIJO', 350, CURRENT_DATE + 20, 3600.00, 'Hugo Ramos', '987000016'),
    ('recepcion@tusalud.com', 'Recepcionista', 'RECEPCION', 'INDEFINIDO', 420, NULL::date, 2800.00, 'Mónica Núñez', '987000017'),
    ('caja@tusalud.com', 'Cajero', 'CAJA', 'INDEFINIDO', 380, NULL::date, 3000.00, 'Elsa Paredes', '987000018'),
    ('enfermeria@tusalud.com', 'Enfermera jefa', 'ENFERMERIA', 'INDEFINIDO', 520, NULL::date, 5200.00, 'Tomás Campos', '987000019')
) AS v (email, cargo, area, contrato, antiguedad, fin, sueldo, contacto, telefono)
JOIN usuarios u ON u.email = v.email;

INSERT INTO turnos_personal (empleado_id, plantilla_id, fecha, notas, creado_por, created_at)
SELECT e.id, p.id, (date_trunc('week', CURRENT_DATE)::date + d.dia), NULL, rrhh.id, NOW()
FROM (VALUES
    ('enfermeria@tusalud.com', 'Mañana'),
    ('recepcion@tusalud.com', 'Mañana'),
    ('lab@tusalud.com', 'Mañana'),
    ('caja@tusalud.com', 'Tarde'),
    ('farmacia@tusalud.com', 'Tarde')) AS v (email, plantilla)
CROSS JOIN (VALUES (0), (1), (2), (3), (4)) AS d (dia)
JOIN usuarios u ON u.email = v.email
JOIN empleados e ON e.usuario_id = u.id
JOIN plantillas_turno p ON p.nombre = v.plantilla
JOIN usuarios rrhh ON rrhh.email = 'rrhh@tusalud.com';

INSERT INTO ausencias (empleado_id, tipo, fecha_inicio, fecha_fin, dias, motivo, estado, resuelto_por, resuelto_en,
                       motivo_resolucion, created_at)
SELECT e.id, v.tipo, CURRENT_DATE + v.inicio, CURRENT_DATE + v.fin, (v.fin - v.inicio + 1), v.motivo, v.estado,
       CASE WHEN v.estado = 'SOLICITADA' THEN NULL ELSE rrhh.id END,
       CASE WHEN v.estado = 'SOLICITADA' THEN NULL ELSE NOW() - INTERVAL '2 days' END,
       v.resolucion, NOW() - INTERVAL '5 days'
FROM (VALUES
    ('almacen@tusalud.com', 'DESCANSO_MEDICO', -1, 1, 'Reposo indicado por el médico', 'APROBADA', 'Certificado presentado'),
    ('enfermeria@tusalud.com', 'VACACIONES', 14, 20, 'Vacaciones anuales', 'APROBADA', 'Aprobadas según el rol de descansos'),
    ('recepcion@tusalud.com', 'PERMISO', 3, 3, 'Trámite personal', 'SOLICITADA', NULL),
    ('gerencia@tusalud.com', 'VACACIONES', 30, 39, 'Vacaciones familiares', 'SOLICITADA', NULL),
    ('caja@tusalud.com', 'PERMISO', 5, 5, 'Cita médica personal', 'RECHAZADA', 'Cobertura insuficiente ese día')
) AS v (email, tipo, inicio, fin, motivo, estado, resolucion)
JOIN usuarios u ON u.email = v.email
JOIN empleados e ON e.usuario_id = u.id
JOIN usuarios rrhh ON rrhh.email = 'rrhh@tusalud.com';
