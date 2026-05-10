-- ============================================================
--  AuraHealth – Script DDL completo para PostgreSQL / pgAdmin
--  Generado a partir del código fuente (repomix-output__8_.xml)
-- ============================================================

-- Crear tipo ENUM para format_type
DO $$ BEGIN
    CREATE TYPE resource_format AS ENUM ('ARTICLE', 'INFOGRAPHIC', 'VIDEO', 'GUIDE');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- ============================================================
-- 1. USERS
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id                 BIGSERIAL       PRIMARY KEY,
    first_name         VARCHAR(255)    NOT NULL,
    last_name          VARCHAR(255)    NOT NULL,
    email              VARCHAR(255)    NOT NULL UNIQUE,
    password_hash      VARCHAR(255)    NOT NULL,
    birth_date         DATE,
    gender             VARCHAR(50),
    is_email_verified  BOOLEAN         NOT NULL DEFAULT FALSE,
    preferred_language VARCHAR(10)     NOT NULL DEFAULT 'es',
    created_at         TIMESTAMP       DEFAULT NOW()
);

-- ============================================================
-- 2. HEALTH_PROFILES  (1-a-1 con users)
-- ============================================================
CREATE TABLE IF NOT EXISTS health_profiles (
    id                 BIGSERIAL       PRIMARY KEY,
    user_id            BIGINT          NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    blood_type         VARCHAR(10),
    blood_pressure     VARCHAR(30),
    glucose_level      NUMERIC(6, 2),
    cholesterol_level  NUMERIC(6, 2),
    allergies          TEXT,
    weight_kg          NUMERIC(6, 2),
    height_cm          NUMERIC(6, 2),
    bmi                NUMERIC(5, 2),
    bmi_category       VARCHAR(20),
    vital_alert_flag   BOOLEAN         NOT NULL DEFAULT FALSE,
    alert_message      TEXT
);

-- ============================================================
-- 3. ACTIVITY_LOGS
-- ============================================================
CREATE TABLE IF NOT EXISTS activity_logs (
    id             BIGSERIAL     PRIMARY KEY,
    user_id        BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    log_date       DATE          NOT NULL,
    steps_count    INTEGER       DEFAULT 0,
    water_liters   NUMERIC(4, 2) DEFAULT 0.00,
    sleep_hours    NUMERIC(4, 2) DEFAULT 0.00,
    calories_kcal  INTEGER       DEFAULT 0
);

-- ============================================================
-- 4. APPOINTMENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS appointments (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    doctor_name      VARCHAR(255),
    specialty        VARCHAR(255),
    clinic_name      VARCHAR(255),
    appointment_date DATE         NOT NULL,
    appointment_time TIME,
    notes            TEXT,
    is_confirmed     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    DEFAULT NOW()
);

-- ============================================================
-- 5. DAILY_TIPS
-- ============================================================
CREATE TABLE IF NOT EXISTS daily_tips (
    id         BIGSERIAL  PRIMARY KEY,
    content    TEXT       NOT NULL,
    category   VARCHAR(255),
    is_active  BOOLEAN    DEFAULT TRUE,
    created_at TIMESTAMP  DEFAULT NOW()
);

-- ============================================================
-- 6. EDUCATIONAL_RESOURCES
-- ============================================================
CREATE TABLE IF NOT EXISTS educational_resources (
    id           BIGSERIAL          PRIMARY KEY,
    title        VARCHAR(255)       NOT NULL,
    category     VARCHAR(255)       NOT NULL,
    description  TEXT,
    content      TEXT,
    image_url    VARCHAR(255),
    author       VARCHAR(255),
    format_type  resource_format,
    download_url VARCHAR(255),
    is_published BOOLEAN            DEFAULT TRUE,
    published_at TIMESTAMP,
    created_at   TIMESTAMP          DEFAULT NOW()
);

-- ============================================================
-- 7. MEDICATIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS medications (
    id                    BIGSERIAL    PRIMARY KEY,
    user_id               BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name                  VARCHAR(255) NOT NULL,
    dosage                VARCHAR(255),
    frequency             VARCHAR(255),
    start_date            DATE,
    end_date              DATE,
    is_shared_with_partner BOOLEAN     NOT NULL DEFAULT FALSE,
    is_completed_today    BOOLEAN      NOT NULL DEFAULT FALSE
);

-- ============================================================
-- 8. REMINDERS
-- ============================================================
CREATE TABLE IF NOT EXISTS reminders (
    id             BIGSERIAL    PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title          VARCHAR(255) NOT NULL,
    reminder_type  VARCHAR(50)  NOT NULL,
    scheduled_date DATE,
    scheduled_time TIME,
    is_done        BOOLEAN      NOT NULL DEFAULT FALSE
);

-- ============================================================
-- 9. USER_FAVORITE_RESOURCES  (tabla puente ManyToMany)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_favorite_resources (
    user_id     BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    resource_id BIGINT    NOT NULL REFERENCES educational_resources(id) ON DELETE CASCADE,
    saved_at    TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (user_id, resource_id)
);

-- ============================================================
-- ÍNDICES útiles
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_activity_logs_user_date   ON activity_logs(user_id, log_date);
CREATE INDEX IF NOT EXISTS idx_appointments_user         ON appointments(user_id);
CREATE INDEX IF NOT EXISTS idx_medications_user          ON medications(user_id);
CREATE INDEX IF NOT EXISTS idx_reminders_user            ON reminders(user_id);
CREATE INDEX IF NOT EXISTS idx_edu_resources_category    ON educational_resources(category);
CREATE INDEX IF NOT EXISTS idx_edu_resources_published   ON educational_resources(is_published);
CREATE INDEX IF NOT EXISTS idx_daily_tips_active         ON daily_tips(is_active);

