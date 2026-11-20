BEGIN;

CREATE TEMP TABLE nombres AS
SELECT * FROM (VALUES
 ('Luis'),('María'),('José'),('Carmen'),('Jorge'),('Rosa'),('Miguel'),('Ana'),('Carlos'),('Lucía'),('Pedro'),('Elena'),
 ('Juan'),('Sofía'),('Diego'),('Valeria'),('Andrés'),('Camila'),('Fernando'),('Paola'),('Raúl'),('Gabriela'),('Héctor'),('Mónica'),
 ('Óscar'),('Patricia'),('Víctor'),('Daniela'),('Eduardo'),('Natalia')) t(n);
CREATE TEMP TABLE apellidos AS
SELECT * FROM (VALUES
 ('Quispe'),('Flores'),('Rojas'),('Huamán'),('Chávez'),('Mamani'),('Vásquez'),('Gutiérrez'),('Ramos'),('Torres'),('Díaz'),('Castro'),
 ('Salazar'),('Ríos'),('Mendoza'),('Cruz'),('Paredes'),('Soto'),('Vega'),('Aguilar'),('Cárdenas'),('Peña'),('Campos'),('Ortiz'),
 ('Delgado'),('Zamora'),('Lozano'),('Espinoza'),('Valdivia'),('Carrasco')) t(a);

CREATE TEMP TABLE n_nombres AS SELECT n, row_number() OVER () AS i FROM nombres;
CREATE TEMP TABLE n_apellidos AS SELECT a, row_number() OVER () AS i FROM apellidos;

CREATE TEMP TABLE nuevos_pacientes AS
SELECT g AS k, gen_random_uuid() AS usuario_id, gen_random_uuid() AS paciente_id,
       (SELECT n FROM n_nombres WHERE i = 1 + (g * 7) % 30) AS nombre,
       (SELECT a FROM n_apellidos WHERE i = 1 + (g * 11) % 30) AS apellido,
       (SELECT a FROM n_apellidos WHERE i = 1 + (g * 13 + 5) % 30) AS apellido2
FROM generate_series(1, 220) g;

INSERT INTO usuarios (id, nombre, apellido, email, password, telefono, estado, created_at)
SELECT usuario_id, nombre, apellido || ' ' || apellido2,
       'qa.paciente' || lpad(k::text, 3, '0') || '@correo.com',
       '$2a$10$2RaRGJ5Ph7t.ev4VxgQcMuSrWKnv7KcO4XRmGhuTV9jmm81Wq6OjK',
       '9' || lpad((10000000 + k * 7919 % 89999999)::text, 8, '0'), 'ACTIVO',
       NOW() - ((k % 90 + 5) || ' days')::interval
FROM nuevos_pacientes;

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT p.usuario_id, r.id FROM nuevos_pacientes p JOIN roles r ON r.nombre = 'ROLE_PACIENTE';

INSERT INTO pacientes (id, alergias, antecedentes, dni, fecha_nacimiento, grupo_sanguineo, historial_clinico_numero, usuario_id, sexo)
SELECT paciente_id,
       CASE WHEN k % 6 = 0 THEN 'Penicilina' WHEN k % 11 = 0 THEN 'Polen' WHEN k % 17 = 0 THEN 'Mariscos' ELSE NULL END,
       CASE WHEN k % 5 = 0 THEN 'Hipertensión arterial' WHEN k % 9 = 0 THEN 'Diabetes tipo 2' WHEN k % 13 = 0 THEN 'Asma' ELSE NULL END,
       (60000000 + k)::text,
       (CURRENT_DATE - ((18 + (k * 37) % 62) * 365 + (k * 53) % 365) * INTERVAL '1 day')::date,
       (ARRAY['O+','O+','A+','A+','B+','AB+','O-','A-'])[1 + k % 8],
       'HC-' || lpad((1000 + k)::text, 6, '0'),
       usuario_id,
       CASE WHEN k % 2 = 0 THEN 'F' ELSE 'M' END::char(1)
FROM nuevos_pacientes;

CREATE TEMP TABLE nuevos_medicos AS
SELECT g AS k, gen_random_uuid() AS usuario_id, gen_random_uuid() AS medico_id,
       (SELECT n FROM n_nombres WHERE i = 1 + (g * 5 + 3) % 30) AS nombre,
       (SELECT a FROM n_apellidos WHERE i = 1 + (g * 9 + 1) % 30) AS apellido
FROM generate_series(1, 8) g;

