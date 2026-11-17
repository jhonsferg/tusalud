INSERT INTO usuarios (nombre, apellido, email, password, telefono, estado, created_at) VALUES
    ('Renato', 'Villanueva', 'imagenes@tusalud.com', '$2a$10$eKaIoJt42G.9.LoLfVjT5O46fIVxZ5M.FRuePYPp.2s3Lyal0IEZS', '912223344', 'ACTIVO', NOW() - INTERVAL '40 days');

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u JOIN roles r ON r.nombre = 'ROLE_IMAGENES' WHERE u.email = 'imagenes@tusalud.com';

INSERT INTO empleados (usuario_id, cargo, area, tipo_contrato, fecha_ingreso, fecha_fin_contrato, estado, sueldo,
                       contacto_emergencia_nombre, contacto_emergencia_telefono, dias_vacaciones_anuales, created_at, updated_at)
SELECT u.id, 'Tecnólogo médico y radiólogo', 'IMAGENES', 'INDEFINIDO', CURRENT_DATE - 620, NULL, 'ACTIVO', 6800.00, 'Carmen Villanueva', '987000020', 30,
       NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days'
FROM usuarios u WHERE u.email = 'imagenes@tusalud.com';

INSERT INTO estudios_imagen (codigo, nombre, modalidad, region, preparacion, precio, duracion_minutos, activo) VALUES
    ('RX-TORAX', 'Radiografía de tórax PA y lateral', 'RX', 'Tórax', NULL, 80.00, 15, true),
    ('RX-ABD', 'Radiografía de abdomen simple', 'RX', 'Abdomen', NULL, 70.00, 15, true),
    ('RX-LUMBAR', 'Radiografía de columna lumbosacra', 'RX', 'Columna', 'Retirar objetos metálicos de la zona', 90.00, 20, true),
    ('RX-RODILLA', 'Radiografía de rodilla', 'RX', 'Miembro inferior', NULL, 65.00, 15, true),
    ('RX-MANO', 'Radiografía de mano y muñeca', 'RX', 'Miembro superior', 'Retirar anillos, relojes y pulseras', 60.00, 15, true),
    ('ECO-ABD', 'Ecografía abdominal completa', 'ECO', 'Abdomen', 'Ayuno de 6 horas', 120.00, 30, true),
    ('ECO-PELV', 'Ecografía pélvica', 'ECO', 'Pelvis', 'Tomar un litro de agua una hora antes y no orinar', 110.00, 30, true),
    ('ECO-TIROIDES', 'Ecografía de tiroides', 'ECO', 'Cuello', NULL, 100.00, 20, true),
    ('ECO-OBST', 'Ecografía obstétrica', 'ECO', 'Pelvis', 'Vejiga moderadamente llena', 130.00, 30, true),
    ('ECO-MAMA', 'Ecografía mamaria', 'ECO', 'Mama', 'Sin cremas ni desodorante el día del estudio', 120.00, 30, true),
    ('ECO-CARD', 'Ecocardiograma doppler', 'ECO', 'Corazón', NULL, 220.00, 40, true),
    ('TC-CRANEO', 'Tomografía de cráneo sin contraste', 'TC', 'Cabeza', 'Retirar objetos metálicos y prótesis removibles', 450.00, 30, true),
    ('TC-TORAX', 'Tomografía de tórax', 'TC', 'Tórax', 'Ayuno de 4 horas e informar alergias al contraste', 520.00, 30, true),
    ('RM-RODILLA', 'Resonancia magnética de rodilla', 'RM', 'Miembro inferior', 'Sin implantes metálicos ni marcapasos', 800.00, 45, true),
    ('RM-LUMBAR', 'Resonancia magnética de columna lumbar', 'RM', 'Columna', 'Sin implantes metálicos ni marcapasos', 850.00, 45, true);

INSERT INTO ordenes_imagen (codigo, fecha, estado, prioridad, paciente_id, medico_id, atencion_id, indicacion_clinica, total, programada_para, sala, realizada_en, realizada_por, observaciones_toma, informada_en, hallazgo_critico, motivo_cancelacion, cobrado, created_at, updated_at)
SELECT 'IMG-000001', NOW() - INTERVAL '30 minutes', 'SOLICITADA', 'URGENTE', p.id, m.id, NULL, 'Dolor torácico persistente de tres días, descartar proceso neumónico', 80, NULL, NULL, NULL, NULL, NULL, NULL, false, NULL, false, NOW(), NOW()
FROM pacientes p JOIN usuarios pu ON pu.id = p.usuario_id, medicos m JOIN usuarios mu ON mu.id = m.usuario_id
WHERE pu.email = 'juan.perez@correo.com' AND mu.email = 'dr.garcia@tusalud.com';

