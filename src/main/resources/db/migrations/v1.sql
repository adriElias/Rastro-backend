-- =========================================
-- RASTRO - DATABASE SCHEMA
-- PostgreSQL
-- =========================================


-- =========================================
-- 1. ENUM TYPES
-- =========================================

CREATE TYPE user_role_enum AS ENUM (
    'ADMIN',
    'USER'
);

CREATE TYPE account_status_enum AS ENUM (
    'ACTIVE',
    'BLOCKED'
);

CREATE TYPE post_type_enum AS ENUM (
    'LOST',
    'FOUND'
);


-- =========================================
-- 2. APP_USER
-- =========================================

CREATE TABLE app_user
(
    id                UUID PRIMARY KEY,

    name              VARCHAR(10)         NOT NULL,
    last_name         VARCHAR(10)         NOT NULL,

    email             VARCHAR(150)        NOT NULL UNIQUE,
    password_hash     VARCHAR(255)        NOT NULL,

    phone_number      VARCHAR(20),

    registration_date TIMESTAMPTZ         NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    role              user_role_enum      NOT NULL
        DEFAULT 'USER',

    account_status    account_status_enum NOT NULL
        DEFAULT 'ACTIVE',

    CONSTRAINT chk_user_name_length
        CHECK (char_length(name) BETWEEN 3 AND 10),

    CONSTRAINT chk_user_last_name_length
        CHECK (char_length(last_name) BETWEEN 3 AND 10)
);


-- =========================================
-- 3. ANIMAL_TYPE
-- =========================================

CREATE TABLE animal_type
(
    id          UUID PRIMARY KEY,

    title       VARCHAR(100) NOT NULL UNIQUE,

    description TEXT
);


-- =========================================
-- 4. PUBLICATION_STATUS
-- =========================================

CREATE TABLE publication_status
(
    id          UUID PRIMARY KEY,

    title       VARCHAR(100) NOT NULL UNIQUE,

    description TEXT
);


-- =========================================
-- 5. LOCATION
-- =========================================

CREATE TABLE location
(
    id        UUID PRIMARY KEY,

    latitude  NUMERIC(9, 6) NOT NULL,

    longitude NUMERIC(9, 6) NOT NULL,

    CONSTRAINT chk_location_latitude
        CHECK (latitude BETWEEN -90 AND 90),

    CONSTRAINT chk_location_longitude
        CHECK (longitude BETWEEN -180 AND 180)
);


-- =========================================
-- 6. POST
-- =========================================

CREATE TABLE post
(
    id                    UUID PRIMARY KEY,

    user_id               UUID         NOT NULL,

    title                 VARCHAR(150)   NOT NULL,

    photo                 VARCHAR(2048),

    description           TEXT           NOT NULL,

    animal_type_id        UUID         NOT NULL,

    created_date          TIMESTAMPTZ    NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    last_update           TIMESTAMPTZ    NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    publication_status_id UUID         NOT NULL,

    is_safekeeping        BOOLEAN        NOT NULL
        DEFAULT FALSE,

    approx_age            VARCHAR(50),

    post_type             post_type_enum NOT NULL,

    location_id           UUID         NOT NULL,

    search_radius         NUMERIC(6, 2)  NOT NULL,

    -- =====================================
    -- FOREIGN KEYS
    -- =====================================

    CONSTRAINT fk_post_user
        FOREIGN KEY (user_id)
            REFERENCES app_user (id),

    CONSTRAINT fk_post_animal_type
        FOREIGN KEY (animal_type_id)
            REFERENCES animal_type (id),

    CONSTRAINT fk_post_publication_status
        FOREIGN KEY (publication_status_id)
            REFERENCES publication_status (id),

    CONSTRAINT fk_post_location
        FOREIGN KEY (location_id)
            REFERENCES location (id),

    -- =====================================
    -- VALIDATIONS
    -- =====================================

    CONSTRAINT chk_post_search_radius
        CHECK (search_radius > 0)
);


-- =========================================
-- 7. TAG
-- =========================================

CREATE TABLE tag
(
    id          UUID PRIMARY KEY,

    title       VARCHAR(100) NOT NULL UNIQUE,

    description TEXT
);


-- =========================================
-- 8. POST_TAG
-- N:M relationship between POST and TAG
-- =========================================

CREATE TABLE post_tag
(
    post_id UUID NOT NULL,

    tag_id  UUID NOT NULL,

    PRIMARY KEY (post_id, tag_id),

    CONSTRAINT fk_post_tag_post
        FOREIGN KEY (post_id)
            REFERENCES post (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_post_tag_tag
        FOREIGN KEY (tag_id)
            REFERENCES tag (id)
            ON DELETE CASCADE
);


-- =========================================
-- 9. MODERATION
-- =========================================

CREATE TABLE moderation
(
    id                    UUID PRIMARY KEY,

    post_id               UUID        NOT NULL,

    admin_id              UUID        NOT NULL,

    action                VARCHAR(50) NOT NULL,

    description           TEXT,

    reason                TEXT,

    reason_date           TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    publication_status_id UUID,

    -- =====================================
    -- FOREIGN KEYS
    -- =====================================

    CONSTRAINT fk_moderation_post
        FOREIGN KEY (post_id)
            REFERENCES post (id),

    CONSTRAINT fk_moderation_admin
        FOREIGN KEY (admin_id)
            REFERENCES app_user (id),

    CONSTRAINT fk_moderation_publication_status
        FOREIGN KEY (publication_status_id)
            REFERENCES publication_status (id)
);


-- =========================================
-- 10. INDEXES
-- =========================================

CREATE INDEX idx_post_user_id
    ON post (user_id);

CREATE INDEX idx_post_animal_type_id
    ON post (animal_type_id);

CREATE INDEX idx_post_publication_status_id
    ON post (publication_status_id);

CREATE INDEX idx_post_post_type
    ON post (post_type);

CREATE INDEX idx_post_created_date
    ON post (created_date);

CREATE INDEX idx_post_location_id
    ON post (location_id);

CREATE INDEX idx_post_tag_tag_id
    ON post_tag (tag_id);

CREATE INDEX idx_moderation_post_id
    ON moderation (post_id);

CREATE INDEX idx_moderation_admin_id
    ON moderation (admin_id);


-- =========================================
-- 11. INITIAL PUBLICATION STATUS
-- =========================================

INSERT INTO publication_status (id, title, description)
VALUES
    (gen_random_uuid(), 'ACTIVE',
     'Publication is active and visible'),

    (gen_random_uuid(), 'RESOLVED',
     'Animal has been found or reunited with its owner'),

    (gen_random_uuid(), 'CLOSED',
     'Publication has been closed by the user'),

    (gen_random_uuid(), 'REMOVED',
     'Publication has been removed by an administrator');


-- =========================================
-- 12. INITIAL ANIMAL TYPE
-- =========================================

INSERT INTO animal_type (id, title, description)
VALUES
    (gen_random_uuid(), 'DOG',
     'Dog'),

    (gen_random_uuid(), 'CAT',
     'Cat'),

    (gen_random_uuid(), 'BIRD',
     'Bird'),

    (gen_random_uuid(), 'OTHER',
     'Other animal');