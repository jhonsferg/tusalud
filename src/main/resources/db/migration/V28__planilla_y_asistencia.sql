ALTER TABLE empleados ADD COLUMN codigo_marcacion varchar(12);

UPDATE empleados SET codigo_marcacion = upper(substring(replace(cast(id AS varchar(40)), '-', ''), 1, 8));

CREATE UNIQUE INDEX uk_empleados_codigo_marcacion ON empleados (codigo_marcacion);

CREATE TABLE parametros_planilla (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    codigo varchar(40) NOT NULL,
    valor numeric(14, 4) NOT NULL,
    vigente_desde date NOT NULL,
    vigente_hasta date,
    descripcion varchar(200) NOT NULL,
    por_validar boolean NOT NULL DEFAULT true,
    creado_por_id uuid,
    created_at timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_parametros_planilla PRIMARY KEY (id),
    CONSTRAINT uk_parametros_planilla_vigencia UNIQUE (codigo, vigente_desde),
    CONSTRAINT fk_parametros_planilla_usuario FOREIGN KEY (creado_por_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_parametros_planilla_codigo ON parametros_planilla (codigo, vigente_desde);

CREATE TABLE conceptos_planilla (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    codigo varchar(30) NOT NULL,
    nombre varchar(100) NOT NULL,
    tipo varchar(20) NOT NULL,
    modo varchar(25) NOT NULL,
    valor_defecto numeric(12, 4) NOT NULL DEFAULT 0,
    afecto_renta boolean NOT NULL DEFAULT true,
    afecto_pension boolean NOT NULL DEFAULT true,
    afecto_essalud boolean NOT NULL DEFAULT true,
    activo boolean NOT NULL DEFAULT true,
    sistema boolean NOT NULL DEFAULT false,
    orden integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_conceptos_planilla PRIMARY KEY (id),
    CONSTRAINT uk_conceptos_planilla_codigo UNIQUE (codigo),
    CONSTRAINT ck_conceptos_planilla_tipo CHECK (tipo IN ('INGRESO', 'DESCUENTO', 'APORTE_EMPLEADOR')),
    CONSTRAINT ck_conceptos_planilla_modo CHECK (modo IN ('MONTO_FIJO', 'PORCENTAJE_BASICO')),
    CONSTRAINT ck_conceptos_planilla_valor CHECK (valor_defecto >= 0)
);

CREATE TABLE empleado_conceptos (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    empleado_id uuid NOT NULL,
    concepto_id uuid NOT NULL,
    valor numeric(12, 4) NOT NULL,
    vigente_desde date NOT NULL,
    vigente_hasta date,
    activo boolean NOT NULL DEFAULT true,
    creado_por_id uuid NOT NULL,
    created_at timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_empleado_conceptos PRIMARY KEY (id),
    CONSTRAINT fk_empleado_conceptos_empleado FOREIGN KEY (empleado_id) REFERENCES empleados (id),
    CONSTRAINT fk_empleado_conceptos_concepto FOREIGN KEY (concepto_id) REFERENCES conceptos_planilla (id),
    CONSTRAINT fk_empleado_conceptos_usuario FOREIGN KEY (creado_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_empleado_conceptos_valor CHECK (valor >= 0)
);

CREATE INDEX idx_empleado_conceptos_empleado ON empleado_conceptos (empleado_id, activo);

CREATE TABLE contratos_laborales (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    empleado_id uuid NOT NULL,
    regimen varchar(20) NOT NULL,
    remuneracion numeric(12, 2) NOT NULL,
    asignacion_familiar boolean NOT NULL DEFAULT false,
    sistema_pensionario varchar(5) NOT NULL,
    afp_nombre varchar(30),
    horas_semanales integer NOT NULL DEFAULT 48,
    banco varchar(60),
    cuenta varchar(30),
    cargo varchar(100) NOT NULL,
    fecha_inicio date NOT NULL,
    fecha_fin date,
    estado varchar(15) NOT NULL,
    creado_por_id uuid NOT NULL,
    created_at timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_contratos_laborales PRIMARY KEY (id),
    CONSTRAINT fk_contratos_laborales_empleado FOREIGN KEY (empleado_id) REFERENCES empleados (id),
    CONSTRAINT fk_contratos_laborales_usuario FOREIGN KEY (creado_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_contratos_laborales_regimen CHECK (regimen IN ('GENERAL', 'PEQUENA_EMPRESA', 'MICROEMPRESA', 'PRACTICAS')),
    CONSTRAINT ck_contratos_laborales_pension CHECK (sistema_pensionario IN ('ONP', 'AFP')),
    CONSTRAINT ck_contratos_laborales_estado CHECK (estado IN ('VIGENTE', 'FINALIZADO')),
    CONSTRAINT ck_contratos_laborales_remuneracion CHECK (remuneracion > 0),
    CONSTRAINT ck_contratos_laborales_horas CHECK (horas_semanales BETWEEN 1 AND 60),
    CONSTRAINT ck_contratos_laborales_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);

CREATE INDEX idx_contratos_laborales_empleado ON contratos_laborales (empleado_id, estado, fecha_inicio);

CREATE TABLE terminales_asistencia (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    nombre varchar(80) NOT NULL,
    token_hash varchar(100) NOT NULL,
    activo boolean NOT NULL DEFAULT true,
    ultimo_uso timestamp(6),
    creado_por_id uuid NOT NULL,
    created_at timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_terminales_asistencia PRIMARY KEY (id),
    CONSTRAINT uk_terminales_asistencia_nombre UNIQUE (nombre),
    CONSTRAINT uk_terminales_asistencia_token UNIQUE (token_hash),
    CONSTRAINT fk_terminales_asistencia_usuario FOREIGN KEY (creado_por_id) REFERENCES usuarios (id)
);

CREATE TABLE marcaciones (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    empleado_id uuid NOT NULL,
    tipo varchar(10) NOT NULL,
    fecha_hora timestamp(6) NOT NULL,
    origen varchar(10) NOT NULL,
    terminal_id uuid,
    registrado_por_id uuid,
    observacion varchar(200),
    created_at timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_marcaciones PRIMARY KEY (id),
    CONSTRAINT fk_marcaciones_empleado FOREIGN KEY (empleado_id) REFERENCES empleados (id),
    CONSTRAINT fk_marcaciones_terminal FOREIGN KEY (terminal_id) REFERENCES terminales_asistencia (id),
    CONSTRAINT fk_marcaciones_usuario FOREIGN KEY (registrado_por_id) REFERENCES usuarios (id),
    CONSTRAINT uk_marcaciones_empleado_instante UNIQUE (empleado_id, fecha_hora, tipo),
    CONSTRAINT ck_marcaciones_tipo CHECK (tipo IN ('ENTRADA', 'SALIDA')),
    CONSTRAINT ck_marcaciones_origen CHECK (origen IN ('MANUAL', 'TERMINAL'))
);

CREATE INDEX idx_marcaciones_empleado_fecha ON marcaciones (empleado_id, fecha_hora);

CREATE TABLE asistencias_dia (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    empleado_id uuid NOT NULL,
    fecha date NOT NULL,
    turno_personal_id uuid,
    programada_inicio time,
    programada_fin time,
    entrada timestamp(6),
    salida timestamp(6),
    minutos_tardanza integer NOT NULL DEFAULT 0,
    minutos_trabajados integer NOT NULL DEFAULT 0,
    minutos_extra integer NOT NULL DEFAULT 0,
    estado varchar(20) NOT NULL,
    extra_destino varchar(15) NOT NULL DEFAULT 'PAGO',
    falta_justificada boolean NOT NULL DEFAULT false,
    justificacion varchar(300),
    ajustado_por_id uuid,
    actualizado_en timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_asistencias_dia PRIMARY KEY (id),
    CONSTRAINT uk_asistencias_dia_empleado_fecha UNIQUE (empleado_id, fecha),
    CONSTRAINT fk_asistencias_dia_empleado FOREIGN KEY (empleado_id) REFERENCES empleados (id),
    CONSTRAINT fk_asistencias_dia_turno FOREIGN KEY (turno_personal_id) REFERENCES turnos_personal (id) ON DELETE SET NULL,
    CONSTRAINT fk_asistencias_dia_usuario FOREIGN KEY (ajustado_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_asistencias_dia_estado CHECK (estado IN (
        'PRESENTE', 'TARDANZA', 'FALTA', 'AUSENCIA_APROBADA', 'SIN_SALIDA', 'SIN_TURNO')),
    CONSTRAINT ck_asistencias_dia_destino CHECK (extra_destino IN ('PAGO', 'COMPENSACION')),
    CONSTRAINT ck_asistencias_dia_minutos CHECK (minutos_tardanza >= 0 AND minutos_trabajados >= 0 AND minutos_extra >= 0)
);

CREATE INDEX idx_asistencias_dia_fecha ON asistencias_dia (fecha, estado);

CREATE TABLE planillas (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    anio integer NOT NULL,
    mes integer NOT NULL,
    codigo varchar(20) NOT NULL,
    estado varchar(15) NOT NULL,
    cantidad_boletas integer NOT NULL DEFAULT 0,
    total_ingresos numeric(14, 2) NOT NULL DEFAULT 0,
    total_descuentos numeric(14, 2) NOT NULL DEFAULT 0,
    total_neto numeric(14, 2) NOT NULL DEFAULT 0,
    total_aporte_empleador numeric(14, 2) NOT NULL DEFAULT 0,
    total_renta numeric(14, 2) NOT NULL DEFAULT 0,
    total_pension numeric(14, 2) NOT NULL DEFAULT 0,
    hash varchar(64),
    calculada_por_id uuid,
    calculada_en timestamp(6),
    aprobada_por_id uuid,
    aprobada_en timestamp(6),
    pagada_por_id uuid,
    pagada_en timestamp(6),
    fecha_pago date,
    referencia_pago varchar(60),
    cerrada_por_id uuid,
    cerrada_en timestamp(6),
    created_at timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_planillas PRIMARY KEY (id),
    CONSTRAINT uk_planillas_periodo UNIQUE (anio, mes),
    CONSTRAINT uk_planillas_codigo UNIQUE (codigo),
    CONSTRAINT fk_planillas_calculada FOREIGN KEY (calculada_por_id) REFERENCES usuarios (id),
    CONSTRAINT fk_planillas_aprobada FOREIGN KEY (aprobada_por_id) REFERENCES usuarios (id),
    CONSTRAINT fk_planillas_pagada FOREIGN KEY (pagada_por_id) REFERENCES usuarios (id),
    CONSTRAINT fk_planillas_cerrada FOREIGN KEY (cerrada_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_planillas_mes CHECK (mes BETWEEN 1 AND 12),
    CONSTRAINT ck_planillas_estado CHECK (estado IN ('BORRADOR', 'CALCULADA', 'APROBADA', 'PAGADA', 'CERRADA'))
);

CREATE TABLE boletas_pago (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    planilla_id uuid NOT NULL,
    empleado_id uuid NOT NULL,
    contrato_id uuid NOT NULL,
    numero varchar(24) NOT NULL,
    empleado_nombre varchar(200) NOT NULL,
    cargo varchar(100) NOT NULL,
    regimen varchar(20) NOT NULL,
    sistema_pensionario varchar(5) NOT NULL,
    banco varchar(60),
    cuenta varchar(30),
    dias_laborados integer NOT NULL,
    dias_falta integer NOT NULL DEFAULT 0,
    dias_no_remunerados integer NOT NULL DEFAULT 0,
    minutos_tardanza integer NOT NULL DEFAULT 0,
    minutos_extra_pagados integer NOT NULL DEFAULT 0,
    remuneracion_basica numeric(12, 2) NOT NULL,
    total_ingresos numeric(12, 2) NOT NULL,
    total_descuentos numeric(12, 2) NOT NULL,
    renta_quinta numeric(12, 2) NOT NULL DEFAULT 0,
    pension numeric(12, 2) NOT NULL DEFAULT 0,
    aporte_empleador numeric(12, 2) NOT NULL DEFAULT 0,
    neto numeric(12, 2) NOT NULL,
    provision_gratificacion numeric(12, 2) NOT NULL DEFAULT 0,
    provision_cts numeric(12, 2) NOT NULL DEFAULT 0,
    provision_vacaciones numeric(12, 2) NOT NULL DEFAULT 0,
    hash varchar(64),
    CONSTRAINT pk_boletas_pago PRIMARY KEY (id),
    CONSTRAINT uk_boletas_pago_numero UNIQUE (numero),
    CONSTRAINT uk_boletas_pago_planilla_empleado UNIQUE (planilla_id, empleado_id),
    CONSTRAINT fk_boletas_pago_planilla FOREIGN KEY (planilla_id) REFERENCES planillas (id) ON DELETE CASCADE,
    CONSTRAINT fk_boletas_pago_empleado FOREIGN KEY (empleado_id) REFERENCES empleados (id),
    CONSTRAINT fk_boletas_pago_contrato FOREIGN KEY (contrato_id) REFERENCES contratos_laborales (id)
);

CREATE INDEX idx_boletas_pago_empleado ON boletas_pago (empleado_id);

CREATE TABLE boleta_lineas (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    boleta_id uuid NOT NULL,
    posicion integer NOT NULL,
    codigo varchar(30) NOT NULL,
    nombre varchar(100) NOT NULL,
    tipo varchar(20) NOT NULL,
    base numeric(12, 2),
    monto numeric(12, 2) NOT NULL,
    CONSTRAINT pk_boleta_lineas PRIMARY KEY (id),
    CONSTRAINT fk_boleta_lineas_boleta FOREIGN KEY (boleta_id) REFERENCES boletas_pago (id) ON DELETE CASCADE,
    CONSTRAINT ck_boleta_lineas_tipo CHECK (tipo IN ('INGRESO', 'DESCUENTO', 'APORTE_EMPLEADOR'))
);

CREATE INDEX idx_boleta_lineas_boleta ON boleta_lineas (boleta_id, posicion);

CREATE TABLE secuencias_planilla (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tipo varchar(10) NOT NULL,
    anio integer NOT NULL,
    ultimo integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_secuencias_planilla PRIMARY KEY (id),
    CONSTRAINT uk_secuencias_planilla UNIQUE (tipo, anio)
);

CREATE TABLE planilla_auditoria (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    fecha timestamp(6) NOT NULL,
    usuario_id uuid NOT NULL,
    entidad varchar(30) NOT NULL,
    entidad_id uuid,
    accion varchar(40) NOT NULL,
    detalle varchar(300),
    CONSTRAINT pk_planilla_auditoria PRIMARY KEY (id),
    CONSTRAINT fk_planilla_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_planilla_auditoria_entidad ON planilla_auditoria (entidad, entidad_id, fecha);

INSERT INTO parametros_planilla (codigo, valor, vigente_desde, descripcion) VALUES
    ('RMV', 1130, DATE '2026-01-01', 'Remuneración mínima vital mensual en soles'),
    ('ASIGNACION_FAMILIAR_PORC', 10, DATE '2026-01-01', 'Asignación familiar como porcentaje de la remuneración mínima vital'),
    ('ESSALUD_TASA', 9, DATE '2026-01-01', 'Aporte del empleador a EsSalud en porcentaje'),
    ('ONP_TASA', 13, DATE '2026-01-01', 'Aporte del trabajador a la ONP en porcentaje'),
    ('AFP_APORTE_TASA', 10, DATE '2026-01-01', 'Aporte obligatorio al fondo de pensiones en porcentaje'),
    ('AFP_SEGURO_TASA', 1.37, DATE '2026-01-01', 'Prima de seguro de la AFP en porcentaje'),
    ('AFP_COMISION_TASA', 1.55, DATE '2026-01-01', 'Comisión de la AFP sobre la remuneración en porcentaje'),
    ('UIT', 5350, DATE '2026-01-01', 'Unidad impositiva tributaria en soles'),
    ('RENTA5_DEDUCCION_UIT', 7, DATE '2026-01-01', 'Deducción anual de la renta de quinta categoría en UIT'),
    ('RENTA5_LIMITE_1_UIT', 5, DATE '2026-01-01', 'Fin del primer tramo de la renta de quinta categoría en UIT'),
    ('RENTA5_LIMITE_2_UIT', 20, DATE '2026-01-01', 'Fin del segundo tramo de la renta de quinta categoría en UIT'),
    ('RENTA5_LIMITE_3_UIT', 35, DATE '2026-01-01', 'Fin del tercer tramo de la renta de quinta categoría en UIT'),
    ('RENTA5_LIMITE_4_UIT', 45, DATE '2026-01-01', 'Fin del cuarto tramo de la renta de quinta categoría en UIT'),
    ('RENTA5_TASA_1', 8, DATE '2026-01-01', 'Tasa del primer tramo de la renta de quinta categoría en porcentaje'),
    ('RENTA5_TASA_2', 14, DATE '2026-01-01', 'Tasa del segundo tramo de la renta de quinta categoría en porcentaje'),
    ('RENTA5_TASA_3', 17, DATE '2026-01-01', 'Tasa del tercer tramo de la renta de quinta categoría en porcentaje'),
    ('RENTA5_TASA_4', 20, DATE '2026-01-01', 'Tasa del cuarto tramo de la renta de quinta categoría en porcentaje'),
    ('RENTA5_TASA_5', 30, DATE '2026-01-01', 'Tasa del quinto tramo de la renta de quinta categoría en porcentaje'),
    ('HORAS_EXTRA_SOBRETASA_1', 25, DATE '2026-01-01', 'Sobretasa de las dos primeras horas extra del día en porcentaje'),
    ('HORAS_EXTRA_SOBRETASA_2', 35, DATE '2026-01-01', 'Sobretasa de las horas extra posteriores en porcentaje'),
    ('HORAS_EXTRA_TRAMO_MINUTOS', 120, DATE '2026-01-01', 'Minutos extra diarios con la primera sobretasa'),
    ('TARDANZA_TOLERANCIA_MINUTOS', 10, DATE '2026-01-01', 'Minutos de tolerancia antes de contar la tardanza'),
    ('DIAS_MES', 30, DATE '2026-01-01', 'Días del mes para valorizar el día de trabajo'),
    ('HORAS_JORNADA', 8, DATE '2026-01-01', 'Horas de la jornada diaria para valorizar la hora de trabajo'),
    ('VACACIONES_DIAS_ANUALES', 30, DATE '2026-01-01', 'Días de vacaciones por año de servicio del régimen general');

INSERT INTO conceptos_planilla (codigo, nombre, tipo, modo, valor_defecto, afecto_renta, afecto_pension, afecto_essalud, activo, sistema, orden) VALUES
    ('BASICO', 'Remuneración básica', 'INGRESO', 'MONTO_FIJO', 0, true, true, true, true, true, 10),
    ('ASIG_FAM', 'Asignación familiar', 'INGRESO', 'MONTO_FIJO', 0, true, true, true, true, true, 20),
    ('HORAS_EXTRA', 'Horas extra', 'INGRESO', 'MONTO_FIJO', 0, true, true, true, true, true, 30),
    ('DESC_FALTAS', 'Descuento por faltas y permisos sin goce', 'DESCUENTO', 'MONTO_FIJO', 0, true, true, true, true, true, 110),
    ('DESC_TARDANZA', 'Descuento por tardanzas', 'DESCUENTO', 'MONTO_FIJO', 0, true, true, true, true, true, 120),
    ('PENSION', 'Aporte al sistema de pensiones', 'DESCUENTO', 'MONTO_FIJO', 0, false, false, false, true, true, 130),
    ('RENTA_5TA', 'Retención de renta de quinta categoría', 'DESCUENTO', 'MONTO_FIJO', 0, false, false, false, true, true, 140),
    ('ESSALUD', 'Aporte a EsSalud', 'APORTE_EMPLEADOR', 'MONTO_FIJO', 0, false, false, false, true, true, 210),
    ('BONO_PRODUCTIVIDAD', 'Bono de productividad', 'INGRESO', 'MONTO_FIJO', 0, true, true, true, true, false, 40),
    ('BONO_GUARDIA', 'Bono por guardia', 'INGRESO', 'MONTO_FIJO', 0, true, true, true, true, false, 50),
    ('MOVILIDAD', 'Movilidad', 'INGRESO', 'MONTO_FIJO', 0, false, false, false, true, false, 60),
    ('ADELANTO', 'Adelanto de sueldo', 'DESCUENTO', 'MONTO_FIJO', 0, false, false, false, true, false, 150),
    ('PRESTAMO', 'Cuota de préstamo', 'DESCUENTO', 'MONTO_FIJO', 0, false, false, false, true, false, 160);

ALTER TABLE asientos DROP CONSTRAINT ck_asientos_origen;
ALTER TABLE asientos ADD CONSTRAINT ck_asientos_origen CHECK (origen IN (
    'MANUAL', 'COBRO', 'COMPRA_PROVEEDOR', 'PAGO_PROVEEDOR', 'MOVIMIENTO_TESORERIA', 'REVERSA',
    'PLANILLA', 'PAGO_PLANILLA'));

ALTER TABLE plantillas_asiento DROP CONSTRAINT ck_plantillas_asiento_evento;
ALTER TABLE plantillas_asiento ADD CONSTRAINT ck_plantillas_asiento_evento CHECK (evento IN (
    'COBRO_EFECTIVO', 'COBRO_ELECTRONICO', 'COMPRA_PROVEEDOR', 'PAGO_PROVEEDOR_EFECTIVO', 'PAGO_PROVEEDOR_BANCO',
    'PLANILLA_APROBADA', 'PLANILLA_PAGADA'));

ALTER TABLE plantilla_asiento_lineas ALTER COLUMN concepto TYPE varchar(30);

ALTER TABLE plantilla_asiento_lineas DROP CONSTRAINT ck_plantilla_lineas_concepto;
ALTER TABLE plantilla_asiento_lineas ADD CONSTRAINT ck_plantilla_lineas_concepto CHECK (concepto IN (
    'TOTAL', 'SUBTOTAL', 'IGV', 'CUBIERTO', 'INGRESO_BRUTO', 'MONTO',
    'REMUNERACION_BRUTA', 'APORTE_EMPLEADOR', 'RETENCION_PENSION', 'RETENCION_RENTA', 'OTROS_DESCUENTOS',
    'NETO_PAGAR'));

ALTER TABLE movimientos_tesoreria DROP CONSTRAINT ck_movimientos_origen;
ALTER TABLE movimientos_tesoreria ADD CONSTRAINT ck_movimientos_origen CHECK (origen IN (
    'MANUAL', 'COBRO', 'PAGO_PROVEEDOR', 'PAGO_PLANILLA'));

INSERT INTO cuentas_contables (codigo, nombre, tipo, naturaleza, acepta_movimientos, sistema) VALUES
    ('403', 'Seguridad social por pagar', 'PASIVO', 'ACREEDORA', false, true),
    ('4031', 'EsSalud por pagar', 'PASIVO', 'ACREEDORA', true, true),
    ('407', 'Administradoras de fondos de pensiones', 'PASIVO', 'ACREEDORA', false, true),
    ('4071', 'AFP y ONP por pagar', 'PASIVO', 'ACREEDORA', true, true),
    ('627', 'Seguridad y previsión social', 'GASTO', 'DEUDORA', false, true),
    ('6271', 'Aporte del empleador a EsSalud', 'GASTO', 'DEUDORA', true, true);

INSERT INTO plantillas_asiento (evento, nombre) VALUES
    ('PLANILLA_APROBADA', 'Planilla mensual aprobada'),
    ('PLANILLA_PAGADA', 'Pago de la planilla por banco');

INSERT INTO plantilla_asiento_lineas (plantilla_id, posicion, lado, concepto, cuenta_id)
SELECT p.id, l.posicion, l.lado, l.concepto, c.id
FROM plantillas_asiento p
JOIN (VALUES
    ('PLANILLA_APROBADA', 0, 'DEBE', 'REMUNERACION_BRUTA', '6211'),
    ('PLANILLA_APROBADA', 1, 'DEBE', 'APORTE_EMPLEADOR', '6271'),
    ('PLANILLA_APROBADA', 2, 'HABER', 'RETENCION_PENSION', '4071'),
    ('PLANILLA_APROBADA', 3, 'HABER', 'RETENCION_RENTA', '4017'),
    ('PLANILLA_APROBADA', 4, 'HABER', 'OTROS_DESCUENTOS', '46'),
    ('PLANILLA_APROBADA', 5, 'HABER', 'APORTE_EMPLEADOR', '4031'),
    ('PLANILLA_APROBADA', 6, 'HABER', 'NETO_PAGAR', '41'),
    ('PLANILLA_PAGADA', 0, 'DEBE', 'NETO_PAGAR', '41'),
    ('PLANILLA_PAGADA', 1, 'HABER', 'NETO_PAGAR', '1041')
) AS l (evento, posicion, lado, concepto, codigo) ON l.evento = p.evento
JOIN cuentas_contables c ON c.codigo = l.codigo;

ALTER TABLE notificaciones DROP CONSTRAINT notificaciones_tipo_check;
ALTER TABLE notificaciones ADD CONSTRAINT notificaciones_tipo_check CHECK (tipo IN (
    'CITA_CREADA', 'CITA_CONFIRMADA', 'CITA_CANCELADA', 'CITA_ATENDIDA', 'CITA_REPROGRAMADA',
    'RECORDATORIO_CITA', 'RECORDATORIO_EVENTO', 'INICIO_SESION', 'CONTRASENA_CAMBIADA', 'PAGO_REGISTRADO',
    'PAGO_REEMBOLSADO', 'MFA_ACTIVADO', 'MFA_DESACTIVADO', 'PACIENTE_COMPARTIDO', 'ROLES_ACTUALIZADOS',
    'SESION_SOSPECHOSA', 'ORDEN_LABORATORIO', 'RESULTADO_LABORATORIO', 'RECETA_DISPENSADA', 'STOCK_BAJO',
    'PRODUCTO_POR_VENCER', 'PACIENTE_EN_ESPERA', 'TURNO_LLAMADO', 'COMPROBANTE_EMITIDO',
    'INGRESO_HOSPITALARIO', 'ALTA_HOSPITALARIA', 'EMERGENCIA_TRIAJE', 'CAJA_DIFERENCIA',
    'SOLICITUD_AUSENCIA', 'AUSENCIA_RESUELTA', 'ORDEN_IMAGEN', 'ESTUDIO_INFORMADO', 'HALLAZGO_CRITICO',
    'EXPORTACION_LISTA', 'PREAUTORIZACION_SOLICITADA', 'PREAUTORIZACION_RESUELTA',
    'TELECONSULTA_PROXIMA', 'TELECONSULTA_SALA_ABIERTA', 'TELECONSULTA_PACIENTE_EN_SALA',
    'DOCUMENTO_EMITIDO', 'CONSENTIMIENTO_PENDIENTE', 'CONSENTIMIENTO_FIRMADO',
    'RESPALDO_FALLIDO', 'RESPALDO_VENCIDO', 'COMPROBANTE_RECHAZADO', 'ARCO_RECIBIDA', 'ARCO_RESUELTA',
    'INCIDENTE_DATOS', 'REQUISICION_ENVIADA', 'REQUISICION_RESUELTA', 'ORDEN_COMPRA_POR_APROBAR', 'ORDEN_COMPRA_RESUELTA',
    'CUENTA_POR_PAGAR_VENCIDA', 'PLANILLA_POR_APROBAR', 'PLANILLA_APROBADA', 'BOLETA_DISPONIBLE', 'SISTEMA'));
