# Endpoints — MVP

Propuesta v1. Deriva de [modelo-datos.md](./modelo-datos.md). Estado: **propuesta**, sin implementar.

API REST JSON bajo `/api`. 39 endpoints que cubren las cinco funcionalidades del MVP.

## Convenciones

- **Sesión.** JWT de acceso de vida corta (15 min) en `Authorization: Bearer`, guardado sólo en memoria del navegador; lleva usuario, rol y `tenant_id`. Se renueva con un refresh token opaco, rotativo y revocable, en cookie `HttpOnly; Secure; SameSite=Strict` acotada a `/api/auth`. Así JavaScript nunca lee la credencial larga (XSS) y la cookie no viaja a ningún otro endpoint (CSRF).
- **Tenant.** En los endpoints privados sale del JWT; nunca viaja en la URL ni en el body. En los públicos sale del `slug`. Un recurso de otro consultorio responde `404`.
- **Identificadores.** Turno, profesional y servicio se identifican por `publicId` (uuid) en toda URL (R6); los parámetros `professionalId` y `serviceId` llevan ese uuid. Paciente, obra social y bloqueo usan su `id` numérico, que sólo resuelve dentro del tenant y nunca aparece en un endpoint público.
- **Tiempo.** Instantes en ISO-8601 con offset (`2026-10-06T14:30:00-03:00`). `from`/`to` definen el intervalo `[from, to)`. `schedule` usa `weekday` 1–7 y `HH:mm` en la zona del tenant.
- **Bajas.** `DELETE` es baja lógica donde el modelo tiene `deleted_at`. Responde `204`.
- **Errores.** `400` validación · `401` sin token o vencido · `404` no existe en este tenant · `409` unicidad (email, slug, DNI, nombre) o turno solapado · `429` límite de tasa.

## Auth

| Método | Ruta | Qué hace |
| --- | --- | --- |
| POST | `/api/auth/register` | Crea `tenant` + `app_user` OWNER + `professional` vinculado, en una transacción. Responde como `login` |
| POST | `/api/auth/login` | Email + contraseña → JWT de acceso en el body y refresh token en la cookie |
| POST | `/api/auth/refresh` | Canjea la cookie por un JWT nuevo y rota el refresh token |
| POST | `/api/auth/logout` | Revoca el refresh token y borra la cookie |
| GET | `/api/auth/me` | Usuario, rol, consultorio y profesional vinculado |

- Reusar un refresh token ya rotado revoca toda la cadena: es la señal de que fue robado.
- `register`, `login` y `refresh` llevan límite de tasa. `login` responde el mismo `401` si el email no existe o si la contraseña es incorrecta.

## Consultorio y perfil

| Método | Ruta | Qué hace |
| --- | --- | --- |
| GET, PATCH | `/api/tenant` | Nombre, slug, zona horaria, moneda |
| GET, POST | `/api/professionals` | Lista / alta (puede no tener cuenta) |
| GET, PATCH, DELETE | `/api/professionals/{publicId}` | Perfil: nombre público, especialidad, matrícula, teléfono, bio |

## Catálogo

| Método | Ruta | Qué hace |
| --- | --- | --- |
| GET, POST | `/api/services` | Servicios con duración, precio e `isBookable` |
| PATCH, DELETE | `/api/services/{publicId}` | Editar / dar de baja. No altera turnos ya reservados (R4) |
| GET, POST | `/api/health-insurers` | Obras sociales y prepagas |
| PATCH, DELETE | `/api/health-insurers/{id}` | Editar / dar de baja |

## Pacientes

| Método | Ruta | Qué hace |
| --- | --- | --- |
| GET | `/api/patients?q=&page=&size=` | Busca por nombre, apellido, DNI, email o teléfono. Paginado |
| POST | `/api/patients` | Alta con `origin = STAFF`. `409` si el DNI ya existe |
| GET, PATCH, DELETE | `/api/patients/{id}` | Ficha |

El historial de turnos de un paciente es `GET /api/appointments?patientId=`.

## Disponibilidad

