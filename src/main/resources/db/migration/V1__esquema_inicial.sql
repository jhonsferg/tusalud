CREATE TABLE atencion_receta (
    atencion_id uuid NOT NULL,
    dosis character varying(100),
    duracion character varying(100),
    frecuencia character varying(100),
    medicamento character varying(150) NOT NULL,
    posicion integer NOT NULL,
    CONSTRAINT atencion_receta_posicion_check CHECK ((posicion >= 0))
);

CREATE TABLE atenciones (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    diagnostico character varying(1000) NOT NULL,
    fecha timestamp(6) without time zone NOT NULL,
    frecuencia_cardiaca integer,
    indicaciones character varying(1000),
    observaciones character varying(1000),
    peso_kg numeric(6,2),
    presion_arterial character varying(10),
    talla_cm numeric(5,1),
    temperatura_c numeric(4,1),
    tratamiento character varying(1000),
    cita_id uuid NOT NULL,
    medico_id uuid NOT NULL,
    paciente_id uuid NOT NULL
);

CREATE TABLE citas (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    created_at timestamp(6) without time zone NOT NULL,
    estado character varying(15) NOT NULL,
    fecha_hora timestamp(6) without time zone NOT NULL,
    monto numeric(10,2) NOT NULL,
    motivo character varying(500) NOT NULL,
    recordatorio_enviado boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    medico_id uuid NOT NULL,
    paciente_id uuid NOT NULL,
    CONSTRAINT citas_estado_check CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'CONFIRMADA'::character varying, 'CANCELADA'::character varying, 'ATENDIDA'::character varying])::text[])))
);

CREATE TABLE cupones (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    activo boolean NOT NULL,
    codigo character varying(30) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    descripcion character varying(255),
    fecha_fin date NOT NULL,
    fecha_inicio date NOT NULL,
    tipo character varying(15) NOT NULL,
    usos_actuales integer NOT NULL,
    usos_maximos integer,
    valor numeric(10,2) NOT NULL,
    CONSTRAINT cupones_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['PORCENTAJE'::character varying, 'MONTO_FIJO'::character varying])::text[])))
);

CREATE TABLE especialidades (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    descripcion character varying(500),
    nombre character varying(100) NOT NULL
);

CREATE TABLE horarios_atencion (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    dia_semana character varying(10) NOT NULL,
    duracion_cita_minutos integer NOT NULL,
    hora_fin time(0) without time zone NOT NULL,
    hora_inicio time(0) without time zone NOT NULL,
    medico_id uuid NOT NULL,
    CONSTRAINT horarios_atencion_dia_semana_check CHECK (((dia_semana)::text = ANY ((ARRAY['MONDAY'::character varying, 'TUESDAY'::character varying, 'WEDNESDAY'::character varying, 'THURSDAY'::character varying, 'FRIDAY'::character varying, 'SATURDAY'::character varying, 'SUNDAY'::character varying])::text[])))
);

CREATE TABLE medicos (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    colegiatura character varying(20) NOT NULL,
    consultorio character varying(50),
    precio_consulta numeric(10,2) NOT NULL,
    especialidad_id uuid NOT NULL,
    usuario_id uuid NOT NULL
);

CREATE TABLE metodos_pago (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    created_at timestamp(6) without time zone NOT NULL,
    marca character varying(15) NOT NULL,
    predeterminado boolean NOT NULL,
    tipo character varying(20) NOT NULL,
    titular character varying(100) NOT NULL,
    ultimos4 character varying(4) NOT NULL,
    vencimiento_anio integer NOT NULL,
    vencimiento_mes integer NOT NULL,
    usuario_id uuid NOT NULL,
    CONSTRAINT metodos_pago_marca_check CHECK (((marca)::text = ANY ((ARRAY['VISA'::character varying, 'MASTERCARD'::character varying, 'AMEX'::character varying, 'OTRA'::character varying])::text[]))),
    CONSTRAINT metodos_pago_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['TARJETA_CREDITO'::character varying, 'TARJETA_DEBITO'::character varying])::text[])))
);

