INSERT INTO sesiones_caja (cajero_id, apertura, monto_apertura, cierre, estado, observaciones)
SELECT u.id, (CURRENT_DATE - 1) + TIME '08:00', 200.00, (CURRENT_DATE - 1) + TIME '18:00', 'CERRADA', NULL
FROM usuarios u WHERE u.email = 'caja@tusalud.com';

INSERT INTO comprobantes (tipo, serie, numero, codigo, fecha, cliente_tipo_doc, cliente_doc, cliente_nombre, subtotal, igv, total,
                          metodo_pago, estado, origen, referencia_id, paciente_id, caja_id)
SELECT 'BOLETA', 'B001', b.n, 'B001-' || LPAD(b.n::text, 6, '0'), (CURRENT_DATE - 1) + TIME '09:00' + (b.n * INTERVAL '40 minutes'),
       'DNI', pa.dni, uu.nombre || ' ' || uu.apellido,
       ROUND(b.total / 1.18, 2), b.total - ROUND(b.total / 1.18, 2), b.total,
       CASE WHEN b.metodo LIKE 'Efectivo%' THEN 'EFECTIVO' ELSE 'TARJETA' END,
       'EMITIDO', 'CITA', b.cita_id, b.paciente_id,
       (SELECT s.id FROM sesiones_caja s JOIN usuarios u ON u.id = s.cajero_id WHERE u.email = 'caja@tusalud.com' ORDER BY s.id LIMIT 1)
FROM (
    SELECT ROW_NUMBER() OVER (ORDER BY c.fecha_hora, c.id) AS n, c.id AS cita_id, c.paciente_id, p.total, p.metodo_descripcion AS metodo
    FROM pagos p
    JOIN citas c ON c.id = p.cita_id
    WHERE c.estado = 'ATENDIDA' AND p.estado = 'PAGADO'
    ORDER BY c.fecha_hora, c.id
    LIMIT 4
) b
JOIN pacientes pa ON pa.id = b.paciente_id
JOIN usuarios uu ON uu.id = pa.usuario_id;

INSERT INTO comprobante_items (comprobante_id, descripcion, cantidad, precio_unitario, subtotal)
SELECT cp.id, 'Consulta de ' || e.nombre || ' con ' || mu.nombre || ' ' || mu.apellido, 1, cp.total, cp.total
FROM comprobantes cp
JOIN citas c ON c.id = cp.referencia_id AND cp.origen = 'CITA'
JOIN especialidades e ON e.id = c.especialidad_id
JOIN medicos m ON m.id = c.medico_id
JOIN usuarios mu ON mu.id = m.usuario_id;

INSERT INTO comprobantes (tipo, serie, numero, codigo, fecha, cliente_tipo_doc, cliente_doc, cliente_nombre, subtotal, igv, total,
                          metodo_pago, estado, origen, caja_id)
SELECT 'FACTURA', 'F001', 1, 'F001-000001', (CURRENT_DATE - 1) + TIME '15:30', 'RUC', '20512345678',
       'Corporación Salud Ocupacional SAC', ROUND(590.00 / 1.18, 2), 590.00 - ROUND(590.00 / 1.18, 2), 590.00,
       'TRANSFERENCIA', 'EMITIDO', 'SERVICIO', s.id
FROM sesiones_caja s JOIN usuarios u ON u.id = s.cajero_id WHERE u.email = 'caja@tusalud.com';

INSERT INTO comprobante_items (comprobante_id, descripcion, cantidad, precio_unitario, subtotal)
SELECT cp.id, 'Evaluación médica ocupacional', 5, 118.00, 590.00 FROM comprobantes cp WHERE cp.codigo = 'F001-000001';

INSERT INTO comprobantes (tipo, serie, numero, codigo, fecha, cliente_tipo_doc, cliente_doc, cliente_nombre, subtotal, igv, total,
                          metodo_pago, estado, motivo_anulacion, anulado_en, origen, caja_id)
SELECT 'BOLETA', 'B001', 5, 'B001-000005', (CURRENT_DATE - 1) + TIME '16:45', 'SIN_DOC', NULL, 'Cliente de mostrador',
       ROUND(45.00 / 1.18, 2), 45.00 - ROUND(45.00 / 1.18, 2), 45.00,
       'EFECTIVO', 'ANULADO', 'Se emitió por error con otro monto', (CURRENT_DATE - 1) + TIME '16:50', 'SERVICIO', s.id
FROM sesiones_caja s JOIN usuarios u ON u.id = s.cajero_id WHERE u.email = 'caja@tusalud.com';

INSERT INTO comprobante_items (comprobante_id, descripcion, cantidad, precio_unitario, subtotal)
SELECT cp.id, 'Certificado médico simple', 1, 45.00, 45.00 FROM comprobantes cp WHERE cp.codigo = 'B001-000005';

UPDATE sesiones_caja s
SET monto_esperado = s.monto_apertura + COALESCE((
        SELECT SUM(c.total) FROM comprobantes c WHERE c.caja_id = s.id AND c.estado = 'EMITIDO' AND c.metodo_pago = 'EFECTIVO'), 0),
    monto_contado = s.monto_apertura + COALESCE((
        SELECT SUM(c.total) FROM comprobantes c WHERE c.caja_id = s.id AND c.estado = 'EMITIDO' AND c.metodo_pago = 'EFECTIVO'), 0) - 5.00,
    diferencia = -5.00,
    observaciones = 'Faltante de S/ 5.00 por un vuelto mal entregado'
WHERE s.estado = 'CERRADA';
