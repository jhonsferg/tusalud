INSERT INTO configuracion_fiscal (
    sede, habilitada, ruc, razon_social, nombre_comercial, direccion, ubigeo, departamento, provincia, distrito,
    codigo_local, proveedor, modo, afectacion_servicios, igv_porcentaje, boletas_por_resumen, dias_limite_baja,
    actualizado_en)
VALUES (
    'PRINCIPAL', true, '20601234565', 'Clínica TuSalud S.A.C.', 'TuSalud', 'Av. Salud 123, Miraflores', '150122',
    'Lima', 'Lima', 'Miraflores', '0000', 'SIMULACION', 'BETA', 'GRAVADO', 18.00, false, 7, now());

INSERT INTO series_comprobante (sede, tipo, serie, correlativo_actual, activa) VALUES
    ('PRINCIPAL', 'FACTURA', 'F001', 0, true),
    ('PRINCIPAL', 'BOLETA', 'B001', 0, true),
    ('PRINCIPAL', 'NOTA_CREDITO', 'FC01', 0, true),
    ('PRINCIPAL', 'NOTA_CREDITO', 'BC01', 0, true),
    ('PRINCIPAL', 'NOTA_DEBITO', 'FD01', 0, true),
    ('PRINCIPAL', 'NOTA_DEBITO', 'BD01', 0, true);

INSERT INTO politicas_privacidad (version, titulo, contenido, vigente_desde, publicada, hash, creada_en)
VALUES (
    1,
    'Política de privacidad y tratamiento de datos personales',
    E'1. Quién trata sus datos\nClínica TuSalud S.A.C. es la titular del banco de datos personales y responsable de su tratamiento.\n\n2. Qué datos tratamos\nTratamos datos de identificación y contacto, datos de salud de su historia clínica, datos de facturación y datos técnicos de acceso a la plataforma.\n\n3. Para qué los usamos\nPrestar la atención de salud, gestionar citas, emitir comprobantes de pago, cumplir obligaciones legales y, solo si usted lo autoriza, enviarle comunicaciones sobre campañas y servicios.\n\n4. Cuánto tiempo los conservamos\nLa historia clínica se conserva durante el plazo que fija la norma sectorial. Los comprobantes se conservan por el plazo tributario. Los datos técnicos se anonimizan o se depuran al vencer los plazos de retención definidos.\n\n5. Con quién los compartimos\nSolo con encargados de tratamiento que prestan servicios a la clínica bajo contrato y con las entidades a las que la ley nos obliga a informar.\n\n6. Sus derechos\nUsted puede ejercer sus derechos de acceso, rectificación, cancelación y oposición desde la sección Mi privacidad. Responderemos dentro de los plazos legales. Los datos que la ley obliga a conservar, como la historia clínica, no se pueden cancelar mientras corra su plazo.\n\n7. Cambios en esta política\nCada versión se conserva y queda registrada la fecha en que usted la aceptó.\n\nEste texto es una plantilla de demostración y debe ser validado por un asesor legal antes de su uso real.',
    CURRENT_DATE - 30,
    true,
    '',
    now());

UPDATE politicas_privacidad
SET hash = encode(sha256(convert_to(titulo || chr(10) || contenido, 'UTF8')), 'hex');

INSERT INTO bancos_datos (
    codigo, nombre, descripcion, finalidad_principal, categorias_datos, contiene_sensibles, responsable, ubicacion,
    medidas_seguridad, transferencia_transfronteriza, numero_inscripcion, fecha_inscripcion, activo, actualizado_en)
