CREATE TABLE aseguradoras (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    codigo character varying(20) NOT NULL,
    nombre character varying(120) NOT NULL,
    tipo character varying(20) NOT NULL,
    ruc character varying(11),
    contacto character varying(150),
    activa boolean NOT NULL DEFAULT true,
    CONSTRAINT uk_aseguradoras_codigo UNIQUE (codigo),
    CONSTRAINT ck_aseguradoras_tipo CHECK (tipo IN ('SIS', 'ESSALUD', 'EPS', 'PRIVADO', 'EMPRESA'))
);

CREATE TABLE convenios (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    aseguradora_id uuid NOT NULL,
    nombre character varying(120) NOT NULL,
    vigencia_desde date NOT NULL,
    vigencia_hasta date NOT NULL,
    tope_anual numeric(12, 2),
    activo boolean NOT NULL DEFAULT true,
    CONSTRAINT fk_convenios_aseguradora FOREIGN KEY (aseguradora_id) REFERENCES aseguradoras (id),
    CONSTRAINT ck_convenios_vigencia CHECK (vigencia_hasta >= vigencia_desde),
    CONSTRAINT ck_convenios_tope CHECK (tope_anual IS NULL OR tope_anual > 0)
);
CREATE INDEX idx_convenios_aseguradora ON convenios (aseguradora_id);

CREATE TABLE reglas_cobertura (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    convenio_id uuid NOT NULL,
    categoria character varying(20) NOT NULL,
    tipo character varying(20) NOT NULL,
    valor numeric(12, 2) NOT NULL,
    tope_por_servicio numeric(12, 2),
    requiere_preautorizacion boolean NOT NULL DEFAULT false,
    CONSTRAINT fk_reglas_convenio FOREIGN KEY (convenio_id) REFERENCES convenios (id) ON DELETE CASCADE,
    CONSTRAINT uk_reglas_convenio_categoria UNIQUE (convenio_id, categoria),
    CONSTRAINT ck_reglas_categoria CHECK (categoria IN ('CONSULTA', 'LABORATORIO', 'IMAGEN', 'HOSPITALIZACION', 'FARMACIA', 'PROCEDIMIENTO')),
    CONSTRAINT ck_reglas_tipo CHECK (tipo IN ('PORCENTAJE', 'COPAGO_FIJO')),
    CONSTRAINT ck_reglas_valor CHECK (valor >= 0),
    CONSTRAINT ck_reglas_tope CHECK (tope_por_servicio IS NULL OR tope_por_servicio > 0)
);

CREATE TABLE convenio_exclusiones (
    convenio_id uuid NOT NULL,
    codigo character varying(40) NOT NULL,
    CONSTRAINT pk_convenio_exclusiones PRIMARY KEY (convenio_id, codigo),
    CONSTRAINT fk_convenio_exclusiones FOREIGN KEY (convenio_id) REFERENCES convenios (id) ON DELETE CASCADE
);

CREATE TABLE polizas (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    paciente_id uuid NOT NULL,
    convenio_id uuid NOT NULL,
    numero character varying(30) NOT NULL,
    titular_nombre character varying(120) NOT NULL,
    parentesco character varying(30) NOT NULL,
    plan character varying(60),
    vigencia_desde date NOT NULL,
    vigencia_hasta date NOT NULL,
    estado character varying(15) NOT NULL DEFAULT 'ACTIVA',
    motivo_suspension character varying(300),
    registrada_por uuid,
    creada_en timestamp(6) without time zone NOT NULL,
    CONSTRAINT fk_polizas_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_polizas_convenio FOREIGN KEY (convenio_id) REFERENCES convenios (id),
    CONSTRAINT fk_polizas_registrada FOREIGN KEY (registrada_por) REFERENCES usuarios (id) ON DELETE SET NULL,
    CONSTRAINT uk_polizas_numero_convenio UNIQUE (numero, convenio_id),
    CONSTRAINT ck_polizas_vigencia CHECK (vigencia_hasta >= vigencia_desde),
    CONSTRAINT ck_polizas_estado CHECK (estado IN ('ACTIVA', 'VENCIDA', 'SUSPENDIDA'))
);
CREATE INDEX idx_polizas_paciente ON polizas (paciente_id);

