CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ───── raíz ────────────────────────────────────────────────────────────────
CREATE TABLE tenant (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  slug        text NOT NULL UNIQUE CHECK (slug ~ '^[a-z0-9-]{3,40}$'),  -- /r/nutri-ana
  name        text NOT NULL,
  timezone    text NOT NULL DEFAULT 'America/Argentina/Buenos_Aires',
  currency    text NOT NULL DEFAULT 'ARS' CHECK (length(currency) = 3),
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now(),
  deleted_at  timestamptz
);

-- ───── identidad ───────────────────────────────────────────────────────────
CREATE TABLE app_user (                      -- "user" es palabra reservada
  id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id     bigint NOT NULL REFERENCES tenant (id),
  email         text NOT NULL,
  password_hash text NOT NULL,               -- nunca "password"
  full_name     text NOT NULL,
  role          text NOT NULL CHECK (role IN ('OWNER','PROFESSIONAL','STAFF')),
  is_active     boolean NOT NULL DEFAULT true,
  created_at    timestamptz NOT NULL DEFAULT now(),
  updated_at    timestamptz NOT NULL DEFAULT now(),
  deleted_at    timestamptz,
  UNIQUE (tenant_id, id)                     -- destino de las FK compuestas
);
-- Global, no por tenant: el login es email + contraseña, sin preguntar el
-- consultorio. La consecuencia es que un email registrado en un consultorio
-- ya no se puede usar en otro. Se levanta junto con membership (ver más abajo).
CREATE UNIQUE INDEX app_user_email_uk ON app_user (lower(email))
  WHERE deleted_at IS NULL;
CREATE INDEX app_user_tenant_ix ON app_user (tenant_id);

CREATE TABLE professional (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  public_id   uuid NOT NULL DEFAULT gen_random_uuid() UNIQUE,  -- viaja en URLs
  tenant_id   bigint NOT NULL REFERENCES tenant (id),
  user_id      bigint UNIQUE,                -- NULL: está en la agenda, no entra al sistema
  display_name text NOT NULL,                -- el nombre público ("Lic. Ana Pérez").
                                             -- app_user.full_name es el de la cuenta:
                                             -- son dos campos con dos propósitos.
  specialty    text,
  license_no   text,                         -- matrícula
  phone        text,
  bio          text,
  avatar_key   text,
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now(),
  deleted_at  timestamptz,
  UNIQUE (tenant_id, id),
  FOREIGN KEY (tenant_id, user_id) REFERENCES app_user (tenant_id, id)
);
CREATE INDEX professional_tenant_ix ON professional (tenant_id);

-- ───── personas y catálogo ─────────────────────────────────────────────────
CREATE TABLE health_insurer (                -- obras sociales y prepagas
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id   bigint NOT NULL REFERENCES tenant (id),
  name        text NOT NULL,
  is_accepted boolean NOT NULL DEFAULT true, -- false: la registro, pero no la atiendo
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now(),
  deleted_at  timestamptz,
  UNIQUE (tenant_id, id)
);
CREATE UNIQUE INDEX health_insurer_name_uk ON health_insurer (tenant_id, lower(name))
  WHERE deleted_at IS NULL;

CREATE TABLE patient (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id   bigint NOT NULL REFERENCES tenant (id),
  first_name  text NOT NULL,
  last_name   text NOT NULL,
  dni         text NOT NULL CHECK (dni ~ '^[0-9]{7,9}$'),  -- sin puntos, se normaliza en la app
  birth_date  date,
  email       text,
  phone       text,                          -- normalizado E.164 en la app
  origin      text NOT NULL DEFAULT 'STAFF'   -- mismo vocabulario que
              CHECK (origin IN ('STAFF','ONLINE')),  -- appointment.origin
  notes       text,
  health_insurer_id bigint,                  -- NULL: paciente particular
  member_number     text,                    -- número de afiliado
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now(),
  deleted_at  timestamptz,
  UNIQUE (tenant_id, id),
  CHECK (email IS NOT NULL OR phone IS NOT NULL),  -- algún canal de contacto
  -- un número de afiliado sin obra social no significa nada
  CHECK (member_number IS NULL OR health_insurer_id IS NOT NULL),
  FOREIGN KEY (tenant_id, health_insurer_id) REFERENCES health_insurer (tenant_id, id)
);

-- El DNI es el identificador de una persona en este dominio y es obligatorio:
-- la reserva pública también lo pide. Por eso deduplica siempre, no sólo cuando
-- alguien se acordó de cargarlo. Sigue siendo parcial sobre deleted_at: una
-- ficha borrada no bloquea volver a dar de alta a la misma persona.
CREATE UNIQUE INDEX patient_dni_uk ON patient (tenant_id, dni)
  WHERE deleted_at IS NULL;
-- Ni el email ni el teléfono deduplican: una madre reserva para sus dos hijos
-- con su celular y su email, y son dos pacientes distintos. Deduplica el DNI.
-- Estos dos índices existen sólo para encontrar la ficha por lo que el paciente
-- dice cuando llama.
CREATE INDEX patient_email_ix ON patient (tenant_id, lower(email))
  WHERE email IS NOT NULL;
CREATE INDEX patient_phone_ix ON patient (tenant_id, phone)
  WHERE phone IS NOT NULL;

