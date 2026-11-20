INSERT INTO quirofanos (codigo, nombre, ubicacion, estado, activo, observaciones) VALUES
    ('QX-01', 'Quirófano 1', 'Segundo piso, bloque quirúrgico', 'DISPONIBLE', true, 'Cirugía general y laparoscópica'),
    ('QX-02', 'Quirófano 2', 'Segundo piso, bloque quirúrgico', 'DISPONIBLE', true, 'Traumatología y cirugía mayor'),
    ('QX-03', 'Sala de procedimientos', 'Segundo piso, bloque quirúrgico', 'MANTENIMIENTO', true, 'En revisión de la ventilación');

INSERT INTO quirofano_equipos (quirofano_id, nombre, serie, activo)
SELECT q.id, e.nombre, e.serie, true
FROM quirofanos q
JOIN (VALUES
    ('QX-01', 'Torre de laparoscopía', 'TL-2201'),
    ('QX-01', 'Mesa quirúrgica eléctrica', 'MQ-1180'),
    ('QX-01', 'Electrobisturí', 'EB-0412'),
    ('QX-01', 'Máquina de anestesia', 'MA-3307'),
    ('QX-02', 'Mesa de ortopedia', 'MO-0921'),
    ('QX-02', 'Arco en C', 'AC-5510'),
    ('QX-02', 'Máquina de anestesia', 'MA-3310'),
    ('QX-02', 'Monitor multiparámetro', 'MM-7745')
) AS e(codigo, nombre, serie) ON e.codigo = q.codigo;

INSERT INTO esterilizadores (codigo, nombre, tipo, activo) VALUES
    ('AUT-01', 'Autoclave a vapor 1', 'VAPOR', true),
    ('AUT-02', 'Esterilizador de plasma', 'PLASMA', true);

INSERT INTO sets_instrumental (codigo, nombre, descripcion, vigencia_dias, activo) VALUES
    ('SET-LAP', 'Set de laparoscopía', 'Instrumental para cirugía laparoscópica abdominal', 30, true),
    ('SET-MAY', 'Set de cirugía mayor', 'Instrumental general para cirugía abierta', 30, true),
    ('SET-MEN', 'Set de cirugía menor', 'Instrumental para procedimientos menores', 14, true);

INSERT INTO ciclos_esterilizacion (numero, esterilizador_id, iniciado_en, temperatura_c, presion_kpa, duracion_min, indicador_quimico, indicador_biologico, resultado, responsable_id, observaciones, cerrado_en, creado_en)
SELECT 'CE-000001', e.id, NOW() - INTERVAL '2 days', 134.0, 210.0, 30, 'CONFORME', 'CONFORME', 'CONFORME', u.id, 'Carga completa de cirugía', NOW() - INTERVAL '1 day 18 hours', NOW() - INTERVAL '2 days'
FROM esterilizadores e, usuarios u WHERE e.codigo = 'AUT-01' AND u.email = 'enfermeria@tusalud.com';

INSERT INTO ciclos_esterilizacion (numero, esterilizador_id, iniciado_en, temperatura_c, presion_kpa, duracion_min, indicador_quimico, indicador_biologico, resultado, responsable_id, observaciones, cerrado_en, creado_en)
SELECT 'CE-000002', e.id, NOW() - INTERVAL '3 hours', 134.0, 212.0, 30, 'CONFORME', 'PENDIENTE', 'EN_PROCESO', u.id, 'A la espera del indicador biológico', NULL, NOW() - INTERVAL '3 hours'
FROM esterilizadores e, usuarios u WHERE e.codigo = 'AUT-01' AND u.email = 'enfermeria@tusalud.com';

INSERT INTO ciclos_esterilizacion (numero, esterilizador_id, iniciado_en, temperatura_c, presion_kpa, duracion_min, indicador_quimico, indicador_biologico, resultado, responsable_id, observaciones, cerrado_en, creado_en)
SELECT 'CE-000003', e.id, NOW() - INTERVAL '6 days', 132.0, 198.0, 30, 'CONFORME', 'NO_CONFORME', 'NO_CONFORME', u.id, 'El indicador biológico mostró crecimiento', NOW() - INTERVAL '5 days', NOW() - INTERVAL '6 days'
FROM esterilizadores e, usuarios u WHERE e.codigo = 'AUT-02' AND u.email = 'enfermeria@tusalud.com';

