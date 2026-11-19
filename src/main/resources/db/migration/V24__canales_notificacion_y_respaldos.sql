CREATE TABLE contactos_canal (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    usuario_id uuid NOT NULL,
    canal varchar(10) NOT NULL,
    destino varchar(150) NOT NULL,
    consentimiento_en timestamp,
    verificado_en timestamp,
    baja_en timestamp,
    rebotado_en timestamp,
    codigo_hash varchar(64),
    codigo_expira timestamp,
    intentos_codigo integer NOT NULL DEFAULT 0,
    ultimo_envio_codigo timestamp,
    envios_codigo_hora integer NOT NULL DEFAULT 0,
    ventana_envios_desde timestamp,
    actualizado_en timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_contactos_canal PRIMARY KEY (id),
    CONSTRAINT fk_contactos_canal_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT uk_contactos_canal_usuario UNIQUE (usuario_id, canal),
    CONSTRAINT ck_contactos_canal_canal CHECK (canal IN ('CORREO', 'SMS', 'WHATSAPP'))
);

CREATE TABLE preferencias_canal (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    usuario_id uuid NOT NULL,
    tipo varchar(40) NOT NULL,
    canal varchar(10) NOT NULL,
    activo boolean NOT NULL,
    CONSTRAINT pk_preferencias_canal PRIMARY KEY (id),
    CONSTRAINT fk_preferencias_canal_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT uk_preferencias_canal UNIQUE (usuario_id, tipo, canal),
    CONSTRAINT ck_preferencias_canal_canal CHECK (canal IN ('CORREO', 'SMS', 'WHATSAPP'))
);

CREATE TABLE configuracion_canales (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    usuario_id uuid NOT NULL,
    silencio_activo boolean NOT NULL DEFAULT true,
    silencio_desde time NOT NULL DEFAULT '22:00',
    silencio_hasta time NOT NULL DEFAULT '07:00',
    CONSTRAINT pk_configuracion_canales PRIMARY KEY (id),
    CONSTRAINT fk_configuracion_canales_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT uk_configuracion_canales_usuario UNIQUE (usuario_id)
);

CREATE TABLE plantillas_mensaje (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tipo varchar(40) NOT NULL,
    canal varchar(10) NOT NULL,
    version integer NOT NULL,
    asunto varchar(150),
    cuerpo varchar(1000) NOT NULL,
    activa boolean NOT NULL,
    motivo varchar(200),
    creado_en timestamp NOT NULL DEFAULT now(),
    creado_por_id uuid,
    CONSTRAINT pk_plantillas_mensaje PRIMARY KEY (id),
    CONSTRAINT fk_plantillas_mensaje_autor FOREIGN KEY (creado_por_id) REFERENCES usuarios (id) ON DELETE SET NULL,
    CONSTRAINT uk_plantillas_mensaje_version UNIQUE (tipo, canal, version),
    CONSTRAINT ck_plantillas_mensaje_canal CHECK (canal IN ('CORREO', 'SMS', 'WHATSAPP'))
);

CREATE UNIQUE INDEX uk_plantillas_mensaje_activa ON plantillas_mensaje (tipo, canal) WHERE activa;

CREATE TABLE entregas_notificacion (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    notificacion_id uuid,
    usuario_id uuid NOT NULL,
    canal varchar(10) NOT NULL,
    tipo varchar(40) NOT NULL,
    destino_mascara varchar(60) NOT NULL,
    estado varchar(10) NOT NULL,
    intentos integer NOT NULL DEFAULT 0,
    proximo_intento timestamp,
    motivo varchar(200),
    id_externo varchar(100),
    clave_idempotencia varchar(100) NOT NULL,
    plantilla_version integer,
    urgente boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    creado_en timestamp NOT NULL DEFAULT now(),
    enviado_en timestamp,
    entregado_en timestamp,
    CONSTRAINT pk_entregas_notificacion PRIMARY KEY (id),
    CONSTRAINT fk_entregas_notificacion FOREIGN KEY (notificacion_id) REFERENCES notificaciones (id) ON DELETE SET NULL,
    CONSTRAINT fk_entregas_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT uk_entregas_idempotencia UNIQUE (clave_idempotencia),
    CONSTRAINT ck_entregas_canal CHECK (canal IN ('CORREO', 'SMS', 'WHATSAPP')),
    CONSTRAINT ck_entregas_estado CHECK (estado IN ('PENDIENTE', 'ENVIADO', 'ENTREGADO', 'FALLIDO', 'REBOTADO'))
);

CREATE INDEX idx_entregas_cola ON entregas_notificacion (estado, proximo_intento);
CREATE INDEX idx_entregas_usuario_canal ON entregas_notificacion (usuario_id, canal, creado_en);
CREATE INDEX idx_entregas_id_externo ON entregas_notificacion (id_externo) WHERE id_externo IS NOT NULL;

CREATE TABLE respaldo_ejecuciones (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tipo varchar(20) NOT NULL,
    estado varchar(10) NOT NULL,
    alcance varchar(10) NOT NULL,
    categoria varchar(10),
    iniciado_en timestamp NOT NULL,
    finalizado_en timestamp NOT NULL,
    bytes_total bigint,
    archivo varchar(200),
    checksum varchar(64),
    migraciones_validadas integer,
    tablas_validadas integer,
    objetos_almacen integer,
    copia_remota boolean NOT NULL DEFAULT false,
    detalle varchar(500),
    registrado_en timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_respaldo_ejecuciones PRIMARY KEY (id),
    CONSTRAINT ck_respaldo_ejecuciones_tipo CHECK (tipo IN ('RESPALDO', 'PRUEBA_RESTAURACION')),
    CONSTRAINT ck_respaldo_ejecuciones_estado CHECK (estado IN ('EXITOSO', 'FALLIDO')),
    CONSTRAINT ck_respaldo_ejecuciones_alcance CHECK (alcance IN ('BASE', 'ALMACEN', 'COMPLETO')),
    CONSTRAINT ck_respaldo_ejecuciones_categoria CHECK (categoria IS NULL OR categoria IN ('DIARIO', 'SEMANAL', 'MENSUAL'))
);

CREATE INDEX idx_respaldo_ejecuciones_inicio ON respaldo_ejecuciones (iniciado_en);

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
    'RESPALDO_FALLIDO', 'RESPALDO_VENCIDO', 'SISTEMA'));
