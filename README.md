# TuSalud

TuSalud es el sistema de gestión de citas y consultas médicas. El repositorio es un monorepo con dos aplicaciones: el backend en Java con Spring Boot, ubicado en la raíz, y el frontend en Angular, ubicado en la carpeta `web`. El backend expone una API REST para administrar especialidades, médicos, horarios de atención y citas, con autenticación JWT sin estado y autorización por roles. Este documento es la única fuente de verdad del proyecto: describe qué hace el sistema, cómo está construido, cómo se configura y cómo se ejecuta y verifica en cada entorno.

## Índice

- [Estructura del monorepo](#estructura-del-monorepo)
- [Stack tecnológico](#stack-tecnológico)
- [Arquitectura](#arquitectura)
- [Modelo de datos](#modelo-de-datos)
- [Reglas de negocio](#reglas-de-negocio)
- [Seguridad](#seguridad)
- [API REST](#api-rest)
- [Diagramas](#diagramas)
  - [Diagrama de arquitectura](#diagrama-de-arquitectura)
  - [Diagrama entidad relación](#diagrama-entidad-relación)
  - [Diagrama de estados de una cita](#diagrama-de-estados-de-una-cita)
  - [Autenticación de una petición protegida](#autenticación-de-una-petición-protegida)
  - [Autenticación](#autenticación)
  - [Especialidades](#especialidades)
  - [Médicos y horarios](#médicos-y-horarios)
  - [Citas](#citas)
  - [Auditoría](#auditoría)
  - [Perfil](#perfil)
  - [Notificaciones en tiempo real](#notificaciones-en-tiempo-real)
  - [Sesiones y alertas de inicio de sesión](#sesiones-y-alertas-de-inicio-de-sesión)
  - [Pagos, tarjetas y cupones](#pagos-tarjetas-y-cupones)
  - [Historia clínica](#historia-clínica)
  - [Reprogramación y valoraciones](#reprogramación-y-valoraciones)
  - [Tarifas por especialidad](#tarifas-por-especialidad)
  - [Roles de usuario](#roles-de-usuario)
  - [Pacientes compartidos](#pacientes-compartidos)
  - [Pagos con Yape y Plin](#pagos-con-yape-y-plin)
  - [Verificación en dos pasos](#verificación-en-dos-pasos)
  - [Renovación del token](#renovación-del-token)
  - [Calendario de citas](#calendario-de-citas)
  - [Planificador de eventos](#planificador-de-eventos)
  - [Personal de la institución](#personal-de-la-institución)
  - [Almacén e inventario](#almacén-e-inventario)
  - [Farmacia](#farmacia)
  - [Laboratorio clínico](#laboratorio-clínico)
  - [Reprogramación de citas](#reprogramación-de-citas)
  - [Recepción y turnos](#recepción-y-turnos)
  - [Caja y facturación](#caja-y-facturación)
  - [Hospitalización y emergencias](#hospitalización-y-emergencias)
  - [Reportes y BI](#reportes-y-bi)
  - [Recursos humanos](#recursos-humanos)
- [Manejo de errores](#manejo-de-errores)
- [Perfiles y configuración](#perfiles-y-configuración)
- [Migraciones con Flyway](#migraciones-con-flyway)
- [Ejecución local](#ejecución-local)
- [Docker](#docker)
- [Aplicación web](#aplicación-web)
- [Observabilidad](#observabilidad)
- [Datos de prueba](#datos-de-prueba)
  - [Datos de demostración](#datos-de-demostración)
- [Pruebas automatizadas](#pruebas-automatizadas)
- [Flujo de trabajo con Git](#flujo-de-trabajo-con-git)
- [Colección Postman](#colección-postman)
- [Variables de entorno](#variables-de-entorno)

## Estructura del monorepo

```text
tusalud/
├── src/                Código fuente y pruebas del backend
├── .githooks/          Hooks de Git para formatear antes de commit y push
├── docker/             Configuración del agente y del Collector de OpenTelemetry, y de pgAdmin
├── postman/            Colección de Postman
├── web/                Workspace de Angular con el portal, una aplicación por rol y las librerías compartidas
├── compose.yaml        Stack completo en modo producción
├── compose.dev.yaml    Sobrescritura para el perfil dev
├── compose.qa.yaml     Sobrescritura para el perfil qa
├── Dockerfile          Imagen del backend
└── pom.xml             Build de Maven del backend
```

El backend y el frontend se construyen por separado. El `.dockerignore` de la raíz excluye la carpeta `web`, de modo que la imagen del backend no incluye el código del frontend.

## Stack tecnológico

| Componente                        | Versión                      |
| --------------------------------- | ---------------------------- |
| Java                              | 25                           |
| Spring Boot                       | 4.1.1                        |
| Spring Framework                  | 7.0.9                        |
| Jackson                           | 3.1.5                        |
| Spring Security                   | 7.1.1                        |
| Hibernate ORM                     | 7.4.5                        |
| JJWT                              | 0.13.0                       |
| springdoc-openapi                 | 3.1.1                        |
| PostgreSQL                        | 18                           |
| H2                                | 2.4.240                      |
| Lombok                            | 1.18.46                      |
| Maven Wrapper con Maven           | 3.9.16                       |
| OpenTelemetry Java Agent          | 2.32.0                       |
| OpenTelemetry Collector Contrib   | 0.162.0                      |
| Jaeger                            | 2.22.0                       |
| Angular                           | 21.2.25                      |
| Angular CLI                       | 21.2.26                      |
| Node.js para compilar el frontend | 24                           |
| nginx                             | 1.30, imagen sin privilegios |
| Spotless Maven Plugin             | 3.10.4                       |
| palantir-java-format              | 2.102.0                      |
| Prettier                          | 3.9.10                       |

## Arquitectura

El proyecto sigue una arquitectura en capas bajo el paquete `com.codesoftlabs.tusalud.app`. Cada petición entra por un controlador, que delega en un servicio con la lógica de negocio y las transacciones, y el servicio accede a la base de datos a través de repositorios con consultas JPQL.

```text
src/main/java/com/codesoftlabs/tusalud/app/
├── config/             Seguridad, CORS, OpenAPI, datos iniciales y telemetría HTTP
├── controllers/        Controladores REST con validaciones y documentación OpenAPI
├── dto/
│   ├── request/        Cuerpos de entrada validados con Jakarta Validation
│   └── response/       Respuestas de la API y formato estándar de error
├── entities/           Entidades JPA y enumeraciones
├── exceptions/         Excepciones de negocio y manejador global de errores
├── repositories/       Repositorios Spring Data con consultas JPQL
├── security/           JWT, filtro de autenticación y UserDetailsService
└── services/
    ├── interfaces/     Contratos de servicio
    └── impl/           Implementaciones transaccionales
```

### Concurrencia con virtual threads

La API corre sobre Java 25 con virtual threads activados mediante `spring.threads.virtual.enabled: true`. Tomcat atiende cada petición en un virtual thread en lugar de tomar uno de un pool limitado de hilos del sistema operativo. Mientras una petición espera a PostgreSQL, su hilo virtual se desmonta y el hilo del sistema queda libre para atender otra, de modo que la API sostiene muchas más peticiones concurrentes con la misma memoria. Las tareas asíncronas y programadas de Spring usan el mismo modelo.

Desde Java 24 un bloque `synchronized` ya no fija el hilo virtual a su hilo del sistema, por lo que las librerías que todavía usan `synchronized` no anulan la ventaja. El límite real de concurrencia contra la base de datos lo marca el pool de conexiones de Hikari: cuando todas las conexiones están ocupadas, las peticiones adicionales esperan su turno sin consumir hilos del sistema.

Las entidades nunca se serializan directamente. Cada respuesta se construye con un DTO, lo que evita ciclos en la serialización y problemas con relaciones perezosas. La propiedad `open-in-view` está desactivada, de modo que toda carga de datos ocurre dentro de la transacción del servicio.

## Modelo de datos

| Entidad           | Atributos principales                                                                                         | Relaciones                                                                                      |
| ----------------- | ------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| `Usuario`         | `nombre`, `apellido`, `email` único e indexado, `password` con hash BCrypt, `telefono`, `estado`, `createdAt` | Muchos a muchos con `Rol`                                                                       |
| `Rol`             | `nombre`: `ROLE_ADMIN`, `ROLE_MEDICO` o `ROLE_PACIENTE`                                                       | Asignado a usuarios                                                                             |
| `Especialidad`    | `nombre` único, `descripcion`, `precioConsulta`                                                               | Muchos a muchos con `Medico`                                                                    |
| `Medico`          | `colegiatura` CMP única, `consultorio`                                                                        | Uno a uno con `Usuario`, muchos a muchos con `Especialidad`, uno a muchos con `HorarioAtencion` |
| `Paciente`        | `dni` único, `historialClinicoNumero` único, `fechaNacimiento`                                                | Uno a uno con `Usuario`                                                                         |
| `HorarioAtencion` | `diaSemana`, `horaInicio`, `horaFin`, `duracionCitaMinutos`                                                   | Muchos a uno con `Medico`                                                                       |
| `Cita`            | `fechaHora`, `motivo`, `estado`, `monto`, `createdAt`, `updatedAt`, `version`                                 | Muchos a uno con `Medico`, con `Paciente` y con `Especialidad`                                  |

Todas las relaciones usan carga perezosa. La tabla `citas` tiene índices por médico y fecha y por paciente y fecha, y el campo `version` aplica bloqueo optimista para que dos operaciones concurrentes no sobrescriban la misma cita.

## Reglas de negocio

### Registro de citas

`CitaService.crearCita` se ejecuta en una única transacción atómica y aplica estas validaciones en orden:

1. El paciente se obtiene del token del usuario autenticado.
2. La fila del médico se bloquea con `SELECT ... FOR UPDATE`, lo que serializa las reservas simultáneas sobre un mismo médico.
3. La fecha y hora se normaliza al minuto y debe ser futura.
4. El médico debe tener un bloque de atención ese día de la semana, la cita debe caber completa dentro del bloque y debe empezar en un turno exacto según la duración configurada.
5. No puede existir otra cita activa del médico ni del paciente que se cruce con esa franja. Las citas canceladas no ocupan horario.
6. La especialidad elegida debe pertenecer al médico y la cita se guarda en estado `PENDIENTE` con el precio vigente de esa especialidad, que queda fijado en la cita.

Cualquier conflicto lanza una excepción de negocio, la transacción hace rollback y la API responde 409.

### Ciclo de vida de una cita

| Transición                               | Quién puede ejecutarla                  |
| ---------------------------------------- | --------------------------------------- |
| `PENDIENTE` a `CONFIRMADA`               | El médico de la cita o un administrador |
| `PENDIENTE` o `CONFIRMADA` a `ATENDIDA`  | Solo el médico de la cita               |
| `PENDIENTE` o `CONFIRMADA` a `CANCELADA` | El paciente dueño o un administrador    |

Un paciente no puede cancelar una cita cuya fecha ya pasó, mientras que un administrador sí. Una cita solo es visible para su paciente, su médico y los administradores.

### Otras reglas

- El email de los usuarios es único sin distinguir mayúsculas y se guarda en minúsculas.
- El DNI y la colegiatura son únicos. La colegiatura se guarda en mayúsculas.
- El número de historia clínica se genera con el formato `HC-000001`.
- Los bloques de horario de un médico no pueden cruzarse en un mismo día, la hora de fin debe ser posterior a la de inicio y el bloque debe admitir al menos una cita.
- Una especialidad con médicos asignados no puede eliminarse.
- La disponibilidad de un médico se calcula a partir de sus bloques y descarta los turnos ocupados y, si la fecha es hoy, los turnos que ya pasaron.

## Seguridad

La API es completamente sin estado. Spring Security 7 trabaja con `SessionCreationPolicy.STATELESS`, CSRF desactivado y un filtro `JwtAuthenticationFilter` que valida el header `Authorization: Bearer <token>` en cada petición.

- Los tokens se firman con HMAC-SHA usando la clave `JWT_SECRET`, que debe estar codificada en Base64 y tener al menos 256 bits. Cada token incluye el email como sujeto, el emisor, la expiración, el id del usuario, su nombre y sus roles. El token de acceso dura 15 minutos y se renueva con un refresh token rotativo.
- Un usuario inactivo no puede iniciar sesión y su token deja de ser válido aunque no haya expirado.
- Las contraseñas se almacenan con BCrypt.
- Todas las entidades y tablas usan UUID versión 4 como identificador, generado en la aplicación y con `gen_random_uuid()` como valor por defecto en la base. Así los ids no se pueden adivinar ni recorrer en orden. Un id con formato inválido en la ruta responde 400 y uno inexistente responde 404. Los códigos legibles de negocio, como los comprobantes, los turnos y los ingresos, siguen siendo correlativos.
- CORS permite peticiones desde `http://localhost:4200` con los métodos GET, POST, PUT, DELETE y OPTIONS. El origen se configura con `CORS_ALLOWED_ORIGINS`.
- La autorización combina reglas por URL en `SecurityConfig` con `@PreAuthorize` en cada método de los controladores.
- Tener el rol correcto no basta para ver los datos de una persona: hace falta una relación con ella. `RelacionAsistencialService` es la única regla y la cumple un médico que atendió al paciente en una cita no cancelada, que tiene un ingreso activo a su cargo o a quien otro médico le compartió el paciente. Ese acceso se aplica a la historia clínica, el laboratorio, la hospitalización y la lista de pacientes del médico.
- Los datos clínicos los modifica solo el personal de salud. El paciente consulta su historia y, si algún dato no es correcto, se comunica con el centro de salud. El médico con relación actualiza los datos médicos y queda registrado quién y cuándo, y cada consulta de una historia ajena se guarda en `auditoria_clinica`.
- Cada rol recibe solo los campos que necesita: recepción y administración no ven diagnósticos ni indicaciones, caja no ve datos clínicos, farmacia solo ve las recetas, el correo y el teléfono de un médico solo los ven el administrador y el propio médico, y las valoraciones públicas abrevian el nombre del paciente. El administrador gestiona la institución pero no lee ni escribe datos clínicos.
- Una atención no se reescribe pasadas 48 horas ni cuando su receta ya fue dispensada.

| Rol             | Permisos                                                                                                                                               |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `ROLE_ADMIN`    | Gestiona especialidades y sus tarifas, médicos, horarios, roles de usuario, cupones y pagos, consulta la auditoría y cancela o confirma cualquier cita |
| `ROLE_MEDICO`   | Consulta su agenda, confirma y atiende sus citas, y comparte pacientes con otros médicos                                                               |
| `ROLE_PACIENTE` | Consulta disponibilidad, agenda citas y consulta o cancela las suyas                                                                                   |

## API REST

La documentación interactiva está en `http://localhost:8080/swagger-ui.html` y la especificación OpenAPI en `http://localhost:8080/v3/api-docs`. Para probar endpoints protegidos desde Swagger UI, obtén un token con el login y pégalo en el botón Authorize.

| Método                          | Ruta                                                                                          | Acceso                                                 | Respuesta |
| ------------------------------- | --------------------------------------------------------------------------------------------- | ------------------------------------------------------ | --------- |
| `POST`                          | `/api/v1/auth/login`                                                                          | Público                                                | 200       |
| `POST`                          | `/api/v1/auth/register`                                                                       | Público, registra pacientes                            | 201       |
| `GET`                           | `/api/v1/especialidades`                                                                      | Autenticado                                            | 200       |
| `GET`                           | `/api/v1/especialidades/{id}`                                                                 | Autenticado                                            | 200       |
| `POST`                          | `/api/v1/especialidades`                                                                      | Admin                                                  | 201       |
| `PUT`                           | `/api/v1/especialidades/{id}`                                                                 | Admin                                                  | 200       |
| `DELETE`                        | `/api/v1/especialidades/{id}`                                                                 | Admin                                                  | 204       |
| `GET`                           | `/api/v1/medicos?especialidadId=`                                                             | Autenticado                                            | 200       |
| `GET`                           | `/api/v1/medicos/{id}`                                                                        | Autenticado                                            | 200       |
| `POST`                          | `/api/v1/medicos`                                                                             | Admin                                                  | 201       |
| `GET`                           | `/api/v1/medicos/{id}/horarios`                                                               | Autenticado                                            | 200       |
| `POST`                          | `/api/v1/medicos/{id}/horarios`                                                               | Admin                                                  | 201       |
| `DELETE`                        | `/api/v1/medicos/{id}/horarios/{horarioId}`                                                   | Admin                                                  | 204       |
| `GET`                           | `/api/v1/medicos/{id}/disponibilidad?fecha=`                                                  | Autenticado                                            | 200       |
| `GET`                           | `/api/v1/medicos/me/agenda?fecha=&inicio=&fin=`                                               | Médico                                                 | 200       |
| `POST`                          | `/api/v1/citas`                                                                               | Paciente                                               | 201       |
| `GET`                           | `/api/v1/citas/mis-citas?inicio=&fin=`                                                        | Paciente                                               | 200       |
| `GET`                           | `/api/v1/citas/{id}`                                                                          | Paciente dueño, médico o admin                         | 200       |
| `PUT`                           | `/api/v1/citas/{id}/cancelar`                                                                 | Paciente dueño o admin                                 | 200       |
| `PUT`                           | `/api/v1/citas/{id}/confirmar`                                                                | Médico de la cita o admin                              | 200       |
| `PUT`                           | `/api/v1/citas/{id}/atender`                                                                  | Médico de la cita                                      | 200       |
| `GET`                           | `/api/v1/admin/auditoria/citas-por-estado`                                                    | Admin                                                  | 200       |
| `GET`                           | `/api/v1/admin/auditoria/citas?inicio=&fin=`                                                  | Admin                                                  | 200       |
| `POST`                          | `/api/v1/auth/logout`                                                                         | Autenticado                                            | 204       |
| `POST`                          | `/api/v1/auth/refresh`                                                                        | Público                                                | 200       |
| `GET`                           | `/api/v1/auth/mfa/estado`                                                                     | Autenticado                                            | 200       |
| `POST`                          | `/api/v1/auth/mfa/configurar`                                                                 | Autenticado                                            | 200       |
| `POST`                          | `/api/v1/auth/mfa/activar`                                                                    | Autenticado                                            | 200       |
| `POST`                          | `/api/v1/auth/mfa/desactivar`                                                                 | Autenticado                                            | 204       |
| `POST`                          | `/api/v1/auth/mfa/codigos-recuperacion/regenerar`                                             | Autenticado                                            | 200       |
| `POST`                          | `/api/v1/auth/mfa/verificar`                                                                  | Público con token de verificación                      | 200       |
| `PUT`                           | `/api/v1/medicos/{id}/especialidades`                                                         | Admin                                                  | 200       |
| `GET`                           | `/api/v1/medicos/{id}/disponibilidad/rango?inicio=&fin=`                                      | Autenticado                                            | 200       |
| `/citas/:id/reprogramar`        | Paciente, admin                                                                               | Reprogramación con el calendario                       |
| `/almacen`                      | Almacén, admin                                                                                | Resumen y alertas del almacén                          |
| `/almacen/productos`            | Almacén, admin, farmacia                                                                      | Productos y lotes                                      |
| `/almacen/ingresos`             | Almacén, admin                                                                                | Ingresos de mercadería                                 |
| `/almacen/proveedores`          | Almacén, admin                                                                                | Proveedores                                            |
| `/almacen/movimientos`          | Almacén, admin, farmacia                                                                      | Kardex                                                 |
| `/farmacia`                     | Farmacia, admin                                                                               | Recetas pendientes y ventas                            |
| `/farmacia/dispensar`           | Farmacia, admin                                                                               | Dispensación y venta libre                             |
| `/farmacia/historial`           | Farmacia, admin                                                                               | Comprobantes emitidos                                  |
| `/mis-medicamentos`             | Paciente                                                                                      | Entregas de la farmacia                                |
| `/laboratorio`                  | Laboratorio, médico, admin                                                                    | Cola de órdenes                                        |
| `/laboratorio/ordenes/:id`      | Laboratorio, médico, admin                                                                    | Detalle, muestra y resultados                          |
| `/laboratorio/examenes`         | Laboratorio, médico, admin                                                                    | Catálogo de exámenes                                   |
| `/laboratorio/solicitar`        | Médico                                                                                        | Solicitud de exámenes                                  |
| `/mis-resultados`               | Paciente                                                                                      | Resultados de laboratorio                              |
| `/recepcion`                    | Recepción, admin                                                                              | Panel de llegadas y cola de turnos                     |
| `/recepcion/pacientes`          | Recepción, admin                                                                              | Búsqueda y alta de pacientes en mostrador              |
| `/recepcion/citas/nueva`        | Recepción, admin                                                                              | Reserva de citas en mostrador                          |
| `/recepcion/pantalla`           | Recepción, admin                                                                              | Sala de espera con los turnos llamados                 |
| `/cola`                         | Médico                                                                                        | Cola de pacientes del médico                           |
| `/caja`                         | Caja                                                                                          | Apertura, resumen y cierre de caja                     |
| `/caja/cobrar`                  | Caja                                                                                          | Cuentas por cobrar y cobro libre                       |
| `/caja/comprobantes`            | Caja, admin                                                                                   | Boletas y facturas, con anulación                      |
| `/caja/historial`               | Caja, admin                                                                                   | Aperturas y cierres con su diferencia                  |
| `/caja/reportes`                | Caja, admin                                                                                   | Reporte de ingresos                                    |
| `/hospitalizacion`              | Enfermería, médico, admin, recepción                                                          | Mapa de camas y ocupación                              |
| `/hospitalizacion/emergencias`  | Enfermería, médico, admin, recepción                                                          | Cola de emergencia y triaje                            |
| `/hospitalizacion/ingresos`     | Enfermería, médico, admin, recepción                                                          | Ingresos hospitalarios                                 |
| `/hospitalizacion/ingresos/:id` | Enfermería, médico, admin                                                                     | Ficha, evoluciones, traslado y alta                    |
| `/hospitalizacion/camas`        | Enfermería, médico, admin                                                                     | Inventario de camas                                    |
| `/mis-ingresos`                 | Paciente                                                                                      | Hospitalizaciones del paciente                         |
| `/reportes/ejecutivo`           | Gerencia, admin                                                                               | Indicadores ejecutivos y series                        |
| `/reportes/financiero`          | Finanzas, gerencia, admin                                                                     | Ingresos, anulaciones y cajas                          |
| `/reportes/operativo`           | Operaciones, gerencia, admin                                                                  | Ocupación, esperas, demanda e inventario               |
| `/reportes/calidad`             | Calidad, gerencia, admin                                                                      | Valoraciones, cancelaciones y entregas                 |
| `/rrhh`                         | Recursos humanos, gerencia, admin                                                             | Resumen de la plantilla                                |
| `/rrhh/empleados`               | Recursos humanos, gerencia, admin                                                             | Fichas laborales                                       |
| `/rrhh/turnos`                  | Recursos humanos, admin                                                                       | Turnos semanales del personal                          |
| `/rrhh/ausencias`               | Recursos humanos, admin                                                                       | Bandeja y calendario de ausencias                      |
| `/mi-espacio`                   | Todo el personal                                                                              | Mis turnos, mis ausencias y mi ficha                   |
| `GET`                           | `/api/v1/calendario/eventos?inicio=&fin=`                                                     | Autenticado, eventos propios                           | 200       |
| `POST`                          | `/api/v1/calendario/eventos`                                                                  | Autenticado                                            | 201       |
| `PUT`                           | `/api/v1/calendario/eventos/{id}`                                                             | Dueño del evento                                       | 200       |
| `PUT`                           | `/api/v1/calendario/eventos/{id}/mover`                                                       | Dueño del evento                                       | 200       |
| `PUT`                           | `/api/v1/calendario/eventos/{id}/completado`                                                  | Dueño del evento                                       | 200       |
| `DELETE`                        | `/api/v1/calendario/eventos/{id}`                                                             | Dueño del evento                                       | 204       |
| `POST`                          | `/api/v1/admin/personal`                                                                      | Admin                                                  | 201       |
| `GET`                           | `/api/v1/almacen/resumen`, `/almacen/alertas`                                                 | Almacén, admin                                         | 200       |
| `GET`                           | `/api/v1/almacen/productos`, `/productos/{id}`, `/productos/{id}/lotes`                       | Almacén, admin, farmacia                               | 200       |
| `POST`                          | `/api/v1/almacen/productos`                                                                   | Almacén, admin                                         | 201       |
| `PUT`                           | `/api/v1/almacen/productos/{id}`                                                              | Almacén, admin                                         | 200       |
| `GET`                           | `/api/v1/almacen/proveedores`                                                                 | Almacén, admin                                         | 200       |
| `POST`                          | `/api/v1/almacen/proveedores`                                                                 | Almacén, admin                                         | 201       |
| `PUT`                           | `/api/v1/almacen/proveedores/{id}`                                                            | Almacén, admin                                         | 200       |
| `GET`                           | `/api/v1/almacen/ingresos`                                                                    | Almacén, admin                                         | 200       |
| `POST`                          | `/api/v1/almacen/ingresos`                                                                    | Almacén, admin                                         | 201       |
| `POST`                          | `/api/v1/almacen/salidas`, `/almacen/ajustes`                                                 | Almacén, admin                                         | 201       |
| `GET`                           | `/api/v1/almacen/movimientos`                                                                 | Almacén, admin                                         | 200       |
| `GET`                           | `/api/v1/farmacia/catalogo`                                                                   | Farmacia, médico, admin, almacén                       | 200       |
| `GET`                           | `/api/v1/farmacia/recetas`                                                                    | Farmacia, admin                                        | 200       |
| `POST`                          | `/api/v1/farmacia/dispensaciones`                                                             | Farmacia, admin                                        | 201       |
| `GET`                           | `/api/v1/farmacia/dispensaciones`, `/dispensaciones/{id}`, `/farmacia/resumen`                | Farmacia, admin                                        | 200       |
| `PUT`                           | `/api/v1/farmacia/dispensaciones/{id}/anular`                                                 | Farmacia, admin                                        | 200       |
| `GET`                           | `/api/v1/farmacia/mis-dispensaciones`                                                         | Paciente                                               | 200       |
| `GET`                           | `/api/v1/laboratorio/examenes`, `/examenes/{id}`                                              | Laboratorio, médico, admin                             | 200       |
| `POST`                          | `/api/v1/laboratorio/examenes`                                                                | Laboratorio, admin                                     | 201       |
| `PUT`                           | `/api/v1/laboratorio/examenes/{id}`                                                           | Laboratorio, admin                                     | 200       |
| `POST`                          | `/api/v1/laboratorio/ordenes`                                                                 | Médico                                                 | 201       |
| `GET`                           | `/api/v1/laboratorio/ordenes`, `/ordenes/{id}`                                                | Laboratorio, admin, médico solicitante, paciente dueño | 200       |
| `PUT`                           | `/api/v1/laboratorio/ordenes/{id}/muestra`, `/items/{itemId}/resultados`, `/validar`          | Laboratorio                                            | 200       |
| `PUT`                           | `/api/v1/laboratorio/ordenes/{id}/cancelar`                                                   | Médico solicitante, laboratorio, admin                 | 200       |
| `GET`                           | `/api/v1/laboratorio/mis-ordenes`                                                             | Paciente                                               | 200       |
| `GET`                           | `/api/v1/laboratorio/resumen`                                                                 | Laboratorio, admin                                     | 200       |
| `GET`                           | `/api/v1/recepcion/citas-hoy`, `/recepcion/resumen`                                           | Recepción, admin                                       | 200       |
| `POST`                          | `/api/v1/recepcion/citas/{id}/llegada`, `/recepcion/turnos`                                   | Recepción, admin                                       | 201       |
| `GET`                           | `/api/v1/recepcion/cola`                                                                      | Recepción, admin, médico                               | 200       |
| `PUT`                           | `/api/v1/recepcion/turnos/{id}/llamar`, `/ausente`, `/cancelar`                               | Recepción, admin, médico                               | 200       |
| `PUT`                           | `/api/v1/recepcion/turnos/{id}/iniciar`, `/finalizar`                                         | Médico del turno                                       | 200       |
| `GET`                           | `/api/v1/recepcion/pantalla`                                                                  | Autenticado                                            | 200       |
| `GET`                           | `/api/v1/recepcion/pacientes?q=`                                                              | Recepción, admin, caja, médico, farmacia, enfermería   | 200       |
| `POST`                          | `/api/v1/recepcion/pacientes`, `/recepcion/citas`                                             | Recepción, admin                                       | 201       |
| `POST`                          | `/api/v1/caja/aperturas`, `/caja/cobros`                                                      | Caja                                                   | 201       |
| `GET`                           | `/api/v1/caja/actual`, `/caja/resumen`                                                        | Caja                                                   | 200       |
| `POST`                          | `/api/v1/caja/cierre`                                                                         | Caja                                                   | 200       |
| `GET`                           | `/api/v1/caja/historial`, `/caja/pendientes`, `/caja/comprobantes`, `/caja/reportes/ingresos` | Caja, admin                                            | 200       |
| `PUT`                           | `/api/v1/caja/comprobantes/{id}/anular`                                                       | Caja, admin                                            | 200       |
| `GET`                           | `/api/v1/hospitalizacion/camas`, `/mapa`, `/resumen`, `/ingresos`, `/emergencias`             | Enfermería, médico, admin, recepción                   | 200       |
| `POST`                          | `/api/v1/hospitalizacion/camas`                                                               | Admin                                                  | 201       |
| `PUT`                           | `/api/v1/hospitalizacion/camas/{id}`                                                          | Admin                                                  | 200       |
| `PUT`                           | `/api/v1/hospitalizacion/camas/{id}/estado`                                                   | Enfermería, admin                                      | 200       |
| `POST`                          | `/api/v1/hospitalizacion/ingresos`                                                            | Médico, admin                                          | 201       |
| `GET`                           | `/api/v1/hospitalizacion/ingresos/{id}`, `/ingresos/{id}/evoluciones`                         | Personal clínico, recepción, paciente dueño            | 200       |
| `PUT`                           | `/api/v1/hospitalizacion/ingresos/{id}/trasladar`                                             | Médico, enfermería, admin                              | 200       |
| `PUT`                           | `/api/v1/hospitalizacion/ingresos/{id}/asignar-cama`                                          | Médico, enfermería                                     | 200       |
| `PUT`                           | `/api/v1/hospitalizacion/ingresos/{id}/alta`, `/cancelar`                                     | Médico, admin                                          | 200       |
| `POST`                          | `/api/v1/hospitalizacion/ingresos/{id}/evoluciones`                                           | Médico, enfermería                                     | 201       |
| `POST`                          | `/api/v1/hospitalizacion/emergencias`                                                         | Enfermería, médico, recepción, admin                   | 201       |
| `GET`                           | `/api/v1/hospitalizacion/mis-ingresos`                                                        | Paciente                                               | 200       |
| `GET`                           | `/api/v1/reportes/ejecutivo`                                                                  | Gerencia, admin                                        | 200       |
| `GET`                           | `/api/v1/reportes/financiero`                                                                 | Finanzas, gerencia, admin                              | 200       |
| `GET`                           | `/api/v1/reportes/operativo`                                                                  | Operaciones, gerencia, admin                           | 200       |
| `GET`                           | `/api/v1/reportes/calidad`                                                                    | Calidad, gerencia, admin                               | 200       |
| `GET`                           | `/api/v1/rrhh/resumen`, `/rrhh/empleados`, `/rrhh/empleados/{id}`                             | Recursos humanos, gerencia, admin                      | 200       |
| `POST`                          | `/api/v1/rrhh/empleados`                                                                      | Recursos humanos                                       | 201       |
| `PUT`                           | `/api/v1/rrhh/empleados/{id}`                                                                 | Recursos humanos                                       | 200       |
| `GET`                           | `/api/v1/rrhh/usuarios-disponibles`, `/rrhh/empleados/{id}/saldo-vacaciones`                  | Recursos humanos                                       | 200       |
| `GET`                           | `/api/v1/rrhh/plantillas-turno`, `/rrhh/turnos`, `/rrhh/turnos/semana`, `/rrhh/ausencias`     | Recursos humanos, admin                                | 200       |
| `POST`                          | `/api/v1/rrhh/turnos`                                                                         | Recursos humanos                                       | 201       |
| `DELETE`                        | `/api/v1/rrhh/turnos/{id}`                                                                    | Recursos humanos                                       | 204       |
| `PUT`                           | `/api/v1/rrhh/ausencias/{id}/aprobar`, `/rechazar`, `/cancelar`                               | Recursos humanos                                       | 200       |
| `GET`                           | `/api/v1/rrhh/mi-ficha`, `/rrhh/mis-turnos`, `/rrhh/mis-ausencias`                            | Todo el personal                                       | 200       |
| `POST`                          | `/api/v1/rrhh/mis-ausencias`                                                                  | Todo el personal                                       | 201       |
| `PUT`                           | `/api/v1/rrhh/mis-ausencias/{id}/cancelar`                                                    | Todo el personal                                       | 200       |
| `GET`                           | `/api/v1/admin/roles`                                                                         | Admin                                                  | 200       |
| `PUT`                           | `/api/v1/admin/usuarios/{id}/roles`                                                           | Admin                                                  | 200       |
| `POST`                          | `/api/v1/pacientes/{id}/compartir`                                                            | Médico que atiende al paciente                         | 201       |
| `DELETE`                        | `/api/v1/pacientes/{id}/compartir/{medicoId}`                                                 | Médico de origen o admin                               | 204       |
| `GET`                           | `/api/v1/pacientes/{id}/compartidos`                                                          | Médico con acceso o admin                              | 200       |
| `POST`                          | `/api/v1/pagos/qr`                                                                            | Paciente                                               | 201       |
| `GET`                           | `/api/v1/pagos/qr/{codigo}`                                                                   | Paciente dueño                                         | 200       |
| `POST`                          | `/api/v1/pagos/qr/{codigo}/simular-confirmacion`                                              | Paciente dueño                                         | 200       |
| `DELETE`                        | `/api/v1/pagos/qr/{codigo}`                                                                   | Paciente dueño                                         | 204       |
| `PUT`                           | `/api/v1/citas/{id}/reprogramar`                                                              | Paciente dueño o admin                                 | 200       |
| `POST`                          | `/api/v1/citas/{id}/pago`                                                                     | Paciente dueño                                         | 201       |
| `POST`                          | `/api/v1/citas/{id}/atencion`                                                                 | Médico de la cita                                      | 201       |
| `PUT`                           | `/api/v1/citas/{id}/atencion`                                                                 | Médico de la cita                                      | 200       |
| `GET`                           | `/api/v1/citas/{id}/atencion`                                                                 | Paciente dueño, médico o admin                         | 200       |
| `POST`                          | `/api/v1/citas/{id}/valoracion`                                                               | Paciente dueño                                         | 201       |
| `GET`                           | `/api/v1/citas/{id}/valoracion`                                                               | Paciente dueño, médico o admin                         | 200       |
| `GET`                           | `/api/v1/medicos/{id}/valoraciones`                                                           | Autenticado                                            | 200       |
| `GET`                           | `/api/v1/medicos/me/pacientes`                                                                | Médico                                                 | 200       |
| `GET`                           | `/api/v1/pacientes/{id}/historia-clinica`                                                     | Médico con citas del paciente o admin                  | 200       |
| `GET`                           | `/api/v1/historia-clinica`                                                                    | Paciente                                               | 200       |
| `PUT`                           | `/api/v1/historia-clinica/datos-medicos`                                                      | Paciente                                               | 200       |
| `GET`                           | `/api/v1/notificaciones`                                                                      | Autenticado                                            | 200       |
| `GET`                           | `/api/v1/notificaciones/resumen`                                                              | Autenticado                                            | 200       |
| `GET`                           | `/api/v1/notificaciones/stream`                                                               | Autenticado                                            | 200       |
| `PUT`                           | `/api/v1/notificaciones/{id}/leida`                                                           | Autenticado                                            | 200       |
| `PUT`                           | `/api/v1/notificaciones/leidas`                                                               | Autenticado                                            | 204       |
| `DELETE`                        | `/api/v1/notificaciones/{id}`                                                                 | Autenticado                                            | 204       |
| `GET`                           | `/api/v1/metodos-pago`                                                                        | Paciente                                               | 200       |
| `POST`                          | `/api/v1/metodos-pago`                                                                        | Paciente                                               | 201       |
| `PUT`                           | `/api/v1/metodos-pago/{id}/predeterminado`                                                    | Paciente                                               | 200       |
| `DELETE`                        | `/api/v1/metodos-pago/{id}`                                                                   | Paciente                                               | 204       |
| `GET`                           | `/api/v1/cupones/validar?codigo=&monto=`                                                      | Paciente                                               | 200       |
| `GET`                           | `/api/v1/pagos`                                                                               | Paciente                                               | 200       |
| `GET`                           | `/api/v1/pagos/{id}`                                                                          | Paciente dueño o admin                                 | 200       |
| `GET`                           | `/api/v1/admin/pagos?inicio=&fin=`                                                            | Admin                                                  | 200       |
| `GET`                           | `/api/v1/admin/cupones`                                                                       | Admin                                                  | 200       |
| `POST`                          | `/api/v1/admin/cupones`                                                                       | Admin                                                  | 201       |
| `PUT`                           | `/api/v1/admin/cupones/{id}`                                                                  | Admin                                                  | 200       |
| `DELETE`                        | `/api/v1/admin/cupones/{id}`                                                                  | Admin                                                  | 204       |
| `GET`                           | `/api/v1/admin/estadisticas`                                                                  | Admin                                                  | 200       |
| `GET`                           | `/api/v1/admin/usuarios?rol=&q=`                                                              | Admin                                                  | 200       |
| `PUT`                           | `/api/v1/admin/usuarios/{id}/estado`                                                          | Admin                                                  | 200       |
| `GET`                           | `/api/v1/perfil/preferencias`                                                                 | Autenticado                                            | 200       |
| `PUT`                           | `/api/v1/perfil/preferencias`                                                                 | Autenticado                                            | 200       |
| `PUT`                           | `/api/v1/perfil/avatar`                                                                       | Autenticado                                            | 200       |
| `DELETE`                        | `/api/v1/perfil/avatar`                                                                       | Autenticado                                            | 204       |
| `GET`                           | `/api/v1/perfil/sesiones`                                                                     | Autenticado                                            | 200       |
| `DELETE`                        | `/api/v1/perfil/sesiones/{id}`                                                                | Autenticado                                            | 204       |
| `DELETE`                        | `/api/v1/perfil/sesiones`                                                                     | Autenticado                                            | 204       |
| `GET`                           | `/api/v1/perfil`                                                                              | Autenticado                                            | 200       |
| `PUT`                           | `/api/v1/perfil`                                                                              | Autenticado                                            | 200       |
| `PUT`                           | `/api/v1/perfil/password`                                                                     | Autenticado                                            | 204       |

Las fechas de los parámetros usan el formato `yyyy-MM-dd` y `fechaHora` usa `yyyy-MM-ddTHH:mm:ss`. Las rutas de creación responden con el header `Location` apuntando al recurso nuevo.

Para reservar una cita, primero inicia sesión y copia el valor de `token` de la respuesta:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"juan.perez@correo.com","password":"Paciente123!"}'
```

Luego usa ese token en la petición:

```bash
TOKEN=<token-de-la-respuesta>

curl -X POST http://localhost:8080/api/v1/citas \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"medicoId":1,"especialidadId":1,"fechaHora":"2026-10-20T10:00:00","motivo":"Control de presión arterial"}'
```

## Diagramas

Los diagramas de esta sección usan Mermaid y GitHub los muestra directamente. Primero describen la arquitectura, el modelo de datos y el ciclo de vida de una cita. Después documentan cada endpoint con un diagrama de secuencia que muestra las clases que atraviesa la petición, las consultas que ejecuta y las respuestas de error posibles.

### Diagrama de arquitectura

```mermaid
flowchart LR
    Cliente[Cliente Angular, Postman o curl] -->|HTTP| Tomcat
    subgraph API[tusalud-api]
        Tomcat --> Telemetria[HttpTelemetryFilter]
        Telemetria --> JWT[JwtAuthenticationFilter]
        JWT --> Autorizacion[Reglas por URL y PreAuthorize]
        Autorizacion --> Controladores
        Controladores --> Servicios
        Servicios --> Repositorios
        Controladores -.->|Excepciones| Errores[GlobalExceptionHandler]
    end
    Repositorios -->|JDBC| PostgreSQL[(PostgreSQL 18)]
    API -.->|OTLP HTTP 4318| Collector[OpenTelemetry Collector]
    Collector -.->|OTLP gRPC 4317| Jaeger
```

### Diagrama entidad relación

```mermaid
erDiagram
    USUARIOS ||--o{ USUARIO_ROLES : tiene
    ROLES ||--o{ USUARIO_ROLES : asignado
    USUARIOS ||--o| MEDICOS : es
    USUARIOS ||--o| PACIENTES : es
    ESPECIALIDADES ||--o{ MEDICO_ESPECIALIDADES : ofrece
    MEDICOS ||--o{ MEDICO_ESPECIALIDADES : atiende
    ESPECIALIDADES ||--o{ CITAS : tarifa
    MEDICOS ||--o{ HORARIOS_ATENCION : define
    MEDICOS ||--o{ CITAS : atiende
    PACIENTES ||--o{ CITAS : reserva

    USUARIOS {
        uuid id PK
        varchar nombre
        varchar apellido
        varchar email UK
        varchar password
        varchar telefono
        varchar estado
        timestamp created_at
    }
    ROLES {
        uuid id PK
        varchar nombre UK
    }
    USUARIO_ROLES {
        uuid usuario_id FK
        uuid rol_id FK
    }
    ESPECIALIDADES {
        uuid id PK
        varchar nombre UK
        varchar descripcion
        numeric precio_consulta
    }
    MEDICO_ESPECIALIDADES {
        uuid medico_id FK
        uuid especialidad_id FK
    }
    MEDICOS {
        uuid id PK
        varchar colegiatura UK
        varchar consultorio
        uuid usuario_id FK
    }
    PACIENTES {
        uuid id PK
        varchar dni UK
        varchar historial_clinico_numero UK
        date fecha_nacimiento
        uuid usuario_id FK
    }
    HORARIOS_ATENCION {
        uuid id PK
        varchar dia_semana
        time hora_inicio
        time hora_fin
        int duracion_cita_minutos
        uuid medico_id FK
    }
    CITAS {
        uuid id PK
        timestamp fecha_hora
        varchar motivo
        varchar estado
        numeric monto
        uuid medico_id FK
        uuid paciente_id FK
        uuid especialidad_id FK
        bigint version
    }
```

### Diagrama de estados de una cita

```mermaid
stateDiagram-v2
    [*] --> PENDIENTE: Paciente reserva
    PENDIENTE --> CONFIRMADA: Médico o admin confirma
    PENDIENTE --> ATENDIDA: Médico atiende
    CONFIRMADA --> ATENDIDA: Médico atiende
    PENDIENTE --> CANCELADA: Paciente dueño o admin cancela
    CONFIRMADA --> CANCELADA: Paciente dueño o admin cancela
    ATENDIDA --> [*]
    CANCELADA --> [*]
```

### Autenticación de una petición protegida

Todas las rutas fuera de `/api/v1/auth/**` atraviesan este flujo antes de llegar a su controlador. Los diagramas de cada endpoint parten del punto en que la petición ya está autenticada y autorizada.

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant T as HttpTelemetryFilter
    participant F as JwtAuthenticationFilter
    participant J as JwtUtils
    participant U as UserDetailsServiceImpl
    participant R as UsuarioRepository
    participant S as Spring Security
    participant K as Controlador
    C->>T: Petición con Authorization Bearer token
    T->>T: Registra headers y body en el span
    T->>F: Continúa la cadena de filtros
    F->>J: parseClaims valida firma, expiración y emisor
    alt Sin token, token inválido o expirado
        F-->>C: 401 desde JwtAuthenticationEntryPoint
    else Token válido
        F->>U: loadUserByUsername con el email del token
        U->>R: findByEmailWithRoles
        R-->>U: Usuario con sus roles
        alt Usuario inactivo o inexistente
            F-->>C: 401 con el motivo
        else Usuario activo
            F->>S: Registra la autenticación en el SecurityContextHolder
            S->>S: Evalúa reglas por URL y PreAuthorize
            alt Rol sin permiso
                S-->>C: 403 ErrorResponse
            else Rol autorizado
                S->>K: Invoca el método del controlador
            end
        end
    end
```

### Autenticación

#### POST /api/v1/auth/login

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant K as AuthController
    participant S as AuthServiceImpl
    participant M as AuthenticationManager
    participant U as UserDetailsServiceImpl
    participant R as UsuarioRepository
    participant J as JwtUtils
    C->>K: POST /api/v1/auth/login con email y password
    alt Cuerpo inválido
        K-->>C: 400 con validationErrors
    end
    K->>S: login
    S->>M: authenticate
    M->>U: loadUserByUsername
    U->>R: findByEmailWithRoles
    R-->>U: Usuario con roles
    M->>M: Compara el hash BCrypt y verifica el estado
    alt Credenciales incorrectas o cuenta inactiva
        M-->>C: 401 ErrorResponse
    else Credenciales válidas
        S->>J: generateToken
        J-->>S: JWT firmado con HMAC-SHA
        S-->>K: AuthResponse
        K-->>C: 200 con token, roles y datos del usuario
    end
```

#### POST /api/v1/auth/register

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant K as AuthController
    participant S as AuthServiceImpl
    participant UR as UsuarioRepository
    participant PR as PacienteRepository
    participant RR as RolRepository
    participant J as JwtUtils
    participant DB as PostgreSQL
    C->>K: POST /api/v1/auth/register con datos personales
    alt Cuerpo inválido
        K-->>C: 400 con validationErrors
    end
    K->>S: registrarPaciente
    S->>UR: existsByEmailIgnoreCase
    alt Email ya registrado
        S-->>C: 409 ErrorResponse
    end
    S->>PR: existsByDni
    alt DNI ya registrado
        S-->>C: 409 ErrorResponse
    end
    S->>RR: findByNombre ROLE_PACIENTE
    S->>S: Hash BCrypt de la contraseña
    S->>UR: save Usuario
    UR->>DB: INSERT usuarios y usuario_roles
    S->>PR: nextCorrelativo para HC-000001
    S->>PR: save Paciente
    PR->>DB: INSERT pacientes
    S->>J: generateToken
    S-->>K: AuthResponse
    K-->>C: 201 con token del nuevo paciente
```

### Especialidades

#### GET /api/v1/especialidades

```mermaid
sequenceDiagram
    autonumber
    actor C as Usuario autenticado
    participant K as EspecialidadController
    participant S as EspecialidadServiceImpl
    participant R as EspecialidadRepository
    participant DB as PostgreSQL
    C->>K: GET /api/v1/especialidades
    K->>S: listar
    S->>R: findAllOrdenadas
    R->>DB: SELECT ordenado por nombre
    DB-->>R: Filas
    S-->>K: Lista de EspecialidadResponse
    K-->>C: 200 con la lista
```

#### GET /api/v1/especialidades/{id}

```mermaid
sequenceDiagram
    autonumber
    actor C as Usuario autenticado
    participant K as EspecialidadController
    participant S as EspecialidadServiceImpl
    participant R as EspecialidadRepository
    C->>K: GET /api/v1/especialidades/{id}
    K->>S: obtener
    S->>R: findById
    alt No existe
        S-->>C: 404 ErrorResponse
    else Existe
        S-->>K: EspecialidadResponse
        K-->>C: 200 con la especialidad
    end
```

#### POST /api/v1/especialidades

```mermaid
sequenceDiagram
    autonumber
    actor C as Admin
    participant K as EspecialidadController
    participant S as EspecialidadServiceImpl
    participant R as EspecialidadRepository
    participant DB as PostgreSQL
    C->>K: POST /api/v1/especialidades con nombre y descripción
    alt Cuerpo inválido
        K-->>C: 400 con validationErrors
    end
    K->>S: crear
    S->>R: existsByNombreIgnoreCase
    alt Nombre duplicado
        S-->>C: 409 ErrorResponse
    else Nombre disponible
        S->>R: save
        R->>DB: INSERT especialidades
        S-->>K: EspecialidadResponse
        K-->>C: 201 con header Location
    end
```

#### PUT /api/v1/especialidades/{id}

```mermaid
sequenceDiagram
    autonumber
    actor C as Admin
    participant K as EspecialidadController
    participant S as EspecialidadServiceImpl
    participant R as EspecialidadRepository
    participant DB as PostgreSQL
    C->>K: PUT /api/v1/especialidades/{id} con nombre y descripción
    alt Cuerpo inválido
        K-->>C: 400 con validationErrors
    end
    K->>S: actualizar
    S->>R: findById
    alt No existe
        S-->>C: 404 ErrorResponse
    end
    S->>R: existsByNombreIgnoreCaseAndIdNot
    alt Otra especialidad usa ese nombre
        S-->>C: 409 ErrorResponse
    else Nombre disponible
        S->>S: Actualiza la entidad gestionada
        S->>DB: UPDATE especialidades al confirmar la transacción
        K-->>C: 200 con la especialidad actualizada
    end
```

#### DELETE /api/v1/especialidades/{id}

```mermaid
sequenceDiagram
    autonumber
    actor C as Admin
    participant K as EspecialidadController
    participant S as EspecialidadServiceImpl
    participant R as EspecialidadRepository
    participant DB as PostgreSQL
    C->>K: DELETE /api/v1/especialidades/{id}
    K->>S: eliminar
    S->>R: findById
    alt No existe
        S-->>C: 404 ErrorResponse
    end
    S->>R: tieneMedicosAsignados
    alt Tiene médicos asignados
        S-->>C: 409 ErrorResponse
    else Sin médicos
        S->>R: delete
        R->>DB: DELETE especialidades
        K-->>C: 204 sin contenido
    end
```

### Médicos y horarios

#### GET /api/v1/medicos

```mermaid
sequenceDiagram
    autonumber
    actor C as Usuario autenticado
    participant K as MedicoController
    participant S as MedicoServiceImpl
    participant ER as EspecialidadRepository
    participant MR as MedicoRepository
    C->>K: GET /api/v1/medicos con especialidadId opcional
    K->>S: listar
    alt Sin especialidadId
        S->>MR: findAllWithDetails
    else Con especialidadId
        S->>ER: existsById
        alt Especialidad inexistente
            S-->>C: 404 ErrorResponse
        end
        S->>MR: findByEspecialidadId solo médicos activos
    end
    MR-->>S: Médicos con usuario y especialidad en una sola consulta
    S-->>K: Lista de MedicoResponse
    K-->>C: 200 con la lista
```

#### GET /api/v1/medicos/{id}

```mermaid
sequenceDiagram
    autonumber
    actor C as Usuario autenticado
    participant K as MedicoController
    participant S as MedicoServiceImpl
    participant MR as MedicoRepository
    C->>K: GET /api/v1/medicos/{id}
    K->>S: obtener
    S->>MR: findByIdWithDetails
    alt No existe
        S-->>C: 404 ErrorResponse
    else Existe
        S-->>K: MedicoResponse
        K-->>C: 200 con el médico
    end
```

#### POST /api/v1/medicos

```mermaid
sequenceDiagram
    autonumber
    actor C as Admin
    participant K as MedicoController
    participant S as MedicoServiceImpl
    participant ER as EspecialidadRepository
    participant UR as UsuarioRepository
    participant MR as MedicoRepository
    participant RR as RolRepository
    participant DB as PostgreSQL
    C->>K: POST /api/v1/medicos con datos del médico y su usuario
    alt Cuerpo inválido
        K-->>C: 400 con validationErrors
    end
    K->>S: crear
    S->>ER: findById especialidadId
    alt Especialidad inexistente
        S-->>C: 404 ErrorResponse
    end
    S->>UR: existsByEmailIgnoreCase
    alt Email duplicado
        S-->>C: 409 ErrorResponse
    end
    S->>MR: existsByColegiaturaIgnoreCase
    alt Colegiatura duplicada
        S-->>C: 409 ErrorResponse
    end
    S->>RR: findByNombre ROLE_MEDICO
    S->>S: Hash BCrypt y colegiatura en mayúsculas
    S->>MR: save Medico con su Usuario en cascada
    MR->>DB: INSERT usuarios, usuario_roles y medicos
    S-->>K: MedicoResponse
    K-->>C: 201 con header Location
```

#### GET /api/v1/medicos/{id}/horarios

```mermaid
sequenceDiagram
    autonumber
    actor C as Usuario autenticado
    participant K as MedicoController
    participant S as MedicoServiceImpl
    participant MR as MedicoRepository
    participant HR as HorarioAtencionRepository
    C->>K: GET /api/v1/medicos/{id}/horarios
    K->>S: listarHorarios
    S->>MR: existsById
    alt Médico inexistente
        S-->>C: 404 ErrorResponse
    else Médico existe
        S->>HR: findByMedicoId ordenado por día y hora
        S-->>K: Lista de HorarioResponse
        K-->>C: 200 con los bloques
    end
```

#### POST /api/v1/medicos/{id}/horarios

```mermaid
sequenceDiagram
    autonumber
    actor C as Admin
    participant K as MedicoController
    participant S as MedicoServiceImpl
    participant MR as MedicoRepository
    participant HR as HorarioAtencionRepository
    participant DB as PostgreSQL
    C->>K: POST /api/v1/medicos/{id}/horarios con día, horas y duración
    alt Cuerpo inválido
        K-->>C: 400 con validationErrors
    end
    K->>S: agregarHorario
    S->>MR: findById
    alt Médico inexistente
        S-->>C: 404 ErrorResponse
    end
    S->>S: Valida que el fin sea posterior al inicio y que quepa una cita
    alt Rango inválido
        S-->>C: 409 ErrorResponse
    end
    S->>HR: existsSolapamiento mismo médico y día
    alt Se cruza con otro bloque
        S-->>C: 409 ErrorResponse
    else Bloque válido
        S->>HR: save
        HR->>DB: INSERT horarios_atencion
        K-->>C: 201 con header Location
    end
```

#### DELETE /api/v1/medicos/{id}/horarios/{horarioId}

```mermaid
sequenceDiagram
    autonumber
    actor C as Admin
    participant K as MedicoController
    participant S as MedicoServiceImpl
    participant MR as MedicoRepository
    participant HR as HorarioAtencionRepository
    participant DB as PostgreSQL
    C->>K: DELETE /api/v1/medicos/{id}/horarios/{horarioId}
    K->>S: eliminarHorario
    S->>MR: findById
    alt Médico inexistente
        S-->>C: 404 ErrorResponse
    end
    S->>HR: findById horarioId
    alt Bloque inexistente o de otro médico
        S-->>C: 404 ErrorResponse
    else Bloque del médico
        S->>HR: delete
        HR->>DB: DELETE horarios_atencion
        K-->>C: 204 sin contenido
    end
```

#### GET /api/v1/medicos/{id}/disponibilidad

```mermaid
sequenceDiagram
    autonumber
    actor C as Usuario autenticado
    participant K as MedicoController
    participant S as CitaServiceImpl
    participant MR as MedicoRepository
    participant HR as HorarioAtencionRepository
    participant CR as CitaRepository
    C->>K: GET /api/v1/medicos/{id}/disponibilidad con fecha
    K->>S: consultarDisponibilidad
    S->>MR: findByIdWithDetails
    alt Médico inexistente
        S-->>C: 404 ErrorResponse
    end
    alt Fecha pasada
        S-->>C: 409 ErrorResponse
    end
    S->>HR: findByMedicoAndDia
    S->>CR: findHorasOcupadas del día sin citas canceladas
    S->>S: Genera turnos por bloque y descarta los ocupados y los ya pasados
    S-->>K: DisponibilidadResponse
    K-->>C: 200 con las horas libres
```

#### GET /api/v1/medicos/me/agenda

```mermaid
sequenceDiagram
    autonumber
    actor C as Médico
    participant K as MedicoController
    participant S as CitaServiceImpl
    participant MR as MedicoRepository
    participant CR as CitaRepository
    C->>K: GET /api/v1/medicos/me/agenda con fecha opcional
    K->>S: agendaMedico con el usuario autenticado
    S->>MR: findByUsuarioEmail
    alt El usuario no tiene ficha de médico
        S-->>C: 404 ErrorResponse
    end
    S->>CR: findAgendaMedicoPorFecha, hoy si no se indica fecha
    CR-->>S: Citas del día con paciente, médico y especialidad
    S-->>K: Lista de CitaResponse
    K-->>C: 200 con la agenda ordenada por hora
```

### Citas

#### POST /api/v1/citas

Este es el flujo transaccional central del sistema. Todas las operaciones dentro del recuadro forman una sola transacción y cualquier excepción provoca rollback.

```mermaid
sequenceDiagram
    autonumber
    actor C as Paciente
    participant K as CitaController
    participant S as CitaServiceImpl
    participant PR as PacienteRepository
    participant MR as MedicoRepository
    participant HR as HorarioAtencionRepository
    participant CR as CitaRepository
    participant DB as PostgreSQL
    C->>K: POST /api/v1/citas con medicoId, fechaHora y motivo
    alt Cuerpo inválido o fecha no futura
        K-->>C: 400 con validationErrors
    end
    K->>S: crearCita con el paciente autenticado
    rect rgba(66, 135, 245, 0.12)
        S->>PR: findByUsuarioEmail
        alt El usuario no tiene ficha de paciente
            S-->>C: 404 ErrorResponse
        end
        S->>MR: findByIdForUpdate
        MR->>DB: SELECT FOR UPDATE bloquea la fila del médico
        alt Médico inexistente
            S-->>C: 404 ErrorResponse
        end
        alt Médico inactivo o fecha no futura
            S-->>C: 409 ErrorResponse
        end
        S->>HR: findByMedicoAndDia
        alt Sin horario ese día o la hora no encaja en un turno
            S-->>C: 409 ErrorResponse con los bloques válidos
        end
        S->>CR: existsSolapamiento y existsCitaMedicoEnFranja
        alt El médico ya tiene una cita en esa franja
            S-->>C: 409 ErrorResponse y rollback
        end
        S->>CR: existsCitaPacienteEnFranja
        alt El paciente ya tiene una cita que se cruza
            S-->>C: 409 ErrorResponse y rollback
        end
        S->>CR: save Cita PENDIENTE con el precio del médico
        CR->>DB: INSERT citas
        S->>DB: COMMIT libera el bloqueo
    end
    S-->>K: CitaResponse
    K-->>C: 201 con header Location
```

#### GET /api/v1/citas/mis-citas

```mermaid
sequenceDiagram
    autonumber
    actor C as Paciente
    participant K as CitaController
    participant S as CitaServiceImpl
    participant PR as PacienteRepository
    participant CR as CitaRepository
    C->>K: GET /api/v1/citas/mis-citas con inicio y fin opcionales
    K->>S: listarMisCitas
    S->>PR: findByUsuarioEmail
    alt El usuario no tiene ficha de paciente
        S-->>C: 404 ErrorResponse
    end
    S->>S: Usa hoy y treinta días después si faltan las fechas
    alt Inicio posterior al fin
        S-->>C: 409 ErrorResponse
    else Rango válido
        S->>CR: findByPacienteAndRango ordenado por fecha
        S-->>K: Lista de CitaResponse
        K-->>C: 200 con las citas
    end
```

#### GET /api/v1/citas/{id}

```mermaid
sequenceDiagram
    autonumber
    actor C as Paciente, médico o admin
    participant K as CitaController
    participant S as CitaServiceImpl
    participant CR as CitaRepository
    C->>K: GET /api/v1/citas/{id}
    K->>S: obtenerCita
    S->>CR: findByIdWithDetails
    alt No existe
        S-->>C: 404 ErrorResponse
    end
    S->>S: Verifica que sea admin, el paciente dueño o el médico de la cita
    alt Sin relación con la cita
        S-->>C: 403 ErrorResponse
    else Autorizado
        S-->>K: CitaResponse
        K-->>C: 200 con la cita
    end
```

#### PUT /api/v1/citas/{id}/cancelar

```mermaid
sequenceDiagram
    autonumber
    actor C as Paciente dueño o admin
    participant K as CitaController
    participant S as CitaServiceImpl
    participant CR as CitaRepository
    participant DB as PostgreSQL
    C->>K: PUT /api/v1/citas/{id}/cancelar
    K->>S: cancelarCita
    S->>CR: findByIdWithDetails
    alt No existe
        S-->>C: 404 ErrorResponse
    end
    alt No es el paciente dueño ni admin
        S-->>C: 403 ErrorResponse
    end
    alt Estado distinto de PENDIENTE o CONFIRMADA
        S-->>C: 409 ErrorResponse
    end
    alt Paciente cancelando una cita ya pasada
        S-->>C: 409 ErrorResponse
    else Cancelación válida
        S->>CR: saveAndFlush con estado CANCELADA
        CR->>DB: UPDATE citas con control de version
        K-->>C: 200 con la cita cancelada
    end
```

#### PUT /api/v1/citas/{id}/confirmar

```mermaid
sequenceDiagram
    autonumber
    actor C as Médico de la cita o admin
    participant K as CitaController
    participant S as CitaServiceImpl
    participant CR as CitaRepository
    participant DB as PostgreSQL
    C->>K: PUT /api/v1/citas/{id}/confirmar
    K->>S: confirmarCita
    S->>CR: findByIdWithDetails
    alt No existe
        S-->>C: 404 ErrorResponse
    end
    alt No es el médico de la cita ni admin
        S-->>C: 403 ErrorResponse
    end
    alt Estado distinto de PENDIENTE
        S-->>C: 409 ErrorResponse
    else Confirmación válida
        S->>CR: saveAndFlush con estado CONFIRMADA
        CR->>DB: UPDATE citas con control de version
        K-->>C: 200 con la cita confirmada
    end
```

#### PUT /api/v1/citas/{id}/atender

```mermaid
sequenceDiagram
    autonumber
    actor C as Médico de la cita
    participant K as CitaController
    participant S as CitaServiceImpl
    participant CR as CitaRepository
    participant DB as PostgreSQL
    C->>K: PUT /api/v1/citas/{id}/atender
    K->>S: atenderCita
    S->>CR: findByIdWithDetails
    alt No existe
        S-->>C: 404 ErrorResponse
    end
    alt No es el médico de la cita
        S-->>C: 403 ErrorResponse
    end
    alt Estado distinto de PENDIENTE o CONFIRMADA
        S-->>C: 409 ErrorResponse
    else Atención válida
        S->>CR: saveAndFlush con estado ATENDIDA
        CR->>DB: UPDATE citas con control de version
        K-->>C: 200 con la cita atendida
    end
```

### Auditoría

#### GET /api/v1/admin/auditoria/citas-por-estado

```mermaid
sequenceDiagram
    autonumber
    actor C as Admin
    participant K as AdminController
    participant S as AuditoriaServiceImpl
    participant CR as CitaRepository
    participant DB as PostgreSQL
    C->>K: GET /api/v1/admin/auditoria/citas-por-estado
    K->>S: contarCitasPorEstado
    S->>CR: contarPorEstado
    CR->>DB: SELECT estado y COUNT agrupado por estado
    CR-->>S: CitasPorEstadoResponse creados por la consulta JPQL
    K-->>C: 200 con la cantidad por estado
```

#### GET /api/v1/admin/auditoria/citas

```mermaid
sequenceDiagram
    autonumber
    actor C as Admin
    participant K as AdminController
    participant S as AuditoriaServiceImpl
    participant CR as CitaRepository
    C->>K: GET /api/v1/admin/auditoria/citas con inicio y fin opcionales
    K->>S: listarCitas
    S->>S: Usa treinta días antes y después de hoy si faltan las fechas
    alt Inicio posterior al fin
        S-->>C: 409 ErrorResponse
    else Rango válido
        S->>CR: findAllByRango con médico y paciente en una sola consulta
        S-->>K: Lista de CitaResponse
        K-->>C: 200 con las citas del rango
    end
```

### Perfil

`PerfilController` permite a cualquier usuario autenticado consultar y mantener sus propios datos. La identidad sale siempre del token, de modo que nadie puede leer ni modificar el perfil de otra persona.

| Endpoint                      | Comportamiento                                                                                                                                                                                                                                                         |
| ----------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `GET /api/v1/perfil`          | Devuelve nombre, apellido, email, teléfono, roles y fecha de alta, más el DNI, la fecha de nacimiento y la historia clínica si es paciente, o la colegiatura, el consultorio y sus especialidades si es médico, además de si tiene activa la verificación en dos pasos |
| `PUT /api/v1/perfil`          | Actualiza nombre, apellido y teléfono. Un paciente puede cambiar además su fecha de nacimiento y un médico su consultorio. El email, el DNI y la colegiatura no se modifican                                                                                           |
| `PUT /api/v1/perfil/password` | Verifica la contraseña actual y guarda la nueva cifrada con BCrypt. Responde 409 si la actual no coincide o si la nueva es igual a la actual, y 400 si la nueva no tiene entre 8 y 72 caracteres                                                                       |

### Notificaciones en tiempo real

Los avisos se guardan en la tabla `notificaciones` y se entregan al navegador por Server-Sent Events, sin sondeo constante. `NotificacionServiceImpl` publica un `NotificacionCreadaEvent` y `NotificacionStreamServiceImpl` lo envía a las conexiones abiertas del usuario solo después de confirmar la transacción, de modo que nunca se anuncia algo que luego se revierta.

| Evento SSE     | Contenido                                              |
| -------------- | ------------------------------------------------------ |
| `conectado`    | Confirma que el flujo quedó abierto                    |
| `notificacion` | El JSON de cada aviso nuevo                            |
| `ping`         | Latido cada 25 segundos para mantener viva la conexión |

El navegador no puede enviar cabeceras con `EventSource`, así que el frontend abre el flujo con `fetch` y el token en la cabecera `Authorization`. Si la conexión se cae, reconecta con espera exponencial de 1 a 30 segundos, y solo consulta el contador cada 2 minutos mientras el flujo esté caído. El filtro de telemetría HTTP excluye este endpoint porque buffereaba el cuerpo de la respuesta, y nginx lo atiende sin buffering y con un timeout de una hora.

Los avisos se generan al crear, confirmar, cancelar, atender y reprogramar una cita, al registrar o reembolsar un pago, al cambiar la contraseña y al iniciar sesión desde un dispositivo nuevo. Una tarea programada cada 15 minutos crea los recordatorios según la antelación que eligió cada usuario en sus preferencias, que se elige de una lista de 15 minutos a 3 días y se guarda en minutos.

### Sesiones y alertas de inicio de sesión

Cada login crea una fila en `sesiones_usuario` y el JWT incluye su identificador en el claim `jti`. El filtro de autenticación rechaza un token cuya sesión esté revocada o vencida, de modo que cerrar una sesión remota o desactivar una cuenta surte efecto al instante aunque el token no haya expirado. Los tokens sin `jti` siguen siendo válidos.

### Pagos, tarjetas y cupones

- Las tarjetas se validan con el algoritmo de Luhn y solo se guardan la marca, los últimos cuatro dígitos y el vencimiento. El número completo y el código de seguridad nunca se almacenan.
- Un cupón puede ser de porcentaje o de monto fijo, con fechas de vigencia y un máximo de usos opcional. El endpoint de validación informa el descuento sin cobrar nada.
- Pagar una cita genera un comprobante con formato `B001-000001` y notifica al paciente. Cancelar una cita pagada marca el pago como reembolsado.

### Historia clínica

El médico registra la atención de una cita con diagnóstico, tratamiento, indicaciones, signos vitales y receta. Registrarla marca la cita como atendida. El paciente consulta su historia completa y mantiene su grupo sanguíneo, alergias y antecedentes. Un médico solo abre la historia de pacientes con los que tiene o tuvo una cita, y un administrador puede abrir cualquiera.

### Reprogramación y valoraciones

Reprogramar aplica las mismas validaciones de horario y solapamiento que la reserva, excluyendo la propia cita, y deja la cita en estado pendiente. Los pacientes valoran de 1 a 5 las citas atendidas, una sola vez por cita, y el promedio del médico se redondea a un decimal.

### Tarifas por especialidad

El precio de la consulta ya no pertenece al médico sino a la especialidad. Un médico puede atender varias especialidades y cada cita guarda la especialidad elegida y el precio vigente al reservar, de modo que un cambio de tarifa solo afecta a las nuevas reservas. El administrador edita el precio con `PUT /api/v1/especialidades/{id}` y las especialidades de un médico con `PUT /api/v1/medicos/{id}/especialidades`.

### Roles de usuario

El administrador consulta los roles con el número de usuarios de cada uno y reemplaza los roles de una persona. Un usuario debe conservar al menos un rol, el rol médico exige un perfil médico y el rol paciente un perfil de paciente, un administrador no puede quitarse a sí mismo el rol de administrador y el sistema nunca queda sin administradores. Al cambiar los roles se cierran las sesiones de la persona afectada y se le notifica.

### Pacientes compartidos

Un médico que atiende a un paciente puede compartirlo con otro colega con una nota. El colega recibe un aviso, ve al paciente en su lista marcado como compartido y puede abrir su historia clínica. Quien compartió, o un administrador, puede dejar de compartirlo en cualquier momento.

### Pagos con Yape y Plin

El pago con billetera genera una intención de pago con un código único, un payload para el QR y un vencimiento de 5 minutos. El cupón se valida al generar el QR y se consume al confirmar. La confirmación pasa por la interfaz `ProveedorBilletera`, cuya implementación simulada confirma el pago con `POST /api/v1/pagos/qr/{codigo}/simular-confirmacion`, habilitado con `app.pagos.simulacion-habilitada` y desactivado en producción. Para integrar un proveedor real basta con otra implementación de esa interfaz que confirme el pago desde su webhook. Al confirmar se crea el pago con su comprobante, igual que con tarjeta. El frontend dibuja el QR en el navegador y consulta el estado cada 3 segundos mientras el diálogo está abierto.

### Verificación en dos pasos

Cualquier usuario puede activar un segundo factor con una aplicación autenticadora. La implementación sigue el estándar TOTP con SHA-1, 6 dígitos, periodos de 30 segundos y una ventana de un periodo hacia cada lado, sin dependencias externas. El secreto se guarda cifrado con AES-GCM usando `MFA_CLAVE_CIFRADO`, que en producción es obligatoria. Al activar se entregan 8 códigos de recuperación de un solo uso, guardados como hash.

Con la verificación activa, `POST /api/v1/auth/login` responde con `mfaRequerido` y un `mfaToken` de 5 minutos, sin token de acceso, y `POST /api/v1/auth/mfa/verificar` lo cambia por la sesión cuando recibe un código válido o uno de recuperación. Cinco fallos seguidos bloquean el intento durante 5 minutos con un 429. Un `mfaToken` nunca sirve como Bearer.

### Renovación del token

El token de acceso vive 15 minutos con `JWT_EXPIRATION_MS` y el refresh token 7 días con `JWT_REFRESH_EXPIRATION_MS`. El refresh token es opaco, se guarda solo como hash SHA-256 y rota en cada uso con `POST /api/v1/auth/refresh`. La sesión del usuario se conserva entre renovaciones, así que la lista de sesiones sigue coherente. Si llega un refresh token que ya fue rotado, se revoca toda la familia, se cierra la sesión y se avisa al usuario. Cerrar la sesión, revocarla, desactivar al usuario o cambiar sus roles revocan sus refresh tokens y una tarea programada purga los vencidos.

El frontend renueva el token un minuto antes de que venza y, si una petición recibe un 401, renueva y la repite una vez. Las peticiones simultáneas comparten una sola renovación para no disparar la detección de reuso. Si el refresh token es rechazado, la sesión se cierra.

### Calendario de citas

`GET /api/v1/medicos/{id}/disponibilidad/rango` devuelve las horas libres de hasta 62 días en una sola petición y `GET /api/v1/medicos/me/agenda` acepta un rango de fechas. Cada cita informa su `duracionMinutos`. Reservar o reprogramar rechaza con 409 una cita que se cruce con otra cita activa del paciente, aunque sea con otro médico.

### Planificador de eventos

Cada usuario tiene su propia agenda de eventos personales en la tabla `eventos_calendario`. Los tipos de evento dependen del rol. Un paciente solo agenda eventos personales y recordatorios, un administrador añade reuniones y tareas y un médico puede crear además bloqueos de agenda. Crear o editar un tipo que el rol no permite responde 403 y `GET /api/v1/calendario/tipos` devuelve los tipos del usuario. Tiene título, notas, lugar, color opcional, duración de hasta 31 días, la marca de todo el día y un aviso previo opcional en minutos. Nadie ve ni modifica los eventos de otra persona.

| Endpoint                                         | Comportamiento                                                        |
| ------------------------------------------------ | --------------------------------------------------------------------- |
| `GET /api/v1/calendario/eventos?inicio=&fin=`    | Eventos que se solapan con el rango, de hasta 100 días                |
| `POST /api/v1/calendario/eventos`                | Crea el evento y responde 201 con `Location`                          |
| `PUT /api/v1/calendario/eventos/{id}`            | Reemplaza los datos del evento                                        |
| `PUT /api/v1/calendario/eventos/{id}/mover`      | Cambia el inicio y el fin, y es lo que usa el arrastre del calendario |
| `PUT /api/v1/calendario/eventos/{id}/completado` | Marca una tarea como completada o pendiente                           |
| `DELETE /api/v1/calendario/eventos/{id}`         | Elimina el evento                                                     |

Un bloqueo quita de la disponibilidad del médico los turnos que se solapan con él. Reservar o reprogramar sobre un bloqueo responde 409, y crear o mover un bloqueo sobre una cita pendiente o confirmada también responde 409. La tarea programada de recordatorios crea una notificación `RECORDATORIO_EVENTO` cuando faltan los minutos elegidos para el inicio, una sola vez, y mover el evento reinicia el aviso.

### Personal de la institución

Además de pacientes, médicos y administradores existen once roles de personal: `ROLE_LABORATORIO`, `ROLE_FARMACIA`, `ROLE_ALMACEN`, `ROLE_RECEPCION`, `ROLE_CAJA`, `ROLE_ENFERMERIA`, `ROLE_GERENCIA`, `ROLE_FINANZAS`, `ROLE_OPERACIONES`, `ROLE_RRHH` y `ROLE_CALIDAD`. El administrador los registra con `POST /api/v1/admin/personal` indicando uno o más de esos roles o el de administrador, y los asigna o cambia después desde la gestión de roles. Ese alta no admite roles de médico ni de paciente porque cada uno tiene su propio registro. Cada rol entra a su módulo al iniciar sesión, solo ve en el menú lo que le corresponde y puede agendar reuniones, tareas, recordatorios y eventos personales en el calendario.

### Almacén e inventario

El almacén controla productos de tipo medicamento, insumo, reactivo y material. Cada producto tiene código único, precio de venta, stock mínimo y la marca de si exige receta. El stock se calcula con la suma de sus lotes no vencidos y cada lote guarda número, vencimiento, cantidad, costo y proveedor.

- Un ingreso registra una compra con varios ítems, crea un lote por ítem y deja un movimiento de entrada. Repetir el número de lote de un producto responde 409.
- Las salidas descuentan por FEFO, es decir primero el lote que vence antes. Los lotes vencidos no se usan salvo en la baja por vencimiento, y un stock insuficiente responde 409 sin descontar nada.
- El kardex lista los movimientos con filtros por producto, tipo y fechas, y los ajustes positivos suman stock a un lote existente.
- Las alertas agrupan los productos bajo el mínimo, los lotes que vencen en 60 días y los vencidos. Una tarea diaria a las 7:00 avisa a los usuarios de almacén sin repetir el aviso en el mismo día.

El almacén y el administrador gestionan todo. La farmacia consulta productos, lotes y alertas y el laboratorio solo ve insumos y reactivos. `InventarioService` expone el descuento por FEFO y la devolución de stock para que otros módulos lo reutilicen.

### Farmacia

La farmacia atiende las recetas que registran los médicos al atender una cita y también vende productos sin receta. `GET /api/v1/farmacia/recetas` lista las recetas de los últimos 90 días con su estado pendiente, parcial o dispensada, y `POST /api/v1/farmacia/dispensaciones` emite el comprobante con formato `F001-000001`.

- Un producto que exige receta solo se vende con un ítem de receta del paciente que todavía no fue dispensado. La venta libre acepta únicamente productos sin receta y se registra a nombre de Cliente general.
- El stock de toda la dispensación se valida antes de descontar, así que un fallo no deja descuentos parciales.
- Anular una dispensación devuelve el stock a sus lotes, reabre los ítems de la receta y deja el comprobante anulado. Anularla dos veces responde 409.
- Dispensar avisa al paciente, que ve sus entregas en Mis medicamentos con `GET /api/v1/farmacia/mis-dispensaciones`.

### Laboratorio clínico

El laboratorio mantiene un catálogo de exámenes con sus parámetros y valores de referencia. Un médico solicita una orden para un paciente que atiende o que le compartieron, y la orden avanza por los estados solicitada, muestra tomada, en proceso, completada o cancelada.

- El personal de laboratorio registra la toma de muestra, captura los resultados de cada examen y los valida. Los parámetros numéricos deben ser números y el sistema marca cada valor que cae fuera de su rango de referencia.
- Los resultados guardan una copia del nombre, la unidad y el rango del parámetro, de modo que editar el catálogo después no altera lo ya informado.
- Validar completa la orden y avisa al paciente y al médico solicitante. El paciente solo accede a una orden cuando está completada y puede seguir el avance de las demás desde Mis resultados.
- La cola ordena primero las órdenes urgentes pendientes. Cancelar es posible hasta antes de completar la orden y una orden completada responde 409.

### Reprogramación de citas

Reprogramar tiene su propia pantalla en `/citas/:id/reprogramar`. Muestra el calendario de planificación en modo de selección con los huecos libres del médico, la cita actual resaltada y las demás citas del paciente como horas bloqueadas, y confirma con `PUT /api/v1/citas/{id}/reprogramar`. Solo se reprograman citas pendientes o confirmadas que aún no ocurrieron.

### Recepción y turnos

Recepción es la puerta de entrada de la clínica. Desde `/recepcion` el personal ve las citas de hoy, registra la llegada de cada paciente y administra la cola de espera.

- Registrar la llegada de una cita pendiente o confirmada de hoy crea un turno con numeración por día y prioridad: `A-001` normal, `P-001` preferente y `U-001` urgente. La cola ordena primero los urgentes, luego los preferentes y dentro de cada grupo por hora de llegada. Una segunda llegada de la misma cita responde 409.
- Los pacientes sin cita reciben un turno directo con `POST /api/v1/recepcion/turnos`. Si ya tienen un turno abierto hoy, la respuesta es 409.
- El turno avanza por los estados en espera, llamado, en atención, atendido, ausente y cancelado. Un médico solo opera los turnos que le corresponden y su cola siempre se filtra a los suyos. Cualquier transición inválida responde 409.
- Llegar avisa al médico con `PACIENTE_EN_ESPERA` y llamar avisa al paciente con `TURNO_LLAMADO`, indicando número y consultorio.
- `/recepcion/pantalla` es la sala de espera: muestra los turnos llamados y se actualiza sola.
- El mostrador registra pacientes sin que existan antes con `POST /api/v1/recepcion/pacientes`, que entrega una contraseña temporal una sola vez, y reserva citas con `POST /api/v1/recepcion/citas`, que aplica las mismas reglas de horario y solapes que la reserva del paciente.

### Caja y facturación

Caja cobra las consultas y los servicios, emite comprobantes y cierra cada turno con su diferencia.

- El cajero abre su caja con un fondo inicial. No puede haber dos cajas abiertas del mismo cajero, y cobrar sin caja abierta responde 409.
- Las cuentas por cobrar reúnen las citas atendidas sin pagar y las órdenes de laboratorio sin cobrar. Cobrar una cita deja su pago registrado y la cita figura como pagada para el paciente. También existe el cobro libre de servicios con ítems propios.
- Los comprobantes son boletas con serie `B001` y facturas con serie `F001`, con el IGV de 18% incluido en el precio. Las facturas exigen RUC.
- Los métodos de pago son efectivo, tarjeta, Yape, Plin y transferencia.
- Anular un comprobante exige un motivo, devuelve la cita a no pagada y responde 409 si ya estaba anulado. El cajero solo anula los de su propia caja mientras está abierta y el administrador puede anular cualquiera.
- Al cerrar, el cajero cuenta el efectivo y el sistema calcula la diferencia contra lo esperado, que avisa al administrador cuando hay faltante o sobrante.
- El reporte de ingresos entrega el total del periodo, el detalle por día, por método y por origen, y el historial de cajas lo ven el cajero y el administrador.

### Hospitalización y emergencias

Hospitalización administra las camas, los ingresos y la cola de emergencia.

- Las camas pertenecen a un área, que puede ser hospitalización, UCI, emergencia u observación, y están libres, ocupadas, en limpieza o en mantenimiento. El mapa de camas se agrupa por área y el panel muestra la ocupación. El administrador crea y edita camas, y una cama ocupada no cambia de estado a mano.
- Un médico ingresa a un paciente en una cama libre y un paciente no puede tener dos ingresos activos. Se puede trasladar de cama y el cargo se acumula por tramos de cama, con un mínimo de un día por tramo, de modo que un traslado no pierde lo ya acumulado.
- El triaje de emergencia clasifica al paciente del nivel 1 rojo al nivel 5 azul con sus signos vitales. Los niveles 1 y 2 avisan a los médicos con `EMERGENCIA_TRIAJE`. La cola se ordena por nivel y por espera, y asignar cama solo vale para camas de emergencia, UCI y observación.
- Las evoluciones son médicas o de enfermería y cada rol escribe solo las suyas. El administrador no escribe evoluciones y recibe 403.
- El alta exige diagnóstico e indicaciones, libera la cama a limpieza y responde 409 si el ingreso ya tenía alta. El paciente ve sus ingresos en `/mis-ingresos`, con las evoluciones médicas resumidas y sin signos vitales.

### Reportes y BI

Los cargos gerenciales consultan reportes agregados, de solo lectura y sin datos de pacientes. Los cuatro reportes aceptan `desde`, `hasta` y `agrupacion` con los valores `DIA`, `SEMANA` o `MES`, el rango máximo es de 366 días y un rango inválido responde 400.

- `GET /api/v1/reportes/ejecutivo` es para gerencia y administración. Entrega citas, atendidas, canceladas, tasa de ausencia, ingresos, ticket promedio, ocupación hospitalaria, espera promedio y valoración, cada uno con su valor del periodo anterior y la variación porcentual, más la serie por periodo.
- `GET /api/v1/reportes/financiero` es para finanzas, gerencia y administración. Entrega lo cobrado por día, método de pago, origen, especialidad y médico, la tasa de anulación de comprobantes, las diferencias de caja por cajero y las cuentas por cobrar. La hospitalización se informa aparte como cargo devengado y no suma al total cobrado.
- `GET /api/v1/reportes/operativo` es para operaciones, gerencia y administración. Entrega la ocupación de camas por área y su serie, la estancia promedio, las esperas de turnos con su promedio y su percentil 90, las emergencias por nivel de triaje, la demanda de citas por día y hora, el uso de la agenda por médico y el inventario crítico.
- `GET /api/v1/reportes/calidad` es para calidad, gerencia y administración. Entrega las valoraciones por médico y especialidad con su distribución, las tasas de cancelación y de ausencia por especialidad, los tiempos de entrega de laboratorio y los motivos de anulación.

Cada rol ve solo los reportes de su área y una prueba comprueba que ningún reporte contiene DNI, correo ni nombre de paciente. En la web, cada reporte tiene un selector de rango y agrupación, indicadores con su variación frente al periodo anterior, gráficos, tablas ordenables y, en el operativo, un mapa de calor de la demanda por día y hora.

### Recursos humanos

Recursos humanos administra la ficha laboral, los turnos y las ausencias del personal bajo `/api/v1/rrhh`.

- La ficha laboral vincula un usuario del personal con su cargo, área, tipo de contrato, fechas, estado, sueldo y contacto de emergencia, y cada cambio se audita sin guardar los montos. Un contrato que no es indefinido exige fecha de fin. El sueldo y el contacto de emergencia solo los ven recursos humanos y gerencia, y la administración ve la ficha sin ellos.
- Los turnos salen de plantillas de mañana, tarde y noche. La asignación responde 409 si se cruza con otro turno, si supera 48 horas semanales o si cae sobre una ausencia. La vista semanal muestra las horas de cada empleado y marca el exceso.
- Un empleado solicita ausencias sobre sí mismo, es decir vacaciones, permiso, descanso médico, licencia u otro. Recursos humanos las aprueba o las rechaza con un motivo, no puede resolver la suya propia y las vacaciones no pueden superar el saldo anual. El saldo se calcula por año calendario.
- Cualquier integrante del personal consulta sus turnos y sus ausencias y solicita nuevas desde Mi espacio, con las rutas `/mis-turnos`, `/mis-ausencias` y `/mi-ficha`. Quien no tiene ficha recibe 404 y la web muestra un estado vacío.
- Recursos humanos recibe una notificación `SOLICITUD_AUSENCIA` por cada solicitud y el empleado recibe `AUSENCIA_RESUELTA`, ambas con el enlace `/ausencias`, que cada aplicación lleva a la bandeja o a Mis ausencias según el rol.

## Manejo de errores

`GlobalExceptionHandler` traduce cada excepción a una respuesta con el mismo formato. Los errores 401 y 403 que produce Spring Security antes de llegar a un controlador usan ese mismo formato.

```json
{
  "timestamp": "2026-10-12T20:45:00",
  "status": 409,
  "error": "Conflict",
  "message": "El médico ya tiene una cita registrada en ese horario",
  "path": "/api/v1/citas"
}
```

| Estado | Causa                                                                                                               |
| ------ | ------------------------------------------------------------------------------------------------------------------- |
| 400    | Datos inválidos, JSON mal formado o parámetros incorrectos. Incluye `validationErrors` con el mensaje de cada campo |
| 401    | Falta el token, el token es inválido o expiró, las credenciales son incorrectas o la cuenta está inactiva           |
| 403    | El rol no tiene permiso o el recurso pertenece a otro usuario                                                       |
| 404    | El recurso no existe                                                                                                |
| 405    | El método HTTP no está soportado en la ruta                                                                         |
| 409    | Conflicto de horario, dato duplicado, transición de estado inválida u otra regla de negocio                         |
| 500    | Error no controlado. El detalle se registra en el log y no se expone al cliente                                     |

## Perfiles y configuración

La configuración de Spring vive en `src/main/resources` y se divide en un archivo base y un archivo por entorno. Todos usan la extensión `.yaml`.

| Archivo                  | Propósito                                                                                  |
| ------------------------ | ------------------------------------------------------------------------------------------ |
| `application.yaml`       | Configuración común: Jackson, JPA, JWT, CORS, springdoc, logging base y perfil activo      |
| `application-local.yaml` | Máquina del desarrollador, con la API en el IDE y PostgreSQL de Docker en `localhost:5432` |
| `application-dev.yaml`   | Entorno de desarrollo compartido o stack de Docker en modo desarrollo                      |
| `application-qa.yaml`    | Entorno de QA                                                                              |
| `application-test.yaml`  | Pruebas automatizadas con H2 en memoria                                                    |
| `application-prod.yaml`  | Producción                                                                                 |

| Perfil  | Base de datos                                                                     | `ddl-auto`    | Logs                                                     | `JWT_SECRET` | Datos de prueba                      |
| ------- | --------------------------------------------------------------------------------- | ------------- | -------------------------------------------------------- | ------------ | ------------------------------------ |
| `local` | PostgreSQL en `localhost:5432/tusalud` con usuario y clave `postgres` por defecto | `validate`    | DEBUG, con SQL, parámetros, Spring Web y Spring Security | Opcional     | Activos                              |
| `dev`   | PostgreSQL desde `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`, con valores por defecto | `validate`    | DEBUG de la aplicación, con SQL y parámetros             | Opcional     | Activos                              |
| `qa`    | PostgreSQL desde variables de entorno, con valores por defecto                    | `validate`    | INFO, sin SQL                                            | Opcional     | Activos                              |
| `test`  | H2 en memoria en modo PostgreSQL, con consola H2                                  | `create-drop` | DEBUG de la aplicación, SQL en WARN                      | Opcional     | Activos                              |
| `prod`  | PostgreSQL solo desde variables de entorno, sin valores por defecto               | `validate`    | WARN general e INFO de la aplicación                     | Obligatorio  | Ninguno, solo migraciones de esquema |

Ninguna respuesta de error incluye stacktraces en ningún perfil. En `prod` la consola H2 está deshabilitada y la API no arranca si falta `JWT_SECRET`, `DB_URL`, `DB_USERNAME` o `DB_PASSWORD`.

El esquema y los datos iniciales los gestiona Flyway, por eso Hibernate solo valida que las entidades coincidan con las tablas. La sección de migraciones explica cómo funciona.

### Selección del perfil

El perfil activo se resuelve en este orden de prioridad:

1. La variable de entorno `SPRING_PROFILES_ACTIVE` o la propiedad `-Dspring.profiles.active` al ejecutar la aplicación.
2. El valor que Maven escribe en `application.yaml` al compilar. El archivo contiene `spring.profiles.active: "@spring.profiles.active@"` y el filtrado de recursos lo reemplaza según el perfil de Maven.

Las pruebas siempre usan el perfil `test` gracias a `@ActiveProfiles("test")`, sin importar el perfil de Maven.

### Perfiles de Maven

El `pom.xml` define los perfiles `local`, `dev`, `qa`, `test` y `prod`. Cada uno deja en el jar el perfil de Spring del mismo nombre, y `local` es el perfil por defecto.

```bash
./mvnw spring-boot:run
./mvnw spring-boot:run -Pdev
./mvnw package -Pprod
```

Al cambiar de perfil de Maven conviene ejecutar `./mvnw clean` antes, para que no se reutilicen recursos filtrados con el perfil anterior.

## Migraciones con Flyway

Todos los perfiles que usan PostgreSQL crean y evolucionan la base con Flyway. Los scripts viven en `src/main/resources/db` y se ejecutan al arrancar la API, antes de que Hibernate valide el modelo.

| Carpeta        | Perfiles                      | Contenido                                                                                                                                                                 |
| -------------- | ----------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `db/migration` | `local`, `dev`, `qa` y `prod` | `V1__esquema_inicial.sql` con todas las tablas, restricciones e índices, y `V2__datos_base.sql` con los tres roles y las siete especialidades                             |
| `db/demo`      | `local`, `dev` y `qa`         | `V100__datos_demostracion.sql` con usuarios, médicos, horarios, pacientes, citas, atenciones, recetas, pagos, cupones, tarjetas, valoraciones y notificaciones de ejemplo |

Reglas de uso:

- Un cambio del modelo se publica como una migración nueva, por ejemplo `V3__agrega_columna.sql`. Nunca se edita una migración ya aplicada, porque Flyway verifica su checksum al arrancar.
- Los datos de demostración usan fechas relativas a la fecha de ejecución. Cada cita cae en un día y una hora de atención del médico, calculados con una función temporal de PostgreSQL.
- `baseline-on-migrate` está activo con la versión 1, de modo que una base que ya existía con el esquema anterior se adopta sin volver a ejecutar `V1`.
- La configuración `test` desactiva Flyway porque las pruebas usan H2 con el esquema generado por Hibernate y sus propios inicializadores.
- Producción solo aplica `db/migration`, de modo que nunca recibe datos de demostración.

Para volver a un estado limpio con los datos de demostración basta con recrear el volumen de PostgreSQL:

```bash
docker compose -f compose.yaml -f compose.dev.yaml -f compose.hot.yaml down -v
docker compose -f compose.yaml -f compose.dev.yaml -f compose.hot.yaml up -d --build
```

## Ejecución local

Para trabajar desde el IDE solo hace falta Java 25 y Docker para la base de datos. El Maven Wrapper descarga Maven la primera vez que se ejecuta.

1. Levanta PostgreSQL:

   ```bash
   docker compose up -d postgres
   ```

2. Ejecuta `TusaludApplication` desde el IDE o con el wrapper. El perfil `local` es el predeterminado y se conecta a `jdbc:postgresql://localhost:5432/tusalud` con usuario y clave `postgres`.

   ```bash
   ./mvnw spring-boot:run
   ```

3. Abre `http://localhost:8080/swagger-ui.html` e inicia sesión con alguno de los [usuarios de prueba](#datos-de-prueba).

Para enviar trazas a Jaeger desde el IDE, levanta también `otel-collector` y `jaeger` con Docker Compose y arranca la aplicación con el agente de OpenTelemetry:

```bash
-javaagent:opentelemetry-javaagent.jar -Dotel.service.name=tusalud-api -Dotel.exporter.otlp.endpoint=http://localhost:4318
```

## Docker

### Servicios

| Servicio         | Imagen                                         | Puerto en el host                   |
| ---------------- | ---------------------------------------------- | ----------------------------------- |
| `app`            | `tusalud-api`, construida con el `Dockerfile`  | 8080                                |
| `web`            | `tusalud-web`, construida con `web/Dockerfile` | 4200                                |
| `postgres`       | `postgres:18-alpine`                           | 5432                                |
| `otel-collector` | `otel/opentelemetry-collector-contrib:0.162.0` | 4317 gRPC, 4318 HTTP y 13133 health |
| `jaeger`         | `jaegertracing/jaeger:2.22.0`                  | 16686                               |
| `pgadmin`        | `dpage/pgadmin4`, solo con el perfil `tools`   | 5050                                |

El `Dockerfile` de la API está optimizado en tamaño, arranque y seguridad. La imagen final pesa unos 330 MB:

- Compila con el Maven Wrapper sobre `eclipse-temurin:25-jdk-alpine-3.24` usando una cache de BuildKit para el repositorio de Maven, de modo que las reconstrucciones no vuelven a descargar dependencias.
- Separa el jar de Spring Boot en capas, para que un cambio en el código solo invalide la capa de la aplicación y no la de las dependencias.
- Genera un JRE a medida con `jdeps` y `jlink`. Incluye solo los módulos que usan la aplicación y el agente de OpenTelemetry, y los datos de locale en inglés y español, que la API necesita para nombrar los días. El JRE ocupa unos 95 MB y corre sobre `alpine:3.24`.
- Descarga el agente de OpenTelemetry verificando su SHA-256, de modo que el build falla si el archivo publicado cambia.
- Activa las cabeceras de objetos compactas de Java 25, que reducen el tamaño de cada objeto en memoria.
- Corre con un usuario sin privilegios sobre archivos de solo lectura, y arranca la JVM directamente, sin un shell intermedio, para que reciba las señales de apagado.

El argumento de build `MAVEN_PROFILE` elige el perfil de Maven, `prod` por defecto, y la compilación falla si el `application.yaml` resultante no contiene ese perfil. La imagen define `SPRING_PROFILES_ACTIVE` con el mismo valor. Las opciones adicionales de la JVM se pasan con la variable `JDK_JAVA_OPTIONS`.

### Archivos de Compose

| Archivo            | Rol                                                                                                              |
| ------------------ | ---------------------------------------------------------------------------------------------------------------- |
| `compose.yaml`     | Stack completo en modo producción con PostgreSQL 18, OpenTelemetry Collector, Jaeger, la API y la aplicación web |
| `compose.dev.yaml` | Compila la API con el perfil `dev` y la aplicación web con la configuración `dev`                                |
| `compose.qa.yaml`  | Compila la API con el perfil `qa` y la aplicación web con la configuración `qa`                                  |

### Producción

```bash
docker compose up -d --build
```

`compose.yaml` pasa `JWT_SECRET`, `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` desde `.env`, con valores por defecto que sirven para una máquina local. En un servidor real esos valores deben definirse en `.env`. Para generar un secreto nuevo:

```bash
openssl rand -base64 64 | tr -d '\n'
```

En producción también debes definir `MFA_CLAVE_CIFRADO`, la clave con la que se cifran los secretos de la verificación en dos pasos. La API se niega a arrancar sin ella. La confirmación simulada de pagos con billetera viene desactivada en `compose.yaml` y activa en `compose.dev.yaml` y `compose.qa.yaml`.

En producción no se cargan datos de prueba: solo corren las migraciones de `db/migration`, que crean el esquema, los roles y las especialidades base. Los datos de demostración viven en `db/demo` y únicamente los aplican los perfiles `local`, `dev` y `qa`.

### Desarrollo y QA

```bash
docker compose -f compose.yaml -f compose.dev.yaml up -d --build
docker compose -f compose.yaml -f compose.qa.yaml up -d --build
```

Para no repetir `-f` en cada comando, define `COMPOSE_FILE` en `.env` o en la terminal, por ejemplo `COMPOSE_FILE=compose.yaml:compose.dev.yaml`. En Windows el separador es `;`.

### PostgreSQL 18

Desde la versión 18, la imagen oficial guarda los datos en un subdirectorio por versión, `/var/lib/postgresql/18/docker`. Por eso el volumen `postgres-data` se monta en `/var/lib/postgresql` y no en `/var/lib/postgresql/data`. Un volumen creado con PostgreSQL 17 no es compatible: hay que eliminarlo con `docker compose down -v`, lo que borra los datos, o migrarlos con `pg_dump` y `pg_restore`.

### pgAdmin

```bash
docker compose --profile tools up -d
```

pgAdmin queda en `http://localhost:5050` con el usuario `admin@tusalud.com` y la clave `admin`. El servidor `TuSalud (Docker)` ya viene registrado y la clave de la base es `postgres`.

### URLs

| Servicio              | URL                                     |
| --------------------- | --------------------------------------- |
| Aplicación web        | `http://localhost:4200`                 |
| API                   | `http://localhost:8080`                 |
| Swagger UI            | `http://localhost:8080/swagger-ui.html` |
| OpenAPI               | `http://localhost:8080/v3/api-docs`     |
| Jaeger                | `http://localhost:16686`                |
| OTel Collector health | `http://localhost:13133`                |
| pgAdmin               | `http://localhost:5050`                 |

### Comandos útiles

| Comando                                      | Descripción                                            |
| -------------------------------------------- | ------------------------------------------------------ |
| `docker compose ps`                          | Muestra el estado de los contenedores                  |
| `docker compose logs -f app`                 | Sigue los logs de la API                               |
| `docker compose up -d --build app`           | Reconstruye la API después de cambiar código           |
| `docker compose up -d --build --no-deps web` | Reconstruye solo la aplicación web, sin recrear la API |
| `docker compose down`                        | Detiene el stack y conserva los datos                  |
| `docker compose down -v`                     | Detiene el stack y borra los datos                     |

## Aplicación web

La carpeta `web` es un workspace de Angular 21 con varios proyectos, al estilo de un monorepo. Cada actor de la clínica tiene su propia aplicación, con sus rutas, su menú, su guía de bienvenida y sus flujos, y todas comparten librerías propias. El login es común: vive en un portal y, al autenticarse, el usuario es enviado a la aplicación de su rol. Todo usa componentes standalone, detección de cambios sin zone.js, SCSS, TypeScript en modo estricto y Vitest para las pruebas unitarias.

### Requisitos

- Node.js 20.19, 22.12 o 24, y npm.
- El backend en ejecución en `http://localhost:8080` para consumir la API desde el entorno local.

### Estructura

```text
web/
├── angular.json              Proyectos: portal, catorce aplicaciones por rol y tres librerías
├── environments/             Configuración de cada entorno, compartida por todas las aplicaciones
├── nginx/                    nginx y cabeceras de seguridad para la imagen Docker
├── scripts/
│   ├── serve-dist.mjs        Servidor que sirve todas las aplicaciones compiladas y reenvía /api
│   └── dev.mjs               Arranque del contenedor de desarrollo
├── e2e/                      Pruebas de extremo a extremo con Playwright
├── public/                   Archivos estáticos copiados a cada build
└── projects/
    ├── core/                 Librería @tusalud/core: sesión, clientes de la API, stores, guards,
    │                         interceptores, modelos, utilidades y enrutamiento entre aplicaciones
    ├── ui/                   Librería @tusalud/ui: componentes compartidos, el shell con menú y
    │                         avatar, el calendario y los estilos globales, un entry point por pieza
    ├── features/             Librería @tusalud/features: un entry point por dominio, con las rutas
    │                         y pantallas de citas, hospitalización, laboratorio, caja y los demás
    ├── portal/               Login, registro, verificación en dos pasos y redirección al módulo
    ├── paciente/             Aplicación del paciente
    ├── medico/               Aplicación de médicos y especialistas
    ├── admin/                Aplicación del administrador
    ├── gerencia/             Aplicación de gerencia, con todos los reportes y la plantilla
    ├── finanzas/             Aplicación de finanzas
    ├── operaciones/          Aplicación de operaciones
    ├── calidad/              Aplicación de calidad
    ├── rrhh/                 Aplicación de recursos humanos
    ├── recepcion/            Aplicación de recepción
    ├── caja/                 Aplicación de caja
    ├── enfermeria/           Aplicación de enfermería y hospitalización
    ├── laboratorio/          Aplicación del laboratorio
    ├── farmacia/             Aplicación de la farmacia
    └── almacen/              Aplicación del almacén
```

Cada aplicación tiene solo cinco archivos propios: `app.config.ts`, `app.routes.ts`, `app.id.ts`, `nav.config.ts` y su prueba de humo. Todo lo demás viene de las librerías.

### Aplicaciones por rol

Todas las aplicaciones se sirven bajo el mismo origen, cada una en su ruta base, y el portal en la raíz. Como comparten el origen, comparten también la sesión guardada en `localStorage`.

| Rol              | Aplicación    | Ruta base       | Página de inicio                  |
| ---------------- | ------------- | --------------- | --------------------------------- |
| Sin sesión       | `portal`      | `/`             | `/auth/login`                     |
| Paciente         | `paciente`    | `/paciente/`    | `/paciente/citas`                 |
| Médico           | `medico`      | `/medico/`      | `/medico/agenda`                  |
| Administrador    | `admin`       | `/admin/`       | `/admin/panel`                    |
| Gerencia         | `gerencia`    | `/gerencia/`    | `/gerencia/reportes/ejecutivo`    |
| Finanzas         | `finanzas`    | `/finanzas/`    | `/finanzas/reportes/financiero`   |
| Operaciones      | `operaciones` | `/operaciones/` | `/operaciones/reportes/operativo` |
| Calidad          | `calidad`     | `/calidad/`     | `/calidad/reportes/calidad`       |
| Recursos humanos | `rrhh`        | `/rrhh/`        | `/rrhh/`                          |
| Recepción        | `recepcion`   | `/recepcion/`   | `/recepcion/`                     |
| Caja             | `caja`        | `/caja/`        | `/caja/`                          |
| Enfermería       | `enfermeria`  | `/enfermeria/`  | `/enfermeria/`                    |
| Laboratorio      | `laboratorio` | `/laboratorio/` | `/laboratorio/`                   |
| Farmacia         | `farmacia`    | `/farmacia/`    | `/farmacia/`                      |
| Almacén          | `almacen`     | `/almacen/`     | `/almacen/`                       |

Cada aplicación incluye exactamente las pantallas y permisos de su rol y nada más, de modo que el paquete que descarga un paciente no contiene el código de administración. Esto reduce la superficie expuesta, pero no reemplaza la seguridad real: el control de acceso lo hace siempre el backend con `@PreAuthorize` y la relación asistencial. El frontend solo oculta lo que el rol no puede usar.

### Enrutamiento entre aplicaciones

- Tras iniciar sesión, el portal calcula el módulo principal del usuario y navega a su ruta base con una carga completa de la página. Si el usuario tiene varios roles, el módulo principal sigue la prioridad administrador, médico, enfermería, laboratorio, farmacia, almacén, recepción, caja y paciente, y el menú del avatar ofrece Cambiar de módulo con un enlace a cada aplicación a la que tiene acceso.
- Cada aplicación protege su entrada con `entradaModulo`. Sin sesión envía al login con el `returnUrl` de la pantalla pedida, y con sesión pero sin ningún rol de esa aplicación envía al módulo principal del usuario. El `returnUrl` solo se acepta si apunta a un módulo al que el rol tiene acceso.
- Dentro de una aplicación siguen funcionando los guards por rol de las rutas. Cerrar sesión desde cualquier aplicación lleva al portal.
- Los nombres de ruta de cada dominio coinciden con el de su aplicación, por ejemplo `caja` dentro de `/caja/`. Para no repetir el segmento, cada aplicación configura un prefijo oculto con `provideTusalud({ prefijoOculto })`: un serializador de URL lo quita al escribir la dirección y lo restaura al leerla. Por eso la pantalla de cobro vive en `/caja/cobrar` y no en `/caja/caja/cobrar`, y las de hospitalización de enfermería en `/enfermeria/emergencias`. El código de las pantallas no cambia.

### Crear una aplicación nueva

Una nueva especialidad con flujos propios, o un nuevo cargo, se agrega como un proyecto más:

1. Generar el proyecto con `ng generate application nombre` y copiar el `angular.json` de una aplicación existente para heredar sus configuraciones de build, la ruta base y los entornos.
2. Crear los cinco archivos de la aplicación copiándolos de una existente. `app.routes.ts` compone las rutas desde `@tusalud/features` y protege la entrada con `entradaModulo`, y `nav.config.ts` define el menú y los pasos de la guía de ese rol.
3. Registrar el módulo en `MODULOS` de `@tusalud/core` con su rol, su ruta base, su etiqueta y su icono.
4. Agregar la aplicación a `build:all` y a `test:apps` en `package.json`, a la lista de bases de `nginx/default.conf.template` y al `Dockerfile`.
5. Si la aplicación necesita una pantalla que no existe, se crea en un entry point de `@tusalud/features` o, si es solo suya, dentro del propio proyecto.

### Arquitectura del frontend

La capa `core` concentra la lógica de la aplicación y está completamente implementada y probada. Las pantallas de `features`, el shell y el layout de autenticación están implementados y conectados a la API, con estados de carga, error y vacío, mensajes en español y diseño adaptable a móvil.

Las librerías se consumen por alias de TypeScript, `@tusalud/core`, `@tusalud/ui/*` y `@tusalud/features/*`, directamente sobre su código fuente, así que una aplicación no necesita que se compile antes ninguna librería. Los entry points de `ui` y `features` son uno por pieza o por dominio para que cada aplicación incluya solo lo que importa, y `core` nunca depende de `ui` ni de `features`. `npm run build:libs` compila las tres con ng-packagr como comprobación, en ese orden.

El estado se maneja exclusivamente con signals de Angular. No se usa `zone.js`: la detección de cambios reacciona a los signals y todos los componentes usan `ChangeDetectionStrategy.OnPush`.

| Pieza                 | Responsabilidad                                                                                                                                                                                                                                 |
| --------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `AuthService`         | Guarda la sesión en un signal y expone `isAuthenticated`, `user`, `token`, `roles`, un signal por rol y `homeUrl` como signals derivados. Persiste la sesión en `localStorage`, la valida al restaurarla y la cierra sola cuando el JWT expira. |
| Clientes de la API    | Un servicio por dominio, desde `AuthApiService` hasta `HospitalizacionApiService`, con tipos estrictos y los ids como cadenas UUID.                                                                                                             |
| Stores                | Exponen los datos, el estado de carga y los errores como signals, y mantienen las listas sincronizadas después de cada alta, edición, eliminación o cambio de estado.                                                                           |
| `AsyncState`          | Base de los stores: agrupa los signals `value`, `loading`, `error` y `hasValue`, y cancela la carga anterior cuando empieza una nueva.                                                                                                          |
| `NotificationService` | Lista de notificaciones en un signal, con cierre automático.                                                                                                                                                                                    |
| `LoadingService`      | Cuenta las peticiones en curso y expone `isLoading` como signal.                                                                                                                                                                                |
| `AppShell`            | Layout común con barra superior, menú lateral, avatar, campana de notificaciones y guía. Recibe de cada aplicación su marca, su menú y sus pasos de guía con `provideAppShell` y `provideTourSteps`.                                            |

Los stores se vacían automáticamente al cerrar sesión, de modo que los datos de un usuario nunca quedan visibles para el siguiente.

### Interceptores HTTP

Las peticiones atraviesan tres interceptores funcionales, en este orden:

1. `loadingInterceptor` registra la petición en `LoadingService` mientras está en curso.
2. `authInterceptor` agrega el header `Authorization: Bearer` a las peticiones dirigidas a la API cuando hay sesión.
3. `errorInterceptor` convierte cualquier error en un `ApiError` con el mismo formato que el `ErrorResponse` del backend, muestra una notificación y cierra la sesión si el backend rechaza el token.

Una petición puede excluirse de cada interceptor con los contextos `SKIP_LOADING`, `SKIP_AUTH` y `SKIP_ERROR_NOTIFICATION`. El login y el registro usan `SKIP_AUTH` y `SKIP_ERROR_NOTIFICATION`, porque sus errores se muestran en el propio formulario.

### Rutas

Todas las pantallas se cargan de forma diferida. Los parámetros de ruta llegan a los componentes como inputs con signals gracias a `withComponentInputBinding`, y cada ruta define su título. Las rutas que siguen son las internas de cada aplicación; en el navegador van precedidas de su ruta base, y los ids de las rutas son UUID.

| Ruta                              | Acceso                  | Pantalla                        |
| --------------------------------- | ----------------------- | ------------------------------- |
| `/auth/login`                     | Sin sesión              | Inicio de sesión                |
| `/auth/registro`                  | Sin sesión              | Registro de pacientes           |
| `/`                               | Con sesión              | Redirige a la página del rol    |
| `/especialidades`                 | Admin, paciente         | Lista de especialidades         |
| `/especialidades/nueva`           | Admin                   | Alta de especialidad            |
| `/especialidades/:id/editar`      | Admin                   | Edición de especialidad         |
| `/medicos`                        | Admin, paciente         | Lista de médicos                |
| `/medicos/nuevo`                  | Admin                   | Alta de médico                  |
| `/medicos/:id`                    | Admin, paciente         | Detalle del médico              |
| `/medicos/:id/horarios`           | Admin                   | Bloques de atención             |
| `/medicos/:id/disponibilidad`     | Admin, paciente         | Horas libres del médico         |
| `/citas`                          | Paciente                | Mis citas                       |
| `/citas/nueva`                    | Paciente                | Reserva de cita                 |
| `/citas/:id`                      | Paciente, médico, admin | Detalle de la cita              |
| `/agenda`                         | Médico                  | Agenda del día                  |
| `/calendario`                     | Con sesión              | Calendario y planificador       |
| `/admin/auditoria`                | Admin                   | Reportes de auditoría           |
| `/perfil`                         | Con sesión              | Datos, seguridad, avisos y tema |
| `/notificaciones`                 | Con sesión              | Bandeja de notificaciones       |
| `/citas/:id/atencion`             | Médico                  | Registro de la atención médica  |
| `/historia-clinica`               | Paciente                | Historia clínica del paciente   |
| `/pacientes`                      | Médico, admin           | Pacientes del médico            |
| `/pacientes/:id/historia-clinica` | Médico, admin           | Historia clínica de un paciente |
| `/pagos`                          | Paciente                | Historial de pagos              |
| `/pagos/metodos`                  | Paciente                | Tarjetas guardadas              |
| `/admin/panel`                    | Admin                   | Indicadores de la clínica       |
| `/admin/usuarios`                 | Admin                   | Gestión de cuentas              |
| `/admin/pagos`                    | Admin                   | Pagos registrados               |
| `/admin/cupones`                  | Admin                   | Cupones de descuento            |
| `/acceso-denegado`                | Todos                   | Aviso de permisos insuficientes |

Quien no inició sesión va al login, y si intentaba abrir una ruta protegida, la URL viaja como `returnUrl`. Un usuario autenticado que abre la ruta base de la aplicación de otro rol vuelve a la página de inicio de su propio módulo, por ejemplo el paciente que escribe `/admin/` regresa a `/paciente/citas`. Una ruta que no existe dentro de su aplicación muestra la página no encontrada, y solo si la página de inicio de un rol es la ruta prohibida se muestra `/acceso-denegado`. El menú lateral muestra solo lo que cada rol puede abrir.

La primera vez que un usuario entra, una guía paso a paso resalta las partes de la interfaz que le corresponden según su rol. Se puede volver a abrir desde el menú del avatar o desde la tarjeta al pie del menú lateral, y el avance se guarda en las preferencias con `tourCompletado`.

Las citas se ven en lista o en un calendario propio con vistas de mes, semana, día y agenda. La vista de día y la de semana muestran una línea con la hora actual y las citas que se cruzan se resaltan y se cuentan en la cabecera. El calendario es un componente propio que se parece a Google Calendar: tiene un mini calendario lateral, filtros por tipo de calendario, una fila para los eventos de todo el día, el número de semana y el sombreado del horario en que el médico no atiende. Con la edición activa se arrastra sobre la cuadrícula para crear un evento, se mueve o se estira un evento para reprogramarlo con pasos de 15 minutos, y los atajos T, M, S, D y A llevan a hoy y a cada vista. La página `/calendario` está disponible para todos los roles y junta las citas con las reuniones, tareas y recordatorios personales, y los médicos añaden además sus bloqueos de agenda. La disponibilidad de un médico y la reserva usan el mismo componente en modo de selección. Al reservar, el paciente elige el hueco libre directamente en el calendario: los huecos que chocan con sus propias citas no aparecen, así que no puede elegir una fecha al azar.

### Validaciones

`AppValidators` replica en el cliente las reglas del backend: DNI de 8 dígitos, teléfono de 9 dígitos, contraseña de 8 a 72 caracteres, hora en formato `HH:mm`, precio mayor a cero con hasta dos decimales, duración de cita de 10 a 240 minutos, fecha de nacimiento pasada, fecha y hora de cita futura, rango de fechas y bloque de horario válido. `firstErrorMessage` traduce cada error a un mensaje en español y `applyServerErrors` coloca los `validationErrors` que devuelve el backend en los campos del formulario correspondiente.

### Interfaz y componentes

La interfaz usa PrimeNG 21 con el tema Aura personalizado con una paleta verde azulado propia del sector salud, PrimeIcons y PrimeFlex para las utilidades de maquetación. El preset y las traducciones al español de PrimeNG viven en `core/config/theme.ts` y se registran para todas las aplicaciones con `provideTusalud`, junto con el locale `es-PE` y la moneda PEN.

Solo se programaron como componentes compartidos las piezas que PrimeNG no ofrece:

| Componente      | Función                                                            |
| --------------- | ------------------------------------------------------------------ |
| `PageHeader`    | Título, icono, enlace de retorno y acciones de cada pantalla       |
| `EmptyState`    | Estado vacío o de error con acción opcional                        |
| `FormField`     | Etiqueta, mensaje de error de validación y ayuda de un campo       |
| `EstadoCitaTag` | Etiqueta con color e icono por estado de cita                      |
| `CitaActions`   | Confirmación y ejecución de cancelar, confirmar y atender una cita |

`NotificationOutlet` muestra las notificaciones de `NotificationService` como toasts de PrimeNG y `LoadingBar` muestra una barra de progreso global mientras hay peticiones en curso.

### Pruebas del frontend

Las 238 pruebas unitarias corren con Vitest, repartidas entre los proyectos, y cubren las utilidades, los validadores, `AuthService`, los interceptores, los guards, el enrutamiento entre aplicaciones, las rutas de cada aplicación con sus redirecciones por rol, los stores, la renovación del token, el calendario, la guía de bienvenida y cada uno de los endpoints de los clientes de la API. Cada aplicación tiene una prueba de humo que resuelve todos sus componentes perezosos.

```bash
npm test
```

Las 104 pruebas de extremo a extremo usan Playwright con Chromium, en escritorio y en un viewport móvil, y recorren el login, el registro, la reserva y cancelación de una cita, la agenda del médico, la auditoría, el alta, edición y baja de especialidades, el alta de médicos con sus horarios, el ciclo completo de una cita entre paciente y médico, la edición del perfil, las preferencias de recordatorio, los filtros de fechas y el cambio de contraseña, las notificaciones en tiempo real, los pagos con cupón, la historia clínica, el panel de administración, los filtros y los permisos por rol, la verificación en dos pasos, la guía de bienvenida, los pagos con Yape y Plin, la gestión de tarifas y roles, los pacientes compartidos, el calendario, el planificador de eventos con arrastre, la reprogramación, la renovación de sesión, el alta de personal los flujos de almacén, farmacia y laboratorio, los de recepción, caja y hospitalización, y los reportes gerenciales, los turnos, las ausencias y Mi espacio. Requieren el stack levantado con datos de prueba y atraviesan el portal y las aplicaciones por sus rutas base. El helper `ir` de `e2e/support.ts` construye la dirección real de cada pantalla a partir de la aplicación en la que está el usuario y de su prefijo oculto.

```bash
npm run e2e
```

### Entornos

Cada entorno del frontend coincide con un perfil del backend. El archivo `environment.ts` corresponde a producción y Angular lo reemplaza durante el build según la configuración elegida.

| Configuración | Archivo                | `production` | Optimización | Sourcemaps                           | Hash en nombres |
| ------------- | ---------------------- | ------------ | ------------ | ------------------------------------ | --------------- |
| `local`       | `environment.local.ts` | `false`      | No           | Sí                                   | No              |
| `dev`         | `environment.dev.ts`   | `false`      | No           | Sí                                   | Sí              |
| `qa`          | `environment.qa.ts`    | `true`       | Sí           | Ocultos, sin referencia en el código | Sí              |
| `production`  | `environment.ts`       | `true`       | Sí           | No                                   | Sí              |

Todos los entornos llaman a la API con la ruta relativa `/api/v1`. En local, el servidor de desarrollo de Angular reenvía esas peticiones a `http://localhost:8080` mediante `proxy.conf.json`. En Docker, nginx las reenvía al backend. Así el navegador nunca hace peticiones a otro origen y no depende de CORS.

### Comandos

| Comando                   | Descripción                                                                                                       |
| ------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| `npm run start:paciente`  | Servidor de desarrollo de una aplicación, con el entorno `local`. Hay uno por aplicación y uno para `portal`      |
| `npm run build:libs`      | Compila las librerías core, ui y features                                                                         |
| `npm run build:all`       | Compila el portal y las catorce aplicaciones en producción en `dist/<aplicación>`                                 |
| `npm run build:all:local` | Lo mismo con el entorno `local`. También existen `:dev`, `:qa` y `:prod`                                          |
| `npm run serve:dist`      | Sirve todo lo compilado en `dist` bajo sus rutas base en `http://localhost:4300`                                  |
| `npm test`                | Pruebas unitarias de todos los proyectos. Cada uno tiene su `test:core`, `test:ui`, `test:features` y `test:apps` |
| `npm run e2e`             | Pruebas de extremo a extremo con Playwright                                                                       |
| `npm run format`          | Formatea el código con Prettier                                                                                   |

Todos los comandos se ejecutan dentro de la carpeta `web`. Cada compilación genera el sitio de la aplicación en `web/dist/<aplicación>/browser`. Con `ng serve` se trabaja de una aplicación a la vez: el redirigir a otro módulo tras el login necesita que las demás también estén servidas, para eso existe `npm run serve:dist`.

### Desarrollo con recarga en caliente

`compose.hot.yaml` reemplaza la imagen de nginx por un contenedor de desarrollo que monta la carpeta `web` como volumen. Al arrancar compila una vez el portal y las catorce aplicaciones con el entorno `local` y las sirve todas en `http://localhost:4200` con `scripts/serve-dist.mjs`, que reenvía `/api` al servicio `app`. Compilar todas una vez cuesta menos de dos minutos y casi no ocupa memoria.

La recarga se activa solo en las aplicaciones en las que se trabaja con la variable `WEB_WATCH`, que acepta una lista separada por comas o `all`. Cada una de ellas ejecuta `ng build --watch` con sondeo y vuelve a compilar al guardar un archivo, y el navegador se actualiza al recargar la página. Vigilar todas las aplicaciones a la vez consume más de 13 GB de memoria con solo diez, y crece con cada una, por eso no es el comportamiento por defecto.

```bash
WEB_WATCH=paciente,caja docker compose -f compose.yaml -f compose.dev.yaml -f compose.hot.yaml up -d --build
```

La API, PostgreSQL y los datos de prueba quedan en el mismo stack. Los volúmenes `web-node-modules`, `web-dist` y `web-angular-cache` guardan las dependencias, lo compilado y la caché de Angular. Si cambia `package.json`, hay que reconstruir con `--build` y borrar el volumen de dependencias con `docker volume rm tusalud_web-node-modules`.

### Imagen Docker y Compose

El `Dockerfile` de `web` compila las librerías, el portal y las catorce aplicaciones con `node:24-alpine`, con una cache de npm entre builds, copia cada aplicación a su carpeta y sirve el resultado con `nginxinc/nginx-unprivileged:1.30-alpine-slim`, la rama estable de nginx en su variante mínima. El contenedor corre con el usuario `nginx`, sin privilegios de root, escucha en el puerto 8080 y la imagen final pesa unos 30 MB. El argumento de build `BUILD_CONFIGURATION` elige la configuración de Angular y vale `production` por defecto.

La aplicación web forma parte del stack de Compose como servicio `web` y queda disponible en `http://localhost:4200` al levantar cualquier entorno:

```bash
docker compose up -d --build
docker compose -f compose.yaml -f compose.dev.yaml up -d --build
docker compose -f compose.yaml -f compose.qa.yaml up -d --build
```

`compose.yaml` compila el frontend con la configuración `production`, y los archivos de `dev` y `qa` lo compilan con la configuración del mismo nombre. Así el frontend y el backend de un stack siempre corresponden al mismo entorno.

Para reconstruir solo el frontend después de cambiar su código, sin recrear la API:

```bash
docker compose up -d --build --no-deps web
```

nginx cumple estas funciones:

- Sirve el portal en la raíz y cada aplicación en su ruta base, como `/paciente/` o `/caja/`, y devuelve el `index.html` de la aplicación correspondiente para cualquier ruta que no sea un archivo, de modo que las rutas de Angular funcionan al recargar la página. Una ruta base sin barra final se redirige con 301.
- Reenvía `/api/` al backend definido en `API_UPSTREAM`, que en Compose es `http://app:8080`. Conserva el header `Host` original con su puerto y agrega `X-Forwarded-Host`, `X-Forwarded-For`, `X-Forwarded-Proto` y `X-Real-IP`, de modo que el header `Location` de las respuestas 201 apunta a `http://localhost:4200`. El nombre del backend se resuelve con el DNS indicado en `NGINX_RESOLVER`, que por defecto es el DNS interno de Docker, `127.0.0.11`, por lo que nginx arranca aunque la API todavía no esté disponible.
- Marca los archivos JavaScript y CSS con hash en el nombre como inmutables durante un año y obliga a revalidar el `index.html` de cada aplicación en cada visita.
- Sirve los archivos de texto ya comprimidos. El build genera una versión gzip de cada archivo JavaScript, CSS, HTML y SVG, y nginx la entrega directamente con `gzip_static`, sin comprimir en cada petición. Las respuestas de la API se comprimen al vuelo.
- Agrega las cabeceras de seguridad `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy` y `Cross-Origin-Opener-Policy`, y oculta la versión de nginx.
- Responde `ok` en `/healthz` sin registrar la petición, y el healthcheck de la imagen usa esa ruta.
- Bloquea la descarga de los archivos `.map`, de modo que los sourcemaps ocultos del build de `qa` no queden expuestos.

Como el navegador solo habla con nginx, la API se consume desde el mismo origen que la aplicación y no depende de CORS. La colección de Postman también puede ejecutarse a través del frontend:

```bash
npx newman run postman/Tusalud.postman_collection.json --env-var baseUrl=http://localhost:4200
```

## Observabilidad

La aplicación se instrumenta con el agente Java de OpenTelemetry, que el `Dockerfile` descarga y carga con `-javaagent`. El agente envía trazas, métricas y logs por OTLP al OpenTelemetry Collector, y el Collector reenvía las trazas a Jaeger.

```text
tusalud-api --OTLP HTTP 4318--> otel-collector --OTLP gRPC 4317--> jaeger
```

En la interfaz de Jaeger, `http://localhost:16686`, selecciona el servicio `tusalud-api`. Solo se guardan las peticiones a `/api/**` y cada traza muestra el recorrido completo de la petición por las capas del sistema:

```text
POST /api/v1/citas
└ JwtAuthenticationFilter.doFilterInternal
  ├ UserDetailsServiceImpl.loadUserByUsername
  │ └ UsuarioRepository.findByEmailWithRoles → SELECT
  └ CitaController.crear
    └ CitaServiceImpl.crearCita
      ├ MedicoRepository.findByIdForUpdate → SELECT ... FOR UPDATE
      ├ CitaRepository.existsSolapamiento → SELECT
      └ CitaRepository.save → INSERT
```

La configuración está repartida en tres lugares:

- `docker/otel-agent/agent.properties` desactiva la instrumentación de Hibernate, que duplicaba cada consulta, activa los spans de controladores y crea spans para el filtro JWT, el `UserDetailsService` y los métodos de las interfaces de servicio.
- `docker/otel-collector/config.yaml` aplica un muestreo de cola que conserva solo las trazas con una petición HTTP a `/api/`. Así se descartan el arranque, la carga de datos iniciales, el healthcheck y Swagger UI. Cada traza aparece en Jaeger unos cinco segundos después de la petición, que es el tiempo que el Collector espera para recibirla completa.
- `HttpTelemetryFilter` agrega al span de cada petición a `/api/**` todos los headers de request y response, en `http.request.header.<nombre>` y `http.response.header.<nombre>`, y los cuerpos completos en `http.request.body` y `http.response.body` junto con sus tamaños. No se enmascara nada: el header `Authorization`, las contraseñas y los tokens quedan visibles tal como viajan. Esta configuración está pensada para depuración y no debe usarse en un entorno compartido con datos reales.

Tomcat agrega los headers `Connection`, `Keep-Alive` y `Transfer-Encoding` al enviar la respuesta, fuera del alcance de la API de servlets, por lo que no aparecen en las trazas.

## Datos de prueba

En los perfiles `local`, `dev` y `qa` los datos se cargan con las migraciones de Flyway descritas arriba. No hay inicializadores de datos en código Java: todo dato inicial o de demostración es un script SQL versionado. Las pruebas con H2 crean sus propios datos.

| Rol      | Nombre          | Email                        | Contraseña     |
| -------- | --------------- | ---------------------------- | -------------- |
| Admin    | Administrador   | `admin@tusalud.com`          | `Admin123!`    |
| Médico   | Luis García     | `dr.garcia@tusalud.com`      | `Medico123!`   |
| Médico   | Carmen López    | `dra.lopez@tusalud.com`      | `Medico123!`   |
| Paciente | Juan Pérez      | `juan.perez@correo.com`      | `Paciente123!` |
| Paciente | María Rodríguez | `maria.rodriguez@correo.com` | `Paciente123!` |

- Especialidades: Cardiología, Pediatría, Medicina General y Dermatología.
- Luis García atiende Cardiología de lunes a viernes de 08:00 a 13:00 con citas de 30 minutos, en el Consultorio 101, por 150.00.
- Carmen López atiende Pediatría de lunes a viernes de 14:00 a 19:00 y los sábados de 08:00 a 12:00 con citas de 30 minutos, en el Consultorio 205, por 120.00.
- Se crean dos citas de ejemplo para el siguiente día hábil: Juan Pérez con Luis García a las 09:00 en estado `PENDIENTE` y María Rodríguez con Carmen López a las 15:00 en estado `CONFIRMADA`.

### Datos de demostración

`V100__datos_demostracion.sql` amplía esos datos en los perfiles `local`, `dev` y `qa`, y los scripts V101 a V108 hacen lo mismo con cada módulo.

- Cinco médicos más, de Medicina General, Dermatología, Traumatología, Ginecología y Oftalmología, cada uno con sus horarios.
- Ocho pacientes más con nombres, DNI y fechas de nacimiento ficticios.
- Veintiuna citas en los cuatro estados, repartidas desde tres semanas atrás hasta nueve días adelante. Cada cita cae en un día y una hora de atención del médico, y las fechas se calculan a partir del día en que arranca la API.

El archivo `users.txt` de la raíz lista todos los usuarios con su correo, contraseña y rol, junto con las citas de ejemplo. Para volver a un estado limpio basta con recrear el volumen de la base de datos:

```bash
docker compose -f compose.yaml -f compose.dev.yaml -f compose.hot.yaml down -v
docker compose -f compose.yaml -f compose.dev.yaml -f compose.hot.yaml up -d --build
```

## Pruebas automatizadas

```bash
./mvnw test
```

Las pruebas usan el perfil `test` con H2 en memoria y no necesitan PostgreSQL ni Docker.

| Clase                                                                   | Qué verifica                                                                                            |
| ----------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| `VirtualThreadsTest`                                                    | Que Tomcat atiende las peticiones con virtual threads                                                   |
| `TusaludApplicationTests`                                               | Que el contexto de Spring arranca                                                                       |
| `CitaRepositoryTest`                                                    | Las consultas JPQL de citas, médicos, horarios y auditoría contra datos reales                          |
| `SecurityIntegrationTest`                                               | Respuestas 401 y 403, rutas públicas, CORS, contenido del token y login con BCrypt                      |
| `CitaServiceImplTest`                                                   | Reserva de citas, conflictos de horario con rollback, permisos, transiciones de estado y disponibilidad |
| `AuthServiceImplTest`                                                   | Registro de pacientes, duplicados y login                                                               |
| `MedicoEspecialidadServiceImplTest`                                     | Especialidades, alta de médicos y validación de horarios                                                |
| `HttpTelemetryFilterTest`                                               | Que headers y cuerpos llegan completos al span                                                          |
| `RecepcionServiceTest`, `AdmisionServiceTest`, `RecepcionSeguridadTest` | Llegadas, numeración y cola de turnos, registro de pacientes y citas de mostrador, y permisos por rol   |
| `CajaServiceTest`, `CajaSeguridadTest`                                  | Apertura y cierre, cobros con IGV, anulación, cuentas por cobrar, reporte y permisos                    |
| `HospitalizacionServiceTest`, `HospitalizacionSeguridadTest`            | Camas, ingresos, traslados con cargo por tramos, triaje, evoluciones por rol, alta y permisos           |

## Flujo de trabajo con Git

Todo el desarrollo ocurre directamente sobre la rama `main`. Cada commit agrupa un bloque de archivos relacionados, de modo que el historial se lea por funcionalidad. Los mensajes siguen el formato de Conventional Commits con la descripción en español, por ejemplo `feat(api): agrega la reserva de citas`.

### Hooks de Git

Los hooks viven versionados en la carpeta `.githooks` y formatean el código antes de cada commit y lo verifican antes de cada push. Para activarlos después de clonar el repositorio:

```bash
git config core.hooksPath .githooks
npm --prefix web ci
```

El segundo comando instala Prettier, que los hooks toman de `web/node_modules`.

| Hook         | Qué hace                                                                                                                                                                                                             |
| ------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `pre-commit` | Formatea los archivos Java preparados con Spotless y el resto con Prettier, y los vuelve a agregar al commit. Prettier ignora los tipos de archivo que no reconoce.                                                  |
| `pre-push`   | Verifica que todo el código Java cumpla el formato de Spotless y que todos los archivos versionados cumplan el formato de Prettier. Si algo no está formateado, cancela el push e indica el comando para corregirlo. |

El hook `pre-commit` vuelve a agregar el archivo completo después de formatearlo. Si un archivo tenía cambios preparados solo en parte, el commit incluirá también los cambios que no estaban preparados.

### Formateadores

| Lenguaje                                        | Herramienta                       | Configuración                                                   |
| ----------------------------------------------- | --------------------------------- | --------------------------------------------------------------- |
| Java                                            | Spotless con palantir-java-format | Plugin `spotless-maven-plugin` en `pom.xml`                     |
| TypeScript, HTML y SCSS del frontend            | Prettier                          | `web/.prettierrc`, con ancho de 100 columnas y comillas simples |
| Markdown, YAML y JSON del resto del repositorio | Prettier                          | `.prettierrc` y `.prettierignore` en la raíz                    |

Para formatear todo el repositorio manualmente:

```bash
./mvnw spotless:apply
git ls-files | xargs web/node_modules/.bin/prettier --write --ignore-unknown
```

El archivo `.gitattributes` fija los finales de línea en LF para todo el repositorio, salvo los scripts `.cmd` y `.bat`, que usan CRLF. Así el formato es idéntico en Windows, Linux y macOS, sin importar el valor de `core.autocrlf` de cada equipo.

## Colección Postman

El archivo `postman/Tusalud.postman_collection.json` es la única colección del proyecto y no necesita un environment. La URL base, los tokens de cada usuario y los identificadores que se capturan durante la ejecución viven como variables de la colección. La variable `baseUrl` apunta a `http://localhost:8080`; para probar otro servidor basta con cambiar su valor. Cada petición incluye pruebas automáticas sobre el código de estado y los campos de la respuesta.

Para usarla en Postman, importa el archivo y ejecuta la colección completa con Run collection. Las carpetas están numeradas y se ejecutan en orden, porque cada una reutiliza los tokens e identificadores que capturan las anteriores:

| Carpeta                                                 | Contenido                                                                                                                 |
| ------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| `00 - Autenticación`                                    | Login de los cinco usuarios de prueba, registro de un paciente y errores 400 y 401                                        |
| `01 - Especialidades`                                   | Alta, consulta, actualización, duplicado, acceso sin permiso y eliminación                                                |
| `02 - Médicos y Horarios`                               | Listados, alta de médico, bloques de horario, solapamiento y disponibilidad                                               |
| `03 - Citas (Paciente)`                                 | Reserva en el primer turno libre, conflictos y acceso a citas ajenas                                                      |
| `04 - Citas (Médico)`                                   | Agenda, confirmación, atención y transiciones inválidas                                                                   |
| `05 - Cancelación`                                      | Cancelación de una segunda cita y cancelación repetida                                                                    |
| `06 - Auditoría (Admin)`                                | Reportes por estado y por rango, y accesos no permitidos                                                                  |
| `07 - Seguridad`                                        | Peticiones sin token y con token inválido                                                                                 |
| `08 - Perfil`                                           | Datos personales, contraseña y preferencias                                                                               |
| `09 - Tarifas, roles y pacientes compartidos`           | Tarifa por especialidad, especialidades del médico, roles y pacientes compartidos                                         |
| `10 - Pagos con Yape y Plin`                            | Reserva, generación del QR, consulta y confirmación simulada                                                              |
| `11 - Verificación en dos pasos y renovación del token` | Configuración del autenticador y errores del refresh token                                                                |
| `12 - Calendario y eventos`                             | Eventos personales, arrastre, privacidad y límites del rango                                                              |
| `13 - Personal y almacén`                               | Alta de personal, productos, proveedores, ingresos, salidas y alertas                                                     |
| `14 - Farmacia`                                         | Catálogo, venta libre, anulación y entregas del paciente                                                                  |
| `15 - Laboratorio`                                      | Orden del médico, muestra, resultados fuera de rango, validación y acceso                                                 |
| `16 - Recepción y turnos`                               | Registro de pacientes, turnos, cola, llamado, pantalla y permisos                                                         |
| `17 - Caja y facturación`                               | Apertura, cobros con IGV, factura, anulación, reporte, cierre y permisos                                                  |
| `18 - Hospitalización y emergencia`                     | Ingreso, evoluciones por rol, traslado, triaje, alta y permisos                                                           |
| `19 - Seguridad y permisos`                             | Accesos indebidos por relación, por rol y a datos clínicos, y campos que no se exponen                                    |
| `20 - Reportes y BI`                                    | Los cuatro reportes por rol, validaciones del rango, comparación con el periodo anterior y ausencia de datos de pacientes |
| `21 - Recursos humanos`                                 | Fichas, turnos con choques y exceso de horas, ausencias con aprobación, Mi espacio y permisos                             |

Desde la línea de comandos:

```bash
npx newman run postman/Tusalud.postman_collection.json
```

Para ejecutarla contra otro servidor sin editar el archivo:

```bash
npx newman run postman/Tusalud.postman_collection.json --env-var baseUrl=https://otro-servidor
```

Si los datos de prueba no están cargados, la carpeta de autenticación falla porque no encuentra a los usuarios de prueba. Los perfiles `local`, `dev` y `qa` los traen con Flyway. Un stack de producción no los trae.

La colección puede ejecutarse varias veces sobre la misma base. Genera emails, DNI, colegiaturas y nombres únicos en cada corrida y calcula la fecha de la cita buscando el primer turno libre, sin depender de identificadores fijos. Cada ejecución deja en la base un paciente, un médico y una especialidad nuevos, además de una cita atendida y otra cancelada.

## Variables de entorno

El archivo `.env.example` documenta todas las variables con valores para desarrollo local. Para personalizarlas, cópialo como `.env`, que está excluido del control de versiones.

```bash
cp .env.example .env
```

| Variable                      | Uso                                                                                   | Valor por defecto                                     |
| ----------------------------- | ------------------------------------------------------------------------------------- | ----------------------------------------------------- |
| `SPRING_PROFILES_ACTIVE`      | Perfil de Spring en tiempo de ejecución                                               | El perfil de Maven                                    |
| `DB_URL`                      | URL JDBC de PostgreSQL, obligatoria en `prod`                                         | Según el perfil                                       |
| `DB_USERNAME`                 | Usuario de PostgreSQL, obligatorio en `prod`                                          | `postgres`                                            |
| `DB_PASSWORD`                 | Clave de PostgreSQL, obligatoria en `prod`                                            | `postgres`                                            |
| `JWT_SECRET`                  | Clave HMAC en Base64 de al menos 256 bits, obligatoria en `prod`                      | Solo fuera de `prod`                                  |
| `JWT_EXPIRATION_MS`           | Vigencia del token de acceso en milisegundos                                          | `900000`                                              |
| `JWT_REFRESH_EXPIRATION_MS`   | Vigencia del refresh token en milisegundos                                            | `604800000`                                           |
| `MFA_CLAVE_CIFRADO`           | Clave para cifrar los secretos de la verificación en dos pasos, obligatoria en `prod` | Solo fuera de `prod`                                  |
| `PAGOS_SIMULACION_HABILITADA` | Habilita la confirmación simulada de pagos con Yape y Plin                            | `true`, y `false` en `prod`                           |
| `CORS_ALLOWED_ORIGINS`        | Orígenes permitidos, separados por comas                                              | `http://localhost:4200`                               |
| `SERVER_PORT`                 | Puerto HTTP de la API dentro del contenedor                                           | `8080`                                                |
| `JDK_JAVA_OPTIONS`            | Opciones adicionales de la JVM de la API                                              | `-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError` |
| `OTEL_SERVICE_NAME`           | Nombre del servicio en las trazas                                                     | `tusalud-api`                                         |
| `APP_PORT`                    | Puerto de la API en el host                                                           | `8080`                                                |
| `WEB_PORT`                    | Puerto de la aplicación web en el host                                                | `4200`                                                |
| `POSTGRES_PORT`               | Puerto de PostgreSQL en el host                                                       | `5432`                                                |
| `JAEGER_UI_PORT`              | Puerto de la interfaz de Jaeger en el host                                            | `16686`                                               |
| `OTEL_GRPC_PORT`              | Puerto OTLP gRPC del Collector en el host                                             | `4317`                                                |
| `OTEL_HTTP_PORT`              | Puerto OTLP HTTP del Collector en el host                                             | `4318`                                                |
| `OTEL_HEALTH_PORT`            | Puerto del healthcheck del Collector en el host                                       | `13133`                                               |
| `PGADMIN_PORT`                | Puerto de pgAdmin en el host                                                          | `5050`                                                |
| `PGADMIN_EMAIL`               | Usuario de pgAdmin                                                                    | `admin@tusalud.com`                                   |
| `PGADMIN_PASSWORD`            | Clave de pgAdmin                                                                      | `admin`                                               |