INSERT INTO lotes_esteriles (codigo, set_id, ciclo_id, esterilizado_en, vence_en, estado)
SELECT l.codigo, s.id, c.id, c.iniciado_en + INTERVAL '30 minutes', c.iniciado_en + INTERVAL '30 minutes' + (s.vigencia_dias || ' days')::interval, 'ESTERIL'
FROM (VALUES
    ('LE-000001', 'SET-LAP', 'CE-000001'),
    ('LE-000002', 'SET-MAY', 'CE-000001'),
    ('LE-000003', 'SET-MEN', 'CE-000001'),
    ('LE-000004', 'SET-LAP', 'CE-000002'),
    ('LE-000005', 'SET-MAY', 'CE-000003')
) AS l(codigo, set_codigo, ciclo_numero)
JOIN sets_instrumental s ON s.codigo = l.set_codigo
JOIN ciclos_esterilizacion c ON c.numero = l.ciclo_numero;

UPDATE secuencias_quirofano SET ultimo = 3 WHERE nombre = 'CICLO';
UPDATE secuencias_quirofano SET ultimo = 5 WHERE nombre = 'LOTE';

INSERT INTO cirugias (codigo, paciente_id, solicitante_id, cie10_id, diagnostico_codigo, diagnostico_descripcion, procedimiento, tipo, estado, quirofano_id, inicio_programado, fin_programado, tipo_anestesia, indicaciones_preoperatorias)
SELECT 'CIR-000001', p.id, m.id, c.id, c.codigo, c.descripcion, 'Apendicectomía laparoscópica', 'ELECTIVA', 'PROGRAMADA', q.id,
       date_trunc('day', NOW()) + INTERVAL '2 days 8 hours', date_trunc('day', NOW()) + INTERVAL '2 days 10 hours', 'GENERAL', 'Ayuno de 8 horas y baño preoperatorio'
FROM pacientes p JOIN usuarios up ON up.id = p.usuario_id AND up.email = 'juan.perez@correo.com',
     medicos m JOIN usuarios um ON um.id = m.usuario_id AND um.email = 'dr.garcia@tusalud.com',
     catalogo_cie10 c, quirofanos q
WHERE c.codigo = 'K35.8' AND q.codigo = 'QX-01';

INSERT INTO cirugias (codigo, paciente_id, solicitante_id, cie10_id, diagnostico_codigo, diagnostico_descripcion, procedimiento, tipo, estado, tipo_anestesia, indicaciones_preoperatorias)
SELECT 'CIR-000002', p.id, m.id, c.id, c.codigo, c.descripcion, 'Hernioplastía inguinal', 'ELECTIVA', 'SOLICITADA', 'REGIONAL', 'Suspender anticoagulantes 5 días antes'
FROM pacientes p JOIN usuarios up ON up.id = p.usuario_id AND up.email = 'maria.rodriguez@correo.com',
     medicos m JOIN usuarios um ON um.id = m.usuario_id AND um.email = 'dr.garcia@tusalud.com',
     catalogo_cie10 c
WHERE c.codigo = 'K40.9';

INSERT INTO cirugias (codigo, paciente_id, solicitante_id, cie10_id, diagnostico_codigo, diagnostico_descripcion, procedimiento, tipo, estado, quirofano_id, inicio_programado, fin_programado, tipo_anestesia,
                      inicio_real, fin_real, hallazgos, tecnica, complicaciones, sangrado_ml, diagnostico_postoperatorio, indicaciones_postoperatorias, nota_registrada_en, nota_por_id,
                      asa, anestesia_inicio, anestesia_fin, anestesia_observaciones, anestesia_registrada_en, anestesia_por_id)
SELECT 'CIR-000003', p.id, m.id, c.id, c.codigo, c.descripcion, 'Herniorrafia umbilical', 'ELECTIVA', 'FINALIZADA', q.id,
       date_trunc('day', NOW()) - INTERVAL '10 days' + INTERVAL '9 hours', date_trunc('day', NOW()) - INTERVAL '10 days' + INTERVAL '11 hours', 'REGIONAL',
       date_trunc('day', NOW()) - INTERVAL '10 days' + INTERVAL '9 hours 10 minutes', date_trunc('day', NOW()) - INTERVAL '10 days' + INTERVAL '10 hours 20 minutes',
       'Defecto herniario de 3 cm sin signos de estrangulación', 'Reparación con malla de polipropileno por vía abierta', NULL, 40, 'Hernia umbilical reparada',
       'Reposo relativo y control en 7 días', date_trunc('day', NOW()) - INTERVAL '10 days' + INTERVAL '10 hours 25 minutes', um.id,
       2, date_trunc('day', NOW()) - INTERVAL '10 days' + INTERVAL '9 hours', date_trunc('day', NOW()) - INTERVAL '10 days' + INTERVAL '10 hours 30 minutes',
       'Bloqueo regional sin incidentes', date_trunc('day', NOW()) - INTERVAL '10 days' + INTERVAL '10 hours 35 minutes', ua.id
