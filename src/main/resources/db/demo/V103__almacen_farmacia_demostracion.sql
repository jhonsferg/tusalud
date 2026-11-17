INSERT INTO proveedores (activo, contacto, created_at, direccion, email, razon_social, ruc, telefono) VALUES
    (true, 'Marco Villanueva', NOW(), 'Av. Argentina 1520, Callao', 'ventas@drogueriaandina.pe', 'Droguería Andina S.A.C.', '20512345678', '014512300'),
    (true, 'Elena Cabrera', NOW(), 'Jr. Cusco 340, Lima', 'pedidos@medpacifico.pe', 'Distribuidora Médica Pacífico S.A.', '20498765432', '016234500');

INSERT INTO productos (activo, codigo, created_at, descripcion, nombre, precio_venta, presentacion, principio_activo, requiere_receta, stock_minimo, tipo, unidad, updated_at) VALUES
    (true, 'MED-PARA500', NOW(), 'Analgésico y antipirético', 'Paracetamol 500 mg', 0.30, 'Caja x 100 tabletas', 'Paracetamol', false, 100, 'MEDICAMENTO', 'tableta', NOW()),
    (true, 'MED-IBU400', NOW(), 'Antiinflamatorio no esteroideo', 'Ibuprofeno 400 mg', 0.60, 'Caja x 100 tabletas', 'Ibuprofeno', false, 80, 'MEDICAMENTO', 'tableta', NOW()),
    (true, 'MED-AMOX500', NOW(), 'Antibiótico de amplio espectro', 'Amoxicilina 500 mg', 1.20, 'Caja x 50 cápsulas', 'Amoxicilina', true, 60, 'MEDICAMENTO', 'cápsula', NOW()),
    (true, 'MED-LOSA50', NOW(), 'Antihipertensivo', 'Losartán 50 mg', 0.90, 'Caja x 30 tabletas', 'Losartán potásico', true, 60, 'MEDICAMENTO', 'tableta', NOW()),
    (true, 'MED-METF850', NOW(), 'Antidiabético oral', 'Metformina 850 mg', 0.70, 'Caja x 60 tabletas', 'Metformina', true, 60, 'MEDICAMENTO', 'tableta', NOW()),
    (true, 'MED-OMEP20', NOW(), 'Inhibidor de la bomba de protones', 'Omeprazol 20 mg', 0.80, 'Caja x 28 cápsulas', 'Omeprazol', false, 40, 'MEDICAMENTO', 'cápsula', NOW()),
    (true, 'MED-LORA10', NOW(), 'Antihistamínico', 'Loratadina 10 mg', 0.50, 'Caja x 30 tabletas', 'Loratadina', false, 50, 'MEDICAMENTO', 'tableta', NOW()),
    (true, 'MED-SALB100', NOW(), 'Broncodilatador de rescate', 'Salbutamol inhalador 100 mcg', 22.00, 'Inhalador de 200 dosis', 'Salbutamol', true, 10, 'MEDICAMENTO', 'inhalador', NOW()),
    (true, 'MED-ENAL10', NOW(), 'Antihipertensivo', 'Enalapril 10 mg', 0.45, 'Caja x 30 tabletas', 'Enalapril', true, 60, 'MEDICAMENTO', 'tableta', NOW()),
    (true, 'MED-NAPR550', NOW(), 'Antiinflamatorio y analgésico', 'Naproxeno 550 mg', 1.10, 'Caja x 20 tabletas', 'Naproxeno sódico', true, 40, 'MEDICAMENTO', 'tableta', NOW()),
    (true, 'MED-DICLGEL', NOW(), 'Antiinflamatorio tópico', 'Diclofenaco gel 1 %', 14.50, 'Tubo de 60 g', 'Diclofenaco', false, 15, 'MEDICAMENTO', 'tubo', NOW()),
    (true, 'MED-LAGRIMA', NOW(), 'Lubricante ocular', 'Lágrimas artificiales', 28.00, 'Frasco de 15 ml', 'Hipromelosa', false, 10, 'MEDICAMENTO', 'frasco', NOW()),
    (true, 'MED-VITD1000', NOW(), 'Suplemento vitamínico', 'Vitamina D3 1000 UI', 0.90, 'Frasco x 60 cápsulas', 'Colecalciferol', false, 40, 'MEDICAMENTO', 'cápsula', NOW()),
    (true, 'INS-GUANTES', NOW(), 'Guantes descartables de látex', 'Guantes de látex talla M', 0.00, 'Caja x 100 unidades', NULL, false, 20, 'INSUMO', 'caja', NOW()),
    (true, 'INS-JERINGA5', NOW(), 'Jeringa descartable estéril', 'Jeringa 5 ml con aguja', 0.00, 'Caja x 100 unidades', NULL, false, 100, 'INSUMO', 'unidad', NOW()),
    (true, 'INS-GASA', NOW(), 'Gasa estéril 10 x 10 cm', 'Gasa estéril', 0.00, 'Paquete x 10 unidades', NULL, false, 50, 'INSUMO', 'paquete', NOW()),
    (true, 'REA-GLUCOSA', NOW(), 'Reactivo enzimático para glucosa', 'Reactivo de glucosa', 0.00, 'Kit de 500 ml', NULL, false, 4, 'REACTIVO', 'kit', NOW()),
    (true, 'REA-HEMOGLO', NOW(), 'Reactivo para hemoglobina', 'Reactivo de hemoglobina', 0.00, 'Kit de 250 ml', NULL, false, 4, 'REACTIVO', 'kit', NOW()),
    (true, 'MAT-TERMO', NOW(), 'Termómetro clínico digital', 'Termómetro digital', 18.00, 'Unidad', NULL, false, 5, 'MATERIAL', 'unidad', NOW());

