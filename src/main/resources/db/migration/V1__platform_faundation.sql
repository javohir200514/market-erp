-- Phase 1: platform foundation
-- Shared schema multi-tenancy: every tenant-owned table carries tenant_id (NOT NULL, FK to tenants)
-- and tenant_id participates in every business unique constraint.

CREATE TABLE tenants (
    id              UUID PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    slug            VARCHAR(63)  NOT NULL UNIQUE,
    status          VARCHAR(20)  NOT NULL,
    default_locale  VARCHAR(10)  NOT NULL DEFAULT 'uz',
    time_zone       VARCHAR(64)  NOT NULL DEFAULT 'Asia/Tashkent',
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT chk_tenants_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'CLOSED'))
);

CREATE TABLE permissions (
    id          UUID PRIMARY KEY,
    code        VARCHAR(100) NOT NULL UNIQUE,
    module      VARCHAR(50)  NOT NULL,
    action      VARCHAR(20)  NOT NULL,
    description VARCHAR(300),
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT chk_permissions_action CHECK (action IN ('READ', 'CREATE', 'UPDATE', 'DELETE', 'APPROVE', 'EXPORT'))
);
CREATE INDEX idx_permissions_module ON permissions (module);

CREATE TABLE companies (
    id                UUID PRIMARY KEY,
    tenant_id         UUID         NOT NULL REFERENCES tenants (id),
    code              VARCHAR(30)  NOT NULL,
    legal_name        VARCHAR(250) NOT NULL,
    display_name      VARCHAR(200) NOT NULL,
    tax_id            VARCHAR(50),
    country_code      VARCHAR(2),
    base_currency     VARCHAR(3)   NOT NULL,
    parent_company_id UUID REFERENCES companies (id),
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_companies_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT uq_companies_tenant_id_id UNIQUE (tenant_id, id),
    CONSTRAINT chk_companies_not_own_parent CHECK (parent_company_id IS NULL OR parent_company_id <> id)
);
CREATE INDEX idx_companies_tenant ON companies (tenant_id);
CREATE INDEX idx_companies_parent ON companies (parent_company_id);

CREATE TABLE branches (
    id          UUID PRIMARY KEY,
    tenant_id   UUID         NOT NULL REFERENCES tenants (id),
    company_id  UUID         NOT NULL,
    code        VARCHAR(30)  NOT NULL,
    name        VARCHAR(200) NOT NULL,
    address     VARCHAR(300),
    city        VARCHAR(100),
    phone       VARCHAR(50),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_branches_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT uq_branches_tenant_id_id UNIQUE (tenant_id, id),
    -- composite FK: a branch can only point at a company of the SAME tenant
    CONSTRAINT fk_branches_company FOREIGN KEY (tenant_id, company_id) REFERENCES companies (tenant_id, id)
);
CREATE INDEX idx_branches_tenant_company ON branches (tenant_id, company_id);

CREATE TABLE departments (
    id                   UUID PRIMARY KEY,
    tenant_id            UUID         NOT NULL REFERENCES tenants (id),
    company_id           UUID         NOT NULL,
    branch_id            UUID,
    parent_department_id UUID,
    code                 VARCHAR(30)  NOT NULL,
    name                 VARCHAR(200) NOT NULL,
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    version              BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_departments_company_code UNIQUE (tenant_id, company_id, code),
    CONSTRAINT uq_departments_tenant_id_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_departments_company FOREIGN KEY (tenant_id, company_id) REFERENCES companies (tenant_id, id),
    CONSTRAINT fk_departments_branch FOREIGN KEY (tenant_id, branch_id) REFERENCES branches (tenant_id, id),
    CONSTRAINT fk_departments_parent FOREIGN KEY (tenant_id, parent_department_id) REFERENCES departments (tenant_id, id),
    CONSTRAINT chk_departments_not_own_parent CHECK (parent_department_id IS NULL OR parent_department_id <> id)
);
CREATE INDEX idx_departments_tenant_company ON departments (tenant_id, company_id);

CREATE TABLE users (
    id                    UUID PRIMARY KEY,
    tenant_id             UUID         NOT NULL REFERENCES tenants (id),
    email                 VARCHAR(254) NOT NULL,
    password_hash         VARCHAR(100),
    first_name            VARCHAR(100) NOT NULL,
    last_name             VARCHAR(100) NOT NULL,
    phone                 VARCHAR(50),
    locale                VARCHAR(10)  NOT NULL DEFAULT 'uz',
    time_zone             VARCHAR(64)  NOT NULL DEFAULT 'Asia/Tashkent',
    status                VARCHAR(20)  NOT NULL,
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ,
    last_login_at         TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    version               BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_users_tenant_id_id UNIQUE (tenant_id, id),
    CONSTRAINT chk_users_status CHECK (status IN ('INVITED', 'ACTIVE', 'LOCKED', 'DISABLED'))
);
CREATE UNIQUE INDEX uq_users_tenant_email ON users (tenant_id, lower(email));

CREATE TABLE roles (
    id          UUID PRIMARY KEY,
    tenant_id   UUID         NOT NULL REFERENCES tenants (id),
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(300),
    system_role BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_roles_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT uq_roles_tenant_id_id UNIQUE (tenant_id, id)
);

CREATE TABLE role_permissions (
    role_id       UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions (id),
    PRIMARY KEY (role_id, permission_id)
);
CREATE INDEX idx_role_permissions_permission ON role_permissions (permission_id);

CREATE TABLE user_roles (
    id         UUID PRIMARY KEY,
    tenant_id  UUID        NOT NULL REFERENCES tenants (id),
    user_id    UUID        NOT NULL,
    role_id    UUID        NOT NULL,
    company_id UUID,
    branch_id  UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version    BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT fk_user_roles_user   FOREIGN KEY (tenant_id, user_id)    REFERENCES users (tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role   FOREIGN KEY (tenant_id, role_id)    REFERENCES roles (tenant_id, id),
    CONSTRAINT fk_user_roles_company FOREIGN KEY (tenant_id, company_id) REFERENCES companies (tenant_id, id),
    CONSTRAINT fk_user_roles_branch FOREIGN KEY (tenant_id, branch_id)  REFERENCES branches (tenant_id, id)
);
-- NULL scopes are treated as equal so the same grant cannot be inserted twice
CREATE UNIQUE INDEX uq_user_roles_scope ON user_roles (
    user_id, role_id,
    COALESCE(company_id, '00000000-0000-0000-0000-000000000000'::uuid),
    COALESCE(branch_id,  '00000000-0000-0000-0000-000000000000'::uuid)
);
CREATE INDEX idx_user_roles_tenant_user ON user_roles (tenant_id, user_id);

CREATE TABLE audit_logs (
    id            UUID PRIMARY KEY,
    tenant_id     UUID,
    actor_user_id UUID,
    actor_label   VARCHAR(254),
    action        VARCHAR(20)  NOT NULL,
    entity_type   VARCHAR(100) NOT NULL,
    entity_id     VARCHAR(64),
    old_values    JSONB,
    new_values    JSONB,
    ip_address    VARCHAR(45),
    request_id    VARCHAR(64),
    occurred_at   TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_audit_tenant_time   ON audit_logs (tenant_id, occurred_at DESC);
CREATE INDEX idx_audit_entity        ON audit_logs (tenant_id, entity_type, entity_id);
CREATE INDEX idx_audit_actor         ON audit_logs (tenant_id, actor_user_id, occurred_at DESC);

-- Audit rows are append-only: block UPDATE and DELETE at the database level.
CREATE FUNCTION audit_logs_immutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_logs_immutable
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION audit_logs_immutable();
