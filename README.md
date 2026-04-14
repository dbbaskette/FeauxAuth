# FeauxAuth

A lightweight OAuth 2.0 / OIDC authorization server built for lab and demo environments. FeauxAuth gives you a fully functional auth server with a React admin UI — deploy it locally with Docker or push it to Cloud Foundry in minutes.

> **Not for production use.** FeauxAuth is designed for development, testing, and demos where you need a real OAuth server without the overhead of Keycloak, Auth0, or Spring Authorization Server.

## Features

- **OAuth 2.0 Authorization Code** flow with optional **PKCE**
- **Client Credentials** grant for machine-to-machine authentication
- **OpenID Connect** discovery, ID tokens, userinfo, nonce, and logout
- **JWT access tokens** signed with RS256 (RSA-2048), auto-generated signing keys
- **User roles** — comma-separated roles included as JSON array in JWT claims
- **Opaque refresh tokens** with configurable TTL
- **Token revocation** (RFC 7009) and **introspection** (RFC 7662)
- **Consent screen** — optional per-client, with human-readable scope descriptions
- **Dynamic client registration** (RFC 7591) — self-service client onboarding
- **CORS** — configurable allowed origins for SPA integration
- **Admin REST API** for managing clients, users, and tokens
- **React admin dashboard** with dark theme
- **Token inspector** — paste a JWT to decode, verify signature, and check revocation
- **Seed data** — ships with a demo client and user so you can test immediately
- **MCP server compatible** — works as the auth server for Model Context Protocol servers
- **H2** for zero-config local dev, **Postgres** for Docker and production
- **Cloud Foundry** ready with java-cfenv auto-reconfiguration

## Quick Start

### Docker (recommended)

```bash
git clone https://github.com/dbbaskette/FeauxAuth.git
cd FeauxAuth
docker compose up --build
```

App runs at **http://localhost:8080**. Admin UI at **http://localhost:8080/admin/**.

Default admin credentials: `admin` / `feauxauth`

### Bare Metal

Requires Java 21 and Maven 3.9+.

```bash
git clone https://github.com/dbbaskette/FeauxAuth.git
cd FeauxAuth
mvn clean package
java -jar target/feauxauth-1.0.0.jar
```

This uses an embedded H2 database — no Postgres needed.

### Seed Data

FeauxAuth ships with a pre-configured demo client and user so you can test the OAuth flow immediately:

| Resource | Credential | Value |
|----------|-----------|-------|
| Demo Client | `client_id` | `demo-app` |
| | `client_secret` | `demo-secret` |
| | `redirect_uri` | `http://localhost:3000/callback` |
| | `scopes` | `openid profile email offline_access` |
| Demo User | `email` | `demo@feauxauth.local` |
| | `password` | `password` |
| | `roles` | `user,analyst` |

### Cloud Foundry

```bash
mvn clean package
cf push
```

Requires a Postgres service instance named `feauxauth-db`. The `manifest.yml` is pre-configured.

## Usage

> **Tip:** The seed data includes a `demo-app` client and `demo@feauxauth.local` user. Skip steps 1-2 if you just want to try the flow.

### 1. Create an OAuth Client

Log into the admin UI at `/admin/` or use the API:

```bash
curl -u admin:feauxauth -X POST http://localhost:8080/api/admin/clients \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My App",
    "clientId": "my-app",
    "redirectUris": "http://localhost:3000/callback",
    "allowedScopes": "openid profile email",
    "requirePkce": false,
    "requireConsent": true
  }'
```

Save the `clientSecret` from the response — it won't be shown again.

You can also use **dynamic client registration** (no admin auth needed):

```bash
curl -X POST http://localhost:8080/oauth/register \
  -H "Content-Type: application/json" \
  -d '{
    "client_name": "My App",
    "redirect_uris": ["http://localhost:3000/callback"],
    "scope": "openid profile email",
    "grant_types": ["authorization_code"],
    "token_endpoint_auth_method": "client_secret_post"
  }'
```

### 2. Create a User

```bash
curl -u admin:feauxauth -X POST http://localhost:8080/api/admin/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "displayName": "Alice",
    "password": "password123",
    "roles": "admin,editor"
  }'
```

### 3. Run the OAuth Flow

**Start authorization** (uses the seed demo-app client):
```
http://localhost:8080/oauth/authorize?client_id=demo-app&redirect_uri=http://localhost:3000/callback&response_type=code&scope=openid%20email&state=random123
```

Log in with `demo@feauxauth.local` / `password`. FeauxAuth redirects back with an authorization code.

**Exchange the code for tokens:**
```bash
curl -X POST http://localhost:8080/oauth/token \
  -d "grant_type=authorization_code" \
  -d "code=AUTHORIZATION_CODE" \
  -d "redirect_uri=http://localhost:3000/callback" \
  -d "client_id=demo-app" \
  -d "client_secret=demo-secret"
```