CREATE TABLE verificaciones_cobertura (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    poliza_id uuid NOT NULL,
    vigente boolean NOT NULL,
    estado character varying(30) NOT NULL,
    saldo_tope numeric(12, 2),
    codigo character varying(20) NOT NULL,
    verificada_por uuid NOT NULL,
    fecha timestamp(6) without time zone NOT NULL,
    CONSTRAINT fk_verificaciones_poliza FOREIGN KEY (poliza_id) REFERENCES polizas (id) ON DELETE CASCADE,
    CONSTRAINT fk_verificaciones_usuario FOREIGN KEY (verificada_por) REFERENCES usuarios (id)
);

CREATE TABLE preautorizaciones (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    codigo character varying(20) NOT NULL,
    paciente_id uuid NOT NULL,
    poliza_id uuid NOT NULL,
    categoria character varying(20) NOT NULL,
    referencia_id uuid,
    descripcion character varying(200) NOT NULL,
    monto_estimado numeric(12, 2) NOT NULL,
    estado character varying(15) NOT NULL DEFAULT 'SOLICITADA',
    solicitada_por uuid NOT NULL,
    solicitada_en timestamp(6) without time zone NOT NULL,
    gestionada_por uuid,
    gestionada_en timestamp(6) without time zone,
    codigo_autorizacion character varying(30),
    monto_aprobado numeric(12, 2),
    vigente_hasta date,
    motivo_rechazo character varying(300),
    CONSTRAINT uk_preautorizaciones_codigo UNIQUE (codigo),
    CONSTRAINT fk_preautorizaciones_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_preautorizaciones_poliza FOREIGN KEY (poliza_id) REFERENCES polizas (id),
    CONSTRAINT fk_preautorizaciones_solicitante FOREIGN KEY (solicitada_por) REFERENCES usuarios (id),
    CONSTRAINT fk_preautorizaciones_gestor FOREIGN KEY (gestionada_por) REFERENCES usuarios (id) ON DELETE SET NULL,
    CONSTRAINT ck_preautorizaciones_estado CHECK (estado IN ('SOLICITADA', 'APROBADA', 'RECHAZADA', 'VENCIDA')),
    CONSTRAINT ck_preautorizaciones_categoria CHECK (categoria IN ('CONSULTA', 'LABORATORIO', 'IMAGEN', 'HOSPITALIZACION', 'FARMACIA', 'PROCEDIMIENTO')),
    CONSTRAINT ck_preautorizaciones_monto CHECK (monto_estimado >= 0)
);
CREATE INDEX idx_preautorizaciones_paciente ON preautorizaciones (paciente_id);
CREATE INDEX idx_preautorizaciones_estado ON preautorizaciones (estado);

CREATE TABLE liquidaciones (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    codigo character varying(25) NOT NULL,
    aseguradora_id uuid NOT NULL,
    desde date NOT NULL,
    hasta date NOT NULL,
    total numeric(12, 2) NOT NULL,
    cantidad_cuentas integer NOT NULL,
    estado character varying(15) NOT NULL DEFAULT 'BORRADOR',
    creada_por uuid NOT NULL,
    creada_en timestamp(6) without time zone NOT NULL,
    enviada_en timestamp(6) without time zone,
    pagada_en timestamp(6) without time zone,
    monto_pagado numeric(12, 2),
    monto_glosa numeric(12, 2),
    motivo_glosa character varying(300),
    CONSTRAINT uk_liquidaciones_codigo UNIQUE (codigo),
    CONSTRAINT fk_liquidaciones_aseguradora FOREIGN KEY (aseguradora_id) REFERENCES aseguradoras (id),
    CONSTRAINT fk_liquidaciones_creada FOREIGN KEY (creada_por) REFERENCES usuarios (id),
    CONSTRAINT ck_liquidaciones_estado CHECK (estado IN ('BORRADOR', 'ENVIADA', 'PAGADA', 'OBSERVADA')),
    CONSTRAINT ck_liquidaciones_periodo CHECK (hasta >= desde)
);