INSERT INTO ordenes_imagen (codigo, fecha, estado, prioridad, paciente_id, medico_id, atencion_id, indicacion_clinica, total, programada_para, sala, realizada_en, realizada_por, observaciones_toma, informada_en, hallazgo_critico, motivo_cancelacion, cobrado, created_at, updated_at)
SELECT 'IMG-000002', NOW() - INTERVAL '2 hours', 'PROGRAMADA', 'NORMAL', p.id, m.id, NULL, 'Dolor abdominal recurrente, evaluar vesícula e hígado', 120, date_trunc('day', NOW()) + INTERVAL '1 day 10 hours', 'Sala de ecografía 1', NULL, NULL, NULL, NULL, false, NULL, false, NOW(), NOW()
FROM pacientes p JOIN usuarios pu ON pu.id = p.usuario_id, medicos m JOIN usuarios mu ON mu.id = m.usuario_id
WHERE pu.email = 'carlos.mendoza@correo.com' AND mu.email = 'dr.salazar@tusalud.com';

INSERT INTO ordenes_imagen (codigo, fecha, estado, prioridad, paciente_id, medico_id, atencion_id, indicacion_clinica, total, programada_para, sala, realizada_en, realizada_por, observaciones_toma, informada_en, hallazgo_critico, motivo_cancelacion, cobrado, created_at, updated_at)
SELECT 'IMG-000003', NOW() - INTERVAL '5 hours', 'REALIZADA', 'NORMAL', p.id, m.id, NULL, 'Palpitaciones ocasionales, evaluar función cardiaca', 220, NOW() - INTERVAL '4 hours', 'Sala de ecografía 2', NOW() - INTERVAL '3 hours', (SELECT id FROM usuarios WHERE email = 'imagenes@tusalud.com'), NULL, NULL, false, NULL, false, NOW(), NOW()
FROM pacientes p JOIN usuarios pu ON pu.id = p.usuario_id, medicos m JOIN usuarios mu ON mu.id = m.usuario_id
WHERE pu.email = 'sofia.ramirez@correo.com' AND mu.email = 'dr.garcia@tusalud.com';

INSERT INTO ordenes_imagen (codigo, fecha, estado, prioridad, paciente_id, medico_id, atencion_id, indicacion_clinica, total, programada_para, sala, realizada_en, realizada_por, observaciones_toma, informada_en, hallazgo_critico, motivo_cancelacion, cobrado, created_at, updated_at)
SELECT 'IMG-000004', NOW() - INTERVAL '5 days', 'INFORMADA', 'NORMAL', p.id, m.id, NULL, 'Control por hipertensión y tos crónica', 80, NOW() - INTERVAL '5 days' + INTERVAL '2 hours', 'Sala de rayos X 1', NOW() - INTERVAL '5 days' + INTERVAL '3 hours', (SELECT id FROM usuarios WHERE email = 'imagenes@tusalud.com'), NULL, NOW() - INTERVAL '4 days 20 hours', false, NULL, false, NOW(), NOW()
FROM pacientes p JOIN usuarios pu ON pu.id = p.usuario_id, medicos m JOIN usuarios mu ON mu.id = m.usuario_id
WHERE pu.email = 'carlos.mendoza@correo.com' AND mu.email = 'dr.garcia@tusalud.com';

INSERT INTO ordenes_imagen (codigo, fecha, estado, prioridad, paciente_id, medico_id, atencion_id, indicacion_clinica, total, programada_para, sala, realizada_en, realizada_por, observaciones_toma, informada_en, hallazgo_critico, motivo_cancelacion, cobrado, created_at, updated_at)
SELECT 'IMG-000005', NOW() - INTERVAL '12 days', 'INFORMADA', 'URGENTE', p.id, m.id, NULL, 'Cefalea intensa y súbita, descartar lesión intracraneal', 450, NOW() - INTERVAL '12 days' + INTERVAL '1 hour', 'Sala de tomografía', NOW() - INTERVAL '12 days' + INTERVAL '2 hours', (SELECT id FROM usuarios WHERE email = 'imagenes@tusalud.com'), NULL, NOW() - INTERVAL '11 days 22 hours', true, NULL, false, NOW(), NOW()
FROM pacientes p JOIN usuarios pu ON pu.id = p.usuario_id, medicos m JOIN usuarios mu ON mu.id = m.usuario_id
WHERE pu.email = 'sofia.ramirez@correo.com' AND mu.email = 'dr.salazar@tusalud.com';