Response:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "dGhpcyBpcyBhIHJlZnJl...",
  "id_token": "eyJhbGciOiJSUzI1NiIs..."
}
```

**Get user info:**
```bash
curl -H "Authorization: Bearer ACCESS_TOKEN" \
  http://localhost:8080/oauth/userinfo
```

### 4. PKCE Flow

For public clients (SPAs, mobile apps), use PKCE:

```bash
# Generate code_verifier and code_challenge
CODE_VERIFIER=$(openssl rand -base64 32 | tr -d '=/+' | cut -c1-43)
CODE_CHALLENGE=$(echo -n "$CODE_VERIFIER" | openssl dgst -sha256 -binary | base64 | tr -d '=' | tr '/+' '_-')

# Authorize with code_challenge
http://localhost:8080/oauth/authorize?client_id=my-app&redirect_uri=...&response_type=code&code_challenge=$CODE_CHALLENGE&code_challenge_method=S256

# Exchange with code_verifier (no client_secret needed)
curl -X POST http://localhost:8080/oauth/token \
  -d "grant_type=authorization_code" \
  -d "code=AUTHORIZATION_CODE" \
  -d "redirect_uri=http://localhost:3000/callback" \
  -d "client_id=my-app" \
  -d "code_verifier=$CODE_VERIFIER"
```

### 5. Client Credentials (Machine-to-Machine)

For service-to-service auth with no user involved:

```bash
curl -X POST http://localhost:8080/oauth/token \
  -d "grant_type=client_credentials" \
  -d "client_id=demo-app" \
  -d "client_secret=demo-secret" \
  -d "scope=openid"
```

Response:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

The token's `sub` claim is the `client_id` (no user). Client secret is always required for this grant type, regardless of PKCE settings.

### 6. Token Introspection

Resource servers can validate tokens without parsing JWTs themselves:

```bash
curl -X POST http://localhost:8080/oauth/introspect \
  -d "token=eyJhbGciOiJSUzI1NiIs..."
```

Active token response:
```json
{
  "active": true,
  "sub": "alice@example.com",
  "client_id": "my-app",
  "scope": "openid email",
  "token_type": "Bearer",
  "exp": 1713045600,
  "iat": 1713042000,
  "iss": "http://localhost:8080",
  "jti": "a1b2c3d4-..."
}
```

Expired, revoked, or invalid tokens return `{"active": false}` (HTTP 200, per RFC 7662).

### 7. Logout

OIDC RP-Initiated Logout:

```
GET http://localhost:8080/oauth/logout?id_token_hint=eyJ...&post_logout_redirect_uri=http://localhost:3000&state=xyz
```

This revokes all tokens for the user identified by the `id_token_hint`, invalidates the session, and redirects to the `post_logout_redirect_uri`. Without a redirect URI, a "Signed Out" confirmation page is shown.

### 8. User Roles in Tokens

When users have roles assigned (e.g., `admin,editor`), they appear as a JSON array in JWT claims:

```json
{
  "sub": "alice@example.com",
  "scope": "openid profile email",
  "roles": ["admin", "editor"],
  "email": "alice@example.com",
  "name": "Alice"
}
```

Roles are included in access tokens, ID tokens, and the `/oauth/userinfo` response. Configure roles per user via the admin UI or API.

## Using FeauxAuth with MCP Servers

FeauxAuth can serve as the OAuth authorization server for [Model Context Protocol (MCP)](https://modelcontextprotocol.io) servers that require authentication — for example, a Greenplum MCP server that protects database access behind OAuth login.

### How It Works

The MCP spec uses OAuth 2.1 with mandatory PKCE for HTTP-based servers. Three components are involved:

```
MCP Client (Claude Desktop, etc.)
    |
    |-- 1. Connects to MCP server, receives 401
    |-- 2. Discovers auth server via MCP server's resource metadata
    |-- 3. Fetches FeauxAuth's /.well-known/openid-configuration
    |-- 4. Opens browser → FeauxAuth login page
    |-- 5. User authenticates, gets authorization code
    |-- 6. Exchanges code for JWT access token (PKCE)
    |-- 7. Sends Bearer token to MCP server
    v
MCP Server (e.g., Greenplum MCP)
    |-- Validates JWT signature via FeauxAuth's JWKS endpoint
    |-- Checks issuer, audience, expiration
    |-- Grants access to MCP resources
    v
FeauxAuth (Authorization Server)
    |-- Issues RS256-signed JWT access tokens
    |-- Serves OIDC discovery + JWKS for token verification
