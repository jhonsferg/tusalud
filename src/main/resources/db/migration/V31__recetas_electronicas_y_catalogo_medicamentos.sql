CREATE TABLE principios_activos (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    dci varchar(120) NOT NULL,
    clase_terapeutica varchar(80),
    grupo_alergia varchar(40),
    alias_alergia varchar(250),
    dosis_max_adulto_mg_dia numeric(10, 2),
    dosis_max_pediatrica_mg_kg_dia numeric(8, 3),
    categoria_embarazo varchar(15) NOT NULL DEFAULT 'SIN_CLASIFICAR',
    lactancia varchar(15) NOT NULL DEFAULT 'SIN_CLASIFICAR',
    aviso_renal varchar(300),
    activo boolean NOT NULL DEFAULT true,
    creado_en timestamp(6) NOT NULL DEFAULT now(),
    CONSTRAINT pk_principios_activos PRIMARY KEY (id),
    CONSTRAINT uk_principios_activos_dci UNIQUE (dci),
    CONSTRAINT ck_principios_embarazo CHECK (categoria_embarazo IN ('A', 'B', 'C', 'D', 'X', 'SIN_CLASIFICAR')),
    CONSTRAINT ck_principios_lactancia CHECK (lactancia IN ('COMPATIBLE', 'PRECAUCION', 'CONTRAINDICADO', 'SIN_CLASIFICAR'))
);

CREATE TABLE medicamentos (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    principio_activo_id uuid NOT NULL,
    nombre_comercial varchar(120),
    forma_farmaceutica varchar(60) NOT NULL,
    concentracion varchar(60) NOT NULL,
    via_administracion varchar(30) NOT NULL,
    presentacion varchar(120),
    categoria_control varchar(20) NOT NULL DEFAULT 'NINGUNA',
    requiere_receta boolean NOT NULL DEFAULT true,
    estado_venta varchar(15) NOT NULL DEFAULT 'VIGENTE',
    producto_id uuid,
    en_petitorio boolean NOT NULL DEFAULT true,
    activo boolean NOT NULL DEFAULT true,
    creado_en timestamp(6) NOT NULL DEFAULT now(),
    CONSTRAINT pk_medicamentos PRIMARY KEY (id),
    CONSTRAINT fk_medicamentos_principio FOREIGN KEY (principio_activo_id) REFERENCES principios_activos (id),
    CONSTRAINT fk_medicamentos_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT ck_medicamentos_control CHECK (categoria_control IN ('NINGUNA', 'ANTIBIOTICO', 'PSICOTROPICO', 'ESTUPEFACIENTE')),
    CONSTRAINT ck_medicamentos_estado CHECK (estado_venta IN ('VIGENTE', 'SUSPENDIDO', 'RETIRADO'))
);

CREATE UNIQUE INDEX uk_medicamentos_combinacion
    ON medicamentos (principio_activo_id, lower(forma_farmaceutica), lower(concentracion));
CREATE INDEX idx_medicamentos_principio ON medicamentos (principio_activo_id);
CREATE INDEX idx_medicamentos_producto ON medicamentos (producto_id);

CREATE TABLE interacciones_medicamentosas (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    principio_a_id uuid NOT NULL,
    principio_b_id uuid NOT NULL,
    severidad varchar(15) NOT NULL,
    descripcion varchar(400) NOT NULL,
    recomendacion varchar(400),
    CONSTRAINT pk_interacciones PRIMARY KEY (id),
    CONSTRAINT fk_interacciones_a FOREIGN KEY (principio_a_id) REFERENCES principios_activos (id),
    CONSTRAINT fk_interacciones_b FOREIGN KEY (principio_b_id) REFERENCES principios_activos (id),
    CONSTRAINT uk_interacciones_par UNIQUE (principio_a_id, principio_b_id),
    CONSTRAINT ck_interacciones_distintos CHECK (principio_a_id <> principio_b_id),
    CONSTRAINT ck_interacciones_orden CHECK (principio_a_id < principio_b_id),
    CONSTRAINT ck_interacciones_severidad CHECK (severidad IN ('LEVE', 'MODERADA', 'GRAVE', 'CONTRAINDICADA'))
);

