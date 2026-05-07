-- Per-client role claim — used when minting access tokens for the
-- client_credentials grant. Comma-separated, same shape as oauth_users.roles.
ALTER TABLE oauth_clients ADD COLUMN roles VARCHAR(1024) DEFAULT '';
