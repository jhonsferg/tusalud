CREATE TABLE empleados (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    usuario_id uuid NOT NULL,
    cargo character varying(100) NOT NULL,
    area character varying(20) NOT NULL,
    tipo_contrato character varying(20) NOT NULL,
    fecha_ingreso date NOT NULL,
    fecha_fin_contrato date,
    estado character varying(20) NOT NULL,
    sueldo numeric(12, 2) NOT NULL,
    contacto_emergencia_nombre character varying(150),
    contacto_emergencia_telefono character varying(20),
    dias_vacaciones_anuales integer NOT NULL DEFAULT 30,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT uk_empleados_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_empleados_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT empleados_area_check CHECK (area IN (
        'MEDICA', 'ENFERMERIA', 'LABORATORIO', 'FARMACIA', 'ALMACEN', 'RECEPCION', 'CAJA', 'ADMINISTRACION',
        'FINANZAS', 'OPERACIONES', 'RRHH', 'CALIDAD', 'GERENCIA')),
    CONSTRAINT empleados_contrato_check CHECK (tipo_contrato IN (
        'INDEFINIDO', 'PLAZO_FIJO', 'PART_TIME', 'PRACTICAS', 'LOCACION')),
    CONSTRAINT empleados_estado_check CHECK (estado IN ('ACTIVO', 'LICENCIA', 'CESADO')),
    CONSTRAINT empleados_sueldo_check CHECK (sueldo >= 0),
    CONSTRAINT empleados_vacaciones_check CHECK (dias_vacaciones_anuales BETWEEN 0 AND 60)
);

CREATE INDEX idx_empleados_area_estado ON empleados (area, estado);
CREATE INDEX idx_empleados_fin_contrato ON empleados (fecha_fin_contrato);

CREATE TABLE plantillas_turno (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    nombre character varying(40) NOT NULL,
    hora_inicio time without time zone NOT NULL,
    hora_fin time without time zone NOT NULL,
    activa boolean NOT NULL DEFAULT true,
    CONSTRAINT uk_plantillas_turno_nombre UNIQUE (nombre)
);

INSERT INTO plantillas_turno (nombre, hora_inicio, hora_fin) VALUES
    ('Mañana', '07:00', '15:00'),
    ('Tarde', '15:00', '23:00'),
    ('Noche', '23:00', '07:00');

CREATE TABLE turnos_personal (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    empleado_id uuid NOT NULL,
    plantilla_id uuid NOT NULL,
    fecha date NOT NULL,
    notas character varying(200),
    creado_por uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT uk_turnos_personal UNIQUE (empleado_id, fecha, plantilla_id),
    CONSTRAINT fk_turnos_personal_empleado FOREIGN KEY (empleado_id) REFERENCES empleados (id),
    CONSTRAINT fk_turnos_personal_plantilla FOREIGN KEY (plantilla_id) REFERENCES plantillas_turno (id),
    CONSTRAINT fk_turnos_personal_creador FOREIGN KEY (creado_por) REFERENCES usuarios (id)
);

CREATE INDEX idx_turnos_personal_fecha ON turnos_personal (fecha);
CREATE INDEX idx_turnos_personal_empleado_fecha ON turnos_personal (empleado_id, fecha);

CREATE TABLE ausencias (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    empleado_id uuid NOT NULL,
    tipo character varying(20) NOT NULL,
    fecha_inicio date NOT NULL,
    fecha_fin date NOT NULL,
    dias integer NOT NULL,
    motivo character varying(300),
    estado character varying(20) NOT NULL,
    resuelto_por uuid,
    resuelto_en timestamp(6) without time zone,
    motivo_resolucion character varying(300),
    created_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT fk_ausencias_empleado FOREIGN KEY (empleado_id) REFERENCES empleados (id),
    CONSTRAINT fk_ausencias_resuelto_por FOREIGN KEY (resuelto_por) REFERENCES usuarios (id),
    CONSTRAINT ausencias_tipo_check CHECK (tipo IN ('VACACIONES', 'PERMISO', 'DESCANSO_MEDICO', 'LICENCIA', 'OTRO')),
    CONSTRAINT ausencias_estado_check CHECK (estado IN ('SOLICITADA', 'APROBADA', 'RECHAZADA', 'CANCELADA')),
    CONSTRAINT ausencias_fechas_check CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT ausencias_dias_check CHECK (dias >= 1)
);

CREATE INDEX idx_ausencias_empleado_fechas ON ausencias (empleado_id, fecha_inicio, fecha_fin);
CREATE INDEX idx_ausencias_estado_inicio ON ausencias (estado, fecha_inicio);

CREATE TABLE auditoria_rrhh (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    usuario_id uuid NOT NULL,
    empleado_id uuid,
    accion character varying(40) NOT NULL,
    detalle character varying(300),
    fecha timestamp(6) without time zone NOT NULL,
    CONSTRAINT fk_auditoria_rrhh_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_auditoria_rrhh_empleado FOREIGN KEY (empleado_id) REFERENCES empleados (id)
);

CREATE INDEX idx_auditoria_rrhh_empleado_fecha ON auditoria_rrhh (empleado_id, fecha);