CREATE TABLE recetas_electronicas (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    numero varchar(20) NOT NULL,
    atencion_id uuid NOT NULL,
    paciente_id uuid NOT NULL,
    medico_id uuid NOT NULL,
    estado varchar(20) NOT NULL,
    emitida_en timestamp(6) NOT NULL,
    vigente_hasta timestamp(6) NOT NULL,
    indicaciones varchar(1000),
    diagnostico_codigo varchar(12),
    diagnostico_descripcion varchar(300),
    contenido_canonico text NOT NULL,
    hash varchar(64) NOT NULL,
    firma text NOT NULL,
    firma_algoritmo varchar(30) NOT NULL,
    certificado_huella varchar(64) NOT NULL,
    certificado_sujeto varchar(200) NOT NULL,
    certificado_der text NOT NULL,
    certificado_de_prueba boolean NOT NULL,
    codigo_verificacion varchar(19) NOT NULL,
    control_especial boolean NOT NULL DEFAULT false,
    anulada_en timestamp(6),
    motivo_anulacion varchar(300),
    reemplaza_id uuid,
    CONSTRAINT pk_recetas_electronicas PRIMARY KEY (id),
    CONSTRAINT uk_recetas_numero UNIQUE (numero),
    CONSTRAINT uk_recetas_codigo UNIQUE (codigo_verificacion),
    CONSTRAINT fk_recetas_atencion FOREIGN KEY (atencion_id) REFERENCES atenciones (id),
    CONSTRAINT fk_recetas_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_recetas_medico FOREIGN KEY (medico_id) REFERENCES medicos (id),
    CONSTRAINT fk_recetas_reemplaza FOREIGN KEY (reemplaza_id) REFERENCES recetas_electronicas (id),
    CONSTRAINT ck_recetas_estado CHECK (estado IN ('EMITIDA', 'DISPENSADA_PARCIAL', 'DISPENSADA', 'ANULADA'))
);

CREATE INDEX idx_recetas_paciente ON recetas_electronicas (paciente_id);
CREATE INDEX idx_recetas_atencion ON recetas_electronicas (atencion_id);
CREATE INDEX idx_recetas_medico ON recetas_electronicas (medico_id);
CREATE INDEX idx_recetas_estado_vigencia ON recetas_electronicas (estado, vigente_hasta);

CREATE TABLE receta_electronica_items (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    receta_id uuid NOT NULL,
    posicion integer NOT NULL,
    atencion_posicion integer,
    medicamento_id uuid,
    medicamento_texto varchar(200) NOT NULL,
    dosis varchar(100),
    frecuencia varchar(100),
    duracion varchar(100),
    via varchar(30),
    dosis_unitaria_mg numeric(10, 2),
    veces_por_dia integer,
    cantidad integer NOT NULL,
    cantidad_dispensada integer NOT NULL DEFAULT 0,
    control_especial boolean NOT NULL DEFAULT false,
    CONSTRAINT pk_receta_items PRIMARY KEY (id),
    CONSTRAINT fk_receta_items_receta FOREIGN KEY (receta_id) REFERENCES recetas_electronicas (id),
    CONSTRAINT fk_receta_items_medicamento FOREIGN KEY (medicamento_id) REFERENCES medicamentos (id),
    CONSTRAINT uk_receta_items_posicion UNIQUE (receta_id, posicion),
    CONSTRAINT ck_receta_items_cantidad CHECK (cantidad > 0 AND cantidad_dispensada >= 0 AND cantidad_dispensada <= cantidad)
);

CREATE INDEX idx_receta_items_medicamento ON receta_electronica_items (medicamento_id);

