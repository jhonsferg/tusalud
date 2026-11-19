ALTER TABLE citas ADD COLUMN modalidad character varying(12) NOT NULL DEFAULT 'PRESENCIAL';
ALTER TABLE citas ADD CONSTRAINT ck_citas_modalidad CHECK (modalidad IN ('PRESENCIAL', 'VIRTUAL'));

ALTER TABLE especialidades ADD COLUMN precio_virtual numeric(10, 2);
ALTER TABLE especialidades ADD CONSTRAINT ck_especialidades_precio_virtual CHECK (precio_virtual IS NULL OR precio_virtual >= 0);

CREATE TABLE teleconsultas (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    cita_id uuid NOT NULL,
    sala uuid NOT NULL,
    estado character varying(15) NOT NULL DEFAULT 'PROGRAMADA',
    abierta_en timestamp(6) without time zone,
    inicio_real timestamp(6) without time zone,
    fin_real timestamp(6) without time zone,
    duracion_segundos integer,
    reconexiones integer NOT NULL DEFAULT 0,
    tipo_candidato character varying(10),
    recordatorio_enviado boolean NOT NULL DEFAULT false,
    sala_abierta_notificada boolean NOT NULL DEFAULT false,
    paciente_en_sala_notificado boolean NOT NULL DEFAULT false,
    created_at timestamp(6) without time zone NOT NULL DEFAULT now(),
    updated_at timestamp(6) without time zone,
    CONSTRAINT uk_teleconsultas_cita UNIQUE (cita_id),
    CONSTRAINT uk_teleconsultas_sala UNIQUE (sala),
    CONSTRAINT fk_teleconsultas_cita FOREIGN KEY (cita_id) REFERENCES citas (id),
    CONSTRAINT ck_teleconsultas_estado CHECK (estado IN ('PROGRAMADA', 'SALA_ABIERTA', 'EN_CURSO', 'FINALIZADA', 'CANCELADA', 'NO_ASISTIO')),
    CONSTRAINT ck_teleconsultas_candidato CHECK (tipo_candidato IS NULL OR tipo_candidato IN ('host', 'srflx', 'prflx', 'relay'))
);
CREATE INDEX idx_teleconsultas_estado ON teleconsultas (estado);

CREATE TABLE consentimientos_telemedicina (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    paciente_id uuid NOT NULL,
    version character varying(20) NOT NULL,
    aceptado_en timestamp(6) without time zone NOT NULL DEFAULT now(),
    CONSTRAINT fk_consentimientos_telemedicina_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT uk_consentimientos_telemedicina UNIQUE (paciente_id, version)
);

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
    'TELECONSULTA_PROXIMA', 'TELECONSULTA_SALA_ABIERTA', 'TELECONSULTA_PACIENTE_EN_SALA', 'SISTEMA'));
