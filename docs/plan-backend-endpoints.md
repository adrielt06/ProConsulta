# Plan — backend de endpoints del MVP

Implementa los 39 endpoints de [endpoints-mvp.md](./endpoints-mvp.md) sobre el esquema de [modelo-datos.md](./modelo-datos.md). Estado al 2026-10-13: **paso 0 terminado**; sigue el paso 1. Todavía no hay ningún endpoint implementado.

| Paso | Estado |
| --- | --- |
| 0. Cimientos | Hecho |
| 1 a 9 | Pendientes |

## Stack

Versiones verificadas el 2026-10-06; PostgreSQL y pgAdmin, el 2026-10-13.

| Pieza | Versión | Por qué |
| --- | --- | --- |
| Spring Boot | 4.1.1 | Última estable. Soporte gratuito hasta 2027-07-31; 4.0 lo pierde el 2026-12-31 y 3.5 ya lo perdió |
| Java | 25 (Temurin 25.0.4.1) | LTS más reciente. Boot 4.1 admite de 17 a 26 |
| Build | Maven 3.9.16, vía `mvnw` | El wrapper fija la versión y evita instalar Maven |
| PostgreSQL | 18.6 (`postgres:18.6`) | Última versión mayor, con un año en producción y soporte hasta 2030-11 |
| pgAdmin | 9.17 (`dpage/pgadmin4:9.17`) | Cliente web para ver la base en desarrollo. Sólo en `compose.yaml` |

- **Lo demás sale del BOM de Boot, sin versión explícita:** Spring Security 7.1.1, Hibernate 7.4.5.Final, Flyway 12.4.0, driver PostgreSQL 42.7.13, Testcontainers 2.0.5, JUnit 6.0.3. Subir Boot los sube juntos y ya probados entre sí.
- **Ninguna dependencia fuera del BOM:** el JWT lo firma y valida Spring Security; el límite de tasa es un filtro propio en memoria.
- **Infraestructura presente:** Docker 29.7.2. El JDK lo descarga el IDE, Maven lo cubre el wrapper y PostgreSQL corre en Docker.
- **Frontend** (Angular 22.1.7, Node 24.19.0): sólo se acopla por HTTP + JSON; no hay conflicto de versiones.

## Estructura

`backend/`, junto a `frontend/`. Paquete base `com.proconsulta.backend`. Un paquete por feature (`auth`, `tenant`, `professional`, `catalog`, `patient`, `availability`, `appointment`, `booking`) y `shared` para lo transversal.

## Pasos

Cada paso cierra con sus tests de integración en verde contra PostgreSQL real (Testcontainers): `EXCLUDE USING gist` y los índices parciales no existen en H2.

### 0. Cimientos (hecho)

