ALTER TABLE post
    ADD COLUMN lost_at TIMESTAMPTZ,

    ADD CONSTRAINT chk_post_lost_at_required
        CHECK (post_type <> 'LOST' OR lost_at IS NOT NULL),

    ADD CONSTRAINT chk_post_lost_at_not_after_creation
        CHECK (lost_at IS NULL OR lost_at <= created_date),

    ADD CONSTRAINT chk_post_search_radius_max
        CHECK (search_radius <= 50),

    ADD CONSTRAINT uq_post_location_id
        UNIQUE (location_id);

COMMENT ON COLUMN post.search_radius IS
    'Search radius around the referenced location, expressed in kilometers.';
