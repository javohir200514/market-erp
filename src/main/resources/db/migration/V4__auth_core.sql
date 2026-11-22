
ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS trial_ends_at TIMESTAMPTZ;

-- Slug formati: kichik lotin harflari, raqamlar, o'rtada "-" (login sahifasi shuni kutadi).
-- NOT VALID: eski qatorlar tekshirilmaydi, faqat yangi/o'zgargan qatorlar.
ALTER TABLE tenants
    ADD CONSTRAINT chk_tenants_slug_format
        CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$') NOT VALID;

-- ---------------------------------------------------------------------------
-- 2. Users: butunlik qoidalari
-- ---------------------------------------------------------------------------
ALTER TABLE users
    ADD CONSTRAINT chk_users_failed_attempts_non_negative
        CHECK (failed_login_attempts >= 0);

-- Login so'rovi: tenant + lower(email). Noyob indeks (uq_users_tenant_email) buni qoplaydi.
-- Bloklangan hisoblarni tez topish uchun (monitoring / tozalash joblari):
CREATE INDEX IF NOT EXISTS idx_users_locked_until
    ON users (locked_until)
    WHERE locked_until IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 3. Tizim ruxsatlari (permissions) — global katalog
--    Kod formati: <modul>.<resurs>.<amal>   masalan: platform.user.create
--    Amal qiymatlari V1 dagi chk_permissions_action bilan mos: READ, CREATE, UPDATE, DELETE, APPROVE, EXPORT
-- ---------------------------------------------------------------------------
INSERT INTO permissions (id, code, module, action, description, created_at, updated_at, created_by)
SELECT gen_random_uuid(),
       'platform.' || v.resource || '.' || lower(v.action),
       'platform',
       v.action,
       'Platform: ' || v.resource || ' / ' || lower(v.action),
       now(),
       now(),
       'system'
FROM (VALUES
          ('tenant',     'READ'),
          ('tenant',     'UPDATE'),

          ('company',    'READ'),
          ('company',    'CREATE'),
          ('company',    'UPDATE'),
          ('company',    'DELETE'),

          ('branch',     'READ'),
          ('branch',     'CREATE'),
          ('branch',     'UPDATE'),
          ('branch',     'DELETE'),

          ('department', 'READ'),
          ('department', 'CREATE'),
          ('department', 'UPDATE'),
          ('department', 'DELETE'),

          ('user',       'READ'),
          ('user',       'CREATE'),
          ('user',       'UPDATE'),
          ('user',       'DELETE'),

          ('role',       'READ'),
          ('role',       'CREATE'),
          ('role',       'UPDATE'),
          ('role',       'DELETE'),

          ('audit',      'READ'),
          ('audit',      'EXPORT'),

          ('settings',   'READ'),
          ('settings',   'UPDATE')
     ) AS v(resource, action)
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 4. Mavjud tenantlarning tizim ADMIN rollariga yangi ruxsatlarni berish
--    (yangi tenantlar uchun buni ro'yxatdan o'tish servisi qiladi)
-- ---------------------------------------------------------------------------
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.system_role = TRUE
  AND r.code = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;