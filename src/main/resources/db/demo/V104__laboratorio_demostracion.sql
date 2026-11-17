INSERT INTO examenes_laboratorio (codigo, nombre, categoria, muestra, precio, tiempo_entrega_horas, indicaciones, activo) VALUES
    ('HEM-01', 'Hemograma completo', 'HEMATOLOGIA', 'SANGRE', 35.00, 6, NULL, true),
    ('GLU-01', 'Glucosa en ayunas', 'BIOQUIMICA', 'SANGRE', 15.00, 4, 'Ayuno de 8 horas', true),
    ('LIP-01', 'Perfil lipídico', 'BIOQUIMICA', 'SANGRE', 45.00, 8, 'Ayuno de 12 horas', true),
    ('CRE-01', 'Creatinina sérica', 'BIOQUIMICA', 'SANGRE', 18.00, 4, NULL, true),
    ('URE-01', 'Urea sérica', 'BIOQUIMICA', 'SANGRE', 15.00, 4, NULL, true),
    ('ORI-01', 'Examen completo de orina', 'ORINA', 'ORINA', 20.00, 4, 'Primera orina de la mañana, chorro medio', true),
    ('TSH-01', 'Hormona estimulante de la tiroides (TSH)', 'INMUNOLOGIA', 'SANGRE', 55.00, 24, NULL, true),
    ('HBA-01', 'Hemoglobina glicosilada (HbA1c)', 'BIOQUIMICA', 'SANGRE', 50.00, 24, NULL, true),
    ('PCR-01', 'Proteína C reactiva', 'INMUNOLOGIA', 'SANGRE', 30.00, 6, NULL, true),
    ('CUL-01', 'Cultivo de orina', 'MICROBIOLOGIA', 'ORINA', 60.00, 72, 'Recolectar en frasco estéril, chorro medio', true),
    ('ACU-01', 'Ácido úrico', 'BIOQUIMICA', 'SANGRE', 18.00, 4, 'Ayuno de 8 horas', true),
    ('TRA-01', 'Transaminasas TGO y TGP', 'BIOQUIMICA', 'SANGRE', 40.00, 8, NULL, true),
    ('VIH-01', 'Prueba rápida de VIH', 'INMUNOLOGIA', 'SANGRE', 25.00, 2, NULL, true),
    ('HEC-01', 'Examen parasitológico en heces', 'MICROBIOLOGIA', 'HECES', 22.00, 24, 'Muestra fresca en frasco limpio', true);

INSERT INTO parametros_examen (examen_id, nombre, unidad, tipo, valor_min, valor_max, orden)
SELECT e.id, v.nombre, v.unidad::varchar, v.tipo, v.vmin::numeric, v.vmax::numeric, v.orden
FROM (VALUES
    ('HEM-01', 'Hemoglobina', 'g/dL', 'NUMERICO', 12.0, 17.5, 1),
    ('HEM-01', 'Hematocrito', '%', 'NUMERICO', 36.0, 52.0, 2),
    ('HEM-01', 'Leucocitos', 'x10³/µL', 'NUMERICO', 4.0, 11.0, 3),
    ('HEM-01', 'Plaquetas', 'x10³/µL', 'NUMERICO', 150.0, 450.0, 4),
    ('GLU-01', 'Glucosa', 'mg/dL', 'NUMERICO', 70.0, 100.0, 1),
    ('LIP-01', 'Colesterol total', 'mg/dL', 'NUMERICO', 0.0, 200.0, 1),
    ('LIP-01', 'HDL', 'mg/dL', 'NUMERICO', 40.0, 100.0, 2),
    ('LIP-01', 'LDL', 'mg/dL', 'NUMERICO', 0.0, 130.0, 3),
    ('LIP-01', 'Triglicéridos', 'mg/dL', 'NUMERICO', 0.0, 150.0, 4),
    ('CRE-01', 'Creatinina', 'mg/dL', 'NUMERICO', 0.6, 1.3, 1),
    ('URE-01', 'Urea', 'mg/dL', 'NUMERICO', 15.0, 45.0, 1),
    ('ORI-01', 'Color', NULL, 'TEXTO', NULL, NULL, 1),
    ('ORI-01', 'Aspecto', NULL, 'TEXTO', NULL, NULL, 2),
    ('ORI-01', 'Densidad', NULL, 'NUMERICO', 1.005, 1.030, 3),
    ('ORI-01', 'pH', NULL, 'NUMERICO', 5.0, 8.0, 4),
    ('ORI-01', 'Proteínas', NULL, 'TEXTO', NULL, NULL, 5),
    ('ORI-01', 'Leucocitos en sedimento', 'por campo', 'TEXTO', NULL, NULL, 6),
    ('TSH-01', 'TSH', 'µUI/mL', 'NUMERICO', 0.4, 4.0, 1),
    ('HBA-01', 'HbA1c', '%', 'NUMERICO', 4.0, 5.6, 1),
    ('PCR-01', 'Proteína C reactiva', 'mg/L', 'NUMERICO', 0.0, 5.0, 1),
    ('CUL-01', 'Resultado del cultivo', NULL, 'TEXTO', NULL, NULL, 1),
    ('CUL-01', 'Recuento de colonias', 'UFC/mL', 'TEXTO', NULL, NULL, 2),
    ('ACU-01', 'Ácido úrico', 'mg/dL', 'NUMERICO', 3.5, 7.2, 1),
    ('TRA-01', 'TGO', 'U/L', 'NUMERICO', 0.0, 40.0, 1),
    ('TRA-01', 'TGP', 'U/L', 'NUMERICO', 0.0, 41.0, 2),
    ('VIH-01', 'Resultado', NULL, 'TEXTO', NULL, NULL, 1),
    ('HEC-01', 'Resultado', NULL, 'TEXTO', NULL, NULL, 1)
) AS v(codigo, nombre, unidad, tipo, vmin, vmax, orden)
JOIN examenes_laboratorio e ON e.codigo = v.codigo;