VALUES
    ('PACIENTES', 'Pacientes e historias clínicas',
     'Datos de identificación, contacto y de salud de los pacientes atendidos.',
     'Prestación de servicios de salud y continuidad de la atención',
     'Identificación, contacto, antecedentes, diagnósticos, tratamientos, resultados de exámenes, imágenes',
     true, 'Clínica TuSalud S.A.C.', 'Servidor propio con respaldo cifrado',
     'Control de acceso por rol y relación asistencial, auditoría de accesos, cifrado de respaldos, bloqueo por inactividad',
     false, NULL, NULL, true, now()),
    ('FACTURACION', 'Facturación y cobranzas',
     'Comprobantes de pago, clientes, aseguradoras y cuentas por cobrar.',
     'Cumplimiento de obligaciones tributarias y gestión de cobros',
     'Identificación, documentos tributarios, importes, cobertura de seguros',
     false, 'Clínica TuSalud S.A.C.', 'Servidor propio con respaldo cifrado',
     'Control de acceso por rol, firma digital de comprobantes, bitácora de envíos',
     false, NULL, NULL, true, now()),
    ('PERSONAL', 'Personal y recursos humanos',
     'Datos del personal asistencial y administrativo.',
     'Gestión de la relación laboral y de los turnos de atención',
     'Identificación, contacto, cargo, turnos, ausencias',
     false, 'Clínica TuSalud S.A.C.', 'Servidor propio con respaldo cifrado',
     'Acceso restringido a recursos humanos y administración, auditoría de cambios',
     false, NULL, NULL, true, now());

INSERT INTO finalidades_tratamiento (
    codigo, nombre, descripcion, banco_id, base_legal, obligatoria, categoria_datos, plazo_retencion_dias, activa)
VALUES
    ('ATENCION_SALUD', 'Atención de salud',
     'Registrar y consultar su historia clínica para atenderlo, emitir recetas y documentos médicos.',
     (SELECT id FROM bancos_datos WHERE codigo = 'PACIENTES'),
     'Ejecución de la relación asistencial y obligación legal de llevar la historia clínica', true,
     'Identificación y datos de salud', 7300, true),
    ('FACTURACION_PAGOS', 'Facturación y cobranza',
     'Emitir comprobantes de pago, gestionar cobros y liquidar con aseguradoras.',
     (SELECT id FROM bancos_datos WHERE codigo = 'FACTURACION'),
     'Obligación tributaria y ejecución de la relación contractual', true,
     'Identificación y datos tributarios', 1825, true),
    ('AVISOS_SERVICIO', 'Avisos del servicio por correo, SMS o WhatsApp',
     'Recordarle sus citas y avisarle cuando tenga resultados o documentos disponibles.',
     (SELECT id FROM bancos_datos WHERE codigo = 'PACIENTES'),
     'Consentimiento del titular al activar cada canal', false,
     'Contacto', NULL, true),
    ('COMUNICACIONES_COMERCIALES', 'Comunicaciones comerciales',
     'Enviarle información sobre campañas, programas de prevención y nuevos servicios.',
     (SELECT id FROM bancos_datos WHERE codigo = 'PACIENTES'),
     'Consentimiento del titular', false,
     'Contacto', NULL, true),
    ('INVESTIGACION_ESTADISTICA', 'Estadística e investigación con datos anonimizados',
     'Usar sus datos de forma anonimizada para estadísticas de calidad y morbilidad.',
     (SELECT id FROM bancos_datos WHERE codigo = 'PACIENTES'),
     'Consentimiento del titular', false,
     'Datos de salud anonimizados', NULL, true);

INSERT INTO encargados_tratamiento (
    razon_social, ruc, servicio, categorias_datos, pais, transfronterizo, contrato_vigente_hasta, contacto, activo,
    actualizado_en)
VALUES
    ('Proveedor de mensajería SMS y WhatsApp', NULL, 'Envío de avisos por SMS y WhatsApp',
     'Teléfono y mensaje genérico sin datos clínicos', 'Perú', false, CURRENT_DATE + 365,
     'soporte@proveedor-mensajeria.example', true, now()),
    ('Proveedor de correo transaccional', NULL, 'Envío de avisos por correo electrónico',
     'Correo y mensaje genérico sin datos clínicos', 'Estados Unidos', true, CURRENT_DATE + 200,
     'soporte@proveedor-correo.example', true, now()),
    ('Operador de servicios electrónicos', '20100070970', 'Envío de comprobantes electrónicos a SUNAT',
     'Datos tributarios de clientes', 'Perú', false, CURRENT_DATE + 400,
     'ose@operador.example', true, now());
