CREATE TABLE auditoria_clinica (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    usuario_id uuid NOT NULL,
    paciente_id uuid NOT NULL,
    accion character varying(40) NOT NULL,
    detalle character varying(300),
    fecha timestamp(6) without time zone NOT NULL,
    CONSTRAINT fk_auditoria_clinica_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_auditoria_clinica_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id)
);

CREATE INDEX idx_auditoria_clinica_paciente_fecha ON auditoria_clinica (paciente_id, fecha);
CREATE INDEX idx_auditoria_clinica_usuario_fecha ON auditoria_clinica (usuario_id, fecha);

ALTER TABLE pacientes ADD COLUMN datos_medicos_modificado_por uuid;
ALTER TABLE pacientes ADD COLUMN datos_medicos_modificado_en timestamp(6) without time zone;
ALTER TABLE pacientes
    ADD CONSTRAINT fk_pacientes_datos_medicos_modificado_por FOREIGN KEY (datos_medicos_modificado_por) REFERENCES usuarios (id);
