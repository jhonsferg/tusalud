CREATE TABLE establecimientos_salud (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    codigo_ipress varchar(20),
    nombre varchar(150) NOT NULL,
    nivel varchar(20) NOT NULL,
    distrito varchar(100),
    contacto varchar(150),
    activo boolean NOT NULL DEFAULT true,
    CONSTRAINT pk_establecimientos_salud PRIMARY KEY (id),
    CONSTRAINT ck_establecimientos_nivel CHECK (nivel IN ('PRIMER_NIVEL', 'SEGUNDO_NIVEL', 'TERCER_NIVEL', 'ESPECIALIZADO'))
);

CREATE UNIQUE INDEX uk_establecimientos_ipress ON establecimientos_salud (codigo_ipress) WHERE codigo_ipress IS NOT NULL;

CREATE TABLE secuencias_documentos_medicos (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tipo varchar(20) NOT NULL,
    anio integer NOT NULL,
    ultimo integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_secuencias_documentos_medicos PRIMARY KEY (id),
    CONSTRAINT uk_secuencias_documentos_medicos UNIQUE (tipo, anio)
);

CREATE TABLE documentos_medicos (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tipo varchar(20) NOT NULL,
    estado varchar(10) NOT NULL,
    serie varchar(10),
    anio integer,
    numero integer,
    codigo_verificacion varchar(19),
    paciente_id uuid NOT NULL,
    medico_id uuid NOT NULL,
    atencion_id uuid,
    ingreso_id uuid,
    documento_origen_id uuid,
    autoriza_diagnostico boolean NOT NULL DEFAULT false,
    contenido text NOT NULL,
    contenido_hash varchar(64),
    pdf_hash varchar(64),
    pdf_archivo_id uuid,
    creado_en timestamp NOT NULL DEFAULT now(),
    emitido_en timestamp,
    anulado_en timestamp,
    anulado_por_id uuid,
    motivo_anulacion varchar(300),
    CONSTRAINT pk_documentos_medicos PRIMARY KEY (id),
    CONSTRAINT fk_docmed_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_docmed_medico FOREIGN KEY (medico_id) REFERENCES medicos (id),
    CONSTRAINT fk_docmed_atencion FOREIGN KEY (atencion_id) REFERENCES atenciones (id),
    CONSTRAINT fk_docmed_ingreso FOREIGN KEY (ingreso_id) REFERENCES ingresos_hospitalarios (id),
    CONSTRAINT fk_docmed_origen FOREIGN KEY (documento_origen_id) REFERENCES documentos_medicos (id),
    CONSTRAINT fk_docmed_archivo FOREIGN KEY (pdf_archivo_id) REFERENCES archivos (id),
    CONSTRAINT fk_docmed_anulador FOREIGN KEY (anulado_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_docmed_tipo CHECK (tipo IN ('CERTIFICADO', 'DESCANSO', 'INFORME', 'REFERENCIA', 'CONTRARREFERENCIA', 'EPICRISIS')),
    CONSTRAINT ck_docmed_estado CHECK (estado IN ('BORRADOR', 'EMITIDO', 'ANULADO'))
);

CREATE UNIQUE INDEX uk_docmed_codigo ON documentos_medicos (codigo_verificacion) WHERE codigo_verificacion IS NOT NULL;
CREATE UNIQUE INDEX uk_docmed_numero ON documentos_medicos (tipo, anio, numero) WHERE numero IS NOT NULL;
CREATE INDEX idx_docmed_paciente ON documentos_medicos (paciente_id, estado);
CREATE INDEX idx_docmed_medico ON documentos_medicos (medico_id, estado);
CREATE INDEX idx_docmed_ingreso ON documentos_medicos (ingreso_id);

CREATE TABLE referencias_clinicas (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    documento_id uuid NOT NULL,
    destino_id uuid NOT NULL,
    especialidad varchar(100) NOT NULL,
    prioridad varchar(10) NOT NULL,
    estado varchar(10) NOT NULL,
    motivo_rechazo varchar(300),
    actualizado_en timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_referencias_clinicas PRIMARY KEY (id),
    CONSTRAINT uk_referencias_documento UNIQUE (documento_id),
    CONSTRAINT fk_referencias_documento FOREIGN KEY (documento_id) REFERENCES documentos_medicos (id),
    CONSTRAINT fk_referencias_destino FOREIGN KEY (destino_id) REFERENCES establecimientos_salud (id),
    CONSTRAINT ck_referencias_prioridad CHECK (prioridad IN ('NORMAL', 'PREFERENTE', 'URGENTE')),
    CONSTRAINT ck_referencias_estado CHECK (estado IN ('ENVIADA', 'RECIBIDA', 'ATENDIDA', 'RECHAZADA'))
);

ALTER TABLE documentos_medicos ADD COLUMN referencia_id uuid;
ALTER TABLE documentos_medicos ADD CONSTRAINT fk_docmed_referencia FOREIGN KEY (referencia_id) REFERENCES referencias_clinicas (id);

CREATE TABLE consentimiento_plantillas (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    codigo varchar(30) NOT NULL,
    nombre varchar(150) NOT NULL,
    texto text NOT NULL,
    riesgos text NOT NULL,
    beneficios text NOT NULL,
    alternativas text NOT NULL,
    requiere_testigo boolean NOT NULL DEFAULT false,
    vigencia_dias integer NOT NULL DEFAULT 30,
    version integer NOT NULL DEFAULT 1,
    activa boolean NOT NULL DEFAULT true,
    CONSTRAINT pk_consentimiento_plantillas PRIMARY KEY (id),
    CONSTRAINT uk_consentimiento_plantillas_codigo UNIQUE (codigo),
    CONSTRAINT ck_consentimiento_plantillas_vigencia CHECK (vigencia_dias BETWEEN 1 AND 3650)
);

CREATE TABLE consentimientos (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    plantilla_id uuid NOT NULL,
    plantilla_version integer NOT NULL,
    paciente_id uuid NOT NULL,
    medico_id uuid NOT NULL,
    atencion_id uuid,
    ingreso_id uuid,
    orden_imagen_id uuid,
    procedimiento varchar(200) NOT NULL,
    estado varchar(15) NOT NULL,
    texto_aceptado text NOT NULL,
    requiere_testigo boolean NOT NULL DEFAULT false,
    firmante_tipo varchar(15),
    firmante_nombre varchar(150),
    firmante_dni varchar(12),
    firmante_parentesco varchar(50),
    testigo_nombre varchar(150),
    testigo_dni varchar(12),
    firmado_via varchar(15),
    firma_archivo_id uuid,
    firma_hash varchar(64),
    pdf_archivo_id uuid,
    pdf_hash varchar(64),
    codigo_verificacion varchar(19),
    creado_en timestamp NOT NULL DEFAULT now(),
    firmado_en timestamp,
    vence_en timestamp,
    revocado_en timestamp,
    revocado_por_id uuid,
    motivo_revocacion varchar(300),
    CONSTRAINT pk_consentimientos PRIMARY KEY (id),
    CONSTRAINT fk_consent_plantilla FOREIGN KEY (plantilla_id) REFERENCES consentimiento_plantillas (id),
    CONSTRAINT fk_consent_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_consent_medico FOREIGN KEY (medico_id) REFERENCES medicos (id),
    CONSTRAINT fk_consent_atencion FOREIGN KEY (atencion_id) REFERENCES atenciones (id),
    CONSTRAINT fk_consent_ingreso FOREIGN KEY (ingreso_id) REFERENCES ingresos_hospitalarios (id),
    CONSTRAINT fk_consent_firma FOREIGN KEY (firma_archivo_id) REFERENCES archivos (id),
    CONSTRAINT fk_consent_pdf FOREIGN KEY (pdf_archivo_id) REFERENCES archivos (id),
    CONSTRAINT fk_consent_revocador FOREIGN KEY (revocado_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_consent_estado CHECK (estado IN ('PENDIENTE_FIRMA', 'FIRMADO', 'REVOCADO', 'VENCIDO')),
    CONSTRAINT ck_consent_firmante CHECK (firmante_tipo IS NULL OR firmante_tipo IN ('PACIENTE', 'REPRESENTANTE'))
);

CREATE UNIQUE INDEX uk_consent_codigo ON consentimientos (codigo_verificacion) WHERE codigo_verificacion IS NOT NULL;
CREATE INDEX idx_consent_paciente ON consentimientos (paciente_id, estado);
CREATE INDEX idx_consent_medico ON consentimientos (medico_id, estado);
CREATE INDEX idx_consent_ingreso ON consentimientos (ingreso_id);

ALTER TABLE archivos DROP CONSTRAINT archivos_proposito_check;
ALTER TABLE archivos ADD CONSTRAINT archivos_proposito_check CHECK (proposito IN ('AVATAR', 'IMAGEN_DIAGNOSTICA', 'DOCUMENTO_PACIENTE', 'REPORTE', 'PLANTILLA', 'FIRMA_CONSENTIMIENTO', 'DOCUMENTO_CLINICO'));

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
    'DOCUMENTO_EMITIDO', 'CONSENTIMIENTO_PENDIENTE', 'CONSENTIMIENTO_FIRMADO', 'SISTEMA'));
