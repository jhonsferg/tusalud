ALTER TABLE roles DROP CONSTRAINT roles_nombre_check;
ALTER TABLE roles ADD CONSTRAINT roles_nombre_check CHECK (nombre IN (
    'ROLE_ADMIN', 'ROLE_MEDICO', 'ROLE_PACIENTE', 'ROLE_LABORATORIO', 'ROLE_FARMACIA', 'ROLE_ALMACEN',
    'ROLE_RECEPCION', 'ROLE_CAJA', 'ROLE_ENFERMERIA', 'ROLE_GERENCIA', 'ROLE_FINANZAS', 'ROLE_OPERACIONES',
    'ROLE_RRHH', 'ROLE_CALIDAD', 'ROLE_IMAGENES'));

INSERT INTO roles (nombre)
SELECT 'ROLE_IMAGENES'
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.nombre = 'ROLE_IMAGENES');

ALTER TABLE notificaciones DROP CONSTRAINT notificaciones_tipo_check;
ALTER TABLE notificaciones ADD CONSTRAINT notificaciones_tipo_check CHECK (tipo IN (
    'CITA_CREADA', 'CITA_CONFIRMADA', 'CITA_CANCELADA', 'CITA_ATENDIDA', 'CITA_REPROGRAMADA',
    'RECORDATORIO_CITA', 'RECORDATORIO_EVENTO', 'INICIO_SESION', 'CONTRASENA_CAMBIADA', 'PAGO_REGISTRADO',
    'PAGO_REEMBOLSADO', 'MFA_ACTIVADO', 'MFA_DESACTIVADO', 'PACIENTE_COMPARTIDO', 'ROLES_ACTUALIZADOS',
    'SESION_SOSPECHOSA', 'ORDEN_LABORATORIO', 'RESULTADO_LABORATORIO', 'RECETA_DISPENSADA', 'STOCK_BAJO',
    'PRODUCTO_POR_VENCER', 'PACIENTE_EN_ESPERA', 'TURNO_LLAMADO', 'COMPROBANTE_EMITIDO',
    'INGRESO_HOSPITALARIO', 'ALTA_HOSPITALARIA', 'EMERGENCIA_TRIAJE', 'CAJA_DIFERENCIA',
    'SOLICITUD_AUSENCIA', 'AUSENCIA_RESUELTA', 'ORDEN_IMAGEN', 'ESTUDIO_INFORMADO', 'HALLAZGO_CRITICO',
    'SISTEMA'));

ALTER TABLE comprobantes DROP CONSTRAINT comprobantes_origen_check;
ALTER TABLE comprobantes ADD CONSTRAINT comprobantes_origen_check CHECK (origen IN (
    'CITA', 'FARMACIA', 'LABORATORIO', 'IMAGEN', 'SERVICIO', 'OTRO'));

ALTER TABLE empleados DROP CONSTRAINT empleados_area_check;
ALTER TABLE empleados ADD CONSTRAINT empleados_area_check CHECK (area IN (
    'MEDICA', 'ENFERMERIA', 'LABORATORIO', 'IMAGENES', 'FARMACIA', 'ALMACEN', 'RECEPCION', 'CAJA', 'ADMINISTRACION',
    'FINANZAS', 'OPERACIONES', 'RRHH', 'CALIDAD', 'GERENCIA'));

CREATE TABLE estudios_imagen (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    codigo character varying(20) NOT NULL,
    nombre character varying(150) NOT NULL,
    modalidad character varying(5) NOT NULL,
    region character varying(60) NOT NULL,
    preparacion character varying(400),
    precio numeric(10,2) NOT NULL,
    duracion_minutos integer NOT NULL,
    activo boolean NOT NULL,
    CONSTRAINT uk_estudios_imagen_codigo UNIQUE (codigo),
    CONSTRAINT estudios_imagen_modalidad_check CHECK (modalidad IN ('RX', 'ECO', 'TC', 'RM')),
    CONSTRAINT estudios_imagen_precio_check CHECK (precio >= 0),
    CONSTRAINT estudios_imagen_duracion_check CHECK (duracion_minutos BETWEEN 5 AND 240)
);

