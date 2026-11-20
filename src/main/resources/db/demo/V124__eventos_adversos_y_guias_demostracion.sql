INSERT INTO guias_clinicas (codigo, version, titulo, resumen, contenido, estado, vigente_desde, por_validar, creado_por_id, creado_en)
SELECT v.codigo, 1, v.titulo, v.resumen, v.contenido, 'VIGENTE', CURRENT_DATE - 90, true,
       (SELECT id FROM usuarios WHERE email = 'calidad@tusalud.com'), NOW() - INTERVAL '90 days'
FROM (VALUES
    ('GPC-HTA', 'Hipertensión arterial en el adulto', 'Diagnóstico, metas y tratamiento inicial de la hipertensión.',
     'Confirmar el diagnóstico con mediciones repetidas en dos visitas. Metas individualizadas según riesgo. Iniciar con medidas de estilo de vida y valorar un fármaco de primera línea según comorbilidades. Control en cuatro semanas y luego cada tres meses hasta lograr la meta.'),
    ('GPC-DM2', 'Diabetes mellitus tipo 2', 'Tamizaje, metas glucémicas y esquema escalonado.',
     'Meta de hemoglobina glicosilada individualizada. Metformina como primera línea salvo contraindicación. Evaluación anual de pie, retina y función renal. Educación diabetológica en cada control.'),
    ('GPC-ASM', 'Asma en adultos y niños', 'Clasificación de control y manejo escalonado.',
     'Clasificar el control con base en síntomas diurnos, nocturnos y uso de rescate. Escalonar el tratamiento de control según el nivel. Revisar la técnica inhalatoria y el plan de acción por escrito.'),
    ('GPC-IRA', 'Infección respiratoria aguda', 'Criterios de antibiótico y signos de alarma.',
     'Distinguir cuadros virales de bacterianos con criterios clínicos. Evitar antibióticos sin indicación. Explicar signos de alarma y reevaluar si no mejora en setenta y dos horas.'),
    ('GPC-ITU', 'Infección del tracto urinario no complicada', 'Diagnóstico y tratamiento ambulatorio.',
     'Diagnóstico clínico con apoyo de examen de orina. Esquema corto según el antibiograma local. Derivar ante fiebre, embarazo o recurrencia.'),
    ('GPC-GAS', 'Gastritis y enfermedad por reflujo', 'Manejo inicial y signos de alarma.',
     'Medidas higiénico dietéticas y un inhibidor de la bomba de protones por ocho semanas. Investigar Helicobacter pylori ante síntomas persistentes. Endoscopía ante signos de alarma.'),
    ('GPC-LUM', 'Lumbalgia aguda inespecífica', 'Evaluación, analgesia y retorno a la actividad.',
     'Descartar banderas rojas. Analgesia escalonada y actividad temprana. Evitar reposo prolongado e imágenes sin indicación.'),
    ('GPC-CRE', 'Control del niño sano y crecimiento', 'Controles, vacunas y alertas del crecimiento.',
     'Controles periódicos con peso, talla y perímetro cefálico en curvas de referencia. Verificar el esquema de vacunación en cada visita. Orientar alimentación, sueño y desarrollo.'),
    ('GPC-GES', 'Control prenatal', 'Controles, exámenes y signos de alarma del embarazo.',
     'Primer control antes de las catorce semanas. Exámenes de rutina por trimestre. Educar sobre signos de alarma y plan de parto.'),
    ('GPC-DEP', 'Depresión en atención primaria', 'Tamizaje, severidad y seguimiento.',
     'Tamizaje con cuestionario validado. Clasificar la severidad y evaluar riesgo suicida. Psicoterapia y fármacos según severidad, con seguimiento a las dos semanas.')
) AS v(codigo, titulo, resumen, contenido);

INSERT INTO guia_cie10 (guia_id, codigo)
SELECT g.id, c.codigo FROM guias_clinicas g
JOIN (VALUES
    ('GPC-HTA', 'I10'), ('GPC-HTA', 'I11'),
    ('GPC-DM2', 'E11'),
    ('GPC-ASM', 'J45'),
    ('GPC-IRA', 'J06'), ('GPC-IRA', 'J20'),
    ('GPC-ITU', 'N39'),
    ('GPC-GAS', 'K29'), ('GPC-GAS', 'K21'),
    ('GPC-LUM', 'M54'),
    ('GPC-CRE', 'Z00'),
    ('GPC-GES', 'Z34'),
    ('GPC-DEP', 'F32')
) AS c(guia, codigo) ON c.guia = g.codigo;

INSERT INTO eventos_adversos (anio, numero, codigo, tipo, severidad, area, descripcion, anonimo, notificante_id, estado, ocurrido_en, notificado_en)
SELECT EXTRACT(YEAR FROM CURRENT_DATE)::int, v.numero, 'EA-' || EXTRACT(YEAR FROM CURRENT_DATE)::int || '-' || lpad(v.numero::text, 4, '0'),
       v.tipo, v.severidad, v.area, v.descripcion, v.anonimo,
       (SELECT id FROM usuarios WHERE email = v.correo), 'NOTIFICADO',
       NOW() - (v.dias || ' days')::interval, NOW() - (v.dias || ' days')::interval + INTERVAL '3 hours'
FROM (VALUES
    (1, 'CASI_INCIDENTE', 'LEVE', 'Farmacia', 'Se detectó antes de dispensar una etiqueta con una concentración equivocada.', false, 'farmacia@tusalud.com', 12),
    (2, 'EVENTO_ADVERSO', 'MODERADO', 'Hospitalización', 'Caída de un paciente al levantarse de la cama sin lesión grave.', false, 'enfermeria@tusalud.com', 8),
    (3, 'INCIDENTE_SIN_DANO', 'LEVE', 'Laboratorio', 'Una muestra llegó sin identificación completa y se rechazó antes de procesarla.', true, 'lab@tusalud.com', 4)
) AS v(numero, tipo, severidad, area, descripcion, anonimo, correo, dias);