FROM pacientes p JOIN usuarios up ON up.id = p.usuario_id AND up.email = 'carlos.mendoza@correo.com',
     medicos m JOIN usuarios um ON um.id = m.usuario_id AND um.email = 'dr.garcia@tusalud.com',
     usuarios ua,
     catalogo_cie10 c, quirofanos q
WHERE c.codigo = 'K42.9' AND q.codigo = 'QX-02' AND ua.email = 'dr.salazar@tusalud.com';

UPDATE secuencias_quirofano SET ultimo = 3 WHERE nombre = 'CIRUGIA';

INSERT INTO cirugia_equipo (cirugia_id, usuario_id, rol)
SELECT c.id, u.id, e.rol
FROM cirugias c
JOIN (VALUES
    ('CIR-000001', 'dr.garcia@tusalud.com', 'CIRUJANO'),
    ('CIR-000001', 'dr.salazar@tusalud.com', 'ANESTESIOLOGO'),
    ('CIR-000001', 'enfermeria@tusalud.com', 'INSTRUMENTISTA'),
    ('CIR-000003', 'dr.garcia@tusalud.com', 'CIRUJANO'),
    ('CIR-000003', 'dr.salazar@tusalud.com', 'ANESTESIOLOGO'),
    ('CIR-000003', 'enfermeria@tusalud.com', 'INSTRUMENTISTA')
) AS e(codigo, email, rol) ON e.codigo = c.codigo
JOIN usuarios u ON u.email = e.email;

INSERT INTO cirugia_checklists (cirugia_id, momento, items, observaciones, completado_por_id, completado_en)
SELECT c.id, k.momento, k.items, NULL, u.id, c.inicio_real + k.desplazamiento
FROM cirugias c
JOIN usuarios u ON u.email = 'enfermeria@tusalud.com'
JOIN (VALUES
    ('ANTES_INDUCCION', 'IDENTIDAD_CONFIRMADA,SITIO_MARCADO,CONSENTIMIENTOS_VERIFICADOS,EQUIPO_ANESTESIA_REVISADO,ALERGIAS_REVISADAS', INTERVAL '-5 minutes'),
    ('ANTES_INCISION', 'EQUIPO_SE_PRESENTO,PROCEDIMIENTO_CONFIRMADO,ANTIBIOTICO_ADMINISTRADO,ESTERILIDAD_CONFIRMADA,IMAGENES_DISPONIBLES', INTERVAL '5 minutes'),
    ('ANTES_SALIDA', 'PROCEDIMIENTO_REGISTRADO,CONTEO_INSTRUMENTAL_COMPLETO,MUESTRAS_ETIQUETADAS,PROBLEMAS_DEL_EQUIPO_REVISADOS,PLAN_POSTOPERATORIO_DEFINIDO', INTERVAL '65 minutes')
) AS k(momento, items, desplazamiento) ON true
WHERE c.codigo = 'CIR-000003';

INSERT INTO cirugia_eventos (cirugia_id, fecha, usuario_id, accion, detalle)
SELECT c.id, c.creado_en, u.id, 'SOLICITADA', 'ELECTIVA'
FROM cirugias c JOIN usuarios u ON u.email = 'dr.garcia@tusalud.com';

INSERT INTO cirugia_eventos (cirugia_id, fecha, usuario_id, accion, detalle)
SELECT c.id, NOW() - INTERVAL '1 day', u.id, 'PROGRAMADA', 'QX-01'
FROM cirugias c JOIN usuarios u ON u.email = 'enfermeria@tusalud.com' WHERE c.codigo = 'CIR-000001';

INSERT INTO cirugia_eventos (cirugia_id, fecha, usuario_id, accion, detalle)
SELECT c.id, c.inicio_real, u.id, 'INICIADA', NULL
FROM cirugias c JOIN usuarios u ON u.email = 'dr.garcia@tusalud.com' WHERE c.codigo = 'CIR-000003';

INSERT INTO cirugia_eventos (cirugia_id, fecha, usuario_id, accion, detalle)
SELECT c.id, c.fin_real, u.id, 'FINALIZADA', NULL
FROM cirugias c JOIN usuarios u ON u.email = 'dr.garcia@tusalud.com' WHERE c.codigo = 'CIR-000003';
