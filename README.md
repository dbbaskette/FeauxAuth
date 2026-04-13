# FeauxAuth

A lightweight OAuth 2.0 / OIDC authorization server built for lab and demo environments. FeauxAuth gives you a fully functional auth server with a React admin UI — deploy it locally with Docker or push it to Cloud Foundry in minutes.

> **Not for production use.** FeauxAuth is designed for development, testing, and demos where you need a real OAuth server without the overhead of Keycloak, Auth0, or Spring Authorization Server.

## Features

- **OAuth 2.0 Authorization Code** flow with optional **PKCE**
- **OpenID Connect** discovery, ID tokens, userinfo endpoint, and nonce support
- **JWT access tokens** signed with RS256 (RSA-2048), auto-generated signing keys
- **Opaque refresh tokens** with configurable TTL
- **Token revocation** (RFC 7009)
- **Admin REST API** for managing clients, users, and tokens
- **React admin dashboard** with dark theme
- **Token inspector** — paste a JWT to decode, verify signature, and check revocation
- **Seed data** — ships with a demo client and user so you can test immediately
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
    "requirePkce": false
  }'
```

Save the `clientSecret` from the response — it won't be shown again.

### 2. Create a User

```bash
curl -u admin:feauxauth -X POST http://localhost:8080/api/admin/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "displayName": "Alice",
    "password": "password123"
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

## API Reference

### OIDC Discovery

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/.well-known/openid-configuration` | None | OIDC discovery document |
| GET | `/.well-known/jwks.json` | None | Public signing keys (JWKS) |

### OAuth Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/oauth/authorize` | None | Renders login page |
| POST | `/oauth/authorize` | Session | Processes login, redirects with auth code |
| POST | `/oauth/token` | Client credentials or PKCE | Exchanges code or refresh token for tokens |
| GET | `/oauth/userinfo` | Bearer token | Returns user claims |
| POST | `/oauth/revoke` | None | Revokes an access or refresh token |

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
│   ├── config/          # Security, app config, SPA routing
│   ├── model/           # JPA entities (6)
│   ├── repository/      # Spring Data repositories (6)
│   ├── service/         # Business logic (Key, Token, AuthCode, PKCE, Client, User)
│   └── controller/      # OAuth endpoints + admin API
├── src/main/resources/
│   ├── application.yml  # Default config (H2)
│   ├── db/migration/    # Flyway SQL
│   └── templates/oauth/ # Thymeleaf login/error pages
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