INSERT INTO ingresos (documento, fecha, observaciones, proveedor_id, total, usuario_id) VALUES
    ('F001-00045', NOW() - INTERVAL '20 days', 'Reposición mensual de medicamentos', (SELECT id FROM proveedores WHERE ruc = '20512345678'), 0, (SELECT id FROM usuarios WHERE email = 'almacen@tusalud.com')),
    ('F001-00046', NOW() - INTERVAL '18 days', 'Insumos, reactivos y materiales', (SELECT id FROM proveedores WHERE ruc = '20498765432'), 0, (SELECT id FROM usuarios WHERE email = 'almacen@tusalud.com'));

INSERT INTO lotes (cantidad_actual, cantidad_inicial, costo_unitario, created_at, fecha_vencimiento, numero_lote, ubicacion, ingreso_id, producto_id, proveedor_id)
SELECT v.actual, v.inicial, v.costo, NOW() - INTERVAL '20 days', CURRENT_DATE + v.dias, v.lote, v.ubicacion, i.id, p.id, i.proveedor_id
FROM (VALUES
    ('F001-00045', 'MED-PARA500', 'PAR-2501', 400, 500, 480, 0.12, 'Estante A1'),
    ('F001-00045', 'MED-PARA500', 'PAR-2402', 25, 100, 80, 0.11, 'Estante A1'),
    ('F001-00045', 'MED-IBU400', 'IBU-2501', 300, 300, 300, 0.25, 'Estante A1'),
    ('F001-00045', 'MED-AMOX500', 'AMO-2301', -10, 60, 30, 0.55, 'Estante A2'),
    ('F001-00045', 'MED-AMOX500', 'AMO-2502', 200, 150, 150, 0.58, 'Estante A2'),
    ('F001-00045', 'MED-LOSA50', 'LOS-2501', 350, 240, 240, 0.35, 'Estante A2'),
    ('F001-00045', 'MED-METF850', 'MET-2501', 280, 200, 200, 0.28, 'Estante A3'),
    ('F001-00045', 'MED-OMEP20', 'OME-2501', 260, 60, 8, 0.30, 'Estante A3'),
    ('F001-00045', 'MED-LORA10', 'LOR-2501', 320, 120, 120, 0.18, 'Estante A3'),
    ('F001-00045', 'MED-SALB100', 'SAL-2501', 180, 12, 3, 12.50, 'Refrigerador 1'),
    ('F001-00045', 'MED-ENAL10', 'ENA-2501', 300, 180, 150, 0.15, 'Estante A4'),
    ('F001-00045', 'MED-NAPR550', 'NAP-2501', 250, 90, 80, 0.50, 'Estante A4'),
    ('F001-00045', 'MED-DICLGEL', 'DIC-2501', 220, 60, 60, 6.50, 'Estante B1'),
    ('F001-00045', 'MED-LAGRIMA', 'LAG-2501', 150, 45, 45, 13.00, 'Estante B1'),
    ('F001-00045', 'MED-VITD1000', 'VIT-2501', 400, 100, 100, 0.35, 'Estante B2'),
    ('F001-00046', 'INS-GUANTES', 'GUA-2501', 500, 60, 60, 12.00, 'Almacén 2'),
    ('F001-00046', 'INS-JERINGA5', 'JER-2501', 600, 400, 400, 0.15, 'Almacén 2'),
    ('F001-00046', 'INS-GASA', 'GAS-2501', 30, 200, 100, 0.40, 'Almacén 2'),
    ('F001-00046', 'REA-GLUCOSA', 'GLU-2501', 120, 12, 12, 85.00, 'Refrigerador 2'),
    ('F001-00046', 'REA-HEMOGLO', 'HEM-2501', 150, 8, 8, 95.00, 'Refrigerador 2'),
    ('F001-00046', 'MAT-TERMO', 'TER-2501', 700, 25, 24, 8.50, 'Almacén 1')
) AS v (documento, codigo, lote, dias, inicial, actual, costo, ubicacion)
JOIN productos p ON p.codigo = v.codigo
JOIN ingresos i ON i.documento = v.documento;

