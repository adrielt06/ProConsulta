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

| Tecnología | Versión |
| --- | --- |
| Spring Boot | 4.1.1 |
| Java | 25 (Temurin) |
| Maven (vía `mvnw`) | 3.9.16 |
| PostgreSQL | 18.6 |
| pgAdmin (sólo desarrollo) | 9.17 |

- Trabajo: Spring Boot en `backend/`, paquete base `com.proconsulta.backend`, un paquete por feature y `shared` para lo transversal.
- Las demás librerías toman su versión del BOM de Spring Boot; no se les fija versión en `pom.xml`.
- Comandos (`backend/`): `.\mvnw.cmd test` (requiere Docker abierto y `JAVA_HOME` apuntando a un JDK 25), `.\mvnw.cmd spring-boot:run` (levanta `compose.yaml` y aplica las migraciones).
- Antes de arrancar la app: copiar `backend/.env.example` como `backend/.env` y completar `JWT_SECRET`.
- Esquema: lo crea Flyway desde `backend/src/main/resources/db/migration/`. Una migración ya compartida no se edita; cada cambio es un archivo nuevo.
- Contrato y plan: `docs/endpoints-mvp.md`, `docs/modelo-datos.md`, `docs/plan-backend-endpoints.md`.
