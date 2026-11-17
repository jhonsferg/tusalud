INSERT INTO eventos_calendario (usuario_id, titulo, descripcion, ubicacion, tipo, inicio, fin, todo_el_dia, color, completado, recordatorio_minutos, recordatorio_enviado, created_at, updated_at)
SELECT u.id, 'Tomar la medicación', 'Antihipertensivo con el desayuno', NULL, 'RECORDATORIO',
       CURRENT_DATE + 1 + TIME '08:00', CURRENT_DATE + 1 + TIME '08:15', false, 'NARANJA', false, 15, false, NOW(), NOW()
FROM usuarios u WHERE u.email = 'juan.perez@correo.com';

INSERT INTO eventos_calendario (usuario_id, titulo, descripcion, ubicacion, tipo, inicio, fin, todo_el_dia, color, completado, recordatorio_minutos, recordatorio_enviado, created_at, updated_at)
SELECT u.id, 'Cita con el dentista particular', 'Limpieza dental', 'Clínica dental del centro', 'PERSONAL',
       CURRENT_DATE + 3 + TIME '15:00', CURRENT_DATE + 3 + TIME '16:00', false, 'AZUL', false, 30, false, NOW(), NOW()
FROM usuarios u WHERE u.email = 'juan.perez@correo.com';

INSERT INTO eventos_calendario (usuario_id, titulo, descripcion, ubicacion, tipo, inicio, fin, todo_el_dia, color, completado, recordatorio_minutos, recordatorio_enviado, created_at, updated_at)
SELECT u.id, 'Reunión del servicio de cardiología', 'Casos clínicos de la semana', 'Sala de juntas 2', 'REUNION',
       CURRENT_DATE + 2 + TIME '12:00', CURRENT_DATE + 2 + TIME '12:30', false, 'MORADO', false, 15, false, NOW(), NOW()
FROM usuarios u WHERE u.email = 'dr.garcia@tusalud.com';

INSERT INTO eventos_calendario (usuario_id, titulo, descripcion, ubicacion, tipo, inicio, fin, todo_el_dia, color, completado, recordatorio_minutos, recordatorio_enviado, created_at, updated_at)
SELECT u.id, 'Bloqueo de agenda', 'Capacitación interna, sin atención a pacientes', NULL, 'BLOQUEO',
       d.dia + TIME '11:00', d.dia + TIME '12:00', false, 'GRIS', false, NULL, false, NOW(), NOW()
FROM usuarios u
CROSS JOIN LATERAL (
    SELECT (CURRENT_DATE + n) AS dia
    FROM generate_series(1, 14) AS n
    WHERE EXTRACT(ISODOW FROM CURRENT_DATE + n) BETWEEN 1 AND 5
      AND NOT EXISTS (
          SELECT 1 FROM citas c
          JOIN medicos m ON m.id = c.medico_id
          WHERE m.usuario_id = u.id
            AND c.estado IN ('PENDIENTE', 'CONFIRMADA')
            AND c.fecha_hora >= (CURRENT_DATE + n) + TIME '10:30'
            AND c.fecha_hora < (CURRENT_DATE + n) + TIME '12:00')
    ORDER BY n
    LIMIT 1
) d
WHERE u.email = 'dr.garcia@tusalud.com';

INSERT INTO eventos_calendario (usuario_id, titulo, descripcion, ubicacion, tipo, inicio, fin, todo_el_dia, color, completado, recordatorio_minutos, recordatorio_enviado, created_at, updated_at)
SELECT u.id, 'Revisar el reporte mensual de ingresos', NULL, NULL, 'RECORDATORIO',
       CURRENT_DATE + 4 + TIME '10:00', CURRENT_DATE + 4 + TIME '10:30', false, 'VERDE', false, 60, false, NOW(), NOW()
FROM usuarios u WHERE u.email = 'admin@tusalud.com';
