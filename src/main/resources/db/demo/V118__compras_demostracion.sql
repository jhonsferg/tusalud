UPDATE proveedores SET condicion_pago_dias = 30, plazo_entrega_dias = 3, banco = 'BCP', cuenta_bancaria = '191-2345678-0-11' WHERE ruc = '20512345678';
UPDATE proveedores SET condicion_pago_dias = 15, plazo_entrega_dias = 5, banco = 'Interbank', cuenta_bancaria = '200-3012345678' WHERE ruc = '20498765432';

INSERT INTO proveedores (activo, contacto, created_at, direccion, email, razon_social, ruc, telefono, condicion_pago_dias, plazo_entrega_dias, banco, cuenta_bancaria, notas) VALUES
    (true, 'Lucía Paredes', NOW(), 'Av. Venezuela 2100, Lima', 'ventas@biomedica.pe', 'Biomédica del Perú S.A.C.', '20601234565', '017654300', 30, 7, 'BBVA', '0011-0345-0200123456', 'Insumos y material de curación'),
    (true, 'Héctor Salas', NOW(), 'Jr. Lampa 780, Lima', 'pedidos@farmaprime.pe', 'Farmaprime Distribuciones E.I.R.L.', '20509876518', '014443322', 0, 2, 'Scotiabank', '000-1234567', 'Pago al contado'),
    (true, 'Rosa Quispe', NOW(), 'Av. Colonial 1450, Callao', 'ventas@labsur.pe', 'Laboratorios del Sur S.A.', '20555667711', '015556677', 45, 10, 'BCP', '194-9988776-0-22', 'Reactivos y descartables de laboratorio');

INSERT INTO requisiciones (codigo, estado, prioridad, area, solicitante_id, motivo, sugerida, creada_en, enviada_en)
SELECT 'REQ-2026-000001', 'ENVIADA', 'URGENTE', 'FARMACIA', u.id, 'Reposición de analgésicos para la farmacia', false, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'
FROM usuarios u WHERE u.email = 'farmacia@tusalud.com';

INSERT INTO requisicion_items (requisicion_id, producto_id, cantidad, observacion, posicion)
SELECT r.id, p.id, 300, 'Stock por debajo del mínimo', 0
FROM requisiciones r, productos p WHERE r.codigo = 'REQ-2026-000001' AND p.codigo = 'MED-PARA500';

INSERT INTO requisicion_items (requisicion_id, producto_id, cantidad, observacion, posicion)
SELECT r.id, p.id, 200, NULL, 1
FROM requisiciones r, productos p WHERE r.codigo = 'REQ-2026-000001' AND p.codigo = 'MED-IBU400';

INSERT INTO secuencias_compras (tipo, anio, ultimo) VALUES ('REQ', 2026, 1) ON CONFLICT (tipo, anio) DO NOTHING;

INSERT INTO ordenes_compra (estado, proveedor_id, creada_por_id, nivel_aprobacion, afecta_igv, subtotal, igv, total, condicion_pago_dias, fecha_entrega_estimada, observaciones, creada_en, enviada_en)
SELECT 'PENDIENTE_APROBACION', pv.id, u.id, 'FINANZAS', true, 180.00, 32.40, 212.40, 30, CURRENT_DATE + 5, 'Reposición mensual de insumos', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'
FROM proveedores pv, usuarios u WHERE pv.ruc = '20512345678' AND u.email = 'almacen@tusalud.com';

INSERT INTO orden_compra_items (orden_id, producto_id, cantidad, precio_unitario, subtotal, posicion)
SELECT o.id, p.id, 150, 0.80, 120.00, 0
FROM ordenes_compra o, productos p WHERE o.estado = 'PENDIENTE_APROBACION' AND o.observaciones = 'Reposición mensual de insumos' AND p.codigo = 'MED-LORA10';

INSERT INTO orden_compra_items (orden_id, producto_id, cantidad, precio_unitario, subtotal, posicion)
SELECT o.id, p.id, 100, 0.60, 60.00, 1
FROM ordenes_compra o, productos p WHERE o.estado = 'PENDIENTE_APROBACION' AND o.observaciones = 'Reposición mensual de insumos' AND p.codigo = 'MED-ENAL10';

INSERT INTO comprobantes_proveedor (proveedor_id, tipo, serie, numero, ruc_emisor, fecha_emision, fecha_vencimiento, subtotal, igv, total, monto_pagado, monto_credito, estado, observaciones, registrado_por_id, registrado_en)
SELECT pv.id, 'FACTURA', 'F001', '4501', pv.ruc, CURRENT_DATE - 50, CURRENT_DATE - 20, 850.00, 153.00, 1003.00, 0, 0, 'PENDIENTE', 'Medicamentos del mes anterior', u.id, NOW() - INTERVAL '50 days'
FROM proveedores pv, usuarios u WHERE pv.ruc = '20512345678' AND u.email = 'finanzas@tusalud.com';

INSERT INTO comprobantes_proveedor (proveedor_id, tipo, serie, numero, ruc_emisor, fecha_emision, fecha_vencimiento, subtotal, igv, total, monto_pagado, monto_credito, estado, observaciones, registrado_por_id, registrado_en)
SELECT pv.id, 'FACTURA', 'F002', '188', pv.ruc, CURRENT_DATE - 10, CURRENT_DATE + 5, 400.00, 72.00, 472.00, 200.00, 0, 'PARCIAL', 'Material de curación', u.id, NOW() - INTERVAL '10 days'
FROM proveedores pv, usuarios u WHERE pv.ruc = '20601234565' AND u.email = 'finanzas@tusalud.com';

INSERT INTO pagos_proveedor (comprobante_id, monto, metodo, referencia, fecha, registrado_por_id, registrado_en)
SELECT c.id, 200.00, 'TRANSFERENCIA', 'OP-2026-0345', CURRENT_DATE - 5, u.id, NOW() - INTERVAL '5 days'
FROM comprobantes_proveedor c, usuarios u WHERE c.serie = 'F002' AND c.numero = '188' AND u.email = 'finanzas@tusalud.com';
