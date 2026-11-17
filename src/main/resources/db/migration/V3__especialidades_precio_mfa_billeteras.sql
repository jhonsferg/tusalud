ALTER TABLE especialidades ADD COLUMN precio_consulta numeric(10,2);

UPDATE especialidades e
SET precio_consulta = COALESCE((SELECT ROUND(AVG(m.precio_consulta), 2) FROM medicos m WHERE m.especialidad_id = e.id), 100.00);

UPDATE especialidades SET precio_consulta = 150.00 WHERE nombre = 'Cardiología';
UPDATE especialidades SET precio_consulta = 120.00 WHERE nombre = 'Pediatría';
UPDATE especialidades SET precio_consulta = 90.00 WHERE nombre = 'Medicina General';
UPDATE especialidades SET precio_consulta = 130.00 WHERE nombre = 'Dermatología';
UPDATE especialidades SET precio_consulta = 160.00 WHERE nombre = 'Traumatología';
UPDATE especialidades SET precio_consulta = 140.00 WHERE nombre = 'Ginecología';
UPDATE especialidades SET precio_consulta = 110.00 WHERE nombre = 'Oftalmología';

ALTER TABLE especialidades ALTER COLUMN precio_consulta SET NOT NULL;

CREATE TABLE medico_especialidades (
    medico_id uuid NOT NULL,
    especialidad_id uuid NOT NULL,
    PRIMARY KEY (medico_id, especialidad_id),
    CONSTRAINT fk_medico_especialidades_medico FOREIGN KEY (medico_id) REFERENCES medicos (id),
    CONSTRAINT fk_medico_especialidades_especialidad FOREIGN KEY (especialidad_id) REFERENCES especialidades (id)
);

CREATE INDEX idx_medico_especialidades_especialidad ON medico_especialidades (especialidad_id);

INSERT INTO medico_especialidades (medico_id, especialidad_id) SELECT id, especialidad_id FROM medicos;

ALTER TABLE citas ADD COLUMN especialidad_id uuid;

UPDATE citas c SET especialidad_id = m.especialidad_id FROM medicos m WHERE m.id = c.medico_id;

ALTER TABLE citas ALTER COLUMN especialidad_id SET NOT NULL;
ALTER TABLE citas ADD CONSTRAINT fk_citas_especialidad FOREIGN KEY (especialidad_id) REFERENCES especialidades (id);

ALTER TABLE medicos DROP COLUMN especialidad_id;
ALTER TABLE medicos DROP COLUMN precio_consulta;

CREATE TABLE paciente_compartido (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    paciente_id uuid NOT NULL,
    medico_origen_id uuid NOT NULL,
    medico_destino_id uuid NOT NULL,
    nota character varying(500),
    created_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT uk_paciente_compartido UNIQUE (paciente_id, medico_destino_id),
    CONSTRAINT fk_paciente_compartido_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_paciente_compartido_origen FOREIGN KEY (medico_origen_id) REFERENCES medicos (id),
    CONSTRAINT fk_paciente_compartido_destino FOREIGN KEY (medico_destino_id) REFERENCES medicos (id)
);

CREATE INDEX idx_paciente_compartido_destino ON paciente_compartido (medico_destino_id);

CREATE TABLE intenciones_pago (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    codigo character varying(36) NOT NULL,
    cita_id uuid NOT NULL,
    usuario_id uuid NOT NULL,
    billetera character varying(10) NOT NULL,
    monto numeric(10,2) NOT NULL,
    descuento numeric(10,2) NOT NULL,
    total numeric(10,2) NOT NULL,
    codigo_cupon character varying(30),
    estado character varying(15) NOT NULL,
    pago_id uuid,
    expira_en timestamp(6) without time zone NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT uk_intenciones_pago_codigo UNIQUE (codigo),
    CONSTRAINT fk_intenciones_pago_cita FOREIGN KEY (cita_id) REFERENCES citas (id),
    CONSTRAINT fk_intenciones_pago_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_intenciones_pago_pago FOREIGN KEY (pago_id) REFERENCES pagos (id)
);

CREATE INDEX idx_intenciones_pago_cita ON intenciones_pago (cita_id);

CREATE TABLE mfa_usuario (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    usuario_id uuid NOT NULL,
    secreto_cifrado character varying(255) NOT NULL,
    activo boolean NOT NULL,
    intentos_fallidos integer NOT NULL,
    bloqueado_hasta timestamp(6) without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT uk_mfa_usuario_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_mfa_usuario_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE TABLE mfa_codigos_recuperacion (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    usuario_id uuid NOT NULL,
    codigo_hash character varying(64) NOT NULL,
    usado boolean NOT NULL,
    CONSTRAINT fk_mfa_codigos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_mfa_codigos_usuario ON mfa_codigos_recuperacion (usuario_id);

ALTER TABLE preferencias_usuario ADD COLUMN tour_completado boolean NOT NULL DEFAULT false;

ALTER TABLE notificaciones DROP CONSTRAINT notificaciones_tipo_check;
ALTER TABLE notificaciones ADD CONSTRAINT notificaciones_tipo_check CHECK (tipo IN (
    'CITA_CREADA', 'CITA_CONFIRMADA', 'CITA_CANCELADA', 'CITA_ATENDIDA', 'CITA_REPROGRAMADA',
    'RECORDATORIO_CITA', 'INICIO_SESION', 'CONTRASENA_CAMBIADA', 'PAGO_REGISTRADO', 'PAGO_REEMBOLSADO',
    'MFA_ACTIVADO', 'MFA_DESACTIVADO', 'PACIENTE_COMPARTIDO', 'ROLES_ACTUALIZADOS', 'SISTEMA'));