```

### Step 1: Register an OAuth Client for the MCP Client

Create a PKCE-enabled client in FeauxAuth for the MCP client application (e.g., Claude Desktop). MCP clients are public clients, so enable `requirePkce` and note that no `client_secret` is needed at the token endpoint.

```bash
curl -u admin:feauxauth -X POST http://localhost:8080/api/admin/clients \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Claude Desktop",
    "clientId": "claude-desktop",
    "redirectUris": "http://localhost:3000/callback\nhttp://127.0.0.1:3000/callback",
    "allowedScopes": "openid profile email",
    "requirePkce": true,
    "accessTokenTtl": 3600,
    "refreshTokenTtl": 2592000
  }'
```

Or create it through the admin UI at `/admin/clients/new`.

### Step 2: Create Users

Create user accounts for the people who will authenticate through the MCP flow:

```bash
curl -u admin:feauxauth -X POST http://localhost:8080/api/admin/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "analyst@example.com",
    "displayName": "Data Analyst",
    "password": "secure-password"
  }'
```

Or use the seed demo user (`demo@feauxauth.local` / `password`) for testing.

### Step 3: Configure the MCP Server

The MCP server acts as a **resource server** — it validates tokens issued by FeauxAuth. Configuration depends on the MCP server implementation.

**Spring Security-based MCP servers** (e.g., a Java/Spring Greenplum MCP server):

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080
          jwk-set-uri: http://localhost:8080/.well-known/jwks.json
```

**Generic MCP servers** need these FeauxAuth endpoints:

| Purpose | URL |
|---------|-----|
| OIDC Discovery | `http://localhost:8080/.well-known/openid-configuration` |
| JWKS (public keys) | `http://localhost:8080/.well-known/jwks.json` |
| Issuer (for `iss` claim validation) | `http://localhost:8080` |

The MCP server should also serve a **Protected Resource Metadata** document at `/.well-known/oauth-protected-resource` (per RFC 9728) so MCP clients can discover FeauxAuth automatically:

```json
{
  "resource": "https://greenplum-mcp.example.com",
  "authorization_servers": ["http://localhost:8080"],
  "scopes_supported": ["openid", "profile", "email"],
  "bearer_methods_supported": ["header"]
}
```

### Step 4: Configure the MCP Client

**Claude Desktop** (`claude_desktop_config.json`):

For remote MCP servers with OAuth, the client discovers the auth server automatically through the MCP server's resource metadata. No FeauxAuth URL needed in the client config:

```json
{
  "mcpServers": {
    "greenplum": {
      "url": "https://greenplum-mcp.example.com/mcp",
      "transport": "streamable-http"
    }
  }
}
```

When Claude Desktop connects, it will:
1. Receive a `401` from the MCP server
2. Discover FeauxAuth via the server's `/.well-known/oauth-protected-resource`
3. Open a browser window to FeauxAuth's login page
4. Complete the PKCE authorization code flow
5. Use the resulting JWT for all subsequent MCP requests

### Token Verification

MCP servers validate FeauxAuth JWTs by:

1. **Fetching the public key** from `/.well-known/jwks.json`
2. **Verifying the RS256 signature** using the key matching the token's `kid` header
3. **Checking standard claims**: `iss` matches FeauxAuth's issuer URL, `exp` is in the future, `aud` matches the expected client

Example decoded FeauxAuth access token payload:
```json
{
  "iss": "http://localhost:8080",
  "sub": "analyst@example.com",
  "aud": "claude-desktop",
  "exp": 1713045600,
  "iat": 1713042000,
  "jti": "a1b2c3d4-...",
  "scope": "openid profile email",
  "email": "analyst@example.com",
  "name": "Data Analyst",
  "roles": ["user", "analyst"]
}
```

### Docker Compose with an MCP Server

To run FeauxAuth alongside an MCP server locally:

```yaml
services:
  feauxauth:
    build: ./FeauxAuth
    ports:
      - "8080:8080"
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/feauxauth
      DATABASE_USERNAME: feauxauth
      DATABASE_PASSWORD: feauxauth
      FEAUXAUTH_ISSUER: http://localhost:8080

  your-mcp-server:
    image: your-mcp-server:latest
    ports:
      - "9090:9090"
    environment:
      OAUTH_ISSUER_URI: http://feauxauth:8080
      OAUTH_JWKS_URI: http://feauxauth:8080/.well-known/jwks.json

  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: feauxauth
      POSTGRES_USER: feauxauth
      POSTGRES_PASSWORD: feauxauth
```

> **Note:** Inside Docker networking, the MCP server reaches FeauxAuth at `http://feauxauth:8080`. But the `FEAUXAUTH_ISSUER` must be set to the externally reachable URL (`http://localhost:8080`) since that's what appears in JWTs and must match what clients see.

## API Reference

