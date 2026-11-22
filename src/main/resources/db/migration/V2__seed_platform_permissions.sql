-- Seed the platform-core permission catalog.
-- Tenant roles receive the permissions available at the time of tenant registration.
INSERT INTO permissions (id, code, module, action, description, created_at, updated_at, version)
VALUES
    ('10000000-0000-7000-8000-000000000001', 'platform.tenant:read',       'platform', 'READ',   'View tenant settings',              now(), now(), 0),
    ('10000000-0000-7000-8000-000000000002', 'platform.tenant:update',     'platform', 'UPDATE', 'Update tenant settings',            now(), now(), 0),
    ('10000000-0000-7000-8000-000000000003', 'platform.company:read',     'platform', 'READ',   'View companies',                     now(), now(), 0),
    ('10000000-0000-7000-8000-000000000004', 'platform.company:create',   'platform', 'CREATE', 'Create companies',                   now(), now(), 0),
    ('10000000-0000-7000-8000-000000000005', 'platform.company:update',   'platform', 'UPDATE', 'Update companies',                   now(), now(), 0),
    ('10000000-0000-7000-8000-000000000006', 'platform.company:delete',   'platform', 'DELETE', 'Deactivate companies',               now(), now(), 0),
    ('10000000-0000-7000-8000-000000000007', 'platform.branch:read',      'platform', 'READ',   'View branches',                      now(), now(), 0),
    ('10000000-0000-7000-8000-000000000008', 'platform.branch:create',    'platform', 'CREATE', 'Create branches',                    now(), now(), 0),
    ('10000000-0000-7000-8000-000000000009', 'platform.branch:update',    'platform', 'UPDATE', 'Update branches',                    now(), now(), 0),
    ('10000000-0000-7000-8000-000000000010', 'platform.branch:delete',    'platform', 'DELETE', 'Deactivate branches',                now(), now(), 0),
    ('10000000-0000-7000-8000-000000000011', 'platform.department:read', 'platform', 'READ',   'View departments',                   now(), now(), 0),
    ('10000000-0000-7000-8000-000000000012', 'platform.department:create','platform', 'CREATE', 'Create departments',                now(), now(), 0),
    ('10000000-0000-7000-8000-000000000013', 'platform.department:update','platform', 'UPDATE', 'Update departments',                now(), now(), 0),
    ('10000000-0000-7000-8000-000000000014', 'platform.department:delete','platform', 'DELETE', 'Deactivate departments',            now(), now(), 0),
    ('10000000-0000-7000-8000-000000000015', 'platform.user:read',        'platform', 'READ',   'View users',                         now(), now(), 0),
    ('10000000-0000-7000-8000-000000000016', 'platform.user:create',      'platform', 'CREATE', 'Create users',                       now(), now(), 0),
    ('10000000-0000-7000-8000-000000000017', 'platform.user:update',      'platform', 'UPDATE', 'Update users',                       now(), now(), 0),
    ('10000000-0000-7000-8000-000000000018', 'platform.user:delete',      'platform', 'DELETE', 'Disable users',                      now(), now(), 0),
    ('10000000-0000-7000-8000-000000000019', 'platform.role:read',        'platform', 'READ',   'View roles',                         now(), now(), 0),
    ('10000000-0000-7000-8000-000000000020', 'platform.role:create',      'platform', 'CREATE', 'Create roles',                       now(), now(), 0),
    ('10000000-0000-7000-8000-000000000021', 'platform.role:update',      'platform', 'UPDATE', 'Update roles',                       now(), now(), 0),
    ('10000000-0000-7000-8000-000000000022', 'platform.role:delete',      'platform', 'DELETE', 'Delete custom roles',                now(), now(), 0),
    ('10000000-0000-7000-8000-000000000023', 'platform.role:approve',     'platform', 'APPROVE','Approve restricted role operations', now(), now(), 0),
    ('10000000-0000-7000-8000-000000000024', 'platform.audit:read',       'platform', 'READ',   'View audit logs',                    now(), now(), 0)
ON CONFLICT (code) DO NOTHING;
