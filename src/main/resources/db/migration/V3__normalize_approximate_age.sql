LOCK TABLE post IN ACCESS EXCLUSIVE MODE;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM post
        WHERE approx_age IS NOT NULL
    ) THEN
        RAISE EXCEPTION USING
            ERRCODE = '23514',
            MESSAGE = 'Cannot migrate post.approx_age: non-null legacy values require manual review';
    END IF;
END
$$;

CREATE TYPE age_unit_enum AS ENUM (
    'MONTHS',
    'YEARS'
);

ALTER TABLE post
    ALTER COLUMN approx_age TYPE INTEGER
        USING approx_age::INTEGER,

    ADD COLUMN age_unit age_unit_enum,

    ADD CONSTRAINT chk_post_approx_age_positive
        CHECK (approx_age IS NULL OR approx_age > 0),

    ADD CONSTRAINT chk_post_approx_age_unit_pair
        CHECK (
            (approx_age IS NULL AND age_unit IS NULL)
            OR (approx_age IS NOT NULL AND age_unit IS NOT NULL)
        );

COMMENT ON COLUMN post.approx_age IS
    'Positive approximate animal age; must be provided together with age_unit.';

COMMENT ON COLUMN post.age_unit IS
    'Approximate animal age unit: MONTHS or YEARS; must be provided together with approx_age.';