UPDATE ingresos SET total = (SELECT COALESCE(SUM(l.cantidad_inicial * l.costo_unitario), 0) FROM lotes l WHERE l.ingreso_id = ingresos.id);

INSERT INTO movimientos_inventario (cantidad, fecha, motivo, observaciones, referencia, tipo, lote_id, producto_id, usuario_id)
SELECT l.cantidad_inicial, i.fecha, 'COMPRA', NULL, 'ING-' || LPAD(DENSE_RANK() OVER (ORDER BY i.fecha, i.documento)::text, 6, '0'), 'ENTRADA', l.id, l.producto_id, i.usuario_id
FROM lotes l JOIN ingresos i ON i.id = l.ingreso_id;

INSERT INTO dispensaciones (comprobante, estado, fecha, metodo_pago, observaciones, total, atencion_id, paciente_id, usuario_id)
SELECT 'F001-000001', 'EMITIDA', NOW() - INTERVAL '2 days', 'EFECTIVO', NULL, 13.50, a.id, a.paciente_id, (SELECT id FROM usuarios WHERE email = 'farmacia@tusalud.com')
FROM atenciones a JOIN citas c ON c.id = a.cita_id WHERE c.motivo = 'Seguimiento de hipertensión';

INSERT INTO dispensaciones (comprobante, estado, fecha, metodo_pago, observaciones, total, atencion_id, paciente_id, usuario_id)
SELECT 'F001-000002', 'EMITIDA', NOW() - INTERVAL '1 day', 'YAPE', 'Se completará el gel en la siguiente visita', 11.00, a.id, a.paciente_id, (SELECT id FROM usuarios WHERE email = 'farmacia@tusalud.com')
FROM atenciones a JOIN citas c ON c.id = a.cita_id WHERE c.motivo = 'Dolor de rodilla tras una lesión deportiva';

INSERT INTO dispensaciones (comprobante, estado, fecha, metodo_pago, observaciones, total, atencion_id, paciente_id, usuario_id)
SELECT 'F001-000003', 'EMITIDA', NOW() - INTERVAL '3 hours', 'TARJETA', 'Venta libre', 24.00, NULL, p.id, (SELECT id FROM usuarios WHERE email = 'farmacia@tusalud.com')
FROM pacientes p JOIN usuarios u ON u.id = p.usuario_id WHERE u.email = 'juan.perez@correo.com';

INSERT INTO dispensacion_items (cantidad, precio_unitario, receta_posicion, subtotal, dispensacion_id, producto_id)
SELECT v.cantidad, p.precio_venta, v.posicion, p.precio_venta * v.cantidad, d.id, p.id
FROM (VALUES
    ('F001-000001', 'MED-ENAL10', 30, 0),
    ('F001-000002', 'MED-NAPR550', 10, 0),
    ('F001-000003', 'MED-PARA500', 20, NULL),
    ('F001-000003', 'MAT-TERMO', 1, NULL)
) AS v (comprobante, codigo, cantidad, posicion)
JOIN dispensaciones d ON d.comprobante = v.comprobante
JOIN productos p ON p.codigo = v.codigo;

INSERT INTO movimientos_inventario (cantidad, fecha, motivo, observaciones, referencia, tipo, lote_id, producto_id, usuario_id)
SELECT v.cantidad, d.fecha, 'DISPENSACION', NULL, v.comprobante, 'SALIDA', l.id, l.producto_id, d.usuario_id
FROM (VALUES
    ('F001-000001', 'ENA-2501', 30),
    ('F001-000002', 'NAP-2501', 10),
    ('F001-000003', 'PAR-2501', 20),
    ('F001-000003', 'TER-2501', 1)
) AS v (comprobante, lote, cantidad)
JOIN dispensaciones d ON d.comprobante = v.comprobante
JOIN lotes l ON l.numero_lote = v.lote;

UPDATE atencion_receta SET dispensado = true
WHERE posicion = 0 AND atencion_id IN (
    SELECT a.id FROM atenciones a JOIN citas c ON c.id = a.cita_id
    WHERE c.motivo IN ('Seguimiento de hipertensión', 'Dolor de rodilla tras una lesión deportiva'));