INSERT INTO ordenes_laboratorio (codigo, fecha, estado, prioridad, paciente_id, medico_id, atencion_id, indicaciones, total, toma_muestra_en, completada_en, motivo_cancelacion, created_at, updated_at)
SELECT 'LAB-000001', NOW() - INTERVAL '30 minutes', 'SOLICITADA', 'URGENTE', p.id, m.id, NULL, 'Dolor torácico, descartar proceso inflamatorio', 65.00, NULL, NULL, NULL, NOW(), NOW()
FROM pacientes p JOIN usuarios pu ON pu.id = p.usuario_id, medicos m JOIN usuarios mu ON mu.id = m.usuario_id
WHERE pu.email = 'juan.perez@correo.com' AND mu.email = 'dr.garcia@tusalud.com';

INSERT INTO ordenes_laboratorio (codigo, fecha, estado, prioridad, paciente_id, medico_id, atencion_id, indicaciones, total, toma_muestra_en, completada_en, motivo_cancelacion, created_at, updated_at)
SELECT 'LAB-000002', NOW() - INTERVAL '3 hours', 'MUESTRA_TOMADA', 'NORMAL', p.id, m.id, NULL, 'Control metabólico, paciente en ayunas', 60.00, NOW() - INTERVAL '2 hours', NULL, NULL, NOW(), NOW()
FROM pacientes p JOIN usuarios pu ON pu.id = p.usuario_id, medicos m JOIN usuarios mu ON mu.id = m.usuario_id
WHERE pu.email = 'carlos.mendoza@correo.com' AND mu.email = 'dr.salazar@tusalud.com';

INSERT INTO ordenes_laboratorio (codigo, fecha, estado, prioridad, paciente_id, medico_id, atencion_id, indicaciones, total, toma_muestra_en, completada_en, motivo_cancelacion, created_at, updated_at)
SELECT 'LAB-000003', NOW() - INTERVAL '1 day', 'EN_PROCESO', 'NORMAL', p.id, m.id, NULL, 'Palpitaciones ocasionales, descartar anemia y alteración tiroidea', 90.00, NOW() - INTERVAL '22 hours', NULL, NULL, NOW(), NOW()
FROM pacientes p JOIN usuarios pu ON pu.id = p.usuario_id, medicos m JOIN usuarios mu ON mu.id = m.usuario_id
WHERE pu.email = 'sofia.ramirez@correo.com' AND mu.email = 'dr.garcia@tusalud.com';

INSERT INTO ordenes_laboratorio (codigo, fecha, estado, prioridad, paciente_id, medico_id, atencion_id, indicaciones, total, toma_muestra_en, completada_en, motivo_cancelacion, created_at, updated_at)
SELECT 'LAB-000004', NOW() - INTERVAL '5 days', 'COMPLETADA', 'NORMAL', p.id, m.id, NULL, 'Seguimiento de hipertensión y riesgo cardiovascular', 110.00, NOW() - INTERVAL '5 days' + INTERVAL '1 hour', NOW() - INTERVAL '4 days', NULL, NOW(), NOW()
FROM pacientes p JOIN usuarios pu ON pu.id = p.usuario_id, medicos m JOIN usuarios mu ON mu.id = m.usuario_id
WHERE pu.email = 'carlos.mendoza@correo.com' AND mu.email = 'dr.garcia@tusalud.com';

