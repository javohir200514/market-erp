
-- AuthRepository JDBC INSERT amallari uchun xavfsiz default qiymatlar.

ALTER TABLE tenants
    ALTER COLUMN version SET DEFAULT 0;

ALTER TABLE roles
    ALTER COLUMN version SET DEFAULT 0;

ALTER TABLE users
    ALTER COLUMN version SET DEFAULT 0,
    ALTER COLUMN failed_login_attempts SET DEFAULT 0;

ALTER TABLE user_roles
    ALTER COLUMN version SET DEFAULT 0;

ALTER TABLE permissions
    ALTER COLUMN version SET DEFAULT 0;
