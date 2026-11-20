CREATE TABLE eventos_adversos (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    anio integer NOT NULL,
    numero integer NOT NULL,
    codigo varchar(20) NOT NULL,
    tipo varchar(25) NOT NULL,
    severidad varchar(12) NOT NULL,
    area varchar(60) NOT NULL,
    descripcion varchar(2000) NOT NULL,
    anonimo boolean NOT NULL DEFAULT false,
    notificante_id uuid NOT NULL,
    paciente_id uuid,
    cirugia_id uuid,
    dispensacion_id uuid,
    clasificacion varchar(40),
    estado varchar(12) NOT NULL DEFAULT 'NOTIFICADO',
    ocurrido_en timestamp(6) NOT NULL,
    notificado_en timestamp(6) NOT NULL,
    analisis_metodo varchar(15),
    analisis_conclusion varchar(1000),
    cerrado_en timestamp(6),
    cerrado_por_id uuid,
    verificacion varchar(1000),
    accion_eficaz boolean,
    CONSTRAINT pk_eventos_adversos PRIMARY KEY (id),
    CONSTRAINT uk_eventos_adversos_codigo UNIQUE (codigo),
    CONSTRAINT uk_eventos_adversos_numero UNIQUE (anio, numero),
    CONSTRAINT fk_eventos_adversos_notificante FOREIGN KEY (notificante_id) REFERENCES usuarios (id),
    CONSTRAINT fk_eventos_adversos_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_eventos_adversos_cirugia FOREIGN KEY (cirugia_id) REFERENCES cirugias (id),
    CONSTRAINT fk_eventos_adversos_dispensacion FOREIGN KEY (dispensacion_id) REFERENCES dispensaciones (id),
    CONSTRAINT fk_eventos_adversos_cierre FOREIGN KEY (cerrado_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_eventos_adversos_tipo CHECK (tipo IN ('EVENTO_ADVERSO', 'CASI_INCIDENTE', 'INCIDENTE_SIN_DANO')),
    CONSTRAINT ck_eventos_adversos_severidad CHECK (severidad IN ('LEVE', 'MODERADO', 'GRAVE', 'CENTINELA')),
    CONSTRAINT ck_eventos_adversos_estado CHECK (estado IN ('NOTIFICADO', 'EN_ANALISIS', 'EN_ACCION', 'CERRADO')),
    CONSTRAINT ck_eventos_adversos_metodo CHECK (analisis_metodo IS NULL OR analisis_metodo IN ('CINCO_PORQUES', 'ESPINA_PESCADO')),
    CONSTRAINT ck_eventos_adversos_anonimo CHECK (NOT anonimo OR (paciente_id IS NULL AND cirugia_id IS NULL AND dispensacion_id IS NULL))
);

CREATE INDEX idx_eventos_adversos_estado ON eventos_adversos (estado, notificado_en);
CREATE INDEX idx_eventos_adversos_notificante ON eventos_adversos (notificante_id, notificado_en);

CREATE TABLE evento_causas (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    evento_id uuid NOT NULL,
    posicion integer NOT NULL,
    categoria varchar(30) NOT NULL,
    texto varchar(500) NOT NULL,
    CONSTRAINT pk_evento_causas PRIMARY KEY (id),
    CONSTRAINT uk_evento_causas_posicion UNIQUE (evento_id, posicion),
    CONSTRAINT fk_evento_causas_evento FOREIGN KEY (evento_id) REFERENCES eventos_adversos (id)
);

CREATE TABLE evento_acciones (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    evento_id uuid NOT NULL,
    descripcion varchar(500) NOT NULL,
    responsable_id uuid NOT NULL,
    fecha_limite date NOT NULL,
    estado varchar(10) NOT NULL DEFAULT 'PENDIENTE',
    completada_en timestamp(6),
    creado_en timestamp(6) NOT NULL,
    CONSTRAINT pk_evento_acciones PRIMARY KEY (id),
    CONSTRAINT fk_evento_acciones_evento FOREIGN KEY (evento_id) REFERENCES eventos_adversos (id),
    CONSTRAINT fk_evento_acciones_responsable FOREIGN KEY (responsable_id) REFERENCES usuarios (id),
    CONSTRAINT ck_evento_acciones_estado CHECK (estado IN ('PENDIENTE', 'HECHA'))
);

CREATE INDEX idx_evento_acciones_evento ON evento_acciones (evento_id);

CREATE TABLE guias_clinicas (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    codigo varchar(20) NOT NULL,
    version integer NOT NULL,
    titulo varchar(200) NOT NULL,
    especialidad_id uuid,
    resumen varchar(1000) NOT NULL,
    contenido text NOT NULL,
    estado varchar(10) NOT NULL DEFAULT 'VIGENTE',
    vigente_desde date NOT NULL,
    vigente_hasta date,
    por_validar boolean NOT NULL DEFAULT true,
    creado_por_id uuid,
    creado_en timestamp(6) NOT NULL,
    CONSTRAINT pk_guias_clinicas PRIMARY KEY (id),
    CONSTRAINT uk_guias_clinicas_version UNIQUE (codigo, version),
    CONSTRAINT fk_guias_clinicas_especialidad FOREIGN KEY (especialidad_id) REFERENCES especialidades (id),
    CONSTRAINT fk_guias_clinicas_autor FOREIGN KEY (creado_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_guias_clinicas_estado CHECK (estado IN ('VIGENTE', 'OBSOLETA', 'BORRADOR'))
);

CREATE INDEX idx_guias_clinicas_estado ON guias_clinicas (estado, titulo);

CREATE TABLE guia_cie10 (
    guia_id uuid NOT NULL,
    codigo varchar(10) NOT NULL,
    CONSTRAINT pk_guia_cie10 PRIMARY KEY (guia_id, codigo),
    CONSTRAINT fk_guia_cie10_guia FOREIGN KEY (guia_id) REFERENCES guias_clinicas (id)
);
