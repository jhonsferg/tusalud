INSERT INTO aseguradoras (codigo, nombre, tipo, ruc, contacto, activa) VALUES
    ('SIS', 'Seguro Integral de Salud', 'SIS', '20999000011', 'convenios@sis.demo.pe', true),
    ('ESSALUD', 'EsSalud Convenios', 'ESSALUD', '20999000022', 'convenios@essalud.demo.pe', true),
    ('PACIFICO', 'Pacífico EPS', 'EPS', '20999000033', 'cobranzas@pacifico.demo.pe', true),
    ('RIMAC', 'Rímac Seguros', 'PRIVADO', '20999000044', 'liquidaciones@rimac.demo.pe', true);

INSERT INTO convenios (aseguradora_id, nombre, vigencia_desde, vigencia_hasta, tope_anual, activo) VALUES
    ((SELECT id FROM aseguradoras WHERE codigo = 'SIS'), 'Convenio SIS atención ambulatoria', DATE_TRUNC('year', CURRENT_DATE)::date, (DATE_TRUNC('year', CURRENT_DATE) + INTERVAL '1 year - 1 day')::date, NULL, true),
    ((SELECT id FROM aseguradoras WHERE codigo = 'ESSALUD'), 'Convenio EsSalud complementario', DATE_TRUNC('year', CURRENT_DATE)::date, (DATE_TRUNC('year', CURRENT_DATE) + INTERVAL '1 year - 1 day')::date, 30000.00, true),
    ((SELECT id FROM aseguradoras WHERE codigo = 'PACIFICO'), 'Plan Integral Pacífico EPS', DATE_TRUNC('year', CURRENT_DATE)::date, (DATE_TRUNC('year', CURRENT_DATE) + INTERVAL '1 year - 1 day')::date, 20000.00, true),
    ((SELECT id FROM aseguradoras WHERE codigo = 'RIMAC'), 'Plan Salud Total Rímac', DATE_TRUNC('year', CURRENT_DATE)::date, (DATE_TRUNC('year', CURRENT_DATE) + INTERVAL '1 year - 1 day')::date, 15000.00, true);

INSERT INTO reglas_cobertura (convenio_id, categoria, tipo, valor, tope_por_servicio, requiere_preautorizacion)
SELECT c.id, r.categoria, r.tipo, r.valor, r.tope, r.preaut
FROM convenios c
JOIN (VALUES
    ('Convenio SIS atención ambulatoria', 'CONSULTA', 'PORCENTAJE', 90.00, NULL::numeric, false),
    ('Convenio SIS atención ambulatoria', 'LABORATORIO', 'PORCENTAJE', 90.00, NULL::numeric, false),
    ('Convenio SIS atención ambulatoria', 'IMAGEN', 'PORCENTAJE', 80.00, 500.00, true),
    ('Convenio EsSalud complementario', 'CONSULTA', 'PORCENTAJE', 95.00, NULL::numeric, false),
    ('Convenio EsSalud complementario', 'LABORATORIO', 'PORCENTAJE', 95.00, NULL::numeric, false),
    ('Convenio EsSalud complementario', 'IMAGEN', 'PORCENTAJE', 90.00, NULL::numeric, true),
    ('Convenio EsSalud complementario', 'HOSPITALIZACION', 'PORCENTAJE', 90.00, NULL::numeric, true),
    ('Plan Integral Pacífico EPS', 'CONSULTA', 'COPAGO_FIJO', 30.00, NULL::numeric, false),
    ('Plan Integral Pacífico EPS', 'LABORATORIO', 'PORCENTAJE', 80.00, NULL::numeric, false),
    ('Plan Integral Pacífico EPS', 'IMAGEN', 'PORCENTAJE', 70.00, 800.00, true),
    ('Plan Integral Pacífico EPS', 'HOSPITALIZACION', 'PORCENTAJE', 80.00, NULL::numeric, true),
    ('Plan Integral Pacífico EPS', 'FARMACIA', 'PORCENTAJE', 60.00, NULL::numeric, false),
    ('Plan Salud Total Rímac', 'CONSULTA', 'PORCENTAJE', 80.00, NULL::numeric, false),
    ('Plan Salud Total Rímac', 'LABORATORIO', 'PORCENTAJE', 80.00, NULL::numeric, false),
    ('Plan Salud Total Rímac', 'IMAGEN', 'PORCENTAJE', 80.00, NULL::numeric, true),
    ('Plan Salud Total Rímac', 'HOSPITALIZACION', 'PORCENTAJE', 80.00, NULL::numeric, true)
) AS r (convenio, categoria, tipo, valor, tope, preaut) ON r.convenio = c.nombre;

INSERT INTO convenio_exclusiones (convenio_id, codigo)
SELECT id, 'ECO-MAMA' FROM convenios WHERE nombre = 'Plan Integral Pacífico EPS';

