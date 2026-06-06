-- 1. Idempotent type registration block using clean string bounds
DO $$
BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'device_state') THEN
CREATE TYPE device_state AS ENUM ('AVAILABLE', 'IN_USE', 'INACTIVE');
END IF;
END $$;

-- 2. Base Table Layout
CREATE TABLE IF NOT EXISTS devices
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    external_id   UUID NOT NULL,
    name          VARCHAR(255) NOT NULL,
    brand         VARCHAR(255) NOT NULL,
    state         device_state NOT NULL    DEFAULT 'AVAILABLE',
    version       BIGINT       NOT NULL    DEFAULT 0,
    creation_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_devices_external_id UNIQUE (external_id)
                                );

-- 3. Manage Search Indexes
DROP INDEX IF EXISTS idx_devices_brand_creation_time;
CREATE INDEX idx_devices_brand_creation_time ON devices (brand, creation_time);

DROP INDEX IF EXISTS idx_devices_state_creation_time;
CREATE INDEX idx_devices_state_creation_time ON devices (state, creation_time);

DROP INDEX IF EXISTS idx_devices_creation_time;
CREATE INDEX idx_devices_creation_time ON devices (creation_time DESC);

-- 4. Function to enforce UPDATE constraints
CREATE OR REPLACE FUNCTION enforce_device_update_restrictions()
    RETURNS TRIGGER AS $$
BEGIN
    IF NEW.creation_time IS DISTINCT FROM OLD.creation_time THEN
        RAISE EXCEPTION 'The creation_time field is immutable and cannot be updated.';
END IF;

    IF OLD.state = 'IN_USE' THEN
        IF NEW.name IS DISTINCT FROM OLD.name OR NEW.brand IS DISTINCT FROM OLD.brand THEN
            RAISE EXCEPTION 'Cannot update name or brand while the device is in-use.';
END IF;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 5. Function to enforce DELETE constraints
CREATE OR REPLACE FUNCTION enforce_device_delete_restrictions()
    RETURNS TRIGGER AS $$
BEGIN
    IF OLD.state = 'IN_USE' THEN
        RAISE EXCEPTION 'Cannot delete a device that is currently in-use.';
END IF;

RETURN OLD;
END;
$$ LANGUAGE plpgsql;

-- 6. Rebind Idempotent Triggers
DROP TRIGGER IF EXISTS trg_devices_update ON devices;
CREATE TRIGGER trg_devices_update
    BEFORE UPDATE ON devices
    FOR EACH ROW
    EXECUTE FUNCTION enforce_device_update_restrictions();

DROP TRIGGER IF EXISTS trg_devices_delete ON devices;
CREATE TRIGGER trg_devices_delete
    BEFORE DELETE ON devices
    FOR EACH ROW
    EXECUTE FUNCTION enforce_device_delete_restrictions();
