-- Seed an example public OAuth client that requires PKCE.
-- client_secret placeholder (plaintext: demo-secret); unused by public PKCE clients.
INSERT INTO oauth_clients (
    id, client_id, client_secret_hash, name, redirect_uris,
    allowed_scopes, access_token_ttl, refresh_token_ttl,
    require_pkce, enabled, require_consent
)
VALUES (
    '00000000-0000-0000-0000-0000000000a1',
    'example-pkce-client',
    '$2a$10$LAFFnpbkFdjVc38XzPeNqOlLxxlfUVwtEuJAr6uF3wjPVam0.rjI2',
    'Example PKCE Client',
    'http://localhost:3000/callback,http://127.0.0.1:3000/callback',
    'openid profile email offline_access',
    3600,
    2592000,
    true,
    true,
    false
);

-- Give the demo user a couple of sample roles
UPDATE oauth_users SET roles = 'analyst,user' WHERE id = '00000000-0000-0000-0000-000000000002';