Lo que quedó construido, con los desvíos respecto del plan original, está en [Resultado del paso 0](#resultado-del-paso-0).

1. JDK 25 (Temurin). Con IntelliJ se descarga desde el IDE (`File > Project Structure > SDKs > Download JDK…`), que gestiona una versión por proyecto; no hace falta instalar un Java global.
2. Generar el proyecto en start.spring.io: Maven, Java 25, Boot 4.1.1, Group `com.proconsulta`, Artifact `backend`, y las dependencias Spring Web, Validation, Spring Data JPA, PostgreSQL Driver, Flyway Migration, Spring Security, OAuth2 Resource Server, Testcontainers y Docker Compose Support. Descomprimir en `backend/`.
3. Fijar la imagen de PostgreSQL: el proyecto generado trae `postgres:latest` en `compose.yaml` y en `TestcontainersConfiguration.java`. Reemplazar ambos por `postgres:18.6`; `latest` cambia de versión mayor sin aviso. La menor se sube cada trimestre.
4. Migración `V1__init.sql` con el DDL de `modelo-datos.md` más la tabla `refresh_token`. Hibernate en `ddl-auto=validate`, para que una entidad que no coincide con el esquema impida arrancar.
5. `refresh_token`, una fila por sesión: `id uuid`, `tenant_id`, `user_id`, `token_hash`, `previous_token_hash`, `expires_at`, `revoked_at`. Es la décima tabla de `modelo-datos.md`, que explica su objetivo.
6. Transversal en `shared`: contexto de tenant (`@TenantId`, limpiado en `finally`, falla si falta), traducción de errores a `400`/`404`/`409` y cadena de seguridad (todo `/api` exige JWT salvo `register`, `login`, `refresh` y `/api/public/**`).
7. Secretos por variable de entorno (`JWT_SECRET`), documentados en `.env.example`. Sin el secreto la app no arranca.

Precauciones para que el IDE y la terminal usen el mismo Java y el mismo Maven:

- **Abrir `backend/` como proyecto**, no la raíz del repo. Así `.idea/` queda cubierta por el `.gitignore` generado y `compose.yaml` queda en el directorio de trabajo, que es donde Spring Boot lo busca.
- **SDK del proyecto en 25** (`Project Structure > Project`). Con otro JDK el build falla con `release version 25 not supported`.
- **Maven del IDE igual al del wrapper.** En `Settings > Build Tools > Maven`: "Use Maven wrapper", y "Project JDK" en `Runner > JRE` y en `Importing > JDK for importer`.
- **`JAVA_HOME` en la terminal** apuntando al JDK 25; `mvnw` lo necesita. En IntelliJ se define por proyecto en `Settings > Tools > Terminal > Environment variables`, sin tocar las variables de Windows.

Verificar: `.\mvnw.cmd -v` muestra Java 25 y Maven 3.9.16; la app arranca, Flyway aplica `V1` y un request sin token a una ruta privada responde `401`.

#### Resultado del paso 0

`.\mvnw.cmd test` pasa con 20 tests contra PostgreSQL 18.6.

| Pieza | Dónde | Qué hace |
| --- | --- | --- |
| Esquema | `db/migration/V1__init.sql` | Las diez tablas, copiadas del DDL de `modelo-datos.md` |
| Secreto del JWT | `shared/JwtConfig`, `.env.example` | Firma y valida con `JWT_SECRET`; rechaza claves de menos de 32 bytes |
| Seguridad | `shared/SecurityConfig` | Rutas públicas sin token; el resto exige JWT o responde `401` |
| Errores | `shared/ApiExceptionHandler` | Respuestas `ProblemDetail`: `400` con detalle por campo, `404` y `409` |
| Tenant | `shared/TenantContext`, `TenantFilter`, `TenantIdentifierResolver` | El consultorio sale del dato `tenant_id` del JWT y llega a Hibernate |
| Base local | `compose.yaml` | PostgreSQL y pgAdmin en <http://localhost:8081>, sólo desde la propia máquina |

Desvíos y decisiones que no estaban en el plan:

- **Zona horaria en UTC.** Java en Windows informa la zona como `America/Buenos_Aires`, un nombre antiguo que la imagen de PostgreSQL rechaza. La app la fija en UTC en `main()` y los tests en el `argLine` de surefire (`pom.xml`). Coincide con R2: la zona vive en `tenant.timezone`.
- **Dos cadenas de seguridad.** Las rutas públicas tienen la suya, sin validación de JWT. Con una sola, un token vencido en el header hacía responder `401` a `/api/auth/refresh`, que se usa justamente cuando el token venció.
- **Sin consultorio, Hibernate usa el tenant `0`** en vez de fallar: las lecturas vuelven vacías y las escrituras chocan con la clave foránea. Si fallara, `register` y `login` no podrían abrir una sesión antes de conocer el consultorio. El código que necesita el consultorio usa `TenantContext.require()`, que sí falla.
- **`.env` local.** `application.properties` lee `backend/.env` si existe; cada integrante crea el suyo desde `.env.example`. En producción van variables de entorno reales.
- **Errores de integridad no previstos** (clave foránea, `CHECK`) responden `400` genérico; el mensaje de la base queda en el log y no llega al cliente.

Pendiente de pasos posteriores:

- El consultorio de las rutas públicas, que sale del `slug`, se resuelve en el paso 8.
- El aislamiento entre dos consultorios se prueba en el paso 2, con la primera entidad.
- De los seis mensajes de `409` se probaron dos (slug y turno solapado); el resto, con sus endpoints.

### 1. Auth

`register` → `login` → `refresh` → `logout` → `me`. Va primero porque `register` es la única forma de crear un consultorio y todo lo privado necesita su JWT.

A resolver en este paso, heredado del paso 0:

- Los JWT se emiten con el dato `tenant_id`, que es el que lee `TenantFilter`.
- `register` crea el consultorio dentro del mismo request, pero Hibernate fija el tenant al abrir la sesión. Las primeras filas (`tenant`, `app_user`, `professional`) necesitan otra vía, por ejemplo SQL directo en la misma transacción.
- `login` y `refresh` buscan al usuario antes de conocer su consultorio: esas dos búsquedas saltean el filtro de tenant de forma acotada.
- `logout` es ruta privada. Si el JWT ya venció, el front renueva primero; la alternativa es pasarlo a la cadena pública y que funcione sólo con la cookie.

Verificar: registrar y leer `me`; `refresh` rota el token; reusar un token rotado revoca la sesión; `login` responde el mismo `401` con email inexistente o contraseña incorrecta.

### 2. Consultorio y profesionales

`/api/tenant` y `/api/professionals`. Es la primera entidad con tenant, así que acá se prueba el aislamiento.

Verificar: el consultorio B recibe `404` al pedir un profesional del A.

### 3. Catálogo

`/api/services` y `/api/health-insurers`. No dependen entre sí.

Verificar: nombre duplicado responde `409`; tras una baja lógica el nombre se puede reusar.

### 4. Pacientes

`/api/patients`. Necesita las obras sociales del paso 3.

Verificar: DNI duplicado responde `409`; la búsqueda encuentra por DNI, apellido, email y teléfono.

### 5. Horarios y bloqueos

`schedules` y `time-off`. Necesitan al profesional del paso 2.

Verificar: `PUT` reemplaza la semana completa; `end_time` anterior a `start_time` responde `400`.

### 6. Turnos

`/api/appointments`. Cruza profesional, paciente y servicio (pasos 2, 3 y 4).

Verificar: un turno solapado responde `409`; cancelar libera el horario; subir el precio del servicio no cambia el del turno ya reservado.

### 7. Disponibilidad

`availability`. Va después de turnos porque los resta: `schedule` − `time_off` − turnos no cancelados.

Verificar: un caso con franja, bloqueo y turno tomado devuelve exactamente los horarios esperados, en la zona del tenant.

### 8. Reserva pública

`GET /api/public/{slug}` → `availability` → `POST appointments` → `GET appointments/{publicId}`. Va al final porque reutiliza todo lo anterior.

Verificar: un DNI existente no sobrescribe la ficha; un `startsAt` no ofrecido se rechaza; ninguna respuesta incluye datos de pacientes.

### 9. Límite de tasa

Filtro en memoria sobre `register`, `login`, `refresh` y el `POST` público. Responde `429`.

Verificar: superar el límite responde `429`. Techo conocido: el contador vive en memoria, así que sólo sirve con una instancia.

## Al terminar

- Mantener al día la sección de backend de `AGENTS.md` (versiones y comandos).
- Para consumir la API desde Angular, configurar el proxy del servidor de desarrollo (`/api` → `localhost:8080`). Con front y API en el mismo origen la cookie `SameSite=Strict` funciona y no hace falta CORS.