INSERT INTO usuarios (id, nombre, apellido, email, password, telefono, estado, created_at)
SELECT usuario_id, nombre, apellido, 'qa.medico' || lpad(k::text, 2, '0') || '@tusalud.com',
       '$2a$10$LnkKfNnzSze9qUa3qoZp0OyVBKApVqMBHV5vY1k0YhvEC5pGtJqKm',
       '9' || lpad((20000000 + k * 104729 % 79999999)::text, 8, '0'), 'ACTIVO', NOW() - INTERVAL '120 days'
FROM nuevos_medicos;

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT m.usuario_id, r.id FROM nuevos_medicos m JOIN roles r ON r.nombre = 'ROLE_MEDICO';

INSERT INTO medicos (id, colegiatura, consultorio, usuario_id)
SELECT medico_id, 'CMP-9' || lpad(k::text, 4, '0'), 'Consultorio ' || (500 + k), usuario_id FROM nuevos_medicos;

INSERT INTO medico_especialidades (medico_id, especialidad_id)
SELECT m.medico_id, e.id
FROM nuevos_medicos m
JOIN (SELECT id, row_number() OVER (ORDER BY nombre) AS i FROM especialidades) e ON e.i = 1 + (m.k - 1) % 7;

INSERT INTO medico_especialidades (medico_id, especialidad_id)
SELECT m.medico_id, e.id
FROM nuevos_medicos m
JOIN (SELECT id, row_number() OVER (ORDER BY nombre) AS i FROM especialidades) e ON e.i = 1 + (m.k + 1) % 7
WHERE m.k % 2 = 0;

INSERT INTO horarios_atencion (dia_semana, duracion_cita_minutos, hora_fin, hora_inicio, medico_id)
SELECT d, 30, CASE WHEN m.k % 2 = 0 THEN TIME '19:00' ELSE TIME '13:00' END,
       CASE WHEN m.k % 2 = 0 THEN TIME '14:00' ELSE TIME '08:00' END, m.medico_id
FROM nuevos_medicos m
CROSS JOIN (VALUES ('MONDAY'),('TUESDAY'),('WEDNESDAY'),('THURSDAY'),('FRIDAY')) d(d);

CREATE TEMP TABLE todos_pacientes AS
SELECT id, row_number() OVER (ORDER BY id) AS i FROM pacientes;
SELECT count(*) AS total_pacientes FROM todos_pacientes \gset

CREATE TEMP TABLE candidatos AS
SELECT h.medico_id,
       d::date + h.hora_inicio + (s * h.duracion_cita_minutos) * INTERVAL '1 minute' AS fh
FROM generate_series(CURRENT_DATE - 120, CURRENT_DATE + 21, INTERVAL '1 day') d
JOIN horarios_atencion h ON h.dia_semana = (ARRAY['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'])[EXTRACT(ISODOW FROM d)::int]
CROSS JOIN LATERAL generate_series(0, GREATEST(((EXTRACT(EPOCH FROM (h.hora_fin - h.hora_inicio)) / 60)::int / h.duracion_cita_minutos) - 1, 0)) s
WHERE random() < 0.40;

DELETE FROM candidatos c USING citas x WHERE x.medico_id = c.medico_id AND x.fecha_hora = c.fh;

CREATE TEMP TABLE citas_nuevas AS
SELECT gen_random_uuid() AS id, c.medico_id, c.fh,
       (SELECT tp.id FROM todos_pacientes tp WHERE tp.i = pz.ix) AS paciente_id,
       (SELECT especialidad_id FROM medico_especialidades me WHERE me.medico_id = c.medico_id ORDER BY random() LIMIT 1) AS especialidad_id,
       random() AS r1, random() AS r2
FROM candidatos c CROSS JOIN LATERAL (SELECT 1 + floor(random() * :total_pacientes)::int AS ix OFFSET 0) pz;

CREATE TEMP TABLE citas_final AS
SELECT cn.*,
       CASE WHEN cn.fh < NOW() THEN CASE WHEN r1 < 0.78 THEN 'ATENDIDA' ELSE 'CANCELADA' END
            ELSE CASE WHEN r1 < 0.06 THEN 'CANCELADA' WHEN r1 < 0.55 THEN 'CONFIRMADA' ELSE 'PENDIENTE' END END AS estado,
       CASE WHEN r2 < 0.15 THEN 'VIRTUAL' ELSE 'PRESENCIAL' END AS modalidad,
       e.precio_consulta, e.precio_virtual
FROM citas_nuevas cn JOIN especialidades e ON e.id = cn.especialidad_id
WHERE cn.paciente_id IS NOT NULL;

