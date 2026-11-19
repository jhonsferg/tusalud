CREATE TABLE cie10_versiones (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    etiqueta character varying(60) NOT NULL,
    origen character varying(15) NOT NULL,
    total integer NOT NULL DEFAULT 0,
    altas integer NOT NULL DEFAULT 0,
    cambios integer NOT NULL DEFAULT 0,
    creada_por uuid,
    creada_en timestamp(6) without time zone NOT NULL,
    activa boolean NOT NULL DEFAULT false,
    CONSTRAINT cie10_versiones_pkey PRIMARY KEY (id),
    CONSTRAINT cie10_versiones_origen_check CHECK (origen IN ('MUESTRA', 'IMPORTACION')),
    CONSTRAINT fk_cie10_versiones_usuario FOREIGN KEY (creada_por) REFERENCES usuarios (id)
);

CREATE TABLE catalogo_cie10 (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    codigo character varying(8) NOT NULL,
    descripcion character varying(300) NOT NULL,
    texto_busqueda character varying(400) NOT NULL,
    capitulo character varying(200),
    grupo character varying(160),
    sexo character(1),
    edad_minima integer,
    edad_maxima integer,
    activo boolean NOT NULL DEFAULT true,
    muestra boolean NOT NULL DEFAULT false,
    version_id uuid,
    actualizado_en timestamp(6) without time zone NOT NULL,
    CONSTRAINT catalogo_cie10_pkey PRIMARY KEY (id),
    CONSTRAINT uk_cie10_codigo UNIQUE (codigo),
    CONSTRAINT catalogo_cie10_sexo_check CHECK (sexo IN ('M', 'F')),
    CONSTRAINT catalogo_cie10_edades_check CHECK (edad_minima IS NULL OR edad_maxima IS NULL OR edad_minima <= edad_maxima),
    CONSTRAINT fk_cie10_version FOREIGN KEY (version_id) REFERENCES cie10_versiones (id)
);

CREATE INDEX idx_cie10_codigo_prefijo ON catalogo_cie10 (lower(codigo) varchar_pattern_ops);
CREATE INDEX idx_cie10_texto_prefijo ON catalogo_cie10 (texto_busqueda varchar_pattern_ops);
CREATE INDEX idx_cie10_activo ON catalogo_cie10 (activo);

CREATE TABLE atencion_diagnosticos (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    atencion_id uuid NOT NULL,
    cie10_id uuid NOT NULL,
    codigo character varying(8) NOT NULL,
    descripcion character varying(300) NOT NULL,
    tipo character varying(12) NOT NULL,
    principal boolean NOT NULL DEFAULT false,
    observacion character varying(200),
    posicion integer NOT NULL DEFAULT 0,
    CONSTRAINT atencion_diagnosticos_pkey PRIMARY KEY (id),
    CONSTRAINT uk_atencion_diagnostico_codigo UNIQUE (atencion_id, codigo),
    CONSTRAINT atencion_diagnosticos_tipo_check CHECK (tipo IN ('PRESUNTIVO', 'DEFINITIVO', 'REPETIDO')),
    CONSTRAINT fk_atencion_diagnosticos_atencion FOREIGN KEY (atencion_id) REFERENCES atenciones (id) ON DELETE CASCADE,
    CONSTRAINT fk_atencion_diagnosticos_cie10 FOREIGN KEY (cie10_id) REFERENCES catalogo_cie10 (id)
);

CREATE INDEX idx_atencion_diagnosticos_codigo ON atencion_diagnosticos (codigo);
CREATE INDEX idx_atencion_diagnosticos_principal ON atencion_diagnosticos (principal, atencion_id);

ALTER TABLE pacientes ADD COLUMN sexo character(1);
ALTER TABLE pacientes ADD CONSTRAINT pacientes_sexo_check CHECK (sexo IN ('M', 'F'));

ALTER TABLE ingresos_hospitalarios
    ADD COLUMN cie10_ingreso_codigo character varying(8),
    ADD COLUMN cie10_ingreso_descripcion character varying(300),
    ADD COLUMN cie10_alta_codigo character varying(8),
    ADD COLUMN cie10_alta_descripcion character varying(300);