CREATE TABLE receta_alertas (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    receta_id uuid NOT NULL,
    clave varchar(120) NOT NULL,
    tipo varchar(20) NOT NULL,
    severidad varchar(20) NOT NULL,
    mensaje varchar(500) NOT NULL,
    justificacion varchar(500),
    justificada_por_id uuid,
    justificada_en timestamp(6),
    CONSTRAINT pk_receta_alertas PRIMARY KEY (id),
    CONSTRAINT fk_receta_alertas_receta FOREIGN KEY (receta_id) REFERENCES recetas_electronicas (id),
    CONSTRAINT fk_receta_alertas_usuario FOREIGN KEY (justificada_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_receta_alertas_tipo CHECK (tipo IN ('ALERGIA', 'INTERACCION', 'DUPLICIDAD', 'DOSIS_MAXIMA', 'EMBARAZO', 'LACTANCIA', 'RENAL')),
    CONSTRAINT ck_receta_alertas_severidad CHECK (severidad IN ('INFORMATIVA', 'MODERADA', 'GRAVE', 'CONTRAINDICADA'))
);

CREATE INDEX idx_receta_alertas_receta ON receta_alertas (receta_id);

CREATE TABLE receta_dispensaciones (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    item_id uuid NOT NULL,
    cantidad integer NOT NULL,
    fecha timestamp(6) NOT NULL,
    usuario_id uuid NOT NULL,
    dispensacion_id uuid,
    revertida boolean NOT NULL DEFAULT false,
    CONSTRAINT pk_receta_dispensaciones PRIMARY KEY (id),
    CONSTRAINT fk_receta_disp_item FOREIGN KEY (item_id) REFERENCES receta_electronica_items (id),
    CONSTRAINT ck_receta_disp_cantidad CHECK (cantidad > 0)
);

CREATE INDEX idx_receta_disp_item ON receta_dispensaciones (item_id);
CREATE INDEX idx_receta_disp_dispensacion ON receta_dispensaciones (dispensacion_id);

ALTER TABLE notificaciones DROP CONSTRAINT notificaciones_tipo_check;
ALTER TABLE notificaciones ADD CONSTRAINT notificaciones_tipo_check CHECK (tipo IN (
    'CITA_CREADA', 'CITA_CONFIRMADA', 'CITA_CANCELADA', 'CITA_ATENDIDA', 'CITA_REPROGRAMADA',
    'RECORDATORIO_CITA', 'RECORDATORIO_EVENTO', 'INICIO_SESION', 'CONTRASENA_CAMBIADA', 'PAGO_REGISTRADO',
    'PAGO_REEMBOLSADO', 'MFA_ACTIVADO', 'MFA_DESACTIVADO', 'PACIENTE_COMPARTIDO', 'ROLES_ACTUALIZADOS',
    'SESION_SOSPECHOSA', 'ORDEN_LABORATORIO', 'RESULTADO_LABORATORIO', 'RECETA_DISPENSADA', 'STOCK_BAJO',
    'PRODUCTO_POR_VENCER', 'PACIENTE_EN_ESPERA', 'TURNO_LLAMADO', 'COMPROBANTE_EMITIDO', 'INGRESO_HOSPITALARIO',
    'ALTA_HOSPITALARIA', 'EMERGENCIA_TRIAJE', 'CAJA_DIFERENCIA', 'SOLICITUD_AUSENCIA', 'AUSENCIA_RESUELTA',
    'ORDEN_IMAGEN', 'ESTUDIO_INFORMADO', 'HALLAZGO_CRITICO', 'EXPORTACION_LISTA', 'PREAUTORIZACION_SOLICITADA',
    'PREAUTORIZACION_RESUELTA', 'TELECONSULTA_PROXIMA', 'TELECONSULTA_SALA_ABIERTA', 'TELECONSULTA_PACIENTE_EN_SALA', 'DOCUMENTO_EMITIDO',
    'CONSENTIMIENTO_PENDIENTE', 'CONSENTIMIENTO_FIRMADO', 'RESPALDO_FALLIDO', 'RESPALDO_VENCIDO', 'COMPROBANTE_RECHAZADO',
    'ARCO_RECIBIDA', 'ARCO_RESUELTA', 'INCIDENTE_DATOS', 'REQUISICION_ENVIADA', 'REQUISICION_RESUELTA',
    'ORDEN_COMPRA_POR_APROBAR', 'ORDEN_COMPRA_RESUELTA', 'CUENTA_POR_PAGAR_VENCIDA', 'PLANILLA_POR_APROBAR', 'PLANILLA_APROBADA',
    'BOLETA_DISPONIBLE', 'VACUNA_PROXIMA', 'PLAN_ODONTOLOGICO_PRESENTADO', 'CIRUGIA_PROGRAMADA', 'CIRUGIA_ASIGNADA',
    'ESTERILIZACION_ALERTA', 'RECETA_EMITIDA', 'SISTEMA'));