INSERT INTO ordenes_imagen (codigo, fecha, estado, prioridad, paciente_id, medico_id, atencion_id, indicacion_clinica, total, programada_para, sala, realizada_en, realizada_por, observaciones_toma, informada_en, hallazgo_critico, motivo_cancelacion, cobrado, created_at, updated_at)
SELECT 'IMG-000006', NOW() - INTERVAL '8 days', 'CANCELADA', 'NORMAL', p.id, m.id, NULL, 'Control de rodilla', 65, NULL, NULL, NULL, NULL, NULL, NULL, false, 'El paciente prefirió realizarlo en otro centro', false, NOW(), NOW()
FROM pacientes p JOIN usuarios pu ON pu.id = p.usuario_id, medicos m JOIN usuarios mu ON mu.id = m.usuario_id
WHERE pu.email = 'juan.perez@correo.com' AND mu.email = 'dr.garcia@tusalud.com';

INSERT INTO items_orden_imagen (orden_id, estudio_id, posicion)
SELECT o.id, e.id, (ROW_NUMBER() OVER (PARTITION BY v.orden ORDER BY v.n) - 1)::integer
FROM (SELECT x.*, ROW_NUMBER() OVER () AS n FROM (VALUES
    ('IMG-000001', 'RX-TORAX'),
    ('IMG-000002', 'ECO-ABD'),
    ('IMG-000003', 'ECO-CARD'),
    ('IMG-000004', 'RX-TORAX'),
    ('IMG-000005', 'TC-CRANEO'),
    ('IMG-000006', 'RX-RODILLA')
) AS x(orden, estudio)) AS v
JOIN ordenes_imagen o ON o.codigo = v.orden
JOIN estudios_imagen e ON e.codigo = v.estudio;