CREATE TABLE notificaciones (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    created_at timestamp(6) without time zone NOT NULL,
    enlace character varying(255),
    leida boolean NOT NULL,
    mensaje character varying(500) NOT NULL,
    tipo character varying(30) NOT NULL,
    titulo character varying(150) NOT NULL,
    usuario_id uuid NOT NULL,
    CONSTRAINT notificaciones_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['CITA_CREADA'::character varying, 'CITA_CONFIRMADA'::character varying, 'CITA_CANCELADA'::character varying, 'CITA_ATENDIDA'::character varying, 'CITA_REPROGRAMADA'::character varying, 'RECORDATORIO_CITA'::character varying, 'INICIO_SESION'::character varying, 'CONTRASENA_CAMBIADA'::character varying, 'PAGO_REGISTRADO'::character varying, 'PAGO_REEMBOLSADO'::character varying, 'SISTEMA'::character varying])::text[])))
);

CREATE TABLE pacientes (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    alergias character varying(1000),
    antecedentes character varying(2000),
    dni character varying(8) NOT NULL,
    fecha_nacimiento date,
    grupo_sanguineo character varying(3),
    historial_clinico_numero character varying(20) NOT NULL,
    usuario_id uuid NOT NULL
);

CREATE TABLE pagos (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    codigo_cupon character varying(30),
    comprobante character varying(20) NOT NULL,
    descuento numeric(10,2) NOT NULL,
    estado character varying(15) NOT NULL,
    fecha_pago timestamp(6) without time zone NOT NULL,
    metodo_descripcion character varying(100) NOT NULL,
    monto numeric(10,2) NOT NULL,
    total numeric(10,2) NOT NULL,
    cita_id uuid NOT NULL,
    metodo_pago_id uuid,
    CONSTRAINT pagos_estado_check CHECK (((estado)::text = ANY ((ARRAY['PAGADO'::character varying, 'REEMBOLSADO'::character varying])::text[])))
);

CREATE TABLE preferencias_usuario (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    alertas_inicio_sesion boolean NOT NULL,
    minutos_antelacion_recordatorio integer DEFAULT 1440 NOT NULL,
    recordatorios_activos boolean NOT NULL,
    usuario_id uuid NOT NULL,
    CONSTRAINT preferencias_usuario_minutos_check CHECK ((minutos_antelacion_recordatorio >= 15 AND minutos_antelacion_recordatorio <= 4320))
);

CREATE TABLE roles (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    nombre character varying(30) NOT NULL,
    CONSTRAINT roles_nombre_check CHECK (((nombre)::text = ANY ((ARRAY['ROLE_ADMIN'::character varying, 'ROLE_MEDICO'::character varying, 'ROLE_PACIENTE'::character varying])::text[])))
);

CREATE TABLE sesiones_usuario (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    creada_en timestamp(6) without time zone NOT NULL,
    dispositivo character varying(120) NOT NULL,
    expira_en timestamp(6) without time zone NOT NULL,
    ip character varying(45),
    revocada boolean NOT NULL,
    token_id uuid NOT NULL,
    ultimo_acceso timestamp(6) without time zone NOT NULL,
    user_agent character varying(500),
    usuario_id uuid NOT NULL
);

CREATE TABLE usuario_roles (
    usuario_id uuid NOT NULL,
    rol_id uuid NOT NULL
);

CREATE TABLE usuarios (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    apellido character varying(100) NOT NULL,
    avatar character varying(450000),
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(150) NOT NULL,
    estado character varying(15) NOT NULL,
    nombre character varying(100) NOT NULL,
    password character varying(100) NOT NULL,
    telefono character varying(20),
    CONSTRAINT usuarios_estado_check CHECK (((estado)::text = ANY ((ARRAY['ACTIVO'::character varying, 'INACTIVO'::character varying])::text[])))
);

CREATE TABLE valoraciones (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    comentario character varying(500),
    created_at timestamp(6) without time zone NOT NULL,
    puntuacion integer NOT NULL,
    cita_id uuid NOT NULL,
    medico_id uuid NOT NULL,
    paciente_id uuid NOT NULL
);

ALTER TABLE ONLY atencion_receta
    ADD CONSTRAINT atencion_receta_pkey PRIMARY KEY (atencion_id, posicion);

ALTER TABLE ONLY atenciones
    ADD CONSTRAINT atenciones_pkey PRIMARY KEY (id);

ALTER TABLE ONLY citas
    ADD CONSTRAINT citas_pkey PRIMARY KEY (id);

ALTER TABLE ONLY cupones
    ADD CONSTRAINT cupones_pkey PRIMARY KEY (id);

