CREATE TABLE archivos (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    bucket character varying(63) NOT NULL,
    clave character varying(120) NOT NULL,
    nombre_original character varying(150) NOT NULL,
    tipo_contenido character varying(100) NOT NULL,
    tamano bigint NOT NULL,
    sha256 character varying(64) NOT NULL,
    proposito character varying(30) NOT NULL,
    propietario_id uuid,
    subido_por uuid NOT NULL,
    referencia_tipo character varying(40),
    referencia_id uuid,
    created_at timestamp(6) without time zone NOT NULL,
    eliminado boolean NOT NULL DEFAULT false,
    eliminado_en timestamp(6) without time zone,
    CONSTRAINT uk_archivos_bucket_clave UNIQUE (bucket, clave),
    CONSTRAINT fk_archivos_propietario FOREIGN KEY (propietario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_archivos_subido_por FOREIGN KEY (subido_por) REFERENCES usuarios (id),
    CONSTRAINT archivos_proposito_check CHECK (proposito IN ('AVATAR', 'IMAGEN_DIAGNOSTICA', 'DOCUMENTO_PACIENTE', 'REPORTE', 'PLANTILLA')),
    CONSTRAINT archivos_tamano_check CHECK (tamano BETWEEN 1 AND 26214400)
);

CREATE INDEX idx_archivos_referencia ON archivos (referencia_tipo, referencia_id);
CREATE INDEX idx_archivos_propietario ON archivos (propietario_id, proposito);

DELETE FROM archivos_imagen;

ALTER TABLE archivos_imagen DROP CONSTRAINT archivos_imagen_tipo_check;
ALTER TABLE archivos_imagen DROP CONSTRAINT archivos_imagen_tamano_check;
ALTER TABLE archivos_imagen DROP CONSTRAINT fk_archivos_imagen_subido_por;
ALTER TABLE archivos_imagen
    DROP COLUMN nombre,
    DROP COLUMN tipo_contenido,
    DROP COLUMN tamano,
    DROP COLUMN contenido,
    DROP COLUMN subido_por,
    DROP COLUMN subido_en,
    ADD COLUMN archivo_id uuid NOT NULL;
ALTER TABLE archivos_imagen
    ADD CONSTRAINT fk_archivos_imagen_archivo FOREIGN KEY (archivo_id) REFERENCES archivos (id),
    ADD CONSTRAINT uk_archivos_imagen_archivo UNIQUE (archivo_id);

ALTER TABLE usuarios ADD COLUMN avatar_archivo_id uuid;
ALTER TABLE usuarios ADD CONSTRAINT fk_usuarios_avatar_archivo FOREIGN KEY (avatar_archivo_id) REFERENCES archivos (id);
ALTER TABLE usuarios DROP COLUMN avatar;
