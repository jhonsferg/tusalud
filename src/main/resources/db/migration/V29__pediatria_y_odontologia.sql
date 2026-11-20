CREATE TABLE vacunas (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    codigo varchar(20) NOT NULL,
    nombre varchar(120) NOT NULL,
    descripcion varchar(300),
    activo boolean NOT NULL DEFAULT true,
    CONSTRAINT pk_vacunas PRIMARY KEY (id),
    CONSTRAINT uk_vacunas_codigo UNIQUE (codigo)
);

CREATE TABLE esquemas_vacunacion (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    nombre varchar(150) NOT NULL,
    vigente_desde date NOT NULL,
    vigente_hasta date,
    por_validar boolean NOT NULL DEFAULT true,
    fuente varchar(250),
    activo boolean NOT NULL DEFAULT true,
    creado_en timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_esquemas_vacunacion PRIMARY KEY (id)
);

CREATE TABLE esquema_dosis (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    esquema_id uuid NOT NULL,
    vacuna_id uuid NOT NULL,
    numero_dosis integer NOT NULL,
    etiqueta varchar(60) NOT NULL,
    edad_minima_dias integer NOT NULL,
    edad_recomendada_dias integer NOT NULL,
    edad_maxima_dias integer,
    intervalo_minimo_dias integer,
    CONSTRAINT pk_esquema_dosis PRIMARY KEY (id),
    CONSTRAINT fk_esquema_dosis_esquema FOREIGN KEY (esquema_id) REFERENCES esquemas_vacunacion (id),
    CONSTRAINT fk_esquema_dosis_vacuna FOREIGN KEY (vacuna_id) REFERENCES vacunas (id),
    CONSTRAINT uk_esquema_dosis UNIQUE (esquema_id, vacuna_id, numero_dosis),
    CONSTRAINT ck_esquema_dosis_edades CHECK (edad_minima_dias >= 0 AND edad_recomendada_dias >= edad_minima_dias)
);

