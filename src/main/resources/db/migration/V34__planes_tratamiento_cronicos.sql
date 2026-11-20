CREATE TABLE planes_tratamiento (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    paciente_id uuid NOT NULL,
    medico_id uuid NOT NULL,
    programa varchar(15) NOT NULL,
    diagnostico_codigo varchar(10),
    objetivos varchar(1000) NOT NULL,
    intervenciones varchar(2000),
    medicacion varchar(1000),
    receta_id uuid,
    estado varchar(12) NOT NULL DEFAULT 'ACTIVO',
    inicio date NOT NULL,
    fin date,
    frecuencia_control_dias integer NOT NULL DEFAULT 30,
    creado_en timestamp(6) NOT NULL,
    CONSTRAINT pk_planes_tratamiento PRIMARY KEY (id),
    CONSTRAINT fk_planes_tratamiento_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_planes_tratamiento_medico FOREIGN KEY (medico_id) REFERENCES medicos (id),
    CONSTRAINT fk_planes_tratamiento_receta FOREIGN KEY (receta_id) REFERENCES recetas_electronicas (id),
    CONSTRAINT ck_planes_tratamiento_programa CHECK (programa IN ('HIPERTENSION', 'DIABETES', 'ASMA', 'OTRO')),
    CONSTRAINT ck_planes_tratamiento_estado CHECK (estado IN ('ACTIVO', 'COMPLETADO', 'SUSPENDIDO')),
    CONSTRAINT ck_planes_tratamiento_frecuencia CHECK (frecuencia_control_dias BETWEEN 1 AND 365)
);

CREATE INDEX idx_planes_tratamiento_paciente ON planes_tratamiento (paciente_id, estado);
CREATE INDEX idx_planes_tratamiento_medico ON planes_tratamiento (medico_id, estado);

CREATE TABLE plan_metas (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    plan_id uuid NOT NULL,
    parametro varchar(25) NOT NULL,
    valor_min numeric(8, 2),
    valor_max numeric(8, 2),
    unidad varchar(15) NOT NULL,
    CONSTRAINT pk_plan_metas PRIMARY KEY (id),
    CONSTRAINT uk_plan_metas_parametro UNIQUE (plan_id, parametro),
    CONSTRAINT fk_plan_metas_plan FOREIGN KEY (plan_id) REFERENCES planes_tratamiento (id),
    CONSTRAINT ck_plan_metas_parametro CHECK (parametro IN ('PRESION_SISTOLICA', 'PRESION_DIASTOLICA', 'GLUCOSA_AYUNO', 'HEMOGLOBINA_GLICOSILADA', 'PESO_KG', 'FLUJO_ESPIRATORIO')),
    CONSTRAINT ck_plan_metas_rango CHECK (valor_min IS NOT NULL OR valor_max IS NOT NULL)
);

CREATE TABLE plan_controles (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    plan_id uuid NOT NULL,
    numero integer NOT NULL,
    fecha_programada date NOT NULL,
    estado varchar(10) NOT NULL DEFAULT 'PENDIENTE',
    cita_id uuid,
    cumplido_en timestamp(6),
    alertado_en timestamp(6),
    CONSTRAINT pk_plan_controles PRIMARY KEY (id),
    CONSTRAINT uk_plan_controles_numero UNIQUE (plan_id, numero),
    CONSTRAINT fk_plan_controles_plan FOREIGN KEY (plan_id) REFERENCES planes_tratamiento (id),
    CONSTRAINT fk_plan_controles_cita FOREIGN KEY (cita_id) REFERENCES citas (id),
    CONSTRAINT ck_plan_controles_estado CHECK (estado IN ('PENDIENTE', 'CUMPLIDO', 'OMITIDO'))
);

CREATE INDEX idx_plan_controles_fecha ON plan_controles (estado, fecha_programada);

CREATE TABLE plan_mediciones (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    plan_id uuid NOT NULL,
    numero integer NOT NULL,
    parametro varchar(25) NOT NULL,
    valor numeric(8, 2) NOT NULL,
    medido_en timestamp(6) NOT NULL,
    origen varchar(10) NOT NULL,
    registrado_por_id uuid NOT NULL,
    fuera_de_rango boolean NOT NULL,
    CONSTRAINT pk_plan_mediciones PRIMARY KEY (id),
    CONSTRAINT uk_plan_mediciones_numero UNIQUE (plan_id, numero),
    CONSTRAINT fk_plan_mediciones_plan FOREIGN KEY (plan_id) REFERENCES planes_tratamiento (id),
    CONSTRAINT fk_plan_mediciones_usuario FOREIGN KEY (registrado_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_plan_mediciones_origen CHECK (origen IN ('PACIENTE', 'PERSONAL'))
);

CREATE INDEX idx_plan_mediciones_plan ON plan_mediciones (plan_id, numero);