CREATE TABLE ordenes_imagen (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    codigo character varying(20) NOT NULL,
    fecha timestamp(6) without time zone NOT NULL,
    estado character varying(15) NOT NULL,
    prioridad character varying(10) NOT NULL,
    paciente_id uuid NOT NULL,
    medico_id uuid NOT NULL,
    atencion_id uuid,
    indicacion_clinica character varying(500) NOT NULL,
    total numeric(10,2) NOT NULL,
    programada_para timestamp(6) without time zone,
    sala character varying(40),
    realizada_en timestamp(6) without time zone,
    realizada_por uuid,
    observaciones_toma character varying(300),
    informada_en timestamp(6) without time zone,
    hallazgo_critico boolean NOT NULL DEFAULT false,
    motivo_cancelacion character varying(300),
    cobrado boolean NOT NULL DEFAULT false,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT uk_ordenes_imagen_codigo UNIQUE (codigo),
    CONSTRAINT fk_ordenes_imagen_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_ordenes_imagen_medico FOREIGN KEY (medico_id) REFERENCES medicos (id),
    CONSTRAINT fk_ordenes_imagen_atencion FOREIGN KEY (atencion_id) REFERENCES atenciones (id),
    CONSTRAINT fk_ordenes_imagen_realizada_por FOREIGN KEY (realizada_por) REFERENCES usuarios (id),
    CONSTRAINT ordenes_imagen_estado_check CHECK (estado IN ('SOLICITADA', 'PROGRAMADA', 'REALIZADA', 'INFORMADA', 'CANCELADA')),
    CONSTRAINT ordenes_imagen_prioridad_check CHECK (prioridad IN ('NORMAL', 'URGENTE'))
);

CREATE INDEX idx_ordenes_imagen_estado_fecha ON ordenes_imagen (estado, fecha);
CREATE INDEX idx_ordenes_imagen_paciente ON ordenes_imagen (paciente_id);
CREATE INDEX idx_ordenes_imagen_medico ON ordenes_imagen (medico_id);
CREATE INDEX idx_ordenes_imagen_programada ON ordenes_imagen (programada_para);

CREATE TABLE items_orden_imagen (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    orden_id uuid NOT NULL,
    estudio_id uuid NOT NULL,
    posicion integer NOT NULL,
    CONSTRAINT fk_items_orden_imagen_orden FOREIGN KEY (orden_id) REFERENCES ordenes_imagen (id),
    CONSTRAINT fk_items_orden_imagen_estudio FOREIGN KEY (estudio_id) REFERENCES estudios_imagen (id)
);

CREATE INDEX idx_items_orden_imagen_orden ON items_orden_imagen (orden_id);

CREATE TABLE archivos_imagen (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    orden_id uuid NOT NULL,
    nombre character varying(100) NOT NULL,
    tipo_contenido character varying(30) NOT NULL,
    tamano integer NOT NULL,
    contenido bytea NOT NULL,
    posicion integer NOT NULL,
    subido_por uuid NOT NULL,
    subido_en timestamp(6) without time zone NOT NULL,
    CONSTRAINT fk_archivos_imagen_orden FOREIGN KEY (orden_id) REFERENCES ordenes_imagen (id),
    CONSTRAINT fk_archivos_imagen_subido_por FOREIGN KEY (subido_por) REFERENCES usuarios (id),
    CONSTRAINT archivos_imagen_tipo_check CHECK (tipo_contenido IN ('image/jpeg', 'image/png', 'application/pdf')),
    CONSTRAINT archivos_imagen_tamano_check CHECK (tamano BETWEEN 1 AND 5242880)
);

CREATE INDEX idx_archivos_imagen_orden ON archivos_imagen (orden_id);

CREATE TABLE informes_imagen (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    orden_id uuid NOT NULL,
    version integer NOT NULL,
    hallazgos character varying(4000) NOT NULL,
    conclusion character varying(2000) NOT NULL,
    hallazgo_critico boolean NOT NULL DEFAULT false,
    firma_nombre character varying(150) NOT NULL,
    firma_colegiatura character varying(30) NOT NULL,
    autor_id uuid NOT NULL,
    motivo_correccion character varying(300),
    created_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT uk_informes_imagen_orden_version UNIQUE (orden_id, version),
    CONSTRAINT fk_informes_imagen_orden FOREIGN KEY (orden_id) REFERENCES ordenes_imagen (id),
    CONSTRAINT fk_informes_imagen_autor FOREIGN KEY (autor_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_informes_imagen_orden ON informes_imagen (orden_id);
