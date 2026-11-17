ALTER TABLE roles DROP CONSTRAINT roles_nombre_check;
ALTER TABLE roles ADD CONSTRAINT roles_nombre_check CHECK (nombre IN (
    'ROLE_ADMIN', 'ROLE_MEDICO', 'ROLE_PACIENTE', 'ROLE_LABORATORIO', 'ROLE_FARMACIA', 'ROLE_ALMACEN',
    'ROLE_RECEPCION', 'ROLE_CAJA', 'ROLE_ENFERMERIA', 'ROLE_GERENCIA', 'ROLE_FINANZAS', 'ROLE_OPERACIONES',
    'ROLE_RRHH', 'ROLE_CALIDAD'));

INSERT INTO roles (nombre)
SELECT v.nombre FROM (VALUES ('ROLE_GERENCIA'), ('ROLE_FINANZAS'), ('ROLE_OPERACIONES'), ('ROLE_RRHH'), ('ROLE_CALIDAD')) AS v (nombre)
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.nombre = v.nombre);

ALTER TABLE notificaciones DROP CONSTRAINT notificaciones_tipo_check;
ALTER TABLE notificaciones ADD CONSTRAINT notificaciones_tipo_check CHECK (tipo IN (
    'CITA_CREADA', 'CITA_CONFIRMADA', 'CITA_CANCELADA', 'CITA_ATENDIDA', 'CITA_REPROGRAMADA',
    'RECORDATORIO_CITA', 'RECORDATORIO_EVENTO', 'INICIO_SESION', 'CONTRASENA_CAMBIADA', 'PAGO_REGISTRADO',
    'PAGO_REEMBOLSADO', 'MFA_ACTIVADO', 'MFA_DESACTIVADO', 'PACIENTE_COMPARTIDO', 'ROLES_ACTUALIZADOS',
    'SESION_SOSPECHOSA', 'ORDEN_LABORATORIO', 'RESULTADO_LABORATORIO', 'RECETA_DISPENSADA', 'STOCK_BAJO',
    'PRODUCTO_POR_VENCER', 'PACIENTE_EN_ESPERA', 'TURNO_LLAMADO', 'COMPROBANTE_EMITIDO',
    'INGRESO_HOSPITALARIO', 'ALTA_HOSPITALARIA', 'EMERGENCIA_TRIAJE', 'CAJA_DIFERENCIA',
    'SOLICITUD_AUSENCIA', 'AUSENCIA_RESUELTA', 'SISTEMA'));

CREATE INDEX idx_citas_fecha_hora_estado ON citas (fecha_hora, estado);
CREATE INDEX idx_pagos_fecha_pago_estado ON pagos (fecha_pago, estado);
CREATE INDEX idx_valoraciones_created_at ON valoraciones (created_at);
CREATE INDEX idx_ordenes_lab_completada_en ON ordenes_laboratorio (completada_en);
CREATE INDEX idx_ingresos_hosp_fecha_alta ON ingresos_hospitalarios (fecha_alta);
CREATE INDEX idx_ingresos_hosp_fecha_ingreso ON ingresos_hospitalarios (fecha_ingreso);
CREATE INDEX idx_turnos_creado_en ON turnos (creado_en);
CREATE INDEX idx_sesiones_caja_cierre ON sesiones_caja (cierre);
CREATE INDEX idx_lotes_vencimiento ON lotes (fecha_vencimiento);
