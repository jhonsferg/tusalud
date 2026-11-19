CREATE TABLE configuracion_fiscal (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    sede varchar(20) NOT NULL,
    habilitada boolean NOT NULL DEFAULT false,
    ruc varchar(11) NOT NULL,
    razon_social varchar(150) NOT NULL,
    nombre_comercial varchar(150),
    direccion varchar(200) NOT NULL,
    ubigeo varchar(6) NOT NULL,
    departamento varchar(60) NOT NULL,
    provincia varchar(60) NOT NULL,
    distrito varchar(60) NOT NULL,
    codigo_local varchar(4) NOT NULL DEFAULT '0000',
    proveedor varchar(12) NOT NULL,
    modo varchar(12) NOT NULL,
    url_servicio varchar(300),
    afectacion_servicios varchar(10) NOT NULL DEFAULT 'GRAVADO',
    igv_porcentaje numeric(5, 2) NOT NULL DEFAULT 18.00,
    boletas_por_resumen boolean NOT NULL DEFAULT false,
    dias_limite_baja integer NOT NULL DEFAULT 7,
    actualizado_en timestamp(6) without time zone NOT NULL,
    actualizado_por_id uuid,
    CONSTRAINT uk_configuracion_fiscal_sede UNIQUE (sede),
    CONSTRAINT fk_configuracion_fiscal_usuario FOREIGN KEY (actualizado_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_configuracion_fiscal_proveedor CHECK (proveedor IN ('SIMULACION', 'SUNAT', 'OSE', 'PSE')),
    CONSTRAINT ck_configuracion_fiscal_modo CHECK (modo IN ('BETA', 'PRODUCCION')),
    CONSTRAINT ck_configuracion_fiscal_afectacion CHECK (afectacion_servicios IN ('GRAVADO', 'EXONERADO', 'INAFECTO'))
);

CREATE TABLE series_comprobante (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    sede varchar(20) NOT NULL,
    tipo varchar(15) NOT NULL,
    serie varchar(4) NOT NULL,
    correlativo_actual integer NOT NULL DEFAULT 0,
    activa boolean NOT NULL DEFAULT true,
    CONSTRAINT uk_series_comprobante UNIQUE (sede, serie),
    CONSTRAINT ck_series_comprobante_tipo CHECK (tipo IN ('FACTURA', 'BOLETA', 'NOTA_CREDITO', 'NOTA_DEBITO'))
);

CREATE TABLE resumenes_sunat (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    tipo varchar(20) NOT NULL,
    identificador varchar(20) NOT NULL,
    fecha_referencia date NOT NULL,
    fecha_generacion date NOT NULL,
    estado varchar(10) NOT NULL,
    ticket varchar(40),
    xml_clave varchar(200),
    xml_sha256 varchar(64),
    cdr_clave varchar(200),
    cdr_codigo varchar(10),
    cdr_descripcion varchar(400),
    motivo varchar(250),
    intentos integer NOT NULL DEFAULT 0,
    proximo_intento timestamp(6) without time zone,
    ultimo_error varchar(400),
    creado_por_id uuid,
    creado_en timestamp(6) without time zone NOT NULL,
    CONSTRAINT uk_resumenes_sunat_identificador UNIQUE (identificador),
    CONSTRAINT fk_resumenes_sunat_usuario FOREIGN KEY (creado_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_resumenes_sunat_tipo CHECK (tipo IN ('RESUMEN_DIARIO', 'COMUNICACION_BAJA')),
    CONSTRAINT ck_resumenes_sunat_estado CHECK (estado IN ('PENDIENTE', 'ENVIADO', 'ACEPTADO', 'RECHAZADO', 'ERROR'))
);

CREATE INDEX idx_resumenes_sunat_estado ON resumenes_sunat (estado, proximo_intento);

CREATE TABLE documentos_electronicos (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    tipo varchar(15) NOT NULL,
    serie varchar(4) NOT NULL,
    numero integer NOT NULL,
    sede varchar(20) NOT NULL,
    emitido_en timestamp(6) without time zone NOT NULL,
    moneda varchar(3) NOT NULL DEFAULT 'PEN',
    comprobante_id uuid,
    documento_referencia_id uuid,
    motivo_codigo varchar(2),
    motivo_descripcion varchar(250),
    cliente_tipo_doc varchar(10) NOT NULL,
    cliente_doc varchar(11),
    cliente_nombre varchar(150) NOT NULL,
    cliente_direccion varchar(200),
    op_gravada numeric(12, 2) NOT NULL DEFAULT 0,
    op_exonerada numeric(12, 2) NOT NULL DEFAULT 0,
    op_inafecta numeric(12, 2) NOT NULL DEFAULT 0,
    igv numeric(12, 2) NOT NULL DEFAULT 0,
    total numeric(12, 2) NOT NULL,
    estado varchar(25) NOT NULL,
    digest_value varchar(100),
    xml_clave varchar(200),
    xml_sha256 varchar(64),
    cdr_clave varchar(200),
    cdr_codigo varchar(10),
    cdr_descripcion varchar(400),
    cdr_recibido_en timestamp(6) without time zone,
    intentos integer NOT NULL DEFAULT 0,
    proximo_intento timestamp(6) without time zone,
    ultimo_error varchar(400),
    resumen_id uuid,
    baja_resumen_id uuid,
    motivo_baja varchar(250),
    paciente_id uuid,
    creado_por_id uuid,
    creado_en timestamp(6) without time zone NOT NULL,
    CONSTRAINT uk_documentos_electronicos_numero UNIQUE (serie, numero),
    CONSTRAINT fk_docelec_comprobante FOREIGN KEY (comprobante_id) REFERENCES comprobantes (id),
    CONSTRAINT fk_docelec_referencia FOREIGN KEY (documento_referencia_id) REFERENCES documentos_electronicos (id),
    CONSTRAINT fk_docelec_resumen FOREIGN KEY (resumen_id) REFERENCES resumenes_sunat (id),
    CONSTRAINT fk_docelec_baja FOREIGN KEY (baja_resumen_id) REFERENCES resumenes_sunat (id),
    CONSTRAINT fk_docelec_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_docelec_usuario FOREIGN KEY (creado_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_docelec_tipo CHECK (tipo IN ('FACTURA', 'BOLETA', 'NOTA_CREDITO', 'NOTA_DEBITO')),
    CONSTRAINT ck_docelec_cliente CHECK (cliente_tipo_doc IN ('DNI', 'RUC', 'SIN_DOC')),
    CONSTRAINT ck_docelec_estado CHECK (estado IN (
        'PENDIENTE', 'ACEPTADO', 'ACEPTADO_OBSERVACIONES', 'RECHAZADO', 'ANULADO', 'ERROR'))
);

CREATE UNIQUE INDEX uk_docelec_comprobante ON documentos_electronicos (comprobante_id) WHERE comprobante_id IS NOT NULL;
CREATE INDEX idx_docelec_emitido ON documentos_electronicos (emitido_en);
CREATE INDEX idx_docelec_estado ON documentos_electronicos (estado, proximo_intento);
CREATE INDEX idx_docelec_referencia ON documentos_electronicos (documento_referencia_id);
CREATE INDEX idx_docelec_paciente ON documentos_electronicos (paciente_id);

CREATE TABLE documento_electronico_items (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    documento_id uuid NOT NULL,
    posicion integer NOT NULL DEFAULT 0,
    codigo varchar(30),
    descripcion varchar(200) NOT NULL,
    unidad varchar(3) NOT NULL,
    cantidad numeric(12, 3) NOT NULL,
    valor_unitario numeric(14, 6) NOT NULL,
    precio_unitario numeric(14, 6) NOT NULL,
    afectacion varchar(10) NOT NULL,
    valor_venta numeric(12, 2) NOT NULL,
    igv numeric(12, 2) NOT NULL,
    total numeric(12, 2) NOT NULL,
    CONSTRAINT fk_docelec_items_documento FOREIGN KEY (documento_id) REFERENCES documentos_electronicos (id),
    CONSTRAINT ck_docelec_items_afectacion CHECK (afectacion IN ('GRAVADO', 'EXONERADO', 'INAFECTO'))
);

CREATE INDEX idx_docelec_items_documento ON documento_electronico_items (documento_id);

CREATE TABLE intercambios_sunat (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    documento_id uuid,
    resumen_id uuid,
    operacion varchar(20) NOT NULL,
    proveedor varchar(12) NOT NULL,
    exitoso boolean NOT NULL,
    codigo_respuesta varchar(10),
    mensaje varchar(500),
    duracion_ms integer NOT NULL DEFAULT 0,
    hash_envio varchar(64),
    creado_en timestamp(6) without time zone NOT NULL,
    CONSTRAINT fk_intercambios_documento FOREIGN KEY (documento_id) REFERENCES documentos_electronicos (id),
    CONSTRAINT fk_intercambios_resumen FOREIGN KEY (resumen_id) REFERENCES resumenes_sunat (id)
);

CREATE INDEX idx_intercambios_documento ON intercambios_sunat (documento_id);
CREATE INDEX idx_intercambios_creado ON intercambios_sunat (creado_en);

CREATE TABLE bancos_datos (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    codigo varchar(30) NOT NULL,
    nombre varchar(150) NOT NULL,
    descripcion varchar(500) NOT NULL,
    finalidad_principal varchar(300) NOT NULL,
    categorias_datos varchar(400) NOT NULL,
    contiene_sensibles boolean NOT NULL,
    responsable varchar(150) NOT NULL,
    ubicacion varchar(150) NOT NULL,
    medidas_seguridad varchar(600) NOT NULL,
    transferencia_transfronteriza boolean NOT NULL DEFAULT false,
    numero_inscripcion varchar(40),
    fecha_inscripcion date,
    activo boolean NOT NULL DEFAULT true,
    actualizado_en timestamp(6) without time zone NOT NULL,
    CONSTRAINT uk_bancos_datos_codigo UNIQUE (codigo)
);

CREATE TABLE finalidades_tratamiento (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    codigo varchar(40) NOT NULL,
    nombre varchar(150) NOT NULL,
    descripcion varchar(500) NOT NULL,
    banco_id uuid,
    base_legal varchar(250) NOT NULL,
    obligatoria boolean NOT NULL,
    categoria_datos varchar(200) NOT NULL,
    plazo_retencion_dias integer,
    activa boolean NOT NULL DEFAULT true,
    CONSTRAINT uk_finalidades_codigo UNIQUE (codigo),
    CONSTRAINT fk_finalidades_banco FOREIGN KEY (banco_id) REFERENCES bancos_datos (id)
);

CREATE TABLE politicas_privacidad (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    version integer NOT NULL,
    titulo varchar(150) NOT NULL,
    contenido text NOT NULL,
    vigente_desde date NOT NULL,
    publicada boolean NOT NULL DEFAULT false,
    hash varchar(64) NOT NULL,
    creada_por_id uuid,
    creada_en timestamp(6) without time zone NOT NULL,
    CONSTRAINT uk_politicas_privacidad_version UNIQUE (version),
    CONSTRAINT fk_politicas_privacidad_usuario FOREIGN KEY (creada_por_id) REFERENCES usuarios (id)
);

CREATE TABLE aceptaciones_privacidad (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    usuario_id uuid NOT NULL,
    politica_id uuid NOT NULL,
    aceptada_en timestamp(6) without time zone NOT NULL,
    origen varchar(20) NOT NULL,
    CONSTRAINT uk_aceptaciones_privacidad UNIQUE (usuario_id, politica_id),
    CONSTRAINT fk_aceptaciones_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_aceptaciones_politica FOREIGN KEY (politica_id) REFERENCES politicas_privacidad (id)
);

CREATE TABLE consentimientos_tratamiento (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    usuario_id uuid NOT NULL,
    finalidad_id uuid NOT NULL,
    accion varchar(10) NOT NULL,
    politica_version integer,
    origen varchar(20) NOT NULL,
    motivo varchar(300),
    registrado_en timestamp(6) without time zone NOT NULL,
    CONSTRAINT fk_consent_trat_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_consent_trat_finalidad FOREIGN KEY (finalidad_id) REFERENCES finalidades_tratamiento (id),
    CONSTRAINT ck_consent_trat_accion CHECK (accion IN ('OTORGAR', 'REVOCAR'))
);

CREATE INDEX idx_consent_trat_usuario ON consentimientos_tratamiento (usuario_id, finalidad_id, registrado_en);

CREATE TABLE solicitudes_arco (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    codigo varchar(20) NOT NULL,
    usuario_id uuid NOT NULL,
    tipo varchar(15) NOT NULL,
    estado varchar(15) NOT NULL,
    descripcion varchar(1000) NOT NULL,
    campo varchar(30),
    valor_propuesto varchar(200),
    finalidad_id uuid,
    recibida_en timestamp(6) without time zone NOT NULL,
    vence_en date NOT NULL,
    responsable_id uuid,
    resuelta_en timestamp(6) without time zone,
    resolucion varchar(1000),
    exportacion_clave varchar(200),
    exportacion_sha256 varchar(64),
    resultado_json text,
    CONSTRAINT uk_solicitudes_arco_codigo UNIQUE (codigo),
    CONSTRAINT fk_arco_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_arco_responsable FOREIGN KEY (responsable_id) REFERENCES usuarios (id),
    CONSTRAINT fk_arco_finalidad FOREIGN KEY (finalidad_id) REFERENCES finalidades_tratamiento (id),
    CONSTRAINT ck_arco_tipo CHECK (tipo IN ('ACCESO', 'RECTIFICACION', 'CANCELACION', 'OPOSICION')),
    CONSTRAINT ck_arco_estado CHECK (estado IN ('RECIBIDA', 'EN_REVISION', 'ATENDIDA', 'PARCIAL', 'RECHAZADA'))
);

CREATE INDEX idx_arco_usuario ON solicitudes_arco (usuario_id, recibida_en);
CREATE INDEX idx_arco_estado ON solicitudes_arco (estado, vence_en);

CREATE TABLE solicitud_arco_eventos (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    solicitud_id uuid NOT NULL,
    actor_id uuid,
    accion varchar(30) NOT NULL,
    detalle varchar(1000),
    registrado_en timestamp(6) without time zone NOT NULL,
    CONSTRAINT fk_arco_eventos_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitudes_arco (id),
    CONSTRAINT fk_arco_eventos_actor FOREIGN KEY (actor_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_arco_eventos_solicitud ON solicitud_arco_eventos (solicitud_id, registrado_en);

CREATE TABLE politicas_retencion (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    tipo_dato varchar(30) NOT NULL,
    nombre varchar(150) NOT NULL,
    descripcion varchar(400) NOT NULL,
    plazo_dias integer NOT NULL,
    accion varchar(12) NOT NULL,
    base_legal varchar(250) NOT NULL,
    activa boolean NOT NULL DEFAULT true,
    ultima_ejecucion timestamp(6) without time zone,
    CONSTRAINT uk_politicas_retencion_tipo UNIQUE (tipo_dato),
    CONSTRAINT ck_politicas_retencion_accion CHECK (accion IN ('ANONIMIZAR', 'DEPURAR', 'MARCAR'))
);

CREATE TABLE ejecuciones_retencion (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    politica_id uuid NOT NULL,
    ejecutada_en timestamp(6) without time zone NOT NULL,
    afectados integer NOT NULL,
    detalle varchar(400),
    CONSTRAINT fk_ejecuciones_retencion_politica FOREIGN KEY (politica_id) REFERENCES politicas_retencion (id)
);

CREATE INDEX idx_ejecuciones_retencion_politica ON ejecuciones_retencion (politica_id, ejecutada_en);

CREATE TABLE incidentes_datos (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    codigo varchar(20) NOT NULL,
    tipo varchar(20) NOT NULL,
    severidad varchar(10) NOT NULL,
    estado varchar(15) NOT NULL,
    descripcion varchar(1000) NOT NULL,
    datos_afectados varchar(400) NOT NULL,
    titulares_afectados integer NOT NULL DEFAULT 0,
    detectado_en timestamp(6) without time zone NOT NULL,
    notificar_autoridad_antes_de timestamp(6) without time zone NOT NULL,
    autoridad_notificada_en timestamp(6) without time zone,
    titulares_notificados_en timestamp(6) without time zone,
    contencion varchar(1000),
    causa_raiz varchar(1000),
    cerrado_en timestamp(6) without time zone,
    responsable_id uuid,
    registrado_por_id uuid NOT NULL,
    CONSTRAINT uk_incidentes_datos_codigo UNIQUE (codigo),
    CONSTRAINT fk_incidentes_responsable FOREIGN KEY (responsable_id) REFERENCES usuarios (id),
    CONSTRAINT fk_incidentes_registrador FOREIGN KEY (registrado_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_incidentes_tipo CHECK (tipo IN (
        'ACCESO_NO_AUTORIZADO', 'PERDIDA', 'DIVULGACION', 'ALTERACION', 'RANSOMWARE', 'OTRO')),
    CONSTRAINT ck_incidentes_severidad CHECK (severidad IN ('BAJA', 'MEDIA', 'ALTA', 'CRITICA')),
    CONSTRAINT ck_incidentes_estado CHECK (estado IN ('ABIERTO', 'EN_CONTENCION', 'NOTIFICADO', 'CERRADO'))
);

CREATE INDEX idx_incidentes_datos_estado ON incidentes_datos (estado, detectado_en);

CREATE TABLE encargados_tratamiento (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    razon_social varchar(150) NOT NULL,
    ruc varchar(11),
    servicio varchar(200) NOT NULL,
    categorias_datos varchar(300) NOT NULL,
    pais varchar(60) NOT NULL,
    transfronterizo boolean NOT NULL DEFAULT false,
    contrato_vigente_hasta date,
    contacto varchar(150),
    activo boolean NOT NULL DEFAULT true,
    actualizado_en timestamp(6) without time zone NOT NULL
);

INSERT INTO politicas_retencion (tipo_dato, nombre, descripcion, plazo_dias, accion, base_legal, activa) VALUES
    ('ENTREGAS_NOTIFICACION', 'Entregas de avisos externos',
     'Destino y mensaje de los avisos enviados por correo, SMS o WhatsApp, que se anonimizan al vencer el plazo.',
     180, 'ANONIMIZAR', 'Principio de proporcionalidad de la Ley 29733, por confirmar con asesor legal', true),
    ('SESIONES_DISPOSITIVO', 'Sesiones y dispositivos',
     'Sesiones cerradas o vencidas con sus direcciones y agentes de usuario, que se depuran al vencer el plazo.',
     90, 'DEPURAR', 'Principio de proporcionalidad de la Ley 29733, por confirmar con asesor legal', true),
    ('EVENTOS_SEGURIDAD', 'Eventos de seguridad',
     'Direcciones de red de los eventos de seguridad, que se anonimizan al vencer el plazo.',
     365, 'ANONIMIZAR', 'Seguridad del banco de datos, por confirmar con asesor legal', true),
    ('INTERCAMBIOS_SUNAT', 'Bitácora de intercambios con SUNAT',
     'Registro de envíos de comprobantes electrónicos, que se depura al vencer el plazo tributario.',
     1825, 'DEPURAR', 'Plazo de conservación tributaria de cinco años, por confirmar con asesor legal', true),
    ('HISTORIA_CLINICA', 'Historia clínica',
     'Las historias clínicas no se eliminan de forma automática. La tarea solo marca las inactivas para revisión.',
     7300, 'MARCAR', 'Conservación obligatoria de la historia clínica según la norma sectorial, por confirmar con asesor legal',
     true);

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
    'INCIDENTE_DATOS', 'SISTEMA'));
