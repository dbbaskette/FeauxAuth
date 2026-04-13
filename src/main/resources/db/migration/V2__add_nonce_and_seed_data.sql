-- Add nonce column to auth_codes for OIDC ID token support
ALTER TABLE auth_codes ADD COLUMN nonce VARCHAR(255);

-- Seed a demo OAuth client
-- client_secret plaintext: demo-secret
-- BCrypt hash generated with cost factor 10
INSERT INTO oauth_clients (id, client_id, client_secret_hash, name, redirect_uris, allowed_scopes, access_token_ttl, refresh_token_ttl, require_pkce, enabled)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'demo-app',
    '$2a$10$LAFFnpbkFdjVc38XzPeNqOlLxxlfUVwtEuJAr6uF3wjPVam0.rjI2',
    'Demo Application',
    'http://localhost:3000/callback',
    'openid profile email offline_access',
    3600,
    2592000,
    false,
    true
);

-- Seed a demo user
-- password plaintext: password
-- BCrypt hash generated with cost factor 10
INSERT INTO oauth_users (id, email, display_name, password_hash, enabled)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    'demo@feauxauth.local',
    'Demo User',
    '$2a$10$7hS..nT1n.ALeWVGGKbczuGpcDPJ3PntzQNnMutZNT1j7.lfJK0pm',
    true
);