ALTER TABLE ONLY especialidades
    ADD CONSTRAINT especialidades_pkey PRIMARY KEY (id);

ALTER TABLE ONLY horarios_atencion
    ADD CONSTRAINT horarios_atencion_pkey PRIMARY KEY (id);

ALTER TABLE ONLY medicos
    ADD CONSTRAINT medicos_pkey PRIMARY KEY (id);

ALTER TABLE ONLY metodos_pago
    ADD CONSTRAINT metodos_pago_pkey PRIMARY KEY (id);

ALTER TABLE ONLY notificaciones
    ADD CONSTRAINT notificaciones_pkey PRIMARY KEY (id);

ALTER TABLE ONLY pacientes
    ADD CONSTRAINT pacientes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY pagos
    ADD CONSTRAINT pagos_pkey PRIMARY KEY (id);

ALTER TABLE ONLY preferencias_usuario
    ADD CONSTRAINT preferencias_usuario_pkey PRIMARY KEY (id);

ALTER TABLE ONLY roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sesiones_usuario
    ADD CONSTRAINT sesiones_usuario_pkey PRIMARY KEY (id);

ALTER TABLE ONLY atenciones
    ADD CONSTRAINT uk_atenciones_cita UNIQUE (cita_id);

ALTER TABLE ONLY cupones
    ADD CONSTRAINT uk_cupones_codigo UNIQUE (codigo);

ALTER TABLE ONLY especialidades
    ADD CONSTRAINT uk_especialidades_nombre UNIQUE (nombre);

ALTER TABLE ONLY medicos
    ADD CONSTRAINT uk_medicos_colegiatura UNIQUE (colegiatura);

ALTER TABLE ONLY medicos
    ADD CONSTRAINT uk_medicos_usuario UNIQUE (usuario_id);

ALTER TABLE ONLY pacientes
    ADD CONSTRAINT uk_pacientes_dni UNIQUE (dni);

ALTER TABLE ONLY pacientes
    ADD CONSTRAINT uk_pacientes_historial UNIQUE (historial_clinico_numero);

ALTER TABLE ONLY pacientes
    ADD CONSTRAINT uk_pacientes_usuario UNIQUE (usuario_id);

ALTER TABLE ONLY preferencias_usuario
    ADD CONSTRAINT uk_preferencias_usuario UNIQUE (usuario_id);

ALTER TABLE ONLY sesiones_usuario
    ADD CONSTRAINT uk_sesiones_token UNIQUE (token_id);

ALTER TABLE ONLY usuarios
    ADD CONSTRAINT uk_usuarios_email UNIQUE (email);

ALTER TABLE ONLY valoraciones
    ADD CONSTRAINT uk_valoraciones_cita UNIQUE (cita_id);

ALTER TABLE ONLY pagos
    ADD CONSTRAINT ukk7uxl0x06uvdnq393dku1y2s UNIQUE (comprobante);

ALTER TABLE ONLY roles
    ADD CONSTRAINT ukldv0v52e0udsh2h1rs0r0gw1n UNIQUE (nombre);

ALTER TABLE ONLY pagos
    ADD CONSTRAINT uktg86t2pgd2ruwveunl8a2mnkd UNIQUE (cita_id);

ALTER TABLE ONLY usuario_roles
    ADD CONSTRAINT usuario_roles_pkey PRIMARY KEY (usuario_id, rol_id);

ALTER TABLE ONLY usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id);

ALTER TABLE ONLY valoraciones
    ADD CONSTRAINT valoraciones_pkey PRIMARY KEY (id);

CREATE INDEX idx_citas_medico_fecha ON citas USING btree (medico_id, fecha_hora);

CREATE INDEX idx_citas_paciente_fecha ON citas USING btree (paciente_id, fecha_hora);

CREATE INDEX idx_horarios_medico_dia ON horarios_atencion USING btree (medico_id, dia_semana);

CREATE INDEX idx_metodos_pago_usuario ON metodos_pago USING btree (usuario_id);

CREATE INDEX idx_notificaciones_usuario_fecha ON notificaciones USING btree (usuario_id, created_at);

CREATE INDEX idx_sesiones_usuario ON sesiones_usuario USING btree (usuario_id);

CREATE INDEX idx_usuarios_email ON usuarios USING btree (email);