CREATE TABLE cuentas_aseguradora (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    aseguradora_id uuid NOT NULL,
    convenio_id uuid NOT NULL,
    comprobante_id uuid,
    paciente_id uuid NOT NULL,
    categoria character varying(20) NOT NULL,
    origen character varying(20) NOT NULL,
    referencia_id uuid,
    descripcion character varying(200) NOT NULL,
    monto_total numeric(12, 2) NOT NULL,
    monto_cubierto numeric(12, 2) NOT NULL,
    copago numeric(12, 2) NOT NULL,
    poliza_numero character varying(30) NOT NULL,
    fecha timestamp(6) without time zone NOT NULL,
    estado character varying(15) NOT NULL DEFAULT 'PENDIENTE',
    liquidacion_id uuid,
    CONSTRAINT fk_cuentas_aseguradora FOREIGN KEY (aseguradora_id) REFERENCES aseguradoras (id),
    CONSTRAINT fk_cuentas_convenio FOREIGN KEY (convenio_id) REFERENCES convenios (id),
    CONSTRAINT fk_cuentas_comprobante FOREIGN KEY (comprobante_id) REFERENCES comprobantes (id),
    CONSTRAINT fk_cuentas_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_cuentas_liquidacion FOREIGN KEY (liquidacion_id) REFERENCES liquidaciones (id),
    CONSTRAINT uk_cuentas_comprobante UNIQUE (comprobante_id),
    CONSTRAINT ck_cuentas_estado CHECK (estado IN ('PENDIENTE', 'LIQUIDADA', 'PAGADA', 'REVERTIDA')),
    CONSTRAINT ck_cuentas_montos CHECK (monto_cubierto >= 0 AND copago >= 0 AND monto_total >= 0)
);
CREATE INDEX idx_cuentas_aseguradora_estado ON cuentas_aseguradora (aseguradora_id, estado);
CREATE INDEX idx_cuentas_aseguradora_consumo ON cuentas_aseguradora (paciente_id, convenio_id, fecha);

ALTER TABLE comprobantes ADD COLUMN monto_cubierto numeric(12, 2);
ALTER TABLE comprobantes ADD COLUMN aseguradora_nombre character varying(120);

ALTER TABLE notificaciones DROP CONSTRAINT notificaciones_tipo_check;
ALTER TABLE notificaciones ADD CONSTRAINT notificaciones_tipo_check CHECK (tipo IN (
    'CITA_CREADA', 'CITA_CONFIRMADA', 'CITA_CANCELADA', 'CITA_ATENDIDA', 'CITA_REPROGRAMADA',
    'RECORDATORIO_CITA', 'RECORDATORIO_EVENTO', 'INICIO_SESION', 'CONTRASENA_CAMBIADA', 'PAGO_REGISTRADO',
    'PAGO_REEMBOLSADO', 'MFA_ACTIVADO', 'MFA_DESACTIVADO', 'PACIENTE_COMPARTIDO', 'ROLES_ACTUALIZADOS',
    'SESION_SOSPECHOSA', 'ORDEN_LABORATORIO', 'RESULTADO_LABORATORIO', 'RECETA_DISPENSADA', 'STOCK_BAJO',
    'PRODUCTO_POR_VENCER', 'PACIENTE_EN_ESPERA', 'TURNO_LLAMADO', 'COMPROBANTE_EMITIDO',
    'INGRESO_HOSPITALARIO', 'ALTA_HOSPITALARIA', 'EMERGENCIA_TRIAJE', 'CAJA_DIFERENCIA',
    'SOLICITUD_AUSENCIA', 'AUSENCIA_RESUELTA', 'ORDEN_IMAGEN', 'ESTUDIO_INFORMADO', 'HALLAZGO_CRITICO',
    'EXPORTACION_LISTA', 'PREAUTORIZACION_SOLICITADA', 'PREAUTORIZACION_RESUELTA', 'SISTEMA'));