INSERT INTO citas (id, created_at, estado, fecha_hora, monto, motivo, recordatorio_enviado, updated_at, version, medico_id, paciente_id, especialidad_id, motivo_cancelacion, modalidad)
SELECT id, fh - INTERVAL '5 days', estado, fh,
       CASE WHEN modalidad = 'VIRTUAL' AND precio_virtual IS NOT NULL THEN precio_virtual ELSE precio_consulta END,
       (ARRAY['Control de rutina','Dolor de cabeza persistente','Dolor lumbar','Revisión de resultados','Chequeo preventivo','Tos y malestar general','Seguimiento de tratamiento','Consulta por alergias','Dolor abdominal','Evaluación anual'])[1 + floor(r2 * 10)::int % 10],
       fh < NOW(), fh - INTERVAL '2 days', 0, medico_id, paciente_id, especialidad_id,
       CASE WHEN estado = 'CANCELADA' THEN (ARRAY['Paciente no puede asistir','Cambio de horario','Reprogramación solicitada'])[1 + floor(r2 * 3)::int % 3] END,
       modalidad
FROM citas_final;

CREATE TEMP TABLE cie_num AS
SELECT id, codigo, descripcion, row_number() OVER (ORDER BY codigo) AS i FROM catalogo_cie10 WHERE activo;
SELECT count(*) AS total_cie FROM cie_num \gset

CREATE TEMP TABLE atenciones_nuevas AS
SELECT gen_random_uuid() AS id, f.id AS cita_id, f.medico_id, f.paciente_id, f.fh,
       (SELECT cn.id FROM cie_num cn WHERE cn.i = cz.ix) AS cie_id
FROM citas_final f CROSS JOIN LATERAL (SELECT 1 + floor(random() * :total_cie)::int AS ix OFFSET 0) cz WHERE f.estado = 'ATENDIDA';

INSERT INTO atenciones (id, diagnostico, fecha, frecuencia_cardiaca, indicaciones, observaciones, peso_kg, presion_arterial, talla_cm, temperatura_c, tratamiento, cita_id, medico_id, paciente_id)
SELECT a.id, c.descripcion, a.fh + INTERVAL '25 minutes', 60 + floor(random() * 40)::int,
       'Control en 15 días si persisten los síntomas', 'Paciente estable al examen físico',
       round((50 + random() * 45)::numeric, 1), (105 + floor(random() * 35)::int) || '/' || (65 + floor(random() * 25)::int),
       round((150 + random() * 40)::numeric, 1), round((36.2 + random() * 1.6)::numeric, 1),
       (ARRAY['Reposo e hidratación','Antiinflamatorio por 5 días','Dieta y control de peso','Fisioterapia','Antibiótico por 7 días','Medidas generales'])[1 + floor(random() * 6)::int % 6],
       a.cita_id, a.medico_id, a.paciente_id
FROM atenciones_nuevas a JOIN cie_num c ON c.id = a.cie_id;

INSERT INTO atencion_diagnosticos (atencion_id, cie10_id, codigo, descripcion, tipo, principal, observacion, posicion)
SELECT a.id, c.id, c.codigo, c.descripcion, 'DEFINITIVO', true, NULL, 0
FROM atenciones_nuevas a JOIN cie_num c ON c.id = a.cie_id;

INSERT INTO atencion_receta (atencion_id, dosis, duracion, frecuencia, medicamento, posicion, dispensado)
SELECT a.id, m.dosis, m.dur, m.frec, m.med, p.pos, random() < 0.6
FROM atenciones_nuevas a
CROSS JOIN LATERAL (VALUES (0), (1)) p(pos)
JOIN LATERAL (
  SELECT * FROM (VALUES
    ('Paracetamol 500 mg','1 tableta','5 días','cada 8 horas'),
    ('Ibuprofeno 400 mg','1 tableta','5 días','cada 8 horas'),
    ('Amoxicilina 500 mg','1 cápsula','7 días','cada 8 horas'),
    ('Loratadina 10 mg','1 tableta','7 días','cada 24 horas'),
    ('Omeprazol 20 mg','1 cápsula','14 días','cada 24 horas'),
    ('Losartán 50 mg','1 tableta','30 días','cada 24 horas'),
    ('Metformina 850 mg','1 tableta','30 días','cada 12 horas'),
    ('Diclofenaco 50 mg','1 tableta','5 días','cada 12 horas')
  ) v(med, dosis, dur, frec) ORDER BY random() LIMIT 1
) m ON true
WHERE p.pos = 0 OR random() < 0.5;