INSERT INTO archivos_imagen (orden_id, nombre, tipo_contenido, tamano, contenido, posicion, subido_por, subido_en)
SELECT o.id, v.nombre, 'image/png', octet_length(img.b), img.b, v.posicion, u.id, o.realizada_en - INTERVAL '10 minutes'
FROM (SELECT decode('iVBORw0KGgoAAAANSUhEUgAAAMAAAADACAAAAAB3tzPbAAAEiklEQVR4nO2dS3IbMQxEvcwVdKHIlv8XyZonyXlTkuWSYhFEo9Ej6oNeTdmKgUeyCXBGU3n4deV6mJ1AVgUwWwUwW3uA1UF/LltHmRbAFBXAbBXAbBXAbBXAbBXAbBXAbBXAbBXAbBXAbBXAbBXAbJ0L4O+pNH+4AEAVgKUCAFUAlgoAVAFYKgBQEwFG+v36+L75fFm/PX08G9f7T3YA9r8Z/NvvazCbOAAQe/3mADj8X9cLAUCx9+NnAQBzuL1eBACM/TV+BgA0h9vrBQDg2Lvx6wOAc3jkJR1AIPZ2/LoA8BwevKQDiMR+MgDwOXzffIoBQrFf1l2AyBy+Pr5LAYKxN58dgNAcbq+VANHYjx2A2BwCPg4AhGM/dwCCc+j7GAeIrp+X9VsHIDiHfk+BA0TXj8gDno9hgPD6EXnA8zEMEF4/Kg84Pr7V84AmTgFYinuA2EN1Hhj21SAAsYfK6sCuL0wCMHuorA7s+sIkADXvsjowPB9DANy8Cz0wOB9DAOS86zzw8ZwDIOdd6AG7J0UAuDGTesDuSREAbsy0HjB7UgSAHDOpB8xaBgCwYyb1gNmTAgDsmGk9YNUyAIAeM6kHrFrmA/BjJvWAVct8AH7MtB4watmtngc0cQrAUsQDtOfkHujXMheAjyeuA8a5zAXg44nrgNHPuQB8PHUd6PdzHkAintwD3XuMHkAmntoD3X7OA8jEU3ug2895AJl4ag90+7nyQHlgWQ+k4sk90Hvu7QDk4qk90DvTOAC5eGoP9M40DkAuntoDvftzt3oe0MQpAEt34IFvVR2oOnCvdaA8UB4oD5QHygPlgfLATA9o2vYOgOYPFwCoiQBX74FvVR2oOnCvdeBmPHD1zwdSz8haB6Bl1iQDQMdrrXUBWuMZGAB2/bQBQGPXJAPAzXVrQ4DWLtwDzQVol+yB1gCA1s7kgfj3hRoI0MJz283PBYiO01GGDkCLzm0EgO7UjxP0ANqC5wFJ/i4ATXAHAEeKeOD/9HyAluyl1d+d/pEdANByvbT6u9MEQK6XhgCC/U8QINAXsQBwP3eSGwTQMn0EBoCuURIg00do36EhATK9tPQ9stPUMADQx1ZyCAC3h8IAmAcyANweCgNgHsgAQLWskxkIAPXVZm4QALJGEwB8H6F8nzgBwPcR8BvdgXMwAcDvocp36hMA5P2IIUD2JBACWOI8UACzAU401QOjxFCAqXVAAeD2pAkAfg+NAEysAxoArydNAJB9aBRgmgdUAI6PEwBkHxoGcHxMA1BneQpg7GMagDrLUwBjH9MAw3FxcwoBjOsxCUDXYAbg7PeF1ABDH5MAdA2mAIb3iCgA5l5QBmB4PiYAcutH/YSGAOACLQXwk8AHIOMsBnCRz8g6Qn3sAqT2nwTAYC8KAeT2nwzAoC8KAND9jwLA6otWKxjAvF8WyoMG6N5v3P0cBNh9luifdQA/GQ4/BgAOH+a8KwKw5ALoQi0DsEewAJSBlgJYDQCkcRYD2DL0ANRBlgQY/D80Oi0KcA4VwGzdKgDbqJ9JBTBbBsD1qgBmqwBm6x+Eu5efyZPL7gAAAABJRU5ErkJggg==', 'base64') AS b) AS img
CROSS JOIN (VALUES
    ('IMG-000003', 'ecocardiograma-corte-4-camaras.png', 0),
    ('IMG-000004', 'torax-pa.png', 0),
    ('IMG-000004', 'torax-lateral.png', 1),
    ('IMG-000005', 'tc-craneo-corte-axial.png', 0)
) AS v (orden, nombre, posicion)
JOIN ordenes_imagen o ON o.codigo = v.orden
CROSS JOIN (SELECT id FROM usuarios WHERE email = 'imagenes@tusalud.com') AS u;

INSERT INTO informes_imagen (orden_id, version, hallazgos, conclusion, hallazgo_critico, firma_nombre, firma_colegiatura, autor_id, motivo_correccion, created_at)
SELECT o.id, v.version, v.hallazgos, v.conclusion, v.critico, u.nombre || ' ' || u.apellido, 'CMP-45210', u.id, v.motivo,
       o.informada_en + (v.version - 1) * INTERVAL '3 hours'
FROM (VALUES
    ('IMG-000004', 1, 'Campos pulmonares con buena expansión y transparencia conservada. Silueta cardiaca de tamaño normal. Senos costofrénicos libres. Estructuras óseas sin lesiones.', 'Radiografía de tórax sin alteraciones evidentes.', false, NULL::varchar),
    ('IMG-000005', 1, 'Parénquima cerebral sin lesiones focales evidentes. Sistema ventricular de configuración normal. No se observa desviación de la línea media.', 'Tomografía de cráneo sin hallazgos agudos.', false, NULL::varchar),
    ('IMG-000005', 2, 'Al revisar los cortes se identifica una imagen hiperdensa subaracnoidea en la cisura silviana derecha. Sistema ventricular sin dilatación.', 'Hemorragia subaracnoidea. Se recomienda evaluación neuroquirúrgica inmediata.', true, 'Hallazgo adicional identificado en una segunda lectura de los cortes')
) AS v (orden, version, hallazgos, conclusion, critico, motivo)
JOIN ordenes_imagen o ON o.codigo = v.orden
CROSS JOIN (SELECT id, nombre, apellido FROM usuarios WHERE email = 'imagenes@tusalud.com') AS u;
