UPDATE empleados
SET codigo_marcacion = upper(substring(replace(cast(id AS varchar(40)), '-', ''), 1, 8))
WHERE codigo_marcacion IS NULL;

INSERT INTO contratos_laborales (
    empleado_id, regimen, remuneracion, asignacion_familiar, sistema_pensionario, afp_nombre, horas_semanales,
    banco, cuenta, cargo, fecha_inicio, fecha_fin, estado, creado_por_id)
SELECT
    e.id,
    'GENERAL',
    e.sueldo,
    (MOD(ascii(substring(cast(e.id AS varchar(40)), 1, 1)), 3) = 0),
    CASE WHEN MOD(ascii(substring(cast(e.id AS varchar(40)), 2, 1)), 2) = 0 THEN 'AFP' ELSE 'ONP' END,
    CASE WHEN MOD(ascii(substring(cast(e.id AS varchar(40)), 2, 1)), 2) = 0 THEN 'Integra' ELSE NULL END,
    48,
    'Banco de Crédito',
    '19' || substring(replace(cast(e.id AS varchar(40)), '-', ''), 1, 8),
    e.cargo,
    e.fecha_ingreso,
    e.fecha_fin_contrato,
    'VIGENTE',
    (SELECT u.id FROM usuarios u WHERE u.email = 'admin@tusalud.com')
FROM empleados e
WHERE e.estado <> 'CESADO'
  AND NOT EXISTS (SELECT 1 FROM contratos_laborales c WHERE c.empleado_id = e.id);

INSERT INTO empleado_conceptos (empleado_id, concepto_id, valor, vigente_desde, activo, creado_por_id)
SELECT e.id, c.id, 150, CURRENT_DATE - 60, true, (SELECT u.id FROM usuarios u WHERE u.email = 'admin@tusalud.com')
FROM empleados e
JOIN conceptos_planilla c ON c.codigo = 'BONO_PRODUCTIVIDAD'
WHERE e.area = 'MEDICA' AND e.estado = 'ACTIVO';