ALTER TABLE ONLY notificaciones
    ADD CONSTRAINT fk1mxbjb81ft61gwlh0kabubndc FOREIGN KEY (usuario_id) REFERENCES usuarios(id);

ALTER TABLE ONLY atencion_receta
    ADD CONSTRAINT fk1qs2318869d50vsigrtf99d66 FOREIGN KEY (atencion_id) REFERENCES atenciones(id);

ALTER TABLE ONLY pagos
    ADD CONSTRAINT fk2wxxn0u28jwprfqh2t0u5gvsc FOREIGN KEY (metodo_pago_id) REFERENCES metodos_pago(id);

ALTER TABLE ONLY valoraciones
    ADD CONSTRAINT fk3c652gadiyjlvl1naajl2e4ut FOREIGN KEY (medico_id) REFERENCES medicos(id);

ALTER TABLE ONLY horarios_atencion
    ADD CONSTRAINT fk5e3h1w9pnh4ly5sln46158wn2 FOREIGN KEY (medico_id) REFERENCES medicos(id);

ALTER TABLE ONLY sesiones_usuario
    ADD CONSTRAINT fk61yesn8vugoenw8jxfsp6atnj FOREIGN KEY (usuario_id) REFERENCES usuarios(id);

ALTER TABLE ONLY metodos_pago
    ADD CONSTRAINT fk6petg9p5hprht765dv1br1r6q FOREIGN KEY (usuario_id) REFERENCES usuarios(id);

ALTER TABLE ONLY atenciones
    ADD CONSTRAINT fk7xv73wcjex1nvq972x3eihbeg FOREIGN KEY (medico_id) REFERENCES medicos(id);

ALTER TABLE ONLY preferencias_usuario
    ADD CONSTRAINT fk8hynqqmgtbxlgibfkpch9mxu0 FOREIGN KEY (usuario_id) REFERENCES usuarios(id);

ALTER TABLE ONLY pacientes
    ADD CONSTRAINT fk96vvhaactwhv7l8ymuq6b4r6j FOREIGN KEY (usuario_id) REFERENCES usuarios(id);

ALTER TABLE ONLY medicos
    ADD CONSTRAINT fka0dd5a1r4o7vgnqr0aigr69td FOREIGN KEY (usuario_id) REFERENCES usuarios(id);

ALTER TABLE ONLY citas
    ADD CONSTRAINT fkbfl57ey8hx1a3ubtrpy8x9ll FOREIGN KEY (medico_id) REFERENCES medicos(id);

ALTER TABLE ONLY usuario_roles
    ADD CONSTRAINT fkbt9i9yrb9ug88xnh82n9m60pr FOREIGN KEY (rol_id) REFERENCES roles(id);

ALTER TABLE ONLY pagos
    ADD CONSTRAINT fkdyyqrbd5gg0lhhcijte5qkt88 FOREIGN KEY (cita_id) REFERENCES citas(id);

ALTER TABLE ONLY medicos
    ADD CONSTRAINT fkh05vu52pvshce3nfxi3cvju30 FOREIGN KEY (especialidad_id) REFERENCES especialidades(id);

ALTER TABLE ONLY atenciones
    ADD CONSTRAINT fkjbyr5v2p28qftsx56gwdismh1 FOREIGN KEY (cita_id) REFERENCES citas(id);

ALTER TABLE ONLY atenciones
    ADD CONSTRAINT fkjkb460pe3ltyh1dcnaepcl90k FOREIGN KEY (paciente_id) REFERENCES pacientes(id);

ALTER TABLE ONLY citas
    ADD CONSTRAINT fknqrsxxcuysfcxiekvixm7h8r1 FOREIGN KEY (paciente_id) REFERENCES pacientes(id);

ALTER TABLE ONLY valoraciones
    ADD CONSTRAINT fkrh5xlla259pg4dsqypoogk4na FOREIGN KEY (paciente_id) REFERENCES pacientes(id);

ALTER TABLE ONLY valoraciones
    ADD CONSTRAINT fkt0l7eqqdahh9prqctbjh73k8e FOREIGN KEY (cita_id) REFERENCES citas(id);

ALTER TABLE ONLY usuario_roles
    ADD CONSTRAINT fkuu9tea04xb29m2km5lwe46ua FOREIGN KEY (usuario_id) REFERENCES usuarios(id);
