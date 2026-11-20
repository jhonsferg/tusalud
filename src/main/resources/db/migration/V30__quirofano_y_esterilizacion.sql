CREATE TABLE secuencias_quirofano (
    nombre varchar(20) NOT NULL,
    ultimo bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_secuencias_quirofano PRIMARY KEY (nombre)
);

INSERT INTO secuencias_quirofano (nombre, ultimo) VALUES ('CIRUGIA', 0), ('CICLO', 0), ('LOTE', 0);

CREATE TABLE quirofanos (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    codigo varchar(20) NOT NULL,
    nombre varchar(100) NOT NULL,
    ubicacion varchar(150),
    estado varchar(25) NOT NULL DEFAULT 'DISPONIBLE',
    activo boolean NOT NULL DEFAULT true,
    observaciones varchar(300),
    CONSTRAINT pk_quirofanos PRIMARY KEY (id),
    CONSTRAINT uk_quirofanos_codigo UNIQUE (codigo),
    CONSTRAINT ck_quirofanos_estado CHECK (estado IN ('DISPONIBLE', 'MANTENIMIENTO', 'FUERA_DE_SERVICIO'))
);

CREATE TABLE quirofano_equipos (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    quirofano_id uuid NOT NULL,
    nombre varchar(100) NOT NULL,
    serie varchar(60),
    activo boolean NOT NULL DEFAULT true,
    CONSTRAINT pk_quirofano_equipos PRIMARY KEY (id),
    CONSTRAINT fk_quirofano_equipos_quirofano FOREIGN KEY (quirofano_id) REFERENCES quirofanos (id)
);

CREATE INDEX idx_quirofano_equipos_quirofano ON quirofano_equipos (quirofano_id);