| Método | Ruta | Qué hace |
| --- | --- | --- |
| GET, PUT | `/api/professionals/{publicId}/schedules` | `PUT` reemplaza la semana completa |
| GET, POST | `/api/professionals/{publicId}/time-off` | Bloqueos; `GET` acepta `from`/`to` |
| DELETE | `/api/professionals/{publicId}/time-off/{timeOffId}` | Borra el bloqueo |
| GET | `/api/professionals/{publicId}/availability?serviceId=&from=&to=` | Horarios libres: `schedule` − `time_off` − turnos no cancelados, en pasos de `duration_minutes` |

## Turnos

| Método | Ruta | Qué hace |
| --- | --- | --- |
| GET | `/api/appointments?from=&to=&professionalId=&patientId=&status=` | Agenda. También resuelve el resumen del día y el historial del paciente |
| POST | `/api/appointments` | Recibe profesional, paciente, servicio y `startsAt`. Nace `CONFIRMED`, `origin = STAFF` |
| GET | `/api/appointments/{publicId}` | Detalle |
| PATCH | `/api/appointments/{publicId}` | Reprogramar (`startsAt`), notas o cambiar `status` (`CANCELLED` admite `cancelReason`) |

- El servidor calcula `endsAt` y copia `serviceName` y `price`; el cliente no los envía.
- El staff puede agendar fuera de `schedule`; lo único que se impide es el solapamiento.
- `CANCELLED` es terminal: para "revivir" un turno se crea otro.

## Reserva pública

Sin token. Sólo lecturas concretas y crear un turno; nunca leer fichas.

| Método | Ruta | Qué hace |
| --- | --- | --- |
| GET | `/api/public/{slug}` | Consultorio, profesionales (sólo campos públicos) y servicios con `isBookable` |
| GET | `/api/public/{slug}/availability?professionalId=&serviceId=&from=&to=` | Horarios libres, sin datos de los ocupados |
| POST | `/api/public/{slug}/appointments` | Recibe profesional, servicio, `startsAt` y datos del paciente (nombre, apellido, DNI, email o teléfono). Nace `PENDING`, `origin = ONLINE`. Devuelve `publicId` |
| GET | `/api/public/{slug}/appointments/{publicId}` | Confirmación: servicio, profesional, horario y estado |

- El paciente se busca por DNI; si no existe se crea con `origin = ONLINE`. Si existe, el turno se asocia a esa ficha **sin devolver ni sobrescribir sus datos**: de lo contrario, conocer un DNI alcanzaría para leer o alterar una ficha.
- `startsAt` debe ser un horario que `availability` ofrezca; se revalida en el servidor.
- El `POST` lleva límite de tasa: escribe sin autenticación.

## Orden de implementación

Cada paso sólo usa lo que dejaron los anteriores. El detalle está en [plan-backend-endpoints.md](./plan-backend-endpoints.md).

| Paso | Endpoints | Depende de |
| --- | --- | --- |
| 1 | Auth: `register`, `login`, `refresh`, `logout`, `me` | Nada. Crea el consultorio y emite el JWT que exige todo lo demás |
| 2 | `/api/tenant`, `/api/professionals` | 1 |
| 3 | `/api/services`, `/api/health-insurers` | 1 |
| 4 | `/api/patients` | 3: la ficha referencia una obra social |
| 5 | `schedules`, `time-off` | 2: pertenecen a un profesional |
| 6 | `/api/appointments` | 2, 3 y 4: cruza profesional, servicio y paciente |
| 7 | `availability` | 3, 5 y 6: franjas menos bloqueos menos turnos, en pasos del servicio |
| 8 | `/api/public/{slug}` y sus tres rutas | 7, y con él todo lo anterior |
| 9 | Límite de tasa | 1 y 8: protege las rutas sin token |

Los pasos 3–4 y 5 no dependen entre sí y pueden avanzar en paralelo.

## Fuera de alcance

- Alta de usuarios adicionales (`STAFF`, `PROFESSIONAL`): el único usuario de cada consultorio es el `OWNER` del registro, y por eso no hay matriz de permisos.
- Recuperar contraseña y verificar email: requieren envío de correo.
- Subir avatar: `avatar_key` requiere storage.
- Que el paciente cancele o reprograme su turno.
- Métricas de dashboard, pagos y recordatorios (pitch, pág. 9).

## Puntos abiertos

- **Tabla `refresh_token`.** Rotar y revocar exige guardar los refresh tokens en el servidor (hash del token, usuario, vencimiento, revocación). El modelo de datos todavía no la tiene.