CREATE TABLE registros_vacunacion (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    paciente_id uuid NOT NULL,
    vacuna_id uuid NOT NULL,
    numero_dosis integer NOT NULL,
    fecha_aplicacion date NOT NULL,
    lote varchar(40) NOT NULL,
    establecimiento varchar(150) NOT NULL,
    responsable_id uuid NOT NULL,
    atencion_id uuid,
    observaciones varchar(300),
    estado varchar(10) NOT NULL DEFAULT 'VIGENTE',
    anulado_en timestamp,
    anulado_por_id uuid,
    motivo_anulacion varchar(300),
    creado_en timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_registros_vacunacion PRIMARY KEY (id),
    CONSTRAINT fk_regvac_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_regvac_vacuna FOREIGN KEY (vacuna_id) REFERENCES vacunas (id),
    CONSTRAINT fk_regvac_responsable FOREIGN KEY (responsable_id) REFERENCES usuarios (id),
    CONSTRAINT fk_regvac_atencion FOREIGN KEY (atencion_id) REFERENCES atenciones (id),
    CONSTRAINT fk_regvac_anulador FOREIGN KEY (anulado_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_regvac_estado CHECK (estado IN ('VIGENTE', 'ANULADO'))
);

CREATE UNIQUE INDEX uk_regvac_dosis_vigente ON registros_vacunacion (paciente_id, vacuna_id, numero_dosis) WHERE estado = 'VIGENTE';
CREATE INDEX idx_regvac_paciente ON registros_vacunacion (paciente_id, fecha_aplicacion);

CREATE TABLE recordatorios_vacunacion (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    paciente_id uuid NOT NULL,
    vacuna_id uuid NOT NULL,
    numero_dosis integer NOT NULL,
    fecha_programada date NOT NULL,
    enviado_en timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_recordatorios_vacunacion PRIMARY KEY (id),
    CONSTRAINT fk_recvac_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_recvac_vacuna FOREIGN KEY (vacuna_id) REFERENCES vacunas (id),
    CONSTRAINT uk_recvac UNIQUE (paciente_id, vacuna_id, numero_dosis, fecha_programada)
);

CREATE TABLE carnes_vacunacion (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    paciente_id uuid NOT NULL,
    codigo_verificacion varchar(19) NOT NULL,
    contenido_hash varchar(64) NOT NULL,
    emitido_en timestamp NOT NULL DEFAULT now(),
    emitido_por_id uuid NOT NULL,
    CONSTRAINT pk_carnes_vacunacion PRIMARY KEY (id),
    CONSTRAINT fk_carne_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_carne_emisor FOREIGN KEY (emitido_por_id) REFERENCES usuarios (id),
    CONSTRAINT uk_carne_codigo UNIQUE (codigo_verificacion)
);

CREATE TABLE mediciones_crecimiento (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    paciente_id uuid NOT NULL,
    fecha date NOT NULL,
    peso_kg numeric(5, 2),
    talla_cm numeric(5, 1),
    perimetro_cefalico_cm numeric(4, 1),
    atencion_id uuid,
    registrado_por_id uuid NOT NULL,
    observaciones varchar(300),
    anulada boolean NOT NULL DEFAULT false,
    motivo_anulacion varchar(300),
    creado_en timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_mediciones_crecimiento PRIMARY KEY (id),
    CONSTRAINT fk_medcrec_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_medcrec_atencion FOREIGN KEY (atencion_id) REFERENCES atenciones (id),
    CONSTRAINT fk_medcrec_registrador FOREIGN KEY (registrado_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_medcrec_alguna CHECK (peso_kg IS NOT NULL OR talla_cm IS NOT NULL OR perimetro_cefalico_cm IS NOT NULL)
);

CREATE INDEX idx_medcrec_paciente ON mediciones_crecimiento (paciente_id, fecha);

CREATE TABLE referencia_crecimiento (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    indicador varchar(12) NOT NULL,
    sexo char(1) NOT NULL,
    edad_meses integer NOT NULL,
    l numeric(8, 5) NOT NULL,
    m numeric(8, 4) NOT NULL,
    s numeric(8, 5) NOT NULL,
    fuente varchar(80) NOT NULL,
    por_validar boolean NOT NULL DEFAULT true,
    CONSTRAINT pk_referencia_crecimiento PRIMARY KEY (id),
    CONSTRAINT uk_referencia_crecimiento UNIQUE (indicador, sexo, edad_meses),
    CONSTRAINT ck_refcrec_indicador CHECK (indicador IN ('PESO_EDAD', 'TALLA_EDAD', 'PC_EDAD', 'IMC_EDAD')),
    CONSTRAINT ck_refcrec_sexo CHECK (sexo IN ('M', 'F'))
);

CREATE TABLE procedimientos_odontologicos (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    codigo varchar(20) NOT NULL,
    nombre varchar(120) NOT NULL,
    categoria varchar(40) NOT NULL,
    precio numeric(10, 2) NOT NULL,
    requiere_pieza boolean NOT NULL DEFAULT true,
    requiere_superficie boolean NOT NULL DEFAULT false,
    estado_resultante varchar(25),
    requiere_consentimiento boolean NOT NULL DEFAULT false,
    activo boolean NOT NULL DEFAULT true,
    CONSTRAINT pk_procedimientos_odontologicos PRIMARY KEY (id),
    CONSTRAINT uk_procodo_codigo UNIQUE (codigo),
    CONSTRAINT ck_procodo_precio CHECK (precio >= 0)
);

CREATE TABLE odontograma_eventos (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    paciente_id uuid NOT NULL,
    pieza smallint NOT NULL,
    superficie varchar(1),
    estado varchar(25) NOT NULL,
    observacion varchar(300),
    registrado_por_id uuid NOT NULL,
    item_id uuid,
    registrado_en timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_odontograma_eventos PRIMARY KEY (id),
    CONSTRAINT fk_odoeve_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_odoeve_registrador FOREIGN KEY (registrado_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_odoeve_superficie CHECK (superficie IS NULL OR superficie IN ('V', 'L', 'M', 'D', 'O'))
);

CREATE INDEX idx_odoeve_paciente_pieza ON odontograma_eventos (paciente_id, pieza, registrado_en);

CREATE TABLE planes_odontologicos (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    paciente_id uuid NOT NULL,
    medico_id uuid NOT NULL,
    estado varchar(12) NOT NULL DEFAULT 'BORRADOR',
    notas varchar(500),
    total numeric(12, 2) NOT NULL DEFAULT 0,
    creado_en timestamp NOT NULL DEFAULT now(),
    presentado_en timestamp,
    resuelto_en timestamp,
    resuelto_por_id uuid,
    motivo_rechazo varchar(300),
    completado_en timestamp,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_planes_odontologicos PRIMARY KEY (id),
    CONSTRAINT fk_planodo_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_planodo_medico FOREIGN KEY (medico_id) REFERENCES medicos (id),
    CONSTRAINT fk_planodo_resolutor FOREIGN KEY (resuelto_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_planodo_estado CHECK (estado IN ('BORRADOR', 'PRESENTADO', 'ACEPTADO', 'RECHAZADO', 'EN_CURSO', 'COMPLETADO', 'CANCELADO'))
);

CREATE INDEX idx_planodo_paciente ON planes_odontologicos (paciente_id, creado_en);

CREATE TABLE sesiones_odontologicas (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    plan_id uuid NOT NULL,
    medico_id uuid NOT NULL,
    cita_id uuid,
    fecha timestamp NOT NULL DEFAULT now(),
    notas varchar(500),
    CONSTRAINT pk_sesiones_odontologicas PRIMARY KEY (id),
    CONSTRAINT fk_sesodo_plan FOREIGN KEY (plan_id) REFERENCES planes_odontologicos (id),
    CONSTRAINT fk_sesodo_medico FOREIGN KEY (medico_id) REFERENCES medicos (id),
    CONSTRAINT fk_sesodo_cita FOREIGN KEY (cita_id) REFERENCES citas (id)
);

CREATE TABLE plan_odontologico_items (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    plan_id uuid NOT NULL,
    procedimiento_id uuid NOT NULL,
    pieza smallint,
    superficie varchar(1),
    precio numeric(10, 2) NOT NULL,
    estado varchar(10) NOT NULL DEFAULT 'PENDIENTE',
    posicion integer NOT NULL DEFAULT 0,
    consentimiento_id uuid,
    sesion_id uuid,
    realizado_en timestamp,
    observacion varchar(300),
    CONSTRAINT pk_plan_odontologico_items PRIMARY KEY (id),
    CONSTRAINT fk_itemodo_plan FOREIGN KEY (plan_id) REFERENCES planes_odontologicos (id),
    CONSTRAINT fk_itemodo_procedimiento FOREIGN KEY (procedimiento_id) REFERENCES procedimientos_odontologicos (id),
    CONSTRAINT fk_itemodo_consentimiento FOREIGN KEY (consentimiento_id) REFERENCES consentimientos (id),
    CONSTRAINT fk_itemodo_sesion FOREIGN KEY (sesion_id) REFERENCES sesiones_odontologicas (id),
    CONSTRAINT ck_itemodo_estado CHECK (estado IN ('PENDIENTE', 'REALIZADO', 'CANCELADO')),
    CONSTRAINT ck_itemodo_superficie CHECK (superficie IS NULL OR superficie IN ('V', 'L', 'M', 'D', 'O'))
);

CREATE INDEX idx_itemodo_plan ON plan_odontologico_items (plan_id, posicion);

CREATE TABLE planes_odontologicos_pagos (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    plan_id uuid NOT NULL,
    monto numeric(12, 2) NOT NULL,
    metodo varchar(30) NOT NULL,
    referencia varchar(60),
    sesion_caja_id uuid NOT NULL,
    registrado_por_id uuid NOT NULL,
    registrado_en timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_planes_odontologicos_pagos PRIMARY KEY (id),
    CONSTRAINT fk_planpago_plan FOREIGN KEY (plan_id) REFERENCES planes_odontologicos (id),
    CONSTRAINT fk_planpago_sesion FOREIGN KEY (sesion_caja_id) REFERENCES sesiones_caja (id),
    CONSTRAINT fk_planpago_registrador FOREIGN KEY (registrado_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_planpago_monto CHECK (monto > 0)
);

INSERT INTO vacunas (codigo, nombre, descripcion) VALUES
    ('BCG', 'BCG', 'Protege contra las formas graves de tuberculosis'),
    ('HVB', 'Hepatitis B', 'Dosis del recién nacido contra la hepatitis B'),
    ('PENTA', 'Pentavalente', 'Difteria, tos ferina, tétanos, hepatitis B y Haemophilus influenzae tipo b'),
    ('IPV', 'Antipolio inactivada', 'Vacuna inyectable contra la poliomielitis'),
    ('APO', 'Antipolio oral', 'Vacuna oral contra la poliomielitis'),
    ('ROTA', 'Rotavirus', 'Protege contra la diarrea grave por rotavirus'),
    ('NEUMO', 'Neumococo conjugada', 'Protege contra la neumonía y la meningitis por neumococo'),
    ('INFLU', 'Influenza pediátrica', 'Protege contra la influenza estacional'),
    ('SPR', 'Sarampión, paperas y rubéola', 'Triple viral'),
    ('VARICELA', 'Varicela', 'Protege contra la varicela'),
    ('AMA', 'Antiamarílica', 'Protege contra la fiebre amarilla'),
    ('DPT', 'DPT refuerzo', 'Refuerzo contra difteria, tos ferina y tétanos'),
    ('VPH', 'Virus del papiloma humano', 'Protege contra el cáncer de cuello uterino y otras lesiones');

INSERT INTO esquemas_vacunacion (nombre, vigente_desde, por_validar, fuente, activo) VALUES
    ('Esquema nacional de vacunación infantil, resumen', DATE '2024-01-01', true,
     'Resumen del esquema nacional del MINSA. Validar con la norma técnica vigente y con un pediatra antes de usarlo en la práctica clínica.', true);

INSERT INTO esquema_dosis (esquema_id, vacuna_id, numero_dosis, etiqueta, edad_minima_dias, edad_recomendada_dias, edad_maxima_dias, intervalo_minimo_dias)
SELECT e.id, v.id, d.numero, d.etiqueta, d.minima, d.recomendada, d.maxima, d.intervalo
FROM esquemas_vacunacion e
JOIN (VALUES
    ('BCG', 1, 'Única, recién nacido', 0, 0, 365, NULL),
    ('HVB', 1, 'Recién nacido', 0, 0, 30, NULL),
    ('PENTA', 1, 'Primera, 2 meses', 42, 60, NULL, NULL),
    ('PENTA', 2, 'Segunda, 4 meses', 70, 120, NULL, 28),
    ('PENTA', 3, 'Tercera, 6 meses', 98, 180, NULL, 28),
    ('IPV', 1, 'Primera, 2 meses', 42, 60, NULL, NULL),
    ('IPV', 2, 'Segunda, 4 meses', 70, 120, NULL, 28),
    ('APO', 1, 'Tercera de polio, 6 meses', 98, 180, NULL, 28),
    ('APO', 2, 'Primer refuerzo, 18 meses', 480, 540, NULL, 180),
    ('APO', 3, 'Segundo refuerzo, 4 años', 1400, 1460, NULL, 180),
    ('ROTA', 1, 'Primera, 2 meses', 42, 60, 105, NULL),
    ('ROTA', 2, 'Segunda, 4 meses', 70, 120, 240, 28),
    ('NEUMO', 1, 'Primera, 2 meses', 42, 60, NULL, NULL),
    ('NEUMO', 2, 'Segunda, 4 meses', 70, 120, NULL, 28),
    ('NEUMO', 3, 'Refuerzo, 12 meses', 335, 365, NULL, 56),
    ('INFLU', 1, 'Primera, 6 meses', 180, 180, NULL, NULL),
    ('INFLU', 2, 'Segunda, 7 meses', 208, 210, NULL, 28),
    ('SPR', 1, 'Primera, 12 meses', 335, 365, NULL, NULL),
    ('SPR', 2, 'Segunda, 18 meses', 480, 540, NULL, 28),
    ('VARICELA', 1, 'Única, 12 meses', 335, 365, NULL, NULL),
    ('AMA', 1, 'Única, 15 meses', 425, 450, NULL, NULL),
    ('DPT', 1, 'Primer refuerzo, 18 meses', 480, 540, NULL, 180),
    ('DPT', 2, 'Segundo refuerzo, 4 años', 1400, 1460, NULL, 180),
    ('VPH', 1, 'Primera, 9 años', 3285, 3285, 4745, NULL),
    ('VPH', 2, 'Segunda, 6 meses después', 3465, 3465, 4925, 150)
) AS d (codigo, numero, etiqueta, minima, recomendada, maxima, intervalo) ON true
JOIN vacunas v ON v.codigo = d.codigo
WHERE e.activo = true;

INSERT INTO referencia_crecimiento (indicador, sexo, edad_meses, l, m, s, fuente, por_validar)
SELECT x.indicador, x.sexo, x.edad,
       CASE x.indicador
           WHEN 'PESO_EDAD' THEN CASE WHEN x.edad = 0 THEN 0.3487 WHEN x.edad = 1 THEN 0.2297 WHEN x.edad = 2 THEN 0.197 WHEN x.edad = 3 THEN 0.1738
               WHEN x.edad = 6 THEN 0.1257 WHEN x.edad = 9 THEN 0.0735 WHEN x.edad = 12 THEN -0.0506 WHEN x.edad = 18 THEN -0.1295 WHEN x.edad = 24 THEN -0.2024
               WHEN x.edad = 36 THEN -0.2415 WHEN x.edad = 48 THEN -0.2688 WHEN x.edad = 60 THEN -0.279 WHEN x.edad = 72 THEN -0.4 WHEN x.edad = 96 THEN -0.6 ELSE -0.8 END
           WHEN 'TALLA_EDAD' THEN 1
           WHEN 'PC_EDAD' THEN 1
           ELSE CASE WHEN x.edad <= 12 THEN -0.35 WHEN x.edad <= 60 THEN -0.9 WHEN x.edad <= 120 THEN -1.3 ELSE -1.7 END
       END,
       x.m,
       CASE x.indicador
           WHEN 'PESO_EDAD' THEN CASE WHEN x.edad = 0 THEN 0.146 WHEN x.edad = 1 THEN 0.134 WHEN x.edad = 2 THEN 0.124 WHEN x.edad = 3 THEN 0.117 WHEN x.edad = 6 THEN 0.11
               WHEN x.edad = 9 THEN 0.11 WHEN x.edad = 12 THEN 0.113 WHEN x.edad = 18 THEN 0.117 WHEN x.edad = 24 THEN 0.12 WHEN x.edad = 36 THEN 0.124
               WHEN x.edad = 48 THEN 0.131 WHEN x.edad = 60 THEN 0.139 WHEN x.edad = 72 THEN 0.147 WHEN x.edad = 96 THEN 0.16 ELSE 0.172 END
           WHEN 'TALLA_EDAD' THEN CASE WHEN x.edad = 0 THEN 0.0379 WHEN x.edad <= 3 THEN 0.0355 WHEN x.edad <= 12 THEN 0.0325 WHEN x.edad <= 24 THEN 0.0315 WHEN x.edad <= 60 THEN 0.0305
               WHEN x.edad <= 120 THEN 0.039 ELSE 0.042 END
           WHEN 'PC_EDAD' THEN CASE WHEN x.edad = 0 THEN 0.0356 WHEN x.edad <= 3 THEN 0.0325 WHEN x.edad <= 12 THEN 0.0295 ELSE 0.028 END
           ELSE CASE WHEN x.edad = 0 THEN 0.0865 WHEN x.edad <= 12 THEN 0.08 WHEN x.edad <= 60 THEN 0.082 WHEN x.edad <= 120 THEN 0.1 ELSE 0.115 END
       END,
       'Resumen de los patrones de crecimiento de la OMS, valores aproximados', true
FROM (VALUES
    ('PESO_EDAD', 'M', 0, 3.35), ('PESO_EDAD', 'M', 1, 4.47), ('PESO_EDAD', 'M', 2, 5.57), ('PESO_EDAD', 'M', 3, 6.38), ('PESO_EDAD', 'M', 6, 7.93),
    ('PESO_EDAD', 'M', 9, 8.90), ('PESO_EDAD', 'M', 12, 9.65), ('PESO_EDAD', 'M', 18, 10.94), ('PESO_EDAD', 'M', 24, 12.15), ('PESO_EDAD', 'M', 36, 14.34),
    ('PESO_EDAD', 'M', 48, 16.35), ('PESO_EDAD', 'M', 60, 18.34), ('PESO_EDAD', 'M', 72, 20.5), ('PESO_EDAD', 'M', 96, 25.4), ('PESO_EDAD', 'M', 120, 31.2),
    ('PESO_EDAD', 'F', 0, 3.23), ('PESO_EDAD', 'F', 1, 4.19), ('PESO_EDAD', 'F', 2, 5.13), ('PESO_EDAD', 'F', 3, 5.85), ('PESO_EDAD', 'F', 6, 7.30),
    ('PESO_EDAD', 'F', 9, 8.20), ('PESO_EDAD', 'F', 12, 8.95), ('PESO_EDAD', 'F', 18, 10.20), ('PESO_EDAD', 'F', 24, 11.48), ('PESO_EDAD', 'F', 36, 13.85),
    ('PESO_EDAD', 'F', 48, 15.97), ('PESO_EDAD', 'F', 60, 18.22), ('PESO_EDAD', 'F', 72, 20.2), ('PESO_EDAD', 'F', 96, 25.0), ('PESO_EDAD', 'F', 120, 31.9),
    ('TALLA_EDAD', 'M', 0, 49.9), ('TALLA_EDAD', 'M', 1, 54.7), ('TALLA_EDAD', 'M', 2, 58.4), ('TALLA_EDAD', 'M', 3, 61.4), ('TALLA_EDAD', 'M', 6, 67.6),
    ('TALLA_EDAD', 'M', 9, 72.0), ('TALLA_EDAD', 'M', 12, 75.7), ('TALLA_EDAD', 'M', 18, 82.3), ('TALLA_EDAD', 'M', 24, 87.8), ('TALLA_EDAD', 'M', 36, 96.1),
    ('TALLA_EDAD', 'M', 48, 103.3), ('TALLA_EDAD', 'M', 60, 110.0), ('TALLA_EDAD', 'M', 72, 116.0), ('TALLA_EDAD', 'M', 96, 127.3), ('TALLA_EDAD', 'M', 120, 138.4),
    ('TALLA_EDAD', 'M', 144, 149.1), ('TALLA_EDAD', 'M', 168, 163.2), ('TALLA_EDAD', 'M', 192, 172.9), ('TALLA_EDAD', 'M', 216, 176.2), ('TALLA_EDAD', 'M', 228, 176.5),
    ('TALLA_EDAD', 'F', 0, 49.1), ('TALLA_EDAD', 'F', 1, 53.7), ('TALLA_EDAD', 'F', 2, 57.1), ('TALLA_EDAD', 'F', 3, 59.8), ('TALLA_EDAD', 'F', 6, 65.7),
    ('TALLA_EDAD', 'F', 9, 70.1), ('TALLA_EDAD', 'F', 12, 74.0), ('TALLA_EDAD', 'F', 18, 80.7), ('TALLA_EDAD', 'F', 24, 86.4), ('TALLA_EDAD', 'F', 36, 95.1),
    ('TALLA_EDAD', 'F', 48, 102.7), ('TALLA_EDAD', 'F', 60, 109.4), ('TALLA_EDAD', 'F', 72, 115.1), ('TALLA_EDAD', 'F', 96, 127.5), ('TALLA_EDAD', 'F', 120, 138.6),
    ('TALLA_EDAD', 'F', 144, 151.2), ('TALLA_EDAD', 'F', 168, 159.8), ('TALLA_EDAD', 'F', 192, 162.5), ('TALLA_EDAD', 'F', 216, 163.1), ('TALLA_EDAD', 'F', 228, 163.2),
    ('PC_EDAD', 'M', 0, 34.5), ('PC_EDAD', 'M', 1, 37.3), ('PC_EDAD', 'M', 2, 39.1), ('PC_EDAD', 'M', 3, 40.5), ('PC_EDAD', 'M', 6, 43.3),
    ('PC_EDAD', 'M', 9, 45.0), ('PC_EDAD', 'M', 12, 46.1), ('PC_EDAD', 'M', 18, 47.4), ('PC_EDAD', 'M', 24, 48.3), ('PC_EDAD', 'M', 36, 49.5),
    ('PC_EDAD', 'M', 48, 50.2), ('PC_EDAD', 'M', 60, 50.7),
    ('PC_EDAD', 'F', 0, 33.9), ('PC_EDAD', 'F', 1, 36.5), ('PC_EDAD', 'F', 2, 38.3), ('PC_EDAD', 'F', 3, 39.5), ('PC_EDAD', 'F', 6, 42.2),
    ('PC_EDAD', 'F', 9, 43.8), ('PC_EDAD', 'F', 12, 44.9), ('PC_EDAD', 'F', 18, 46.2), ('PC_EDAD', 'F', 24, 47.2), ('PC_EDAD', 'F', 36, 48.5),
    ('PC_EDAD', 'F', 48, 49.3), ('PC_EDAD', 'F', 60, 49.8),
    ('IMC_EDAD', 'M', 0, 13.4), ('IMC_EDAD', 'M', 1, 14.9), ('IMC_EDAD', 'M', 2, 16.3), ('IMC_EDAD', 'M', 3, 16.9), ('IMC_EDAD', 'M', 6, 17.3),
    ('IMC_EDAD', 'M', 9, 17.3), ('IMC_EDAD', 'M', 12, 16.9), ('IMC_EDAD', 'M', 18, 16.4), ('IMC_EDAD', 'M', 24, 16.0), ('IMC_EDAD', 'M', 36, 15.6),
    ('IMC_EDAD', 'M', 48, 15.3), ('IMC_EDAD', 'M', 60, 15.3), ('IMC_EDAD', 'M', 72, 15.3), ('IMC_EDAD', 'M', 96, 15.8), ('IMC_EDAD', 'M', 120, 16.4),
    ('IMC_EDAD', 'M', 144, 17.9), ('IMC_EDAD', 'M', 168, 19.2), ('IMC_EDAD', 'M', 192, 20.5), ('IMC_EDAD', 'M', 216, 21.5), ('IMC_EDAD', 'M', 228, 21.9),
    ('IMC_EDAD', 'F', 0, 13.3), ('IMC_EDAD', 'F', 1, 14.6), ('IMC_EDAD', 'F', 2, 15.8), ('IMC_EDAD', 'F', 3, 16.4), ('IMC_EDAD', 'F', 6, 16.8),
    ('IMC_EDAD', 'F', 9, 16.8), ('IMC_EDAD', 'F', 12, 16.4), ('IMC_EDAD', 'F', 18, 16.0), ('IMC_EDAD', 'F', 24, 15.7), ('IMC_EDAD', 'F', 36, 15.4),
    ('IMC_EDAD', 'F', 48, 15.2), ('IMC_EDAD', 'F', 60, 15.2), ('IMC_EDAD', 'F', 72, 15.2), ('IMC_EDAD', 'F', 96, 15.7), ('IMC_EDAD', 'F', 120, 16.5),
    ('IMC_EDAD', 'F', 144, 18.0), ('IMC_EDAD', 'F', 168, 19.2), ('IMC_EDAD', 'F', 192, 20.1), ('IMC_EDAD', 'F', 216, 20.7), ('IMC_EDAD', 'F', 228, 20.9)
) AS x (indicador, sexo, edad, m);

INSERT INTO procedimientos_odontologicos (codigo, nombre, categoria, precio, requiere_pieza, requiere_superficie, estado_resultante, requiere_consentimiento) VALUES
    ('CONS-ODO', 'Consulta y diagnóstico odontológico', 'Diagnóstico', 60.00, false, false, NULL, false),
    ('PROFILAXIS', 'Profilaxis y limpieza dental', 'Preventiva', 90.00, false, false, NULL, false),
    ('FLUOR', 'Aplicación de flúor', 'Preventiva', 50.00, false, false, NULL, false),
    ('SELLANTE', 'Sellante de fosas y fisuras', 'Preventiva', 70.00, true, true, 'SELLANTE', false),
    ('OBTURACION', 'Obturación con resina', 'Restauradora', 120.00, true, true, 'OBTURADO', false),
    ('CORONA', 'Corona dental', 'Rehabilitadora', 650.00, true, false, 'CORONA', true),
    ('ENDODONCIA', 'Tratamiento de conducto', 'Endodoncia', 450.00, true, false, 'ENDODONCIA', true),
    ('EXODONCIA', 'Extracción dental simple', 'Cirugía', 110.00, true, false, 'AUSENTE', true),
    ('EXODONCIA-Q', 'Extracción dental quirúrgica', 'Cirugía', 280.00, true, false, 'AUSENTE', true),
    ('IMPLANTE', 'Implante dental', 'Implantología', 2200.00, true, false, 'IMPLANTE', true),
    ('BLANQUEAMIENTO', 'Blanqueamiento dental', 'Estética', 380.00, false, false, NULL, false),
    ('ORTODONCIA-C', 'Control de ortodoncia', 'Ortodoncia', 130.00, false, false, NULL, false);

DO $$
DECLARE
    definicion text;
    nueva text;
BEGIN
    SELECT pg_get_constraintdef(oid) INTO definicion
    FROM pg_constraint
    WHERE conname = 'notificaciones_tipo_check' AND conrelid = 'notificaciones'::regclass;
    nueva := replace(
        definicion,
        '''SISTEMA''::character varying',
        '''VACUNA_PROXIMA''::character varying, ''PLAN_ODONTOLOGICO_PRESENTADO''::character varying, ''SISTEMA''::character varying');
    IF position('VACUNA_PROXIMA' IN nueva) = 0 THEN
        RAISE EXCEPTION 'No se pudo ampliar la restricción de tipos de notificación';
    END IF;
    EXECUTE 'ALTER TABLE notificaciones DROP CONSTRAINT notificaciones_tipo_check';
    EXECUTE 'ALTER TABLE notificaciones ADD CONSTRAINT notificaciones_tipo_check ' || nueva;
END $$;