CREATE TABLE cirugias (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    codigo varchar(20) NOT NULL,
    paciente_id uuid NOT NULL,
    solicitante_id uuid NOT NULL,
    cie10_id uuid,
    diagnostico_codigo varchar(20) NOT NULL,
    diagnostico_descripcion varchar(300) NOT NULL,
    procedimiento varchar(200) NOT NULL,
    tipo varchar(15) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'SOLICITADA',
    quirofano_id uuid,
    inicio_programado timestamp,
    fin_programado timestamp,
    tipo_anestesia varchar(15) NOT NULL DEFAULT 'GENERAL',
    ingreso_id uuid,
    consentimiento_quirurgico_id uuid,
    consentimiento_anestesico_id uuid,
    indicaciones_preoperatorias varchar(500),
    motivo varchar(300),
    inicio_real timestamp,
    fin_real timestamp,
    hallazgos text,
    tecnica text,
    complicaciones text,
    sangrado_ml integer,
    diagnostico_postoperatorio varchar(300),
    indicaciones_postoperatorias varchar(500),
    nota_registrada_en timestamp,
    nota_por_id uuid,
    asa smallint,
    anestesia_inicio timestamp,
    anestesia_fin timestamp,
    anestesia_observaciones text,
    anestesia_registrada_en timestamp,
    anestesia_por_id uuid,
    creado_en timestamp NOT NULL DEFAULT now(),
    actualizado_en timestamp NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_cirugias PRIMARY KEY (id),
    CONSTRAINT uk_cirugias_codigo UNIQUE (codigo),
    CONSTRAINT fk_cirugias_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_cirugias_solicitante FOREIGN KEY (solicitante_id) REFERENCES medicos (id),
    CONSTRAINT fk_cirugias_cie10 FOREIGN KEY (cie10_id) REFERENCES catalogo_cie10 (id),
    CONSTRAINT fk_cirugias_quirofano FOREIGN KEY (quirofano_id) REFERENCES quirofanos (id),
    CONSTRAINT fk_cirugias_ingreso FOREIGN KEY (ingreso_id) REFERENCES ingresos_hospitalarios (id),
    CONSTRAINT fk_cirugias_consentimiento_quirurgico FOREIGN KEY (consentimiento_quirurgico_id) REFERENCES consentimientos (id),
    CONSTRAINT fk_cirugias_consentimiento_anestesico FOREIGN KEY (consentimiento_anestesico_id) REFERENCES consentimientos (id),
    CONSTRAINT fk_cirugias_nota_por FOREIGN KEY (nota_por_id) REFERENCES usuarios (id),
    CONSTRAINT fk_cirugias_anestesia_por FOREIGN KEY (anestesia_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_cirugias_tipo CHECK (tipo IN ('ELECTIVA', 'URGENCIA')),
    CONSTRAINT ck_cirugias_estado CHECK (estado IN (
        'SOLICITADA', 'PROGRAMADA', 'EN_PREPARACION', 'EN_CURSO', 'FINALIZADA', 'SUSPENDIDA', 'CANCELADA')),
    CONSTRAINT ck_cirugias_anestesia CHECK (tipo_anestesia IN ('GENERAL', 'REGIONAL', 'LOCAL', 'SEDACION', 'NINGUNA')),
    CONSTRAINT ck_cirugias_asa CHECK (asa IS NULL OR asa BETWEEN 1 AND 6),
    CONSTRAINT ck_cirugias_horario CHECK (fin_programado IS NULL OR inicio_programado IS NULL OR fin_programado > inicio_programado)
);

CREATE INDEX idx_cirugias_paciente ON cirugias (paciente_id);
CREATE INDEX idx_cirugias_agenda ON cirugias (quirofano_id, inicio_programado);
CREATE INDEX idx_cirugias_estado ON cirugias (estado, inicio_programado);

CREATE TABLE cirugia_equipo (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    cirugia_id uuid NOT NULL,
    usuario_id uuid NOT NULL,
    rol varchar(20) NOT NULL,
    asignado_en timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_cirugia_equipo PRIMARY KEY (id),
    CONSTRAINT fk_cirugia_equipo_cirugia FOREIGN KEY (cirugia_id) REFERENCES cirugias (id),
    CONSTRAINT fk_cirugia_equipo_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT uk_cirugia_equipo UNIQUE (cirugia_id, usuario_id, rol),
    CONSTRAINT ck_cirugia_equipo_rol CHECK (rol IN ('CIRUJANO', 'AYUDANTE', 'ANESTESIOLOGO', 'INSTRUMENTISTA', 'CIRCULANTE'))
);

CREATE INDEX idx_cirugia_equipo_usuario ON cirugia_equipo (usuario_id);

CREATE TABLE cirugia_checklists (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    cirugia_id uuid NOT NULL,
    momento varchar(20) NOT NULL,
    items varchar(500) NOT NULL,
    observaciones varchar(300),
    completado_por_id uuid NOT NULL,
    completado_en timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_cirugia_checklists PRIMARY KEY (id),
    CONSTRAINT fk_cirugia_checklists_cirugia FOREIGN KEY (cirugia_id) REFERENCES cirugias (id),
    CONSTRAINT fk_cirugia_checklists_usuario FOREIGN KEY (completado_por_id) REFERENCES usuarios (id),
    CONSTRAINT uk_cirugia_checklists UNIQUE (cirugia_id, momento),
    CONSTRAINT ck_cirugia_checklists_momento CHECK (momento IN ('ANTES_INDUCCION', 'ANTES_INCISION', 'ANTES_SALIDA'))
);

CREATE TABLE cirugia_insumos (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    cirugia_id uuid NOT NULL,
    producto_id uuid NOT NULL,
    cantidad integer NOT NULL,
    referencia varchar(40) NOT NULL,
    registrado_por_id uuid NOT NULL,
    registrado_en timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_cirugia_insumos PRIMARY KEY (id),
    CONSTRAINT fk_cirugia_insumos_cirugia FOREIGN KEY (cirugia_id) REFERENCES cirugias (id),
    CONSTRAINT fk_cirugia_insumos_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_cirugia_insumos_usuario FOREIGN KEY (registrado_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_cirugia_insumos_cantidad CHECK (cantidad > 0)
);

CREATE INDEX idx_cirugia_insumos_cirugia ON cirugia_insumos (cirugia_id);

CREATE TABLE cirugia_eventos (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    cirugia_id uuid NOT NULL,
    fecha timestamp NOT NULL DEFAULT now(),
    usuario_id uuid NOT NULL,
    accion varchar(30) NOT NULL,
    detalle varchar(300),
    CONSTRAINT pk_cirugia_eventos PRIMARY KEY (id),
    CONSTRAINT fk_cirugia_eventos_cirugia FOREIGN KEY (cirugia_id) REFERENCES cirugias (id),
    CONSTRAINT fk_cirugia_eventos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_cirugia_eventos_cirugia ON cirugia_eventos (cirugia_id, fecha);

CREATE TABLE esterilizadores (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    codigo varchar(20) NOT NULL,
    nombre varchar(100) NOT NULL,
    tipo varchar(20) NOT NULL,
    activo boolean NOT NULL DEFAULT true,
    CONSTRAINT pk_esterilizadores PRIMARY KEY (id),
    CONSTRAINT uk_esterilizadores_codigo UNIQUE (codigo),
    CONSTRAINT ck_esterilizadores_tipo CHECK (tipo IN ('VAPOR', 'PLASMA', 'OXIDO_ETILENO'))
);

CREATE TABLE sets_instrumental (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    codigo varchar(20) NOT NULL,
    nombre varchar(120) NOT NULL,
    descripcion varchar(300),
    vigencia_dias integer NOT NULL DEFAULT 30,
    activo boolean NOT NULL DEFAULT true,
    CONSTRAINT pk_sets_instrumental PRIMARY KEY (id),
    CONSTRAINT uk_sets_instrumental_codigo UNIQUE (codigo),
    CONSTRAINT ck_sets_instrumental_vigencia CHECK (vigencia_dias BETWEEN 1 AND 365)
);

CREATE TABLE ciclos_esterilizacion (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    numero varchar(20) NOT NULL,
    esterilizador_id uuid NOT NULL,
    iniciado_en timestamp NOT NULL,
    temperatura_c numeric(5, 1) NOT NULL,
    presion_kpa numeric(6, 1),
    duracion_min integer NOT NULL,
    indicador_quimico varchar(15) NOT NULL DEFAULT 'PENDIENTE',
    indicador_biologico varchar(15) NOT NULL DEFAULT 'PENDIENTE',
    resultado varchar(15) NOT NULL DEFAULT 'EN_PROCESO',
    responsable_id uuid NOT NULL,
    observaciones varchar(300),
    cerrado_en timestamp,
    creado_en timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_ciclos_esterilizacion PRIMARY KEY (id),
    CONSTRAINT uk_ciclos_esterilizacion_numero UNIQUE (numero),
    CONSTRAINT fk_ciclos_esterilizador FOREIGN KEY (esterilizador_id) REFERENCES esterilizadores (id),
    CONSTRAINT fk_ciclos_responsable FOREIGN KEY (responsable_id) REFERENCES usuarios (id),
    CONSTRAINT ck_ciclos_quimico CHECK (indicador_quimico IN ('PENDIENTE', 'CONFORME', 'NO_CONFORME')),
    CONSTRAINT ck_ciclos_biologico CHECK (indicador_biologico IN ('PENDIENTE', 'CONFORME', 'NO_CONFORME')),
    CONSTRAINT ck_ciclos_resultado CHECK (resultado IN ('EN_PROCESO', 'CONFORME', 'NO_CONFORME')),
    CONSTRAINT ck_ciclos_duracion CHECK (duracion_min > 0)
);

CREATE INDEX idx_ciclos_iniciado ON ciclos_esterilizacion (iniciado_en);

CREATE TABLE lotes_esteriles (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    codigo varchar(20) NOT NULL,
    set_id uuid NOT NULL,
    ciclo_id uuid NOT NULL,
    esterilizado_en timestamp NOT NULL,
    vence_en timestamp NOT NULL,
    estado varchar(15) NOT NULL DEFAULT 'ESTERIL',
    CONSTRAINT pk_lotes_esteriles PRIMARY KEY (id),
    CONSTRAINT uk_lotes_esteriles_codigo UNIQUE (codigo),
    CONSTRAINT fk_lotes_esteriles_set FOREIGN KEY (set_id) REFERENCES sets_instrumental (id),
    CONSTRAINT fk_lotes_esteriles_ciclo FOREIGN KEY (ciclo_id) REFERENCES ciclos_esterilizacion (id),
    CONSTRAINT ck_lotes_esteriles_estado CHECK (estado IN ('ESTERIL', 'EN_USO', 'USADO'))
);

CREATE INDEX idx_lotes_esteriles_estado ON lotes_esteriles (estado, vence_en);
CREATE INDEX idx_lotes_esteriles_ciclo ON lotes_esteriles (ciclo_id);

CREATE TABLE cirugia_sets (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    cirugia_id uuid NOT NULL,
    lote_id uuid NOT NULL,
    registrado_por_id uuid NOT NULL,
    registrado_en timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_cirugia_sets PRIMARY KEY (id),
    CONSTRAINT fk_cirugia_sets_cirugia FOREIGN KEY (cirugia_id) REFERENCES cirugias (id),
    CONSTRAINT fk_cirugia_sets_lote FOREIGN KEY (lote_id) REFERENCES lotes_esteriles (id),
    CONSTRAINT fk_cirugia_sets_usuario FOREIGN KEY (registrado_por_id) REFERENCES usuarios (id),
    CONSTRAINT uk_cirugia_sets_lote UNIQUE (lote_id)
);

CREATE INDEX idx_cirugia_sets_cirugia ON cirugia_sets (cirugia_id);

INSERT INTO consentimiento_plantillas (codigo, nombre, texto, riesgos, beneficios, alternativas, requiere_testigo, vigencia_dias, version, activa) VALUES
    ('CIRUGIA_MAYOR', 'Consentimiento para cirugía',
     'Autorizo al equipo quirúrgico a realizar el procedimiento indicado, así como las medidas que resulten necesarias durante la intervención para preservar mi salud. He podido preguntar mis dudas y se me explicó en qué consiste la cirugía.',
     'Sangrado, infección de la herida, reacciones a medicamentos, lesión de estructuras vecinas, necesidad de una nueva intervención, complicaciones propias de mi estado de salud y, de manera excepcional, riesgo vital.',
     'Tratar o aliviar la enfermedad que motiva la cirugía y mejorar mi calidad de vida o mi pronóstico.',
     'Tratamiento médico, observación o un procedimiento menos invasivo, según lo que mi cirujano me explicó.',
     true, 30, 1, true),
    ('ANESTESIA_GENERAL', 'Consentimiento para anestesia',
     'Autorizo la administración de la anestesia indicada por el anestesiólogo y el monitoreo y las medidas de soporte que se requieran durante el procedimiento. Informé mis alergias, medicamentos y antecedentes.',
     'Náuseas y vómitos, dolor de garganta, somnolencia prolongada, reacciones alérgicas, alteraciones del ritmo del corazón o de la respiración y, de manera excepcional, complicaciones graves.',
     'Permitir que el procedimiento se realice sin dolor y con seguridad.',
     'Anestesia regional, local con sedación u otra técnica, según lo que mi anestesiólogo me explicó.',
     false, 30, 1, true)
ON CONFLICT (codigo) DO NOTHING;

ALTER TABLE movimientos_inventario DROP CONSTRAINT movimientos_motivo_check;
ALTER TABLE movimientos_inventario ADD CONSTRAINT movimientos_motivo_check CHECK (motivo IN (
    'COMPRA', 'CONSUMO_INTERNO', 'MERMA', 'VENCIMIENTO', 'AJUSTE_NEGATIVO', 'AJUSTE_POSITIVO',
    'DISPENSACION', 'ANULACION_DISPENSACION', 'DEVOLUCION_PROVEEDOR', 'CONSUMO_QUIRURGICO'));

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
    'ESTERILIZACION_ALERTA', 'SISTEMA'));
