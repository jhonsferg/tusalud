ALTER TABLE proveedores ADD COLUMN condicion_pago_dias integer NOT NULL DEFAULT 0;
ALTER TABLE proveedores ADD COLUMN plazo_entrega_dias integer;
ALTER TABLE proveedores ADD COLUMN banco varchar(80);
ALTER TABLE proveedores ADD COLUMN cuenta_bancaria varchar(40);
ALTER TABLE proveedores ADD COLUMN notas varchar(300);

CREATE TABLE secuencias_compras (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tipo varchar(10) NOT NULL,
    anio integer NOT NULL,
    ultimo integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_secuencias_compras PRIMARY KEY (id),
    CONSTRAINT uk_secuencias_compras UNIQUE (tipo, anio)
);

CREATE TABLE requisiciones (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    codigo varchar(20) NOT NULL,
    estado varchar(15) NOT NULL,
    prioridad varchar(10) NOT NULL,
    area varchar(15) NOT NULL,
    solicitante_id uuid NOT NULL,
    motivo varchar(300),
    sugerida boolean NOT NULL DEFAULT false,
    creada_en timestamp NOT NULL DEFAULT now(),
    enviada_en timestamp,
    resuelta_en timestamp,
    resuelta_por_id uuid,
    motivo_resolucion varchar(300),
    CONSTRAINT pk_requisiciones PRIMARY KEY (id),
    CONSTRAINT uk_requisiciones_codigo UNIQUE (codigo),
    CONSTRAINT fk_requisiciones_solicitante FOREIGN KEY (solicitante_id) REFERENCES usuarios (id),
    CONSTRAINT fk_requisiciones_resolutor FOREIGN KEY (resuelta_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_requisiciones_estado CHECK (estado IN ('BORRADOR', 'ENVIADA', 'EN_COTIZACION', 'ATENDIDA', 'RECHAZADA', 'ANULADA')),
    CONSTRAINT ck_requisiciones_prioridad CHECK (prioridad IN ('NORMAL', 'URGENTE')),
    CONSTRAINT ck_requisiciones_area CHECK (area IN ('ALMACEN', 'FARMACIA'))
);

CREATE INDEX idx_requisiciones_estado ON requisiciones (estado, creada_en);
CREATE INDEX idx_requisiciones_solicitante ON requisiciones (solicitante_id);

CREATE TABLE requisicion_items (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    requisicion_id uuid NOT NULL,
    producto_id uuid NOT NULL,
    cantidad integer NOT NULL,
    observacion varchar(200),
    posicion integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_requisicion_items PRIMARY KEY (id),
    CONSTRAINT fk_requisicion_items_requisicion FOREIGN KEY (requisicion_id) REFERENCES requisiciones (id) ON DELETE CASCADE,
    CONSTRAINT fk_requisicion_items_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT ck_requisicion_items_cantidad CHECK (cantidad > 0)
);

CREATE INDEX idx_requisicion_items_requisicion ON requisicion_items (requisicion_id);

CREATE TABLE cotizaciones (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    requisicion_id uuid NOT NULL,
    proveedor_id uuid NOT NULL,
    estado varchar(12) NOT NULL,
    vigencia_hasta date,
    plazo_entrega_dias integer,
    condicion_pago_dias integer NOT NULL DEFAULT 0,
    observaciones varchar(300),
    total numeric(12,2) NOT NULL,
    registrada_por_id uuid NOT NULL,
    creada_en timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_cotizaciones PRIMARY KEY (id),
    CONSTRAINT uk_cotizaciones_requisicion_proveedor UNIQUE (requisicion_id, proveedor_id),
    CONSTRAINT fk_cotizaciones_requisicion FOREIGN KEY (requisicion_id) REFERENCES requisiciones (id),
    CONSTRAINT fk_cotizaciones_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id),
    CONSTRAINT fk_cotizaciones_registrador FOREIGN KEY (registrada_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_cotizaciones_estado CHECK (estado IN ('VIGENTE', 'SELECCIONADA', 'DESCARTADA'))
);

CREATE TABLE cotizacion_items (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    cotizacion_id uuid NOT NULL,
    producto_id uuid NOT NULL,
    cantidad integer NOT NULL,
    precio_unitario numeric(12,2) NOT NULL,
    posicion integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_cotizacion_items PRIMARY KEY (id),
    CONSTRAINT fk_cotizacion_items_cotizacion FOREIGN KEY (cotizacion_id) REFERENCES cotizaciones (id) ON DELETE CASCADE,
    CONSTRAINT fk_cotizacion_items_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT ck_cotizacion_items_valores CHECK (cantidad > 0 AND precio_unitario > 0)
);

CREATE TABLE ordenes_compra (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    anio integer,
    numero integer,
    codigo varchar(20),
    estado varchar(22) NOT NULL,
    proveedor_id uuid NOT NULL,
    requisicion_id uuid,
    cotizacion_id uuid,
    creada_por_id uuid NOT NULL,
    aprobada_por_id uuid,
    nivel_aprobacion varchar(10),
    afecta_igv boolean NOT NULL DEFAULT true,
    subtotal numeric(12,2) NOT NULL DEFAULT 0,
    igv numeric(12,2) NOT NULL DEFAULT 0,
    total numeric(12,2) NOT NULL DEFAULT 0,
    condicion_pago_dias integer NOT NULL DEFAULT 0,
    fecha_entrega_estimada date,
    observaciones varchar(300),
    hash varchar(64),
    creada_en timestamp NOT NULL DEFAULT now(),
    enviada_en timestamp,
    aprobada_en timestamp,
    motivo_resolucion varchar(300),
    cerrada_en timestamp,
    anulada_en timestamp,
    anulada_por_id uuid,
    CONSTRAINT pk_ordenes_compra PRIMARY KEY (id),
    CONSTRAINT fk_ordenes_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id),
    CONSTRAINT fk_ordenes_requisicion FOREIGN KEY (requisicion_id) REFERENCES requisiciones (id),
    CONSTRAINT fk_ordenes_cotizacion FOREIGN KEY (cotizacion_id) REFERENCES cotizaciones (id),
    CONSTRAINT fk_ordenes_creador FOREIGN KEY (creada_por_id) REFERENCES usuarios (id),
    CONSTRAINT fk_ordenes_aprobador FOREIGN KEY (aprobada_por_id) REFERENCES usuarios (id),
    CONSTRAINT fk_ordenes_anulador FOREIGN KEY (anulada_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_ordenes_estado CHECK (estado IN ('BORRADOR', 'PENDIENTE_APROBACION', 'APROBADA', 'PARCIAL', 'RECIBIDA', 'RECHAZADA', 'CERRADA', 'ANULADA')),
    CONSTRAINT ck_ordenes_nivel CHECK (nivel_aprobacion IS NULL OR nivel_aprobacion IN ('FINANZAS', 'GERENCIA'))
);

CREATE UNIQUE INDEX uk_ordenes_numero ON ordenes_compra (anio, numero) WHERE numero IS NOT NULL;
CREATE UNIQUE INDEX uk_ordenes_codigo ON ordenes_compra (codigo) WHERE codigo IS NOT NULL;
CREATE INDEX idx_ordenes_estado ON ordenes_compra (estado, creada_en);
CREATE INDEX idx_ordenes_proveedor ON ordenes_compra (proveedor_id);

CREATE TABLE orden_compra_items (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    orden_id uuid NOT NULL,
    producto_id uuid NOT NULL,
    cantidad integer NOT NULL,
    precio_unitario numeric(12,2) NOT NULL,
    subtotal numeric(12,2) NOT NULL,
    cantidad_recibida integer NOT NULL DEFAULT 0,
    cantidad_rechazada integer NOT NULL DEFAULT 0,
    posicion integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_orden_compra_items PRIMARY KEY (id),
    CONSTRAINT fk_orden_items_orden FOREIGN KEY (orden_id) REFERENCES ordenes_compra (id) ON DELETE CASCADE,
    CONSTRAINT fk_orden_items_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT ck_orden_items_valores CHECK (cantidad > 0 AND precio_unitario > 0 AND cantidad_recibida >= 0 AND cantidad_rechazada >= 0)
);

CREATE INDEX idx_orden_items_orden ON orden_compra_items (orden_id);

CREATE TABLE recepciones_compra (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    codigo varchar(20) NOT NULL,
    orden_id uuid NOT NULL,
    guia_remision varchar(40),
    observaciones varchar(300),
    recibida_por_id uuid NOT NULL,
    recibida_en timestamp NOT NULL DEFAULT now(),
    ingreso_id uuid,
    con_discrepancia boolean NOT NULL DEFAULT false,
    CONSTRAINT pk_recepciones_compra PRIMARY KEY (id),
    CONSTRAINT uk_recepciones_codigo UNIQUE (codigo),
    CONSTRAINT fk_recepciones_orden FOREIGN KEY (orden_id) REFERENCES ordenes_compra (id),
    CONSTRAINT fk_recepciones_receptor FOREIGN KEY (recibida_por_id) REFERENCES usuarios (id),
    CONSTRAINT fk_recepciones_ingreso FOREIGN KEY (ingreso_id) REFERENCES ingresos (id)
);

CREATE INDEX idx_recepciones_orden ON recepciones_compra (orden_id);

CREATE TABLE recepcion_items (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    recepcion_id uuid NOT NULL,
    orden_item_id uuid NOT NULL,
    producto_id uuid NOT NULL,
    cantidad_recibida integer NOT NULL DEFAULT 0,
    cantidad_rechazada integer NOT NULL DEFAULT 0,
    cantidad_devuelta integer NOT NULL DEFAULT 0,
    numero_lote varchar(40),
    fecha_vencimiento date,
    ubicacion varchar(60),
    lote_id uuid,
    costo_unitario numeric(12,2) NOT NULL,
    motivo_discrepancia varchar(200),
    posicion integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_recepcion_items PRIMARY KEY (id),
    CONSTRAINT fk_recepcion_items_recepcion FOREIGN KEY (recepcion_id) REFERENCES recepciones_compra (id) ON DELETE CASCADE,
    CONSTRAINT fk_recepcion_items_orden_item FOREIGN KEY (orden_item_id) REFERENCES orden_compra_items (id),
    CONSTRAINT fk_recepcion_items_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_recepcion_items_lote FOREIGN KEY (lote_id) REFERENCES lotes (id),
    CONSTRAINT ck_recepcion_items_valores CHECK (cantidad_recibida >= 0 AND cantidad_rechazada >= 0 AND cantidad_devuelta >= 0)
);

CREATE INDEX idx_recepcion_items_recepcion ON recepcion_items (recepcion_id);

CREATE TABLE comprobantes_proveedor (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    proveedor_id uuid NOT NULL,
    orden_id uuid,
    tipo varchar(12) NOT NULL,
    serie varchar(10) NOT NULL,
    numero varchar(20) NOT NULL,
    ruc_emisor varchar(11) NOT NULL,
    fecha_emision date NOT NULL,
    fecha_vencimiento date NOT NULL,
    subtotal numeric(12,2) NOT NULL,
    igv numeric(12,2) NOT NULL DEFAULT 0,
    total numeric(12,2) NOT NULL,
    monto_pagado numeric(12,2) NOT NULL DEFAULT 0,
    monto_credito numeric(12,2) NOT NULL DEFAULT 0,
    estado varchar(10) NOT NULL,
    observaciones varchar(300),
    registrado_por_id uuid NOT NULL,
    registrado_en timestamp NOT NULL DEFAULT now(),
    anulado_en timestamp,
    motivo_anulacion varchar(300),
    CONSTRAINT pk_comprobantes_proveedor PRIMARY KEY (id),
    CONSTRAINT uk_comprobantes_proveedor_documento UNIQUE (proveedor_id, tipo, serie, numero),
    CONSTRAINT fk_comprobantes_proveedor_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id),
    CONSTRAINT fk_comprobantes_proveedor_orden FOREIGN KEY (orden_id) REFERENCES ordenes_compra (id),
    CONSTRAINT fk_comprobantes_proveedor_registrador FOREIGN KEY (registrado_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_comprobantes_proveedor_tipo CHECK (tipo IN ('FACTURA', 'BOLETA', 'RECIBO')),
    CONSTRAINT ck_comprobantes_proveedor_estado CHECK (estado IN ('PENDIENTE', 'PARCIAL', 'PAGADO', 'ANULADO')),
    CONSTRAINT ck_comprobantes_proveedor_montos CHECK (total > 0 AND monto_pagado >= 0 AND monto_credito >= 0 AND monto_pagado + monto_credito <= total)
);

CREATE INDEX idx_comprobantes_proveedor_estado ON comprobantes_proveedor (estado, fecha_vencimiento);

CREATE TABLE pagos_proveedor (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    comprobante_id uuid NOT NULL,
    monto numeric(12,2) NOT NULL,
    metodo varchar(15) NOT NULL,
    referencia varchar(60),
    fecha date NOT NULL,
    registrado_por_id uuid NOT NULL,
    registrado_en timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_pagos_proveedor PRIMARY KEY (id),
    CONSTRAINT fk_pagos_proveedor_comprobante FOREIGN KEY (comprobante_id) REFERENCES comprobantes_proveedor (id),
    CONSTRAINT fk_pagos_proveedor_registrador FOREIGN KEY (registrado_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_pagos_proveedor_monto CHECK (monto > 0),
    CONSTRAINT ck_pagos_proveedor_metodo CHECK (metodo IN ('TRANSFERENCIA', 'EFECTIVO', 'CHEQUE', 'YAPE_PLIN'))
);

CREATE INDEX idx_pagos_proveedor_comprobante ON pagos_proveedor (comprobante_id);

CREATE TABLE devoluciones_proveedor (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    codigo varchar(20) NOT NULL,
    proveedor_id uuid NOT NULL,
    recepcion_id uuid NOT NULL,
    comprobante_id uuid,
    motivo varchar(300) NOT NULL,
    monto numeric(12,2) NOT NULL,
    registrada_por_id uuid NOT NULL,
    registrada_en timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_devoluciones_proveedor PRIMARY KEY (id),
    CONSTRAINT uk_devoluciones_codigo UNIQUE (codigo),
    CONSTRAINT fk_devoluciones_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id),
    CONSTRAINT fk_devoluciones_recepcion FOREIGN KEY (recepcion_id) REFERENCES recepciones_compra (id),
    CONSTRAINT fk_devoluciones_comprobante FOREIGN KEY (comprobante_id) REFERENCES comprobantes_proveedor (id),
    CONSTRAINT fk_devoluciones_registrador FOREIGN KEY (registrada_por_id) REFERENCES usuarios (id)
);

CREATE TABLE devolucion_items (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    devolucion_id uuid NOT NULL,
    recepcion_item_id uuid NOT NULL,
    lote_id uuid NOT NULL,
    cantidad integer NOT NULL,
    costo_unitario numeric(12,2) NOT NULL,
    CONSTRAINT pk_devolucion_items PRIMARY KEY (id),
    CONSTRAINT fk_devolucion_items_devolucion FOREIGN KEY (devolucion_id) REFERENCES devoluciones_proveedor (id) ON DELETE CASCADE,
    CONSTRAINT fk_devolucion_items_recepcion_item FOREIGN KEY (recepcion_item_id) REFERENCES recepcion_items (id),
    CONSTRAINT fk_devolucion_items_lote FOREIGN KEY (lote_id) REFERENCES lotes (id),
    CONSTRAINT ck_devolucion_items_cantidad CHECK (cantidad > 0)
);

CREATE TABLE compras_auditoria (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    fecha timestamp NOT NULL DEFAULT now(),
    usuario_id uuid NOT NULL,
    entidad varchar(20) NOT NULL,
    entidad_id uuid NOT NULL,
    accion varchar(30) NOT NULL,
    detalle varchar(300),
    CONSTRAINT pk_compras_auditoria PRIMARY KEY (id),
    CONSTRAINT fk_compras_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_compras_auditoria_entidad ON compras_auditoria (entidad, entidad_id, fecha);

ALTER TABLE movimientos_inventario DROP CONSTRAINT movimientos_motivo_check;
ALTER TABLE movimientos_inventario ADD CONSTRAINT movimientos_motivo_check CHECK (motivo IN (
    'COMPRA', 'CONSUMO_INTERNO', 'MERMA', 'VENCIMIENTO', 'AJUSTE_NEGATIVO', 'AJUSTE_POSITIVO',
    'DISPENSACION', 'ANULACION_DISPENSACION', 'DEVOLUCION_PROVEEDOR'));

ALTER TABLE notificaciones DROP CONSTRAINT notificaciones_tipo_check;
ALTER TABLE notificaciones ADD CONSTRAINT notificaciones_tipo_check CHECK (tipo IN (
    'CITA_CREADA', 'CITA_CONFIRMADA', 'CITA_CANCELADA', 'CITA_ATENDIDA', 'CITA_REPROGRAMADA',
    'RECORDATORIO_CITA', 'RECORDATORIO_EVENTO', 'INICIO_SESION', 'CONTRASENA_CAMBIADA', 'PAGO_REGISTRADO',
    'PAGO_REEMBOLSADO', 'MFA_ACTIVADO', 'MFA_DESACTIVADO', 'PACIENTE_COMPARTIDO', 'ROLES_ACTUALIZADOS',
    'SESION_SOSPECHOSA', 'ORDEN_LABORATORIO', 'RESULTADO_LABORATORIO', 'RECETA_DISPENSADA', 'STOCK_BAJO',
    'PRODUCTO_POR_VENCER', 'PACIENTE_EN_ESPERA', 'TURNO_LLAMADO', 'COMPROBANTE_EMITIDO',
    'INGRESO_HOSPITALARIO', 'ALTA_HOSPITALARIA', 'EMERGENCIA_TRIAJE', 'CAJA_DIFERENCIA',
    'SOLICITUD_AUSENCIA', 'AUSENCIA_RESUELTA', 'ORDEN_IMAGEN', 'ESTUDIO_INFORMADO', 'HALLAZGO_CRITICO',
    'EXPORTACION_LISTA', 'PREAUTORIZACION_SOLICITADA', 'PREAUTORIZACION_RESUELTA',
    'TELECONSULTA_PROXIMA', 'TELECONSULTA_SALA_ABIERTA', 'TELECONSULTA_PACIENTE_EN_SALA',
    'DOCUMENTO_EMITIDO', 'CONSENTIMIENTO_PENDIENTE', 'CONSENTIMIENTO_FIRMADO',
    'RESPALDO_FALLIDO', 'RESPALDO_VENCIDO', 'COMPROBANTE_RECHAZADO', 'ARCO_RECIBIDA', 'ARCO_RESUELTA',
    'INCIDENTE_DATOS', 'REQUISICION_ENVIADA', 'REQUISICION_RESUELTA', 'ORDEN_COMPRA_POR_APROBAR', 'ORDEN_COMPRA_RESUELTA',
    'CUENTA_POR_PAGAR_VENCIDA', 'SISTEMA'));
