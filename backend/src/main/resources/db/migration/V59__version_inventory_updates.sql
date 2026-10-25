-- Detect stale catalog edits instead of overwriting concurrent inventory changes.
ALTER TABLE product_variants ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