INSERT INTO pagos (codigo_cupon, comprobante, descuento, estado, fecha_pago, metodo_descripcion, monto, total, cita_id, metodo_pago_id)
SELECT NULL, 'B900-' || lpad(row_number() OVER (ORDER BY f.fh)::text, 6, '0'), 0, 'PAGADO',
       f.fh - INTERVAL '2 days',
       (ARRAY['VISA terminada en 4242','MASTERCARD terminada en 4444','YAPE','PLIN','VISA terminada en 1881'])[1 + floor(f.r2 * 5)::int % 5],
       CASE WHEN f.modalidad = 'VIRTUAL' AND f.precio_virtual IS NOT NULL THEN f.precio_virtual ELSE f.precio_consulta END,
       CASE WHEN f.modalidad = 'VIRTUAL' AND f.precio_virtual IS NOT NULL THEN f.precio_virtual ELSE f.precio_consulta END,
       f.id, NULL
FROM citas_final f WHERE f.estado IN ('ATENDIDA', 'CONFIRMADA') AND f.r1 < 0.97;

INSERT INTO valoraciones (comentario, created_at, puntuacion, cita_id, medico_id, paciente_id)
SELECT (ARRAY['Excelente atención','Muy amable y claro','Buena explicación','Tuve que esperar un poco','Todo en orden','Recomendado'])[1 + floor(f.r2 * 6)::int % 6],
       f.fh + INTERVAL '1 day', 3 + floor(random() * 3)::int, f.id, f.medico_id, f.paciente_id
FROM citas_final f WHERE f.estado = 'ATENDIDA' AND f.r2 < 0.35;

CREATE TEMP TABLE ord_lab AS
SELECT gen_random_uuid() AS id, row_number() OVER (ORDER BY a.fh) AS n, a.id AS atencion_id, a.medico_id, a.paciente_id, a.fh,
       (SELECT id FROM examenes_laboratorio WHERE activo ORDER BY random() LIMIT 1) AS examen_id
FROM atenciones_nuevas a WHERE random() < 0.25;

INSERT INTO ordenes_laboratorio (id, codigo, fecha, estado, prioridad, paciente_id, medico_id, atencion_id, indicaciones, total, toma_muestra_en, completada_en, created_at, updated_at, cobrado)
SELECT o.id, 'LAB-QA-' || lpad(o.n::text, 5, '0'), o.fh + INTERVAL '30 minutes', 'COMPLETADA',
       CASE WHEN o.n % 10 = 0 THEN 'URGENTE' ELSE 'NORMAL' END, o.paciente_id, o.medico_id, o.atencion_id,
       'Ayuno de 8 horas', e.precio, o.fh + INTERVAL '1 hour', o.fh + INTERVAL '1 day', o.fh + INTERVAL '30 minutes', o.fh + INTERVAL '1 day', true
FROM ord_lab o JOIN examenes_laboratorio e ON e.id = o.examen_id;

CREATE TEMP TABLE items_lab AS
SELECT gen_random_uuid() AS id, o.id AS orden_id, o.examen_id, o.fh FROM ord_lab o;

INSERT INTO items_orden_lab (id, orden_id, examen_id, estado, observaciones, fecha_resultado, validado_por_id, posicion)
SELECT i.id, i.orden_id, i.examen_id, 'VALIDADO', NULL, i.fh + INTERVAL '1 day',
       (SELECT id FROM usuarios WHERE email = 'lab@tusalud.com'), 0
FROM items_lab i;

INSERT INTO resultados_lab (item_id, parametro_id, parametro_nombre, unidad, valor, valor_min, valor_max, fuera_de_rango, posicion)
SELECT i.id, p.id, p.nombre, p.unidad,
       CASE WHEN p.valor_min IS NOT NULL AND p.valor_max IS NOT NULL
            THEN round((CASE WHEN x.fuera THEN p.valor_max * 1.15 ELSE p.valor_min + random() * (p.valor_max - p.valor_min) END)::numeric, 2)::text
            ELSE 'Negativo' END,
       p.valor_min, p.valor_max, (p.valor_min IS NOT NULL AND p.valor_max IS NOT NULL AND x.fuera), p.orden
FROM items_lab i
JOIN parametros_examen p ON p.examen_id = i.examen_id
CROSS JOIN LATERAL (SELECT random() < 0.12 AS fuera) x;

SELECT 'pacientes' AS t, count(*) FROM pacientes
UNION ALL SELECT 'medicos', count(*) FROM medicos
UNION ALL SELECT 'citas', count(*) FROM citas
UNION ALL SELECT 'atenciones', count(*) FROM atenciones
UNION ALL SELECT 'pagos', count(*) FROM pagos
UNION ALL SELECT 'ordenes_lab', count(*) FROM ordenes_laboratorio
UNION ALL SELECT 'valoraciones', count(*) FROM valoraciones;

COMMIT;
