-- ==============================================================================
-- Module 1: Seed Initial System Roles
-- Reserved Range: V100 - V199
-- ==============================================================================
-- Note: As mandated by architectural security guidelines, no administrative
-- user credentials or passwords are hardcoded in migration scripts.
-- An initial administrator is bootstrapped exclusively through explicit
-- environment variables (BOOTSTRAP_ADMIN_ENABLED=true) on application startup.
-- ==============================================================================

INSERT INTO roles (name, description) VALUES
('ROLE_ADMIN', 'System Administrator with full access'),
('ROLE_FLEET_MANAGER', 'Fleet & Asset Manager'),
('ROLE_DISPATCHER', 'Logistics Dispatcher'),
('ROLE_DRIVER', 'Commercial Fleet Driver'),
('ROLE_VIEWER', 'Auditor & Executive Read-Only Viewer')
ON DUPLICATE KEY UPDATE name=name;