### OIDC Discovery

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/.well-known/openid-configuration` | None | OIDC discovery document |
| GET | `/.well-known/oauth-authorization-server` | None | OAuth 2.0 authorization server metadata |
| GET | `/.well-known/jwks.json` | None | Public signing keys (JWKS) |

### OAuth Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/oauth/authorize` | None | Renders login page |
| POST | `/oauth/authorize` | Session | Processes login, shows consent or redirects with auth code |
| POST | `/oauth/consent` | Session | Processes consent approval/denial |
| POST | `/oauth/token` | Client credentials or PKCE | Exchanges code, refresh token, or client credentials for tokens |
| GET | `/oauth/userinfo` | Bearer token | Returns user claims (includes roles) |
| POST | `/oauth/revoke` | None | Revokes an access or refresh token |
| POST | `/oauth/introspect` | None | Token introspection (RFC 7662) |
| GET | `/oauth/logout` | None | OIDC RP-Initiated Logout |
| POST | `/oauth/register` | None | Dynamic client registration (RFC 7591) |

### Admin API

All admin endpoints require HTTP Basic auth (`ADMIN_USERNAME` / `ADMIN_PASSWORD`).

**Dashboard**

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/admin/dashboard/stats` | Client count, user count, active tokens, signing key ID |

**Clients**

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/admin/clients` | List all clients |
| GET | `/api/admin/clients/{id}` | Get client by ID |
| POST | `/api/admin/clients` | Create client (returns secret) |
| PUT | `/api/admin/clients/{id}` | Update client |
| DELETE | `/api/admin/clients/{id}` | Delete client |
| POST | `/api/admin/clients/{id}/reset-secret` | Regenerate client secret |

**Users**

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/admin/users` | List all users |
| GET | `/api/admin/users/{id}` | Get user by ID |
| POST | `/api/admin/users` | Create user |
| PUT | `/api/admin/users/{id}` | Update user |
| DELETE | `/api/admin/users/{id}` | Delete user |
| POST | `/api/admin/users/{id}/reset-password` | Generate new random password |

**Tokens**

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/admin/tokens?page=0&size=20` | List active tokens (paginated) |
| POST | `/api/admin/tokens/{jti}/revoke` | Revoke a token |
| POST | `/api/admin/tokens/revoke-user/{userId}` | Revoke all tokens for a user |

**Inspector**

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/admin/inspector` | Decode JWT, verify signature, check revocation |

## Configuration

All configuration is via environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `jdbc:h2:file:./feauxauth` | JDBC connection URL |
| `DATABASE_USERNAME` | `sa` | Database username |
| `DATABASE_PASSWORD` | *(empty)* | Database password |
| `FEAUXAUTH_ISSUER` | `http://localhost:8080` | Issuer URL in JWTs and OIDC discovery |
| `ADMIN_USERNAME` | `admin` | Admin UI / API username |
| `ADMIN_PASSWORD` | `feauxauth` | Admin UI / API password |
| `FEAUXAUTH_CORS_ALLOWED_ORIGINS` | `*` | Comma-separated allowed CORS origins for OAuth/OIDC endpoints |

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.4 |
| Security | Spring Security (HTTP Basic + Bearer) |
| Database | H2 (dev) / PostgreSQL 16 (Docker/prod) |
| Migrations | Flyway |
| JWT | nimbus-jose-jwt (RS256) |
| Frontend | React 18, Vite, Tailwind CSS |
| Login UI | Thymeleaf |
| Build | Maven with frontend-maven-plugin |
| Deploy | Docker Compose, Cloud Foundry |

## Project Structure

```
FeauxAuth/
├── src/main/java/com/baskettecase/feauxauth/
│   ├── config/          # Security, CORS, app config, SPA routing
│   ├── model/           # JPA entities (6)
│   ├── repository/      # Spring Data repositories (6)
│   ├── service/         # Business logic (Key, Token, AuthCode, PKCE, Client, User)
│   └── controller/      # OAuth, OIDC, introspection, registration + admin API
├── src/main/resources/
│   ├── application.yml  # Default config (H2)
│   ├── db/migration/    # Flyway SQL (V1-V3)
│   └── templates/oauth/ # Thymeleaf login/consent/logout/error pages
├── frontend/            # React/Vite/Tailwind admin SPA
├── Dockerfile           # Multi-stage build
├── docker-compose.yml   # App + Postgres
└── manifest.yml         # Cloud Foundry deployment
```

## Development

### Run Backend Only

```bash
mvn spring-boot:run
```

### Run Frontend Dev Server

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server runs on port 3000 and proxies API requests to the Spring Boot backend on port 8080.

### Run Tests

```bash
mvn test
```

Tests include unit tests for PKCE and token services, plus integration tests covering the full OAuth authorization code flow.

## Licence

MIT

## Acknowledgements

Built with [Spring Boot](https://spring.io/projects/spring-boot), [nimbus-jose-jwt](https://connect2id.com/products/nimbus-jose-jwt), [React](https://react.dev), [Vite](https://vitejs.dev), and [Tailwind CSS](https://tailwindcss.com).
