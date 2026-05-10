-- =============================================================
--  AURAHEALTH — SCRIPT COMPLETO DE BASE DE DATOS
--  Motor: PostgreSQL 15+
--  Descripción: Script idempotente (DROP + CREATE) para
--  despliegue desde cero. Incluye todas las tablas auditadas
--  en Fase 1 y requeridas por el módulo Java de Fase 2.
--  Orden: las tablas independientes primero, luego las que
--  tienen FK, para evitar errores de referencia.
-- =============================================================


-- -------------------------------------------------------------
--  0. LIMPIEZA PREVIA
--  Elimina objetos en orden inverso de dependencia.
--  Seguro ejecutar múltiples veces (IF EXISTS).
-- -------------------------------------------------------------

DROP TABLE IF EXISTS user_favorite_resources  CASCADE;
DROP TABLE IF EXISTS daily_tips               CASCADE;
DROP TABLE IF EXISTS search_history           CASCADE;
DROP TABLE IF EXISTS family_history           CASCADE;
DROP TABLE IF EXISTS medical_history          CASCADE;
DROP TABLE IF EXISTS health_logs              CASCADE;
DROP TABLE IF EXISTS notifications            CASCADE;
DROP TABLE IF EXISTS partnerships             CASCADE;
DROP TABLE IF EXISTS activity_logs            CASCADE;
DROP TABLE IF EXISTS medications              CASCADE;
DROP TABLE IF EXISTS reminders                CASCADE;
DROP TABLE IF EXISTS educational_resources    CASCADE;
DROP TABLE IF EXISTS health_profiles          CASCADE;
DROP TABLE IF EXISTS users                    CASCADE;

-- Eliminar tipos ENUM si ya existían de versiones anteriores
DROP TYPE IF EXISTS partnership_status CASCADE;
DROP TYPE IF EXISTS resource_format    CASCADE;


-- -------------------------------------------------------------
--  1. TIPOS ENUM
--  Nota: format_type se define como VARCHAR(50) en la tabla
--  educational_resources para compatibilidad directa con
--  @Enumerated(EnumType.STRING) de JPA sin configuración extra.
--  Solo se crea el ENUM de partnerships que sí se usa vía JPA
--  como String (pending / active).
-- -------------------------------------------------------------

CREATE TYPE partnership_status AS ENUM ('pending', 'active');


-- =============================================================
--  BLOQUE A — TABLAS INDEPENDIENTES (sin FK entrantes)
-- =============================================================


-- -------------------------------------------------------------
--  2. USERS
--  Cambios respecto al script original:
--    + is_email_verified  (HU40 — verificación de cuenta)
--    + preferred_language (HU31, HU48 — persistencia de idioma)
--    - weight_kg, height_cm, blood_type, blood_pressure,
--      glucose_mgdl, cholesterol_mgdl, allergies
--      → movidos a health_profiles (alineación con el ER diagram)
-- -------------------------------------------------------------

