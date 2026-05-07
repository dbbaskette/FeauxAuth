-- RFC 8628 OAuth 2.0 Device Authorization Grant
CREATE TABLE device_codes (
    device_code VARCHAR(255) NOT NULL PRIMARY KEY,
    user_code VARCHAR(20) NOT NULL UNIQUE,
    client_id VARCHAR(255) NOT NULL,
    scope VARCHAR(1024) NOT NULL,
    user_id UUID,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    interval_seconds INTEGER NOT NULL DEFAULT 5,
    last_polled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_device_codes_client FOREIGN KEY (client_id) REFERENCES oauth_clients(client_id),
    CONSTRAINT fk_device_codes_user FOREIGN KEY (user_id) REFERENCES oauth_users(id)
);

CREATE INDEX idx_device_codes_user_code ON device_codes(user_code);
CREATE INDEX idx_device_codes_status ON device_codes(status);
