INSERT INTO periodos_contables (anio, mes, estado, abierto_en)
SELECT EXTRACT(YEAR FROM CURRENT_DATE)::integer, EXTRACT(MONTH FROM CURRENT_DATE)::integer, 'ABIERTO', NOW()
ON CONFLICT (anio, mes) DO NOTHING;

INSERT INTO cuentas_tesoreria (nombre, tipo, banco, ultimos4, moneda, cuenta_contable_id, saldo_inicial, predeterminada, activa)
SELECT 'Caja general', 'CAJA', NULL, NULL, 'PEN', c.id, 0, false, true
FROM cuentas_contables c WHERE c.codigo = '1011';

INSERT INTO cuentas_tesoreria (nombre, tipo, banco, ultimos4, moneda, cuenta_contable_id, saldo_inicial, predeterminada, activa)
SELECT 'BCP cuenta corriente', 'BANCO', 'BCP', '1234', 'PEN', c.id, 0, true, true
FROM cuentas_contables c WHERE c.codigo = '1041';

INSERT INTO movimientos_tesoreria (cuenta_id, fecha, tipo, monto, descripcion, referencia, origen, conciliado, registrado_por_id, registrado_en)
SELECT t.id, CURRENT_DATE, 'INGRESO', 20000.00, 'Aporte de capital inicial', 'OP-0001', 'MANUAL', false, u.id, NOW()
FROM cuentas_tesoreria t, usuarios u
WHERE t.nombre = 'BCP cuenta corriente' AND u.email = 'finanzas@tusalud.com';

INSERT INTO asientos (numero, fecha, periodo_id, glosa, origen, referencia_id, estado, total_debe, total_haber, creado_por_id, creado_en, contabilizado_por_id, contabilizado_en)
SELECT 'ASI-' || EXTRACT(YEAR FROM CURRENT_DATE)::integer || '-000001', CURRENT_DATE, p.id,
       'Ingreso en BCP cuenta corriente: Aporte de capital inicial', 'MOVIMIENTO_TESORERIA', m.id, 'CONTABILIZADO',
       20000.00, 20000.00, u.id, NOW(), u.id, NOW()
FROM periodos_contables p, movimientos_tesoreria m, usuarios u
WHERE p.anio = EXTRACT(YEAR FROM CURRENT_DATE)::integer AND p.mes = EXTRACT(MONTH FROM CURRENT_DATE)::integer
  AND m.descripcion = 'Aporte de capital inicial' AND u.email = 'finanzas@tusalud.com';

INSERT INTO asiento_lineas (asiento_id, posicion, cuenta_id, debe, haber, glosa)
SELECT a.id, 0, c.id, 20000.00, 0, 'Aporte de capital inicial'
FROM asientos a, cuentas_contables c
WHERE a.glosa = 'Ingreso en BCP cuenta corriente: Aporte de capital inicial' AND c.codigo = '1041';

INSERT INTO asiento_lineas (asiento_id, posicion, cuenta_id, debe, haber, glosa)
SELECT a.id, 1, c.id, 0, 20000.00, 'Aporte de capital inicial'
FROM asientos a, cuentas_contables c
WHERE a.glosa = 'Ingreso en BCP cuenta corriente: Aporte de capital inicial' AND c.codigo = '5011';

UPDATE movimientos_tesoreria SET asiento_id = (SELECT a.id FROM asientos a WHERE a.referencia_id = movimientos_tesoreria.id)
WHERE descripcion = 'Aporte de capital inicial';

INSERT INTO secuencias_contables (tipo, anio, ultimo)
SELECT 'ASI', EXTRACT(YEAR FROM CURRENT_DATE)::integer, 1
ON CONFLICT (tipo, anio) DO NOTHING;

INSERT INTO extractos_bancarios (cuenta_id, archivo, desde, hasta, lineas, importado_por_id, importado_en)
SELECT t.id, 'extracto-demostracion.csv', CURRENT_DATE, CURRENT_DATE, 2, u.id, NOW()
FROM cuentas_tesoreria t, usuarios u
WHERE t.nombre = 'BCP cuenta corriente' AND u.email = 'finanzas@tusalud.com';

INSERT INTO extracto_lineas (extracto_id, cuenta_id, fecha, descripcion, referencia, monto, huella, estado)
SELECT e.id, e.cuenta_id, CURRENT_DATE, 'Abono transferencia OP-0001', 'OP-0001', 20000.00, md5('demo-extracto-1'), 'PENDIENTE'
FROM extractos_bancarios e WHERE e.archivo = 'extracto-demostracion.csv';

INSERT INTO extracto_lineas (extracto_id, cuenta_id, fecha, descripcion, referencia, monto, huella, estado)
SELECT e.id, e.cuenta_id, CURRENT_DATE, 'Comisión de mantenimiento de cuenta', '', -35.00, md5('demo-extracto-2'), 'PENDIENTE'
FROM extractos_bancarios e WHERE e.archivo = 'extracto-demostracion.csv';