CREATE TABLE users (
    id                   BIGSERIAL    PRIMARY KEY,
    first_name           VARCHAR(255) NOT NULL,
    last_name            VARCHAR(255) NOT NULL,
    email                VARCHAR(255) NOT NULL UNIQUE,
    password_hash        VARCHAR(255) NOT NULL,
    birth_date           DATE,
    gender               VARCHAR(50),
    is_email_verified    BOOLEAN      NOT NULL DEFAULT FALSE,
    preferred_language   VARCHAR(10)  NOT NULL DEFAULT 'es',
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  users                    IS 'Cuentas de usuario de la plataforma AuraHealth';
COMMENT ON COLUMN users.is_email_verified  IS 'HU40 — el usuario confirmó su correo con el código de 6 dígitos';
COMMENT ON COLUMN users.preferred_language IS 'HU31, HU48 — código ISO 639-1: es | en';


-- -------------------------------------------------------------
--  3. EDUCATIONAL_RESOURCES
--  Tabla independiente (no tiene FK hacia otras).
--  Sustenta EP05, EP08, EP09.
--  format_type como VARCHAR(50) para JPA @Enumerated(STRING):
--    valores válidos → ARTICLE | INFOGRAPHIC | VIDEO | GUIDE
-- -------------------------------------------------------------

CREATE TABLE educational_resources (
    id           BIGSERIAL    PRIMARY KEY,
    title        VARCHAR(255) NOT NULL,
    category     VARCHAR(100) NOT NULL,
    description  TEXT,
    content      TEXT,
    image_url    VARCHAR(500),
    author       VARCHAR(255),
    format_type  VARCHAR(50)  NOT NULL DEFAULT 'ARTICLE',
    download_url VARCHAR(500),
    is_published BOOLEAN      NOT NULL DEFAULT TRUE,
    published_at TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_format_type
        CHECK (format_type IN ('ARTICLE', 'INFOGRAPHIC', 'VIDEO', 'GUIDE'))
);

COMMENT ON TABLE  educational_resources             IS 'Recursos educativos para EP05 Recomendaciones, EP08 Biblioteca y EP09 Artículos';
COMMENT ON COLUMN educational_resources.category    IS 'HU34 — valores usados en los botones de filtro: nutrition, prevention, exercise, mental_health';
COMMENT ON COLUMN educational_resources.format_type IS 'HU37 — ARTICLE | INFOGRAPHIC | VIDEO | GUIDE';
COMMENT ON COLUMN educational_resources.download_url IS 'HU29 — URL del PDF descargable';
COMMENT ON COLUMN educational_resources.content     IS 'HU42 — cuerpo completo del artículo para la vista de detalle';

CREATE INDEX idx_resources_category    ON educational_resources (category);
CREATE INDEX idx_resources_format      ON educational_resources (format_type);
CREATE INDEX idx_resources_published   ON educational_resources (is_published);
-- Índice GIN para búsqueda full-text en español (HU41, HU33)
CREATE INDEX idx_resources_fts
    ON educational_resources
    USING gin(to_tsvector('spanish', title || ' ' || COALESCE(description, '')));


-- -------------------------------------------------------------
--  4. DAILY_TIPS
--  Tabla independiente. El Service hace Collections.shuffle()
--  y limit(3) para el banner "Consejos para hoy" (HU04).
-- -------------------------------------------------------------

CREATE TABLE daily_tips (
    id         BIGSERIAL    PRIMARY KEY,
    content    TEXT         NOT NULL,
    category   VARCHAR(100),
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE daily_tips IS 'HU04 (EP05) — consejos de bienestar para el banner diario';


-- =============================================================
--  BLOQUE B — TABLAS CON FK A USERS
-- =============================================================


-- -------------------------------------------------------------
--  5. HEALTH_PROFILES  (1:1 con users)
--  Los campos de salud se separaron de users para alinearse
--  con el ER diagram del proyecto.
-- -------------------------------------------------------------

CREATE TABLE health_profiles (
    user_id           BIGINT       PRIMARY KEY
                                   REFERENCES users (id) ON DELETE CASCADE,
    blood_type        VARCHAR(10),
    blood_pressure    VARCHAR(20),
    glucose_level     DECIMAL(6,2),
    cholesterol_level DECIMAL(6,2),
    allergies         TEXT,
    weight_kg         DECIMAL(5,2),
    height_cm         DECIMAL(5,2)
);

COMMENT ON TABLE health_profiles IS 'EP03 — datos clínicos del usuario; relación 1:1 con users';


-- -------------------------------------------------------------
--  6. REMINDERS
--  Cambios respecto al script original:
--    name       → title          (alineación con ER diagram)
--    category   → reminder_type  (nombre según ER diagram)
--    remind_date → scheduled_date
--    remind_time → scheduled_time
--    is_completed → is_done      (método markAsDone() del UML)
--  Se usa VARCHAR en lugar del ENUM PostgreSQL para
--  mayor flexibilidad en la capa JPA.
--  Valores sugeridos: medical | medicine | exam | vaccine
-- -------------------------------------------------------------

CREATE TABLE reminders (
    id             BIGSERIAL    PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title          VARCHAR(255) NOT NULL,
    reminder_type  VARCHAR(50)  NOT NULL,
    scheduled_date DATE,
    scheduled_time TIME,
    is_done        BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_reminder_type
        CHECK (reminder_type IN ('medical', 'medicine', 'exam', 'vaccine'))
);

COMMENT ON TABLE  reminders               IS 'EP04 — recordatorios de citas, medicamentos, exámenes y vacunas';
COMMENT ON COLUMN reminders.reminder_type IS 'HU16 — medical | medicine | exam | vaccine';
COMMENT ON COLUMN reminders.is_done       IS 'HU03, HU32 — marcado como completado';

CREATE INDEX idx_reminders_user_date ON reminders (user_id, scheduled_date);


-- -------------------------------------------------------------
--  7. MEDICATIONS
--  Cambios respecto al script original:
--    dose         → dosage              (alineación con ER/UML)
--    + start_date, end_date             (HU50 análisis de consumo)
--    - is_completed_today               (reemplazado por health_logs)
-- -------------------------------------------------------------

CREATE TABLE medications (
    id                     BIGSERIAL    PRIMARY KEY,
    user_id                BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name                   VARCHAR(255) NOT NULL,
    dosage                 VARCHAR(100),
    frequency              VARCHAR(100),
    start_date             DATE,
    end_date               DATE,
    is_shared_with_partner BOOLEAN      NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE  medications                      IS 'EP11, EP10 — medicamentos del usuario';
COMMENT ON COLUMN medications.is_shared_with_partner IS 'HU46 — toggle de privacidad del módulo Partner';
COMMENT ON COLUMN medications.start_date           IS 'HU50 — necesario para el análisis de consumo mensual';


-- -------------------------------------------------------------
--  8. HEALTH_LOGS
--  Nueva tabla requerida por EP11.
--  Registra síntomas y consumo real de dosis.
-- -------------------------------------------------------------

CREATE TABLE health_logs (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT    NOT NULL REFERENCES users       (id) ON DELETE CASCADE,
    medication_id BIGINT             REFERENCES medications (id) ON DELETE SET NULL,
    symptom       VARCHAR(255),
    notes         TEXT,
    log_date      DATE      NOT NULL DEFAULT CURRENT_DATE,
    log_time      TIME               DEFAULT CURRENT_TIME,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  health_logs             IS 'EP11 — HU05 síntomas, HU50 dosis consumidas, HU51 cumplimiento';
COMMENT ON COLUMN health_logs.symptom     IS 'HU05 — síntoma o efecto observado tras tomar un medicamento';
COMMENT ON COLUMN health_logs.medication_id IS 'FK opcional; NULL si el log no está vinculado a un medicamento específico';

CREATE INDEX idx_health_logs_user_date ON health_logs (user_id, log_date);


-- -------------------------------------------------------------
--  9. ACTIVITY_LOGS
--  Rediseñado con columnas planas (en lugar del ENUM + valores
--  genéricos del script original) para alinearse con el ER
--  diagram y simplificar el mapeo JPA.
-- -------------------------------------------------------------

CREATE TABLE activity_logs (
    id            BIGSERIAL    PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    log_date      DATE         NOT NULL DEFAULT CURRENT_DATE,
    steps_count   INT                   DEFAULT 0,
    water_liters  DECIMAL(4,2)          DEFAULT 0,
    sleep_hours   DECIMAL(4,2)          DEFAULT 0,
    calories_kcal INT                   DEFAULT 0,

    CONSTRAINT uq_activity_log_per_day UNIQUE (user_id, log_date)
);

COMMENT ON TABLE activity_logs IS 'EP04 (gráficos) — registro diario de actividad física del usuario';

CREATE INDEX idx_activity_logs_user_date ON activity_logs (user_id, log_date DESC);


-- -------------------------------------------------------------
--  10. PARTNERSHIPS  (muchos-a-muchos reflexiva sobre users)
-- -------------------------------------------------------------

CREATE TABLE partnerships (
    id         BIGSERIAL        PRIMARY KEY,
    owner_id   BIGINT           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    partner_id BIGINT           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status     partnership_status NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_partnership UNIQUE (owner_id, partner_id),
    CONSTRAINT chk_no_self_partner CHECK (owner_id <> partner_id)
);

COMMENT ON TABLE  partnerships        IS 'EP10 — HU44 invitación, HU45 progreso, HU46 privacidad de medicamentos';
COMMENT ON COLUMN partnerships.status IS 'pending → invitación enviada | active → vinculación aceptada';

CREATE INDEX idx_partnerships_owner   ON partnerships (owner_id);
CREATE INDEX idx_partnerships_partner ON partnerships (partner_id);


-- -------------------------------------------------------------
--  11. NOTIFICATIONS
-- -------------------------------------------------------------

CREATE TABLE notifications (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title      VARCHAR(255) NOT NULL,
    body       TEXT,
    icon       VARCHAR(50),
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE notifications IS 'EP04, EP10 — alertas de recordatorios y eventos de partner';

CREATE INDEX idx_notifications_user_unread
    ON notifications (user_id, is_read)
    WHERE is_read = FALSE;


-- -------------------------------------------------------------
--  12. SEARCH_HISTORY
-- -------------------------------------------------------------

CREATE TABLE search_history (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    query       VARCHAR(500) NOT NULL,
    searched_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE search_history IS 'EP06 — HU19 historial de búsquedas recientes por usuario';

CREATE INDEX idx_search_history_user
    ON search_history (user_id, searched_at DESC);


-- -------------------------------------------------------------
--  13. FAMILY_HISTORY
-- -------------------------------------------------------------

CREATE TABLE family_history (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    relative   VARCHAR(100) NOT NULL,
    condition  VARCHAR(255) NOT NULL,
    notes      TEXT,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  family_history          IS 'EP03 — HU17 antecedentes familiares del usuario';
COMMENT ON COLUMN family_history.relative IS 'Parentesco: padre, madre, abuelo, etc.';


-- -------------------------------------------------------------
--  14. MEDICAL_HISTORY
-- -------------------------------------------------------------

CREATE TABLE medical_history (
    id             BIGSERIAL    PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    condition      VARCHAR(255) NOT NULL,
    diagnosis_date DATE,
    notes          TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE medical_history IS 'EP03 — HU20, HU27 historial médico personal con fechas';


-- =============================================================
--  BLOQUE C — TABLAS PUENTE (FK a users + a otras tablas)
-- =============================================================


-- -------------------------------------------------------------
--  15. USER_FAVORITE_RESOURCES  (many-to-many users ↔ resources)
--  PK compuesta (user_id, resource_id) — mapea a
--  @EmbeddedId UserFavoriteResourceId en JPA.
-- -------------------------------------------------------------

CREATE TABLE user_favorite_resources (
    user_id     BIGINT    NOT NULL REFERENCES users                (id) ON DELETE CASCADE,
    resource_id BIGINT    NOT NULL REFERENCES educational_resources (id) ON DELETE CASCADE,
    saved_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, resource_id)
);

COMMENT ON TABLE  user_favorite_resources         IS 'EP09 — HU09 artículos guardados como favoritos';
COMMENT ON COLUMN user_favorite_resources.saved_at IS 'Fecha en que el usuario pulsó "Agregar a Favoritos"';

CREATE INDEX idx_favorites_user ON user_favorite_resources (user_id, saved_at DESC);


-- =============================================================
--  VERIFICACIÓN FINAL
--  Ejecuta este bloque para confirmar que todo se creó bien.
-- =============================================================

SELECT
    tablename                               AS tabla,
    pg_total_relation_size(tablename::regclass) AS bytes_totales
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;
