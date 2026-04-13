CREATE TABLE signing_keys (
    kid VARCHAR(255) NOT NULL PRIMARY KEY,
    private_key TEXT NOT NULL,
    public_key TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE oauth_clients (
    id UUID NOT NULL PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL UNIQUE,
    client_secret_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    redirect_uris TEXT NOT NULL,
    allowed_scopes VARCHAR(1024) NOT NULL DEFAULT 'openid profile email',
    access_token_ttl INTEGER NOT NULL DEFAULT 3600,
    refresh_token_ttl INTEGER NOT NULL DEFAULT 2592000,
    require_pkce BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE oauth_users (
    id UUID NOT NULL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

CREATE TABLE auth_codes (
    code VARCHAR(255) NOT NULL PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    redirect_uri VARCHAR(2048) NOT NULL,
    scope VARCHAR(1024) NOT NULL,
    code_challenge VARCHAR(255),
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_auth_codes_client FOREIGN KEY (client_id) REFERENCES oauth_clients(client_id),
    CONSTRAINT fk_auth_codes_user FOREIGN KEY (user_id) REFERENCES oauth_users(id)
);

CREATE TABLE access_tokens (
    jti VARCHAR(255) NOT NULL PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    scope VARCHAR(1024) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_access_tokens_client FOREIGN KEY (client_id) REFERENCES oauth_clients(client_id),
    CONSTRAINT fk_access_tokens_user FOREIGN KEY (user_id) REFERENCES oauth_users(id)
);

CREATE TABLE refresh_tokens (
    token VARCHAR(255) NOT NULL PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    scope VARCHAR(1024) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_client FOREIGN KEY (client_id) REFERENCES oauth_clients(client_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES oauth_users(id)
);
