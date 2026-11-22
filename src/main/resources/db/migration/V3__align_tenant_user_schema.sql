-- Align the initial schema with the current Tenant and User entity mappings.
-- Kept as V3 so databases that already applied V1 are not affected by checksum changes.

ALTER TABLE tenants
    ALTER COLUMN name TYPE VARCHAR(100),
    ALTER COLUMN slug TYPE VARCHAR(100),
    ADD COLUMN code VARCHAR(100),
    ADD COLUMN email VARCHAR(255),
    ADD COLUMN phone VARCHAR(30),
    ADD COLUMN description VARCHAR(500),
    ADD COLUMN default_currency VARCHAR(3) NOT NULL DEFAULT 'UZS';

UPDATE tenants
SET code = UPPER(slug)
WHERE code IS NULL;

ALTER TABLE tenants
    ALTER COLUMN code SET NOT NULL,
    ADD CONSTRAINT uq_tenants_code UNIQUE (code);

ALTER TABLE tenants
    RENAME COLUMN time_zone TO default_time_zone;

ALTER TABLE tenants
    ALTER COLUMN default_time_zone TYPE VARCHAR(50),
    ALTER COLUMN status TYPE VARCHAR(30);

ALTER TABLE tenants
    DROP CONSTRAINT chk_tenants_status,
    ADD CONSTRAINT chk_tenants_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'DISABLED', 'CANCELLED'));

ALTER TABLE users
    ALTER COLUMN email TYPE VARCHAR(255),
    ALTER COLUMN password_hash TYPE VARCHAR(255),
    ALTER COLUMN phone TYPE VARCHAR(30),
    ALTER COLUMN time_zone TYPE VARCHAR(50),
    ALTER COLUMN status TYPE VARCHAR(30);

ALTER TABLE users
    DROP CONSTRAINT chk_users_status,
    ADD CONSTRAINT chk_users_status
        CHECK (status IN ('INVITED', 'ACTIVE', 'DISABLED', 'SUSPENDED'));
