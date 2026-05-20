CREATE TYPE device_state AS ENUM ('available', 'in-use', 'inactive');

CREATE TABLE devices
(
    id            UUID PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    brand         VARCHAR(255) NOT NULL,
    state         device_state NOT NULL    DEFAULT 'available',
    creation_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Function to enforce UPDATE restrictions
CREATE OR REPLACE FUNCTION enforce_device_update_restrictions()
    RETURNS TRIGGER AS $$
BEGIN
    -- Creation time cannot be updated
    IF NEW.creation_time IS DISTINCT FROM OLD.creation_time THEN
        RAISE EXCEPTION 'The creation_time field is immutable and cannot be updated.';
    END IF;

    -- Name and brand cannot be updated if the device is currently in-use,
    IF OLD.state = 'in-use' THEN
        IF NEW.name IS DISTINCT FROM OLD.name OR NEW.brand IS DISTINCT FROM OLD.brand THEN
            RAISE EXCEPTION 'Cannot update name or brand while the device is in-use.';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Function to enforce DELETE restrictions
CREATE OR REPLACE FUNCTION enforce_device_delete_restrictions()
    RETURNS TRIGGER AS $$
BEGIN
    -- In-use devices cannot be deleted
    IF OLD.state = 'in-use' THEN
        RAISE EXCEPTION 'Cannot delete a device that is currently in-use.';
    END IF;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

-- Bind the Triggers to the Table
CREATE TRIGGER trg_devices_update
    BEFORE UPDATE
    ON devices
    FOR EACH ROW
EXECUTE FUNCTION enforce_device_update_restrictions();

CREATE TRIGGER trg_devices_delete
    BEFORE DELETE
    ON devices
    FOR EACH ROW
EXECUTE FUNCTION enforce_device_delete_restrictions();