INSERT INTO polizas (paciente_id, convenio_id, numero, titular_nombre, parentesco, plan, vigencia_desde, vigencia_hasta, estado, motivo_suspension, registrada_por, creada_en)
SELECT p.id, c.id, v.numero, v.titular, v.parentesco, v.plan, v.desde, v.hasta, v.estado, v.motivo,
       (SELECT id FROM usuarios WHERE email = 'recepcion@tusalud.com'), NOW() - INTERVAL '30 days'
FROM (VALUES
    ('camila.torres@correo.com', 'Plan Integral Pacífico EPS', 'PAC-100234', 'Camila Torres', 'TITULAR', 'Plan Integral', DATE_TRUNC('year', CURRENT_DATE)::date, (DATE_TRUNC('year', CURRENT_DATE) + INTERVAL '1 year - 1 day')::date, 'ACTIVA', NULL),
    ('luis.vargas@correo.com', 'Convenio SIS atención ambulatoria', 'SIS-778821', 'Luis Vargas', 'TITULAR', 'SIS Gratuito', DATE_TRUNC('year', CURRENT_DATE)::date, (DATE_TRUNC('year', CURRENT_DATE) + INTERVAL '1 year - 1 day')::date, 'ACTIVA', NULL),
    ('ana.paredes@correo.com', 'Plan Salud Total Rímac', 'RIM-554120', 'Ana Paredes', 'TITULAR', 'Salud Total', DATE_TRUNC('year', CURRENT_DATE)::date, CURRENT_DATE + 20, 'ACTIVA', NULL),
    ('maria.rodriguez@correo.com', 'Convenio EsSalud complementario', 'ESS-330871', 'Rosa Rodríguez', 'MADRE', 'Complementario', DATE_TRUNC('year', CURRENT_DATE)::date, (DATE_TRUNC('year', CURRENT_DATE) + INTERVAL '1 year - 1 day')::date, 'ACTIVA', NULL),
    ('valeria.castillo@correo.com', 'Plan Salud Total Rímac', 'RIM-221907', 'Valeria Castillo', 'TITULAR', 'Salud Total', CURRENT_DATE - 385, CURRENT_DATE - 20, 'ACTIVA', NULL),
    ('diego.herrera@correo.com', 'Convenio SIS atención ambulatoria', 'SIS-990124', 'Diego Herrera', 'TITULAR', 'SIS Gratuito', DATE_TRUNC('year', CURRENT_DATE)::date, (DATE_TRUNC('year', CURRENT_DATE) + INTERVAL '1 year - 1 day')::date, 'SUSPENDIDA', 'Afiliación en revisión por la aseguradora')
) AS v (email, convenio, numero, titular, parentesco, plan, desde, hasta, estado, motivo)
JOIN usuarios u ON u.email = v.email
JOIN pacientes p ON p.usuario_id = u.id
JOIN convenios c ON c.nombre = v.convenio;

INSERT INTO preautorizaciones (codigo, paciente_id, poliza_id, categoria, referencia_id, descripcion, monto_estimado, estado, solicitada_por, solicitada_en, gestionada_por, gestionada_en, codigo_autorizacion, monto_aprobado, vigente_hasta, motivo_rechazo)
SELECT v.codigo, p.id, po.id, v.categoria, NULL, v.descripcion, v.monto, v.estado,
       (SELECT id FROM usuarios WHERE email = 'dr.garcia@tusalud.com'), NOW() - INTERVAL '3 days',
       (SELECT id FROM usuarios WHERE email = 'finanzas@tusalud.com'), NOW() - INTERVAL '2 days',
       v.autorizacion, v.aprobado, v.vigente, v.rechazo
FROM (VALUES
    ('PA-DEMO-00001', 'camila.torres@correo.com', 'PAC-100234', 'IMAGEN', 'Resonancia magnética de rodilla derecha', 650.00, 'APROBADA', 'AUT-7F3A21C9B0', 600.00, CURRENT_DATE + 20, NULL),
    ('PA-DEMO-00002', 'ana.paredes@correo.com', 'RIM-554120', 'HOSPITALIZACION', 'Internación programada para cirugía electiva', 4500.00, 'RECHAZADA', NULL, NULL, NULL, 'El plan no cubre cirugías electivas sin evaluación previa de la aseguradora')
) AS v (codigo, email, poliza, categoria, descripcion, monto, estado, autorizacion, aprobado, vigente, rechazo)
JOIN usuarios u ON u.email = v.email
JOIN pacientes p ON p.usuario_id = u.id
JOIN polizas po ON po.numero = v.poliza AND po.paciente_id = p.id;

