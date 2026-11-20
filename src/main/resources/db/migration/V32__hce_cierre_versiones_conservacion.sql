CREATE TABLE atencion_cierres (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    atencion_id uuid NOT NULL,
    cerrado_por_id uuid NOT NULL,
    cerrado_en timestamp(6) NOT NULL,
    contenido_canonico text NOT NULL,
    hash varchar(64) NOT NULL,
    firma text NOT NULL,
    firma_algoritmo varchar(20) NOT NULL,
    certificado_huella varchar(64) NOT NULL,
    certificado_sujeto varchar(300) NOT NULL,
    certificado_der text NOT NULL,
    certificado_de_prueba boolean NOT NULL,
    CONSTRAINT pk_atencion_cierres PRIMARY KEY (id),
    CONSTRAINT uk_atencion_cierres_atencion UNIQUE (atencion_id),
    CONSTRAINT fk_atencion_cierres_atencion FOREIGN KEY (atencion_id) REFERENCES atenciones (id),
    CONSTRAINT fk_atencion_cierres_usuario FOREIGN KEY (cerrado_por_id) REFERENCES usuarios (id)
);

CREATE TABLE atencion_versiones (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    atencion_id uuid NOT NULL,
    numero integer NOT NULL,
    tipo varchar(10) NOT NULL,
    campo varchar(20),
    valor_anterior text,
    valor_nuevo text NOT NULL,
    motivo varchar(300) NOT NULL,
    autor_id uuid NOT NULL,
    creado_en timestamp(6) NOT NULL,
    CONSTRAINT pk_atencion_versiones PRIMARY KEY (id),
    CONSTRAINT uk_atencion_versiones_numero UNIQUE (atencion_id, numero),
    CONSTRAINT fk_atencion_versiones_atencion FOREIGN KEY (atencion_id) REFERENCES atenciones (id),
    CONSTRAINT fk_atencion_versiones_autor FOREIGN KEY (autor_id) REFERENCES usuarios (id),
    CONSTRAINT ck_atencion_versiones_tipo CHECK (tipo IN ('ADENDA', 'CORRECCION')),
    CONSTRAINT ck_atencion_versiones_campo CHECK (
        (tipo = 'ADENDA' AND campo IS NULL)
        OR (tipo = 'CORRECCION' AND campo IN ('DIAGNOSTICO', 'TRATAMIENTO', 'INDICACIONES', 'OBSERVACIONES'))
    )
);

CREATE INDEX idx_atencion_versiones_atencion ON atencion_versiones (atencion_id, numero);

CREATE TABLE politicas_conservacion (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tipo_registro varchar(40) NOT NULL,
    descripcion varchar(200) NOT NULL,
    anios_minimos integer NOT NULL,
    referencia varchar(300),
    por_confirmar boolean NOT NULL DEFAULT true,
    depuracion_bloqueada boolean NOT NULL DEFAULT true,
    actualizado_en timestamp(6) NOT NULL DEFAULT now(),
    actualizado_por_id uuid,
    CONSTRAINT pk_politicas_conservacion PRIMARY KEY (id),
    CONSTRAINT uk_politicas_conservacion_tipo UNIQUE (tipo_registro),
    CONSTRAINT fk_politicas_conservacion_usuario FOREIGN KEY (actualizado_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_politicas_conservacion_anios CHECK (anios_minimos BETWEEN 1 AND 100)
);

INSERT INTO politicas_conservacion (tipo_registro, descripcion, anios_minimos, referencia) VALUES
    ('HISTORIA_CLINICA', 'Historia clínica del paciente', 20, 'Plazo referencial por confirmar con el asesor legal'),
    ('ATENCION', 'Atención ambulatoria firmada', 20, 'Plazo referencial por confirmar con el asesor legal'),
    ('RECETA', 'Receta electrónica emitida', 10, 'Plazo referencial por confirmar con el asesor legal'),
    ('CONSENTIMIENTO', 'Consentimiento informado firmado', 20, 'Plazo referencial por confirmar con el asesor legal'),
    ('DOCUMENTO_MEDICO', 'Certificados, informes y constancias', 10, 'Plazo referencial por confirmar con el asesor legal'),
    ('LABORATORIO', 'Resultados de laboratorio', 10, 'Plazo referencial por confirmar con el asesor legal'),
    ('IMAGENES', 'Estudios e informes de imágenes', 10, 'Plazo referencial por confirmar con el asesor legal'),
    ('HOSPITALIZACION', 'Ingresos, evoluciones y epicrisis', 20, 'Plazo referencial por confirmar con el asesor legal'),
    ('CIRUGIA', 'Cirugías, notas operatorias y registros anestésicos', 20, 'Plazo referencial por confirmar con el asesor legal');

CREATE TABLE paciente_fusiones (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    destino_paciente_id uuid NOT NULL,
    origen_historial varchar(20) NOT NULL,
    origen_dni varchar(8) NOT NULL,
    origen_nombre varchar(200) NOT NULL,
    motivo varchar(300) NOT NULL,
    resumen text NOT NULL,
    realizado_por_id uuid NOT NULL,
    realizado_en timestamp(6) NOT NULL,
    CONSTRAINT pk_paciente_fusiones PRIMARY KEY (id),
    CONSTRAINT fk_paciente_fusiones_destino FOREIGN KEY (destino_paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_paciente_fusiones_usuario FOREIGN KEY (realizado_por_id) REFERENCES usuarios (id)
);

CREATE TABLE hce_impresiones (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    paciente_id uuid NOT NULL,
    codigo_verificacion varchar(19) NOT NULL,
    contenido_hash varchar(64) NOT NULL,
    atenciones integer NOT NULL,
    emitido_en timestamp(6) NOT NULL,
    emitido_por_id uuid NOT NULL,
    CONSTRAINT pk_hce_impresiones PRIMARY KEY (id),
    CONSTRAINT uk_hce_impresiones_codigo UNIQUE (codigo_verificacion),
    CONSTRAINT fk_hce_impresiones_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_hce_impresiones_usuario FOREIGN KEY (emitido_por_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_hce_impresiones_paciente ON hce_impresiones (paciente_id, emitido_en);
