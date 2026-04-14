-- Feature 2: Add roles to users (comma-separated, e.g. "admin,editor")
ALTER TABLE oauth_users ADD COLUMN roles VARCHAR(1024) DEFAULT '';

-- Feature 6: Add require_consent to clients
ALTER TABLE oauth_clients ADD COLUMN require_consent BOOLEAN NOT NULL DEFAULT FALSE;

-- Feature 1: Make user_id nullable for client_credentials tokens (no user involved)
ALTER TABLE access_tokens ALTER COLUMN user_id DROP NOT NULL;

-- Update seed demo user with sample roles
UPDATE oauth_users SET roles = 'user,analyst' WHERE id = '00000000-0000-0000-0000-000000000002';