INSERT INTO ordenes_laboratorio (codigo, fecha, estado, prioridad, paciente_id, medico_id, atencion_id, indicaciones, total, toma_muestra_en, completada_en, motivo_cancelacion, created_at, updated_at)
SELECT 'LAB-000005', NOW() - INTERVAL '14 days', 'COMPLETADA', 'NORMAL', p.id, m.id, NULL, 'Chequeo general anual', 33.00, NOW() - INTERVAL '14 days' + INTERVAL '1 hour', NOW() - INTERVAL '13 days', NULL, NOW(), NOW()
FROM pacientes p JOIN usuarios pu ON pu.id = p.usuario_id, medicos m JOIN usuarios mu ON mu.id = m.usuario_id
WHERE pu.email = 'sofia.ramirez@correo.com' AND mu.email = 'dr.salazar@tusalud.com';

INSERT INTO items_orden_lab (orden_id, examen_id, estado, observaciones, fecha_resultado, validado_por_id, posicion)
SELECT o.id, e.id, v.estado, v.obs::varchar,
       CASE WHEN v.estado = 'PENDIENTE' THEN NULL ELSE o.fecha + INTERVAL '3 hours' END,
       CASE WHEN v.estado = 'VALIDADO' THEN (SELECT id FROM usuarios WHERE email = 'lab@tusalud.com') ELSE NULL END,
       (ROW_NUMBER() OVER (PARTITION BY v.orden ORDER BY v.n) - 1)::integer
FROM (SELECT x.*, ROW_NUMBER() OVER () AS n FROM (VALUES
    ('LAB-000001', 'HEM-01', 'PENDIENTE', NULL),
    ('LAB-000001', 'PCR-01', 'PENDIENTE', NULL),
    ('LAB-000002', 'GLU-01', 'PENDIENTE', NULL),
    ('LAB-000002', 'LIP-01', 'PENDIENTE', NULL),
    ('LAB-000003', 'HEM-01', 'CON_RESULTADO', 'Leucocitos levemente elevados'),
    ('LAB-000003', 'TSH-01', 'PENDIENTE', NULL),
    ('LAB-000004', 'LIP-01', 'VALIDADO', 'Perfil lipídico alterado, se sugiere control dietético'),
    ('LAB-000004', 'GLU-01', 'VALIDADO', NULL),
    ('LAB-000004', 'HBA-01', 'VALIDADO', NULL),
    ('LAB-000005', 'CRE-01', 'VALIDADO', NULL),
    ('LAB-000005', 'URE-01', 'VALIDADO', NULL)
) AS x(orden, examen, estado, obs)) AS v
JOIN ordenes_laboratorio o ON o.codigo = v.orden
JOIN examenes_laboratorio e ON e.codigo = v.examen;

INSERT INTO resultados_lab (item_id, parametro_id, parametro_nombre, unidad, valor, valor_min, valor_max, fuera_de_rango, posicion)
SELECT i.id, p.id, p.nombre, p.unidad, v.valor, p.valor_min, p.valor_max, v.fuera,
       (ROW_NUMBER() OVER (PARTITION BY v.orden, v.examen ORDER BY v.n) - 1)::integer
FROM (SELECT x.*, ROW_NUMBER() OVER () AS n FROM (VALUES
    ('LAB-000003', 'HEM-01', 'Hemoglobina', '12.9', false),
    ('LAB-000003', 'HEM-01', 'Hematocrito', '38.0', false),
    ('LAB-000003', 'HEM-01', 'Leucocitos', '11.8', true),
    ('LAB-000003', 'HEM-01', 'Plaquetas', '280', false),
    ('LAB-000004', 'LIP-01', 'Colesterol total', '245', true),
    ('LAB-000004', 'LIP-01', 'HDL', '38', true),
    ('LAB-000004', 'LIP-01', 'LDL', '160', true),
    ('LAB-000004', 'LIP-01', 'Triglicéridos', '180', true),
    ('LAB-000004', 'GLU-01', 'Glucosa', '112', true),
    ('LAB-000004', 'HBA-01', 'HbA1c', '6.1', true),
    ('LAB-000005', 'CRE-01', 'Creatinina', '0.9', false),
    ('LAB-000005', 'URE-01', 'Urea', '28', false)
) AS x(orden, examen, parametro, valor, fuera)) AS v
JOIN ordenes_laboratorio o ON o.codigo = v.orden
JOIN examenes_laboratorio e ON e.codigo = v.examen
JOIN items_orden_lab i ON i.orden_id = o.id AND i.examen_id = e.id
JOIN parametros_examen p ON p.examen_id = e.id AND p.nombre = v.parametro;
