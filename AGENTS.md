# AGENTS.md

## Tecnologías del proyecto

### Stack principal

| Tecnología | Versión |
| --- | --- |
| Angular (core) | 22.1.7 |
| Angular CLI | 22.1.8 |
| TypeScript | 6.0.3 |
| Node.js | 24.19.0 |
| npm | 11.6.2 |

### Dependencias de runtime (frontend)

| Paquete | Versión instalada |
| --- | --- |
| @angular/common | 22.1.7 |
| @angular/compiler | 22.1.7 |
| @angular/core | 22.1.7 |
| @angular/forms | 22.1.7 |
| @angular/platform-browser | 22.1.7 |
| @angular/router | 22.1.7 |
| rxjs | 7.8.2 |
| tslib | 2.8.1 |

### Dependencias de desarrollo (frontend)

| Paquete | Versión instalada |
| --- | --- |
| @angular/build | 22.1.8 |
| @angular/compiler-cli | 22.1.7 |
| jsdom | 28.1.0 |
| prettier | 3.9.8 |
| vitest | 4.1.11 |

### Configuración del proyecto

- Trabajo: Angular standalone + routing en `frontend/`
- Sistema operativo del entorno: Windows (win32 x64)
- Comandos (`frontend/`): `npm start` (serve), `npm run build`, `npm test` (vitest)

### Backend

Spring Boot en `backend/`. Contrato y plan: `docs/endpoints-mvp.md`, `docs/modelo-datos.md`, `docs/plan-backend-endpoints.md`.

#### Stack

| Tecnología | Versión |
| --- | --- |
| Spring Boot | 4.1.1 |
| Java | 25 (Temurin) |
| Maven (vía `mvnw`) | 3.9.16 |
| PostgreSQL | 18.6 |
| pgAdmin (sólo desarrollo) | 9.17 |

- Las demás librerías toman su versión del BOM de Spring Boot; no se les fija versión en `pom.xml`.
- Paquete base `com.proconsulta.backend`: un paquete por feature y `shared` para lo transversal.

#### Comandos

Desde `backend/`, con Docker abierto y `JAVA_HOME` apuntando a un JDK 25.

- `.\mvnw.cmd test`: corre los tests contra un PostgreSQL descartable.
- `.\mvnw.cmd spring-boot:run`: levanta `compose.yaml` y aplica las migraciones.
- Antes de arrancar la app: copiar `backend/.env.example` como `backend/.env` y completar `JWT_SECRET`.
- pgAdmin queda en <http://localhost:8081>, sólo desde la propia máquina.

#### Base de datos

- El esquema lo crea Flyway desde `backend/src/main/resources/db/migration/`; Hibernate sólo valida.
- `V1__init.sql` es copia literal del bloque SQL de `docs/modelo-datos.md`.
- Una migración ya compartida no se edita. Cada cambio es un archivo nuevo (`V2__…`) y se refleja en `modelo-datos.md`.

#### Tests

- Corren contra PostgreSQL real (Testcontainers): las restricciones del modelo no existen en H2.
- Los que necesitan el contexto completo van en el paquete base, porque `TestcontainersConfiguration` es package-private.
- Usan HTTP real (`RANDOM_PORT` + `java.net.http`), no MockMvc, que no ejecuta el reenvío a `/error`.
- Si el endpoint todavía no existe, el caso se provoca con un `ProbeController` dentro del test (ver `ErrorHandlingTests`).

#### Errores comunes

- **Pisar el `argLine` de surefire en `pom.xml`.** Fija la zona UTC de los tests; sin eso PostgreSQL rechaza `America/Buenos_Aires`, el nombre que informa Java en Windows.
- **Anotar un `Filter` propio con `@Component`.** Spring Boot lo registraría antes de la seguridad, sin JWT todavía. Se agrega en `SecurityConfig`.
- **Armar respuestas de error a mano en un endpoint.** Para un 404 se lanza `ResponseStatusException`; para un duplicado se deja que la base rechace. `ApiExceptionHandler` traduce.
- **Agregar una restricción única sin mensaje.** Si necesita texto propio, se suma al mapa `CONFLICTS` de `ApiExceptionHandler`.

#### Decisiones

- **El servidor corre en UTC.** La zona de cada consultorio vive en `tenant.timezone`.
- **Dos cadenas de seguridad.** Las rutas públicas tienen la suya, sin validación de JWT, para que un token vencido no bloquee `/api/auth/refresh`.
- **El consultorio sale del dato `tenant_id` del JWT.** Se lee con `TenantContext.require()`, que falla si no hay.
- **Sin consultorio, Hibernate usa el tenant `0`.** Las lecturas vuelven vacías y las escrituras fallan; así `register` y `login` pueden abrir una sesión antes de conocerlo.
- **Secretos por variable de entorno.** `backend/.env` en desarrollo, variables reales en producción.
