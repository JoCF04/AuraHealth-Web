-- =============================================================
--  AURAHEALTH — SQL ADICIONAL: MÓDULO RECORDATORIOS & CITAS
--  Agrega al script base: tabla appointments (nueva entidad).
--  La tabla 'reminders' ya existe en el script principal.
-- =============================================================

-- La entidad Reminder ya tiene su tabla en el script base.
-- Solo necesitas agregar appointments:

CREATE TABLE IF NOT EXISTS appointments (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL
                                  REFERENCES users (id) ON DELETE CASCADE,
    doctor_name      VARCHAR(255),
    specialty        VARCHAR(255),
    clinic_name      VARCHAR(255),
    appointment_date DATE         NOT NULL,
    appointment_time TIME,
    notes            TEXT,
    is_confirmed     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  appointments               IS 'EP04 — HU25 agendar cita, HU26 listar citas médicas';
COMMENT ON COLUMN appointments.is_confirmed  IS 'HU26 — true cuando la clínica confirma la cita';

CREATE INDEX idx_appointments_user_date
    ON appointments (user_id, appointment_date ASC);
