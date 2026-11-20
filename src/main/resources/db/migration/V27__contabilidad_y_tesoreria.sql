CREATE TABLE cuentas_contables (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    codigo varchar(10) NOT NULL,
    nombre varchar(120) NOT NULL,
    tipo varchar(12) NOT NULL,
    naturaleza varchar(10) NOT NULL,
    acepta_movimientos boolean NOT NULL DEFAULT true,
    activa boolean NOT NULL DEFAULT true,
    sistema boolean NOT NULL DEFAULT false,
    creada_en timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_cuentas_contables PRIMARY KEY (id),
    CONSTRAINT uk_cuentas_contables_codigo UNIQUE (codigo),
    CONSTRAINT ck_cuentas_contables_tipo CHECK (tipo IN ('ACTIVO', 'PASIVO', 'PATRIMONIO', 'INGRESO', 'GASTO')),
    CONSTRAINT ck_cuentas_contables_naturaleza CHECK (naturaleza IN ('DEUDORA', 'ACREEDORA'))
);

CREATE TABLE periodos_contables (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    anio integer NOT NULL,
    mes integer NOT NULL,
    estado varchar(10) NOT NULL,
    abierto_en timestamp NOT NULL DEFAULT now(),
    cerrado_en timestamp,
    cerrado_por_id uuid,
    reabierto_en timestamp,
    reabierto_por_id uuid,
    motivo_reapertura varchar(300),
    CONSTRAINT pk_periodos_contables PRIMARY KEY (id),
    CONSTRAINT uk_periodos_contables UNIQUE (anio, mes),
    CONSTRAINT fk_periodos_cerrado_por FOREIGN KEY (cerrado_por_id) REFERENCES usuarios (id),
    CONSTRAINT fk_periodos_reabierto_por FOREIGN KEY (reabierto_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_periodos_mes CHECK (mes BETWEEN 1 AND 12),
    CONSTRAINT ck_periodos_estado CHECK (estado IN ('ABIERTO', 'CERRADO'))
);

CREATE TABLE secuencias_contables (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tipo varchar(10) NOT NULL,
    anio integer NOT NULL,
    ultimo integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_secuencias_contables PRIMARY KEY (id),
    CONSTRAINT uk_secuencias_contables UNIQUE (tipo, anio)
);

CREATE TABLE asientos (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    numero varchar(20),
    fecha date NOT NULL,
    periodo_id uuid NOT NULL,
    glosa varchar(200) NOT NULL,
    origen varchar(30) NOT NULL,
    referencia_id uuid,
    estado varchar(14) NOT NULL,
    total_debe numeric(14, 2) NOT NULL DEFAULT 0,
    total_haber numeric(14, 2) NOT NULL DEFAULT 0,
    creado_por_id uuid,
    creado_en timestamp NOT NULL DEFAULT now(),
    contabilizado_por_id uuid,
    contabilizado_en timestamp,
    revierte_a_id uuid,
    motivo_reversa varchar(300),
    CONSTRAINT pk_asientos PRIMARY KEY (id),
    CONSTRAINT uk_asientos_numero UNIQUE (numero),
    CONSTRAINT uk_asientos_origen UNIQUE (origen, referencia_id),
    CONSTRAINT fk_asientos_periodo FOREIGN KEY (periodo_id) REFERENCES periodos_contables (id),
    CONSTRAINT fk_asientos_creado_por FOREIGN KEY (creado_por_id) REFERENCES usuarios (id),
    CONSTRAINT fk_asientos_contabilizado_por FOREIGN KEY (contabilizado_por_id) REFERENCES usuarios (id),
    CONSTRAINT fk_asientos_revierte FOREIGN KEY (revierte_a_id) REFERENCES asientos (id),
    CONSTRAINT ck_asientos_estado CHECK (estado IN ('BORRADOR', 'CONTABILIZADO', 'REVERTIDO')),
    CONSTRAINT ck_asientos_origen CHECK (origen IN (
        'MANUAL', 'COBRO', 'COMPRA_PROVEEDOR', 'PAGO_PROVEEDOR', 'MOVIMIENTO_TESORERIA', 'REVERSA'))
);

CREATE INDEX idx_asientos_periodo ON asientos (periodo_id, fecha);
CREATE INDEX idx_asientos_fecha ON asientos (fecha);

CREATE TABLE asiento_lineas (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    asiento_id uuid NOT NULL,
    posicion integer NOT NULL DEFAULT 0,
    cuenta_id uuid NOT NULL,
    debe numeric(14, 2) NOT NULL DEFAULT 0,
    haber numeric(14, 2) NOT NULL DEFAULT 0,
    glosa varchar(200),
    CONSTRAINT pk_asiento_lineas PRIMARY KEY (id),
    CONSTRAINT fk_asiento_lineas_asiento FOREIGN KEY (asiento_id) REFERENCES asientos (id) ON DELETE CASCADE,
    CONSTRAINT fk_asiento_lineas_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas_contables (id),
    CONSTRAINT ck_asiento_lineas_importes CHECK (debe >= 0 AND haber >= 0 AND (debe = 0 OR haber = 0) AND (debe > 0 OR haber > 0))
);

CREATE INDEX idx_asiento_lineas_asiento ON asiento_lineas (asiento_id);
CREATE INDEX idx_asiento_lineas_cuenta ON asiento_lineas (cuenta_id);

CREATE TABLE plantillas_asiento (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    evento varchar(30) NOT NULL,
    nombre varchar(120) NOT NULL,
    activa boolean NOT NULL DEFAULT true,
    CONSTRAINT pk_plantillas_asiento PRIMARY KEY (id),
    CONSTRAINT uk_plantillas_asiento_evento UNIQUE (evento),
    CONSTRAINT ck_plantillas_asiento_evento CHECK (evento IN (
        'COBRO_EFECTIVO', 'COBRO_ELECTRONICO', 'COMPRA_PROVEEDOR', 'PAGO_PROVEEDOR_EFECTIVO', 'PAGO_PROVEEDOR_BANCO'))
);

CREATE TABLE plantilla_asiento_lineas (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    plantilla_id uuid NOT NULL,
    posicion integer NOT NULL DEFAULT 0,
    lado varchar(5) NOT NULL,
    concepto varchar(15) NOT NULL,
    cuenta_id uuid NOT NULL,
    CONSTRAINT pk_plantilla_asiento_lineas PRIMARY KEY (id),
    CONSTRAINT fk_plantilla_lineas_plantilla FOREIGN KEY (plantilla_id) REFERENCES plantillas_asiento (id) ON DELETE CASCADE,
    CONSTRAINT fk_plantilla_lineas_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas_contables (id),
    CONSTRAINT ck_plantilla_lineas_lado CHECK (lado IN ('DEBE', 'HABER')),
    CONSTRAINT ck_plantilla_lineas_concepto CHECK (concepto IN ('TOTAL', 'SUBTOTAL', 'IGV', 'CUBIERTO', 'INGRESO_BRUTO', 'MONTO'))
);

CREATE INDEX idx_plantilla_lineas_plantilla ON plantilla_asiento_lineas (plantilla_id, posicion);

CREATE TABLE asientos_pendientes (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    evento varchar(40) NOT NULL,
    referencia_id uuid NOT NULL,
    payload text NOT NULL,
    error varchar(300) NOT NULL,
    intentos integer NOT NULL DEFAULT 1,
    creado_en timestamp NOT NULL DEFAULT now(),
    resuelto_en timestamp,
    CONSTRAINT pk_asientos_pendientes PRIMARY KEY (id)
);

CREATE INDEX idx_asientos_pendientes_abiertos ON asientos_pendientes (resuelto_en, creado_en);

CREATE TABLE cuentas_tesoreria (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    nombre varchar(100) NOT NULL,
    tipo varchar(8) NOT NULL,
    banco varchar(60),
    ultimos4 varchar(4),
    moneda varchar(3) NOT NULL DEFAULT 'PEN',
    cuenta_contable_id uuid NOT NULL,
    saldo_inicial numeric(14, 2) NOT NULL DEFAULT 0,
    predeterminada boolean NOT NULL DEFAULT false,
    activa boolean NOT NULL DEFAULT true,
    creada_en timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_cuentas_tesoreria PRIMARY KEY (id),
    CONSTRAINT uk_cuentas_tesoreria_nombre UNIQUE (nombre),
    CONSTRAINT fk_cuentas_tesoreria_contable FOREIGN KEY (cuenta_contable_id) REFERENCES cuentas_contables (id),
    CONSTRAINT ck_cuentas_tesoreria_tipo CHECK (tipo IN ('CAJA', 'BANCO'))
);

CREATE TABLE movimientos_tesoreria (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    cuenta_id uuid NOT NULL,
    fecha date NOT NULL,
    tipo varchar(8) NOT NULL,
    monto numeric(14, 2) NOT NULL,
    descripcion varchar(200) NOT NULL,
    referencia varchar(60),
    origen varchar(20) NOT NULL,
    referencia_id uuid,
    asiento_id uuid,
    conciliado boolean NOT NULL DEFAULT false,
    registrado_por_id uuid,
    registrado_en timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_movimientos_tesoreria PRIMARY KEY (id),
    CONSTRAINT uk_movimientos_tesoreria_origen UNIQUE (origen, referencia_id),
    CONSTRAINT fk_movimientos_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas_tesoreria (id),
    CONSTRAINT fk_movimientos_asiento FOREIGN KEY (asiento_id) REFERENCES asientos (id),
    CONSTRAINT fk_movimientos_registrado_por FOREIGN KEY (registrado_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_movimientos_tipo CHECK (tipo IN ('INGRESO', 'EGRESO')),
    CONSTRAINT ck_movimientos_monto CHECK (monto > 0),
    CONSTRAINT ck_movimientos_origen CHECK (origen IN ('MANUAL', 'COBRO', 'PAGO_PROVEEDOR'))
);

CREATE INDEX idx_movimientos_cuenta_fecha ON movimientos_tesoreria (cuenta_id, fecha);

CREATE TABLE extractos_bancarios (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    cuenta_id uuid NOT NULL,
    archivo varchar(120) NOT NULL,
    desde date,
    hasta date,
    lineas integer NOT NULL DEFAULT 0,
    importado_por_id uuid NOT NULL,
    importado_en timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_extractos_bancarios PRIMARY KEY (id),
    CONSTRAINT fk_extractos_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas_tesoreria (id),
    CONSTRAINT fk_extractos_importado_por FOREIGN KEY (importado_por_id) REFERENCES usuarios (id)
);

CREATE TABLE extracto_lineas (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    extracto_id uuid NOT NULL,
    cuenta_id uuid NOT NULL,
    fecha date NOT NULL,
    descripcion varchar(200) NOT NULL,
    referencia varchar(60),
    monto numeric(14, 2) NOT NULL,
    huella varchar(64) NOT NULL,
    estado varchar(12) NOT NULL DEFAULT 'PENDIENTE',
    movimiento_id uuid,
    conciliada_por_id uuid,
    conciliada_en timestamp,
    CONSTRAINT pk_extracto_lineas PRIMARY KEY (id),
    CONSTRAINT uk_extracto_lineas_huella UNIQUE (cuenta_id, huella),
    CONSTRAINT fk_extracto_lineas_extracto FOREIGN KEY (extracto_id) REFERENCES extractos_bancarios (id) ON DELETE CASCADE,
    CONSTRAINT fk_extracto_lineas_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas_tesoreria (id),
    CONSTRAINT fk_extracto_lineas_movimiento FOREIGN KEY (movimiento_id) REFERENCES movimientos_tesoreria (id),
    CONSTRAINT fk_extracto_lineas_conciliada_por FOREIGN KEY (conciliada_por_id) REFERENCES usuarios (id),
    CONSTRAINT ck_extracto_lineas_estado CHECK (estado IN ('PENDIENTE', 'CONCILIADA', 'IGNORADA')),
    CONSTRAINT ck_extracto_lineas_monto CHECK (monto <> 0)
);

CREATE INDEX idx_extracto_lineas_estado ON extracto_lineas (cuenta_id, estado, fecha);

CREATE TABLE contabilidad_auditoria (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    fecha timestamp NOT NULL DEFAULT now(),
    usuario_id uuid,
    entidad varchar(20) NOT NULL,
    entidad_id uuid NOT NULL,
    accion varchar(30) NOT NULL,
    detalle varchar(300),
    CONSTRAINT pk_contabilidad_auditoria PRIMARY KEY (id),
    CONSTRAINT fk_contabilidad_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_contabilidad_auditoria_entidad ON contabilidad_auditoria (entidad, entidad_id, fecha);

INSERT INTO cuentas_contables (codigo, nombre, tipo, naturaleza, acepta_movimientos, sistema) VALUES
    ('10', 'Efectivo y equivalentes de efectivo', 'ACTIVO', 'DEUDORA', false, true),
    ('1011', 'Caja', 'ACTIVO', 'DEUDORA', true, true),
    ('1041', 'Cuentas corrientes operativas', 'ACTIVO', 'DEUDORA', true, true),
    ('12', 'Cuentas por cobrar comerciales terceros', 'ACTIVO', 'DEUDORA', false, true),
    ('1212', 'Comprobantes por cobrar de pacientes', 'ACTIVO', 'DEUDORA', true, true),
    ('1213', 'Cuentas por cobrar a aseguradoras', 'ACTIVO', 'DEUDORA', true, true),
    ('16', 'Cuentas por cobrar diversas terceros', 'ACTIVO', 'DEUDORA', true, true),
    ('20', 'Mercaderías', 'ACTIVO', 'DEUDORA', false, true),
    ('2011', 'Medicamentos e insumos médicos', 'ACTIVO', 'DEUDORA', true, true),
    ('33', 'Inmuebles, maquinaria y equipo', 'ACTIVO', 'DEUDORA', true, true),
    ('39', 'Depreciación y amortización acumulada', 'ACTIVO', 'ACREEDORA', true, true),
    ('40', 'Tributos y aportes por pagar', 'PASIVO', 'ACREEDORA', false, true),
    ('40111', 'IGV cuenta propia', 'PASIVO', 'ACREEDORA', true, true),
    ('4017', 'Impuesto a la renta', 'PASIVO', 'ACREEDORA', true, true),
    ('41', 'Remuneraciones y participaciones por pagar', 'PASIVO', 'ACREEDORA', true, true),
    ('42', 'Cuentas por pagar comerciales terceros', 'PASIVO', 'ACREEDORA', false, true),
    ('4212', 'Facturas, boletas y otros comprobantes por pagar', 'PASIVO', 'ACREEDORA', true, true),
    ('46', 'Cuentas por pagar diversas terceros', 'PASIVO', 'ACREEDORA', true, true),
    ('50', 'Capital', 'PATRIMONIO', 'ACREEDORA', false, true),
    ('5011', 'Capital social', 'PATRIMONIO', 'ACREEDORA', true, true),
    ('59', 'Resultados acumulados', 'PATRIMONIO', 'ACREEDORA', false, true),
    ('5911', 'Utilidades acumuladas', 'PATRIMONIO', 'ACREEDORA', true, true),
    ('60', 'Compras', 'GASTO', 'DEUDORA', false, true),
    ('6011', 'Compras de medicamentos e insumos médicos', 'GASTO', 'DEUDORA', true, true),
    ('62', 'Gastos de personal', 'GASTO', 'DEUDORA', false, true),
    ('6211', 'Sueldos y salarios', 'GASTO', 'DEUDORA', true, true),
    ('63', 'Gastos de servicios prestados por terceros', 'GASTO', 'DEUDORA', false, true),
    ('6311', 'Servicios básicos y alquileres', 'GASTO', 'DEUDORA', true, true),
    ('6591', 'Otros gastos de gestión', 'GASTO', 'DEUDORA', true, true),
    ('70', 'Ventas', 'INGRESO', 'ACREEDORA', false, true),
    ('7041', 'Prestación de servicios de salud', 'INGRESO', 'ACREEDORA', true, true),
    ('75', 'Otros ingresos de gestión', 'INGRESO', 'ACREEDORA', true, true);

INSERT INTO plantillas_asiento (evento, nombre) VALUES
    ('COBRO_EFECTIVO', 'Cobro en efectivo en caja'),
    ('COBRO_ELECTRONICO', 'Cobro con tarjeta, billetera o transferencia'),
    ('COMPRA_PROVEEDOR', 'Comprobante de proveedor registrado'),
    ('PAGO_PROVEEDOR_EFECTIVO', 'Pago a proveedor en efectivo'),
    ('PAGO_PROVEEDOR_BANCO', 'Pago a proveedor por banco');

INSERT INTO plantilla_asiento_lineas (plantilla_id, posicion, lado, concepto, cuenta_id)
SELECT p.id, l.posicion, l.lado, l.concepto, c.id
FROM plantillas_asiento p
JOIN (VALUES
    ('COBRO_EFECTIVO', 0, 'DEBE', 'TOTAL', '1011'),
    ('COBRO_EFECTIVO', 1, 'DEBE', 'CUBIERTO', '1213'),
    ('COBRO_EFECTIVO', 2, 'HABER', 'INGRESO_BRUTO', '7041'),
    ('COBRO_EFECTIVO', 3, 'HABER', 'IGV', '40111'),
    ('COBRO_ELECTRONICO', 0, 'DEBE', 'TOTAL', '1041'),
    ('COBRO_ELECTRONICO', 1, 'DEBE', 'CUBIERTO', '1213'),
    ('COBRO_ELECTRONICO', 2, 'HABER', 'INGRESO_BRUTO', '7041'),
    ('COBRO_ELECTRONICO', 3, 'HABER', 'IGV', '40111'),
    ('COMPRA_PROVEEDOR', 0, 'DEBE', 'SUBTOTAL', '6011'),
    ('COMPRA_PROVEEDOR', 1, 'DEBE', 'IGV', '40111'),
    ('COMPRA_PROVEEDOR', 2, 'HABER', 'TOTAL', '4212'),
    ('PAGO_PROVEEDOR_EFECTIVO', 0, 'DEBE', 'MONTO', '4212'),
    ('PAGO_PROVEEDOR_EFECTIVO', 1, 'HABER', 'MONTO', '1011'),
    ('PAGO_PROVEEDOR_BANCO', 0, 'DEBE', 'MONTO', '4212'),
    ('PAGO_PROVEEDOR_BANCO', 1, 'HABER', 'MONTO', '1041')
) AS l (evento, posicion, lado, concepto, codigo) ON l.evento = p.evento
JOIN cuentas_contables c ON c.codigo = l.codigo;