INSERT INTO cuentas_aseguradora (aseguradora_id, convenio_id, comprobante_id, paciente_id, categoria, origen, referencia_id, descripcion, monto_total, monto_cubierto, copago, poliza_numero, fecha, estado)
SELECT a.id, c.id, NULL, p.id, v.categoria, v.origen, NULL, v.descripcion, v.total, v.cubierto, v.total - v.cubierto, v.poliza,
       NOW() - (v.dias || ' days')::interval, 'PENDIENTE'
FROM (VALUES
    ('camila.torres@correo.com', 'PAC-100234', 'PACIFICO', 'CONSULTA', 'CITA', 'Consulta de Cardiología con Luis García', 150.00, 120.00, 5),
    ('camila.torres@correo.com', 'PAC-100234', 'PACIFICO', 'LABORATORIO', 'LABORATORIO', 'Perfil lipídico y glucosa', 95.00, 76.00, 42),
    ('camila.torres@correo.com', 'PAC-100234', 'PACIFICO', 'CONSULTA', 'CITA', 'Consulta de Medicina General', 100.00, 70.00, 78),
    ('ana.paredes@correo.com', 'RIM-554120', 'RIMAC', 'CONSULTA', 'CITA', 'Consulta de Ginecología', 160.00, 128.00, 100),
    ('ana.paredes@correo.com', 'RIM-554120', 'RIMAC', 'IMAGEN', 'IMAGEN', 'Ecografía pélvica', 120.00, 96.00, 22),
    ('luis.vargas@correo.com', 'SIS-778821', 'SIS', 'CONSULTA', 'CITA', 'Consulta de Pediatría', 120.00, 108.00, 10),
    ('luis.vargas@correo.com', 'SIS-778821', 'SIS', 'LABORATORIO', 'LABORATORIO', 'Hemograma completo', 45.00, 40.50, 65),
    ('maria.rodriguez@correo.com', 'ESS-330871', 'ESSALUD', 'CONSULTA', 'CITA', 'Consulta de Cardiología', 150.00, 142.50, 50)
) AS v (email, poliza, aseguradora, categoria, origen, descripcion, total, cubierto, dias)
JOIN usuarios u ON u.email = v.email
JOIN pacientes p ON p.usuario_id = u.id
JOIN polizas po ON po.numero = v.poliza AND po.paciente_id = p.id
JOIN convenios c ON c.id = po.convenio_id
JOIN aseguradoras a ON a.codigo = v.aseguradora;

INSERT INTO liquidaciones (codigo, aseguradora_id, desde, hasta, total, cantidad_cuentas, estado, creada_por, creada_en, enviada_en, pagada_en, monto_pagado, monto_glosa, motivo_glosa)
SELECT v.codigo, a.id, v.desde, v.hasta, 0, 0, v.estado,
       (SELECT id FROM usuarios WHERE email = 'finanzas@tusalud.com'), NOW() - INTERVAL '8 days', NOW() - INTERVAL '7 days',
       CASE WHEN v.estado = 'PAGADA' THEN NOW() - INTERVAL '2 days' END, NULL, NULL, NULL
FROM (VALUES
    ('LQ-DEMO-001', 'RIMAC', 'ENVIADA', CURRENT_DATE - 120, CURRENT_DATE - 90),
    ('LQ-DEMO-002', 'SIS', 'PAGADA', CURRENT_DATE - 70, CURRENT_DATE - 60)
) AS v (codigo, aseguradora, estado, desde, hasta)
JOIN aseguradoras a ON a.codigo = v.aseguradora;

UPDATE cuentas_aseguradora SET estado = 'LIQUIDADA', liquidacion_id = (SELECT id FROM liquidaciones WHERE codigo = 'LQ-DEMO-001')
WHERE aseguradora_id = (SELECT id FROM aseguradoras WHERE codigo = 'RIMAC') AND fecha < NOW() - INTERVAL '90 days';

UPDATE cuentas_aseguradora SET estado = 'PAGADA', liquidacion_id = (SELECT id FROM liquidaciones WHERE codigo = 'LQ-DEMO-002')
WHERE aseguradora_id = (SELECT id FROM aseguradoras WHERE codigo = 'SIS') AND fecha < NOW() - INTERVAL '60 days';

UPDATE liquidaciones l SET
    total = s.total,
    cantidad_cuentas = s.cantidad,
    monto_pagado = CASE WHEN l.estado = 'PAGADA' THEN s.total END,
    monto_glosa = CASE WHEN l.estado = 'PAGADA' THEN 0 END
FROM (
    SELECT liquidacion_id, SUM(monto_cubierto) AS total, COUNT(*) AS cantidad
    FROM cuentas_aseguradora WHERE liquidacion_id IS NOT NULL GROUP BY liquidacion_id
) s
WHERE s.liquidacion_id = l.id;