-- ============================================================
-- DATOS DE PRUEBA  (opcional – comentar si no se necesitan)
-- ============================================================

-- Usuario de prueba (password: "test1234" — en prod usar hash real)
INSERT INTO users (first_name, last_name, email, password_hash, birth_date, gender, is_email_verified, preferred_language)
VALUES
  ('Ana',   'García',   'ana.garcia@demo.com',   '$2a$10$examplehashAna',   '1990-05-15', 'Femenino',   TRUE,  'es'),
  ('Carlos','López',    'carlos.lopez@demo.com',  '$2a$10$examplehashCarlos','1985-11-22', 'Masculino',  TRUE,  'es'),
  ('María', 'Rodríguez','maria.rod@demo.com',     '$2a$10$examplehashMaria', '1995-03-08', 'Femenino',   FALSE, 'es')
ON CONFLICT DO NOTHING;

-- Perfiles de salud
INSERT INTO health_profiles (user_id, blood_type, blood_pressure, glucose_level, cholesterol_level, weight_kg, height_cm, bmi, bmi_category, vital_alert_flag)
VALUES
  (1, 'O+',  '120/80', 95.00,  185.00, 62.00,  165.00, 22.77, 'Normal',     FALSE),
  (2, 'A+',  '130/85', 110.00, 210.00, 82.00,  178.00, 25.87, 'Sobrepeso',  FALSE),
  (3, 'B-',  '118/76', 88.00,  175.00, 55.00,  160.00, 21.48, 'Normal',     FALSE)
ON CONFLICT DO NOTHING;

-- Tips diarios
INSERT INTO daily_tips (content, category, is_active) VALUES
  ('Toma al menos 8 vasos de agua al día para mantenerte hidratado.',        'Hidratación', TRUE),
  ('Caminar 30 minutos al día reduce el riesgo cardiovascular en un 30%.',   'Actividad',   TRUE),
  ('Dormir entre 7 y 9 horas mejora tu sistema inmunológico.',               'Sueño',       TRUE),
  ('Consume al menos 5 porciones de frutas y verduras diariamente.',         'Nutrición',   TRUE),
  ('El estrés crónico puede elevar tu presión arterial. Practica meditación.','Bienestar',  TRUE)
ON CONFLICT DO NOTHING;

-- Recursos educativos
INSERT INTO educational_resources (title, category, description, author, format_type, is_published, published_at, created_at) VALUES
  ('Guía de alimentación saludable',        'Nutrición',    'Todo lo que necesitas saber sobre una dieta equilibrada.',   'Equipo AuraHealth', 'GUIDE',       TRUE, NOW(), NOW()),
  ('Cómo controlar tu presión arterial',    'Cardiología',  'Consejos prácticos para mantener la presión bajo control.',  'Dr. Ramírez',       'ARTICLE',     TRUE, NOW(), NOW()),
  ('Yoga para principiantes',               'Actividad',    'Rutina básica de yoga para mejorar tu flexibilidad.',        'Instructora López', 'VIDEO',       TRUE, NOW(), NOW()),
  ('Infografía: señales de alerta glucosa', 'Diabetes',     'Identifica los síntomas de glucosa alta o baja.',            'Equipo AuraHealth', 'INFOGRAPHIC', TRUE, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Medicamentos de prueba
INSERT INTO medications (user_id, name, dosage, frequency, start_date, is_shared_with_partner, is_completed_today) VALUES
  (1, 'Paracetamol', '500mg',  'Cada 8 horas',  '2025-01-10', FALSE, FALSE),
  (2, 'Metformina',  '850mg',  'Cada 12 horas', '2024-06-01', TRUE,  TRUE),
  (3, 'Vitamina D',  '1000 UI','Una vez al día', '2025-03-15', FALSE, FALSE)
ON CONFLICT DO NOTHING;

-- Recordatorios de prueba
INSERT INTO reminders (user_id, title, reminder_type, scheduled_date, scheduled_time, is_done) VALUES
  (1, 'Tomar Paracetamol',           'MEDICATION',   '2026-05-12', '08:00', FALSE),
  (2, 'Cita con cardiólogo',         'APPOINTMENT',  '2026-05-20', '10:30', FALSE),
  (3, 'Medir glucosa',               'VITAL_SIGN',   '2026-05-11', '07:00', FALSE)
ON CONFLICT DO NOTHING;

-- Logs de actividad
INSERT INTO activity_logs (user_id, log_date, steps_count, water_liters, sleep_hours, calories_kcal) VALUES
  (1, '2026-05-09', 8500,  2.00, 7.50, 1800),
  (1, '2026-05-10', 6200,  1.75, 8.00, 1650),
  (2, '2026-05-09', 10000, 2.50, 6.00, 2200),
  (3, '2026-05-10', 4300,  1.50, 9.00, 1400)
ON CONFLICT DO NOTHING;

-- Citas médicas
INSERT INTO appointments (user_id, doctor_name, specialty, clinic_name, appointment_date, appointment_time, is_confirmed) VALUES
  (1, 'Dra. Pereira',  'Nutricionista', 'Clínica Salud Integral', '2026-05-20', '09:00', TRUE),
  (2, 'Dr. Sánchez',   'Cardiólogo',    'Hospital Central',        '2026-05-22', '11:00', FALSE),
  (3, 'Dra. Torres',   'Médico General','Centro Médico Norte',     '2026-05-25', '15:30', TRUE)
ON CONFLICT DO NOTHING;

-- Favoritos de recursos educativos
INSERT INTO user_favorite_resources (user_id, resource_id) VALUES
  (1, 1), (1, 3),
  (2, 2), (2, 4),
  (3, 1)
ON CONFLICT DO NOTHING;
