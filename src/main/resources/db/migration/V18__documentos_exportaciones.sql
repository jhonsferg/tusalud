CREATE TABLE plantillas_documento (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    codigo character varying(40) NOT NULL,
    version integer NOT NULL,
    archivo_id uuid,
    activa boolean NOT NULL,
    restauracion boolean NOT NULL DEFAULT false,
    motivo character varying(200),
    creado_por uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT uk_plantillas_documento_version UNIQUE (codigo, version),
    CONSTRAINT fk_plantillas_documento_archivo FOREIGN KEY (archivo_id) REFERENCES archivos (id),
    CONSTRAINT fk_plantillas_documento_usuario FOREIGN KEY (creado_por) REFERENCES usuarios (id),
    CONSTRAINT plantillas_documento_version_check CHECK (version >= 1)
);

CREATE INDEX idx_plantillas_documento_codigo_activa ON plantillas_documento (codigo, activa);

CREATE TABLE exportaciones (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    tipo character varying(40) NOT NULL,
    formato character varying(10) NOT NULL,
    parametros text,
    estado character varying(15) NOT NULL,
    archivo_id uuid,
    nombre character varying(150),
    tamano bigint,
    error character varying(300),
    solicitado_por uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    iniciada_en timestamp(6) without time zone,
    terminada_en timestamp(6) without time zone,
    expira_en timestamp(6) without time zone,
    CONSTRAINT fk_exportaciones_archivo FOREIGN KEY (archivo_id) REFERENCES archivos (id),
    CONSTRAINT fk_exportaciones_usuario FOREIGN KEY (solicitado_por) REFERENCES usuarios (id),
    CONSTRAINT exportaciones_formato_check CHECK (formato IN ('PDF', 'XLSX')),
    CONSTRAINT exportaciones_estado_check CHECK (estado IN ('SOLICITADA', 'PROCESANDO', 'LISTA', 'ERROR', 'EXPIRADA'))
);

CREATE INDEX idx_exportaciones_usuario_fecha ON exportaciones (solicitado_por, created_at DESC);
CREATE INDEX idx_exportaciones_estado_expira ON exportaciones (estado, expira_en);

ALTER TABLE notificaciones DROP CONSTRAINT notificaciones_tipo_check;
ALTER TABLE notificaciones ADD CONSTRAINT notificaciones_tipo_check CHECK (tipo IN (
    'CITA_CREADA', 'CITA_CONFIRMADA', 'CITA_CANCELADA', 'CITA_ATENDIDA', 'CITA_REPROGRAMADA',
    'RECORDATORIO_CITA', 'RECORDATORIO_EVENTO', 'INICIO_SESION', 'CONTRASENA_CAMBIADA', 'PAGO_REGISTRADO',
    'PAGO_REEMBOLSADO', 'MFA_ACTIVADO', 'MFA_DESACTIVADO', 'PACIENTE_COMPARTIDO', 'ROLES_ACTUALIZADOS',
    'SESION_SOSPECHOSA', 'ORDEN_LABORATORIO', 'RESULTADO_LABORATORIO', 'RECETA_DISPENSADA', 'STOCK_BAJO',
    'PRODUCTO_POR_VENCER', 'PACIENTE_EN_ESPERA', 'TURNO_LLAMADO', 'COMPROBANTE_EMITIDO',
    'INGRESO_HOSPITALARIO', 'ALTA_HOSPITALARIA', 'EMERGENCIA_TRIAJE', 'CAJA_DIFERENCIA',
    'SOLICITUD_AUSENCIA', 'AUSENCIA_RESUELTA', 'ORDEN_IMAGEN', 'ESTUDIO_INFORMADO', 'HALLAZGO_CRITICO',
    'EXPORTACION_LISTA', 'SISTEMA'));