CREATE TABLE service (
  id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  public_id        uuid NOT NULL DEFAULT gen_random_uuid() UNIQUE,  -- viaja en URLs
  tenant_id        bigint NOT NULL REFERENCES tenant (id),
  name             text NOT NULL,
  description      text,
  duration_minutes int NOT NULL CHECK (duration_minutes BETWEEN 5 AND 480),
  price            numeric(12,2) NOT NULL CHECK (price >= 0),
  is_bookable      boolean NOT NULL DEFAULT true,  -- lo cobro, pero no lo publico
  created_at       timestamptz NOT NULL DEFAULT now(),
  updated_at       timestamptz NOT NULL DEFAULT now(),
  deleted_at       timestamptz,
  UNIQUE (tenant_id, id)
);
CREATE UNIQUE INDEX service_name_uk ON service (tenant_id, lower(name))
  WHERE deleted_at IS NULL;

-- ───── disponibilidad ──────────────────────────────────────────────────────
CREATE TABLE schedule (                      -- regla semanal recurrente
  id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id       bigint NOT NULL REFERENCES tenant (id),
  professional_id bigint NOT NULL,
  weekday         smallint NOT NULL CHECK (weekday BETWEEN 1 AND 7),  -- ISO-8601
  start_time      time NOT NULL,
  end_time        time NOT NULL,
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now(),
  CHECK (end_time > start_time),
  -- la grilla se recorre en pasos de service.duration_minutes, no de la franja
  UNIQUE (tenant_id, professional_id, weekday, start_time),
  FOREIGN KEY (tenant_id, professional_id) REFERENCES professional (tenant_id, id)
);
CREATE INDEX schedule_lookup_ix ON schedule (tenant_id, professional_id, weekday);

CREATE TABLE time_off (                      -- feriado, vacaciones, bloqueo puntual
  id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id       bigint NOT NULL REFERENCES tenant (id),
  professional_id bigint NOT NULL,           -- un cierre del consultorio con N
                                             -- profesionales son N filas; saca un
                                             -- OR de la consulta más caliente
  starts_at       timestamptz NOT NULL,
  ends_at         timestamptz NOT NULL,
  reason          text,
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now(),
  CHECK (ends_at > starts_at),
  FOREIGN KEY (tenant_id, professional_id) REFERENCES professional (tenant_id, id)
);
CREATE INDEX time_off_lookup_ix ON time_off (tenant_id, professional_id, starts_at);

-- ───── el turno ────────────────────────────────────────────────────────────
CREATE TABLE appointment (
  id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  public_id        uuid NOT NULL DEFAULT gen_random_uuid() UNIQUE,  -- lo único en URLs
  tenant_id        bigint NOT NULL REFERENCES tenant (id),
  professional_id  bigint NOT NULL,
  patient_id       bigint NOT NULL,
  service_id       bigint NOT NULL,
  starts_at        timestamptz NOT NULL,
  ends_at          timestamptz NOT NULL,
  status           text NOT NULL DEFAULT 'PENDING'
                   CHECK (status IN ('PENDING','CONFIRMED','ATTENDED','NO_SHOW','CANCELLED')),
  service_name     text NOT NULL,            -- ┐ foto del servicio al reservar: cambiar
  price            numeric(12,2) NOT NULL,   -- ┘ el precio no reescribe la de marzo.
                                             --   La duración NO se copia: es ends_at
                                             --   − starts_at. La moneda tampoco: es
                                             --   tenant.currency.
  origin           text NOT NULL DEFAULT 'STAFF'   -- mismo vocabulario que
                   CHECK (origin IN ('STAFF','ONLINE')),  -- patient.origin
  notes            text,
  cancelled_at     timestamptz,
  cancel_reason    text,
  created_at       timestamptz NOT NULL DEFAULT now(),
  updated_at       timestamptz NOT NULL DEFAULT now(),
  CHECK (ends_at > starts_at),
  CHECK ((status = 'CANCELLED') = (cancelled_at IS NOT NULL)),
  -- sin UNIQUE (tenant_id, id): en el MVP nada referencia al turno. El índice
  -- entra con la migración de v1.1, que de todos modos va a existir.
  FOREIGN KEY (tenant_id, professional_id) REFERENCES professional (tenant_id, id),
  FOREIGN KEY (tenant_id, patient_id)      REFERENCES patient (tenant_id, id),
  FOREIGN KEY (tenant_id, service_id)      REFERENCES service (tenant_id, id),
  CONSTRAINT appointment_no_overlap EXCLUDE USING gist (
    professional_id WITH =,
    tstzrange(starts_at, ends_at, '[)') WITH &&
  ) WHERE (status <> 'CANCELLED')
);
CREATE INDEX appointment_agenda_ix  ON appointment (tenant_id, professional_id, starts_at);
CREATE INDEX appointment_patient_ix ON appointment (tenant_id, patient_id, starts_at DESC);

-- ───── sesiones ────────────────────────────────────────────────────────────
CREATE TABLE refresh_token (                 -- una fila por sesión abierta
  id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),  -- viaja dentro del token
  tenant_id           bigint NOT NULL REFERENCES tenant (id),
  user_id             bigint NOT NULL,
  token_hash          text NOT NULL,         -- nunca el token en claro
  previous_token_hash text,                  -- tolera dos pestañas que renuevan a la vez
  expires_at          timestamptz NOT NULL,
  revoked_at          timestamptz,           -- logout o reuso detectado
  created_at          timestamptz NOT NULL DEFAULT now(),
  updated_at          timestamptz NOT NULL DEFAULT now(),
  FOREIGN KEY (tenant_id, user_id) REFERENCES app_user (tenant_id, id)
);
CREATE INDEX refresh_token_user_ix ON refresh_token (tenant_id, user_id);
