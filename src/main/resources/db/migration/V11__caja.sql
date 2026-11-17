CREATE TABLE sesiones_caja (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    cajero_id uuid NOT NULL,
    apertura timestamp(6) without time zone NOT NULL,
    monto_apertura numeric(10, 2) NOT NULL,
    cierre timestamp(6) without time zone,
    monto_contado numeric(10, 2),
    monto_esperado numeric(10, 2),
    diferencia numeric(10, 2),
    estado character varying(10) NOT NULL,
    observaciones character varying(300),
    CONSTRAINT fk_sesiones_caja_cajero FOREIGN KEY (cajero_id) REFERENCES usuarios (id),
    CONSTRAINT sesiones_caja_estado_check CHECK (estado IN ('ABIERTA', 'CERRADA')),
    CONSTRAINT sesiones_caja_monto_apertura_check CHECK (monto_apertura >= 0)
);

CREATE INDEX idx_sesiones_caja_cajero_estado ON sesiones_caja (cajero_id, estado);
CREATE INDEX idx_sesiones_caja_apertura ON sesiones_caja (apertura);

CREATE TABLE comprobantes (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    tipo character varying(10) NOT NULL,
    serie character varying(4) NOT NULL,
    numero integer NOT NULL,
    codigo character varying(12) NOT NULL,
    fecha timestamp(6) without time zone NOT NULL,
    cliente_tipo_doc character varying(10) NOT NULL,
    cliente_doc character varying(11),
    cliente_nombre character varying(150) NOT NULL,
    subtotal numeric(10, 2) NOT NULL,
    igv numeric(10, 2) NOT NULL,
    total numeric(10, 2) NOT NULL,
    metodo_pago character varying(15) NOT NULL,
    estado character varying(10) NOT NULL,
    motivo_anulacion character varying(300),
    anulado_en timestamp(6) without time zone,
    origen character varying(15) NOT NULL,
    referencia_id uuid,
    paciente_id uuid,
    caja_id uuid NOT NULL,
    CONSTRAINT uk_comprobantes_serie_numero UNIQUE (serie, numero),
    CONSTRAINT fk_comprobantes_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id),
    CONSTRAINT fk_comprobantes_caja FOREIGN KEY (caja_id) REFERENCES sesiones_caja (id),
    CONSTRAINT comprobantes_tipo_check CHECK (tipo IN ('BOLETA', 'FACTURA')),
    CONSTRAINT comprobantes_cliente_tipo_doc_check CHECK (cliente_tipo_doc IN ('DNI', 'RUC', 'SIN_DOC')),
    CONSTRAINT comprobantes_metodo_pago_check CHECK (metodo_pago IN ('EFECTIVO', 'TARJETA', 'YAPE', 'PLIN', 'TRANSFERENCIA')),
    CONSTRAINT comprobantes_estado_check CHECK (estado IN ('EMITIDO', 'ANULADO')),
    CONSTRAINT comprobantes_origen_check CHECK (origen IN ('CITA', 'FARMACIA', 'LABORATORIO', 'SERVICIO', 'OTRO'))
);

CREATE INDEX idx_comprobantes_fecha ON comprobantes (fecha);
CREATE INDEX idx_comprobantes_caja ON comprobantes (caja_id);
CREATE INDEX idx_comprobantes_origen ON comprobantes (origen, referencia_id);

CREATE TABLE comprobante_items (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    comprobante_id uuid NOT NULL,
    descripcion character varying(200) NOT NULL,
    cantidad integer NOT NULL,
    precio_unitario numeric(10, 2) NOT NULL,
    subtotal numeric(10, 2) NOT NULL,
    posicion integer NOT NULL DEFAULT 0,
    CONSTRAINT fk_comprobante_items_comprobante FOREIGN KEY (comprobante_id) REFERENCES comprobantes (id)
);

CREATE INDEX idx_comprobante_items_comprobante ON comprobante_items (comprobante_id);

ALTER TABLE ordenes_laboratorio ADD COLUMN cobrado boolean NOT NULL DEFAULT false;
