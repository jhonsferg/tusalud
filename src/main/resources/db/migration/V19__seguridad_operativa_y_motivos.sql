CREATE TABLE eventos_seguridad (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    tipo character varying(40) NOT NULL,
    severidad character varying(10) NOT NULL,
    usuario_id uuid,
    actor_id uuid,
    ip_enmascarada character varying(45),
    dispositivo character varying(120),
    detalle character varying(300),
    creado_en timestamp(6) without time zone NOT NULL,
    CONSTRAINT ck_eventos_seguridad_severidad CHECK (severidad IN ('INFO', 'AVISO', 'ALTA')),
    CONSTRAINT fk_eventos_seguridad_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE SET NULL,
    CONSTRAINT fk_eventos_seguridad_actor FOREIGN KEY (actor_id) REFERENCES usuarios (id) ON DELETE SET NULL
);

CREATE INDEX idx_eventos_seguridad_creado ON eventos_seguridad (creado_en DESC);
CREATE INDEX idx_eventos_seguridad_tipo_creado ON eventos_seguridad (tipo, creado_en DESC);
CREATE INDEX idx_eventos_seguridad_usuario_creado ON eventos_seguridad (usuario_id, creado_en DESC);

ALTER TABLE citas ADD COLUMN motivo_cancelacion character varying(300);
ALTER TABLE citas ADD COLUMN cancelada_por_id uuid;
ALTER TABLE citas
    ADD CONSTRAINT fk_citas_cancelada_por FOREIGN KEY (cancelada_por_id) REFERENCES usuarios (id);

ALTER TABLE dispensaciones ADD COLUMN motivo_anulacion character varying(300);

ALTER TABLE ingresos_hospitalarios ADD COLUMN motivo_cancelacion character varying(300);
