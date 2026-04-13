# FeauxAuth — Implementation Design Spec

**Date**: 2026-04-13
**Status**: Approved
**Package**: `com.baskettecase.feauxauth`

## Overview

FeauxAuth is a self-hosted, Cloud Foundry-deployable OAuth 2.0 Authorization Server + OIDC Provider for lab, demo, and development environments. It eliminates the need to wire up Google, GitHub, or any external IdP when standing up demo apps. Any app that speaks standard OAuth 2.0 / OIDC points at FeauxAuth's discovery URL and gets working auth in minutes.

**Not for production use.** No rate limiting, no MFA, no audit logs, no credential rotation.

## Technology Stack

| Layer | Choice | Notes |
|-------|--------|-------|
| Language | Java 21 | |
| Framework | Spring Boot 3.x | Web, Security, Data JPA |
| Build | Maven | frontend-maven-plugin for React build |
| Database (local) | H2 file mode | `default` profile |
| Database (CF) | Postgres via service binding | `cloud` profile |
| Migrations | Flyway | Versioned SQL, works on H2 and Postgres |
| JWT | nimbus-jose-jwt | RS256, RSA-2048 |
| Admin UI | React 18 + Vite + Tailwind CSS | SPA served from Spring Boot static resources |
| OAuth login page | Thymeleaf | Server-rendered, part of the OAuth flow |
| CF Buildpack | java_buildpack | Single JAR deployment |

## Build Order (Core-out)

1. **Project scaffold** — pom.xml, application.yml, application-cloud.yml, manifest.yml, main class, Flyway migrations
2. **Crypto & token layer** — RSA keypair generation, TokenService, PkceService, AuthCodeService
3. **JPA entities & repositories** — OAuthClient, OAuthUser, AuthCode, AccessToken, RefreshToken, SigningKey
4. **OAuth endpoints** — /oauth/authorize, /oauth/token, /oauth/revoke, /oauth/userinfo, /.well-known/*
5. **Security config** — Split chains: HTTP Basic for /admin + /api/admin, Bearer for /oauth/userinfo, public for discovery/authorize/token
6. **Admin REST API + React SPA** — CRUD for clients/users/tokens, dashboard stats, token inspector
7. **Integration tests** — Full auth code flow (with/without PKCE), refresh, revocation

## Database Strategy

- **Local dev** (`default` profile): H2 file mode at `./feauxauth.db`
- **Cloud Foundry** (`cloud` profile): Binds to a Postgres service instance. Spring Boot auto-configures the datasource from `VCAP_SERVICES`.
- Flyway migrations are dialect-compatible (H2 + Postgres).
- manifest.yml sets `SPRING_PROFILES_ACTIVE: cloud` and binds the `feauxauth-db` service.

## Data Model

### oauth_clients
| Column | Type | Notes |
|--------|------|-------|
| id | UUID | PK |
| client_id | varchar | Unique, human-readable |
| client_secret_hash | varchar | BCrypt |
| name | varchar | Display name |
| redirect_uris | text | Newline-separated |
| allowed_scopes | varchar | Space-separated |
| access_token_ttl | integer | Default 3600 |
| refresh_token_ttl | integer | Default 2592000 |
| require_pkce | boolean | Optional |
| enabled | boolean | |
| created_at | timestamp | |

### oauth_users
| Column | Type | Notes |
|--------|------|-------|
| id | UUID | PK |
| email | varchar | Unique, used as login + sub claim |
| display_name | varchar | |
| password_hash | varchar | BCrypt |
| enabled | boolean | |
| created_at | timestamp | |
| last_login_at | timestamp | Nullable |

### auth_codes
| Column | Type | Notes |
|--------|------|-------|
| code | varchar | PK, random 256-bit |
| client_id | varchar | FK |
| user_id | UUID | FK |
| redirect_uri | varchar | Exact match |
| scope | varchar | |
| code_challenge | varchar | PKCE, nullable |
| expires_at | timestamp | 120 seconds |
| used | boolean | Single-use |

### access_tokens
| Column | Type | Notes |
|--------|------|-------|
| jti | varchar | PK, JWT ID |
| client_id | varchar | FK |
| user_id | UUID | FK |
| scope | varchar | |
| expires_at | timestamp | |
| revoked | boolean | |
| created_at | timestamp | |

### refresh_tokens
| Column | Type | Notes |
|--------|------|-------|
| token | varchar | PK, opaque 256-bit |
| client_id | varchar | FK |
| user_id | UUID | FK |
| scope | varchar | |
| expires_at | timestamp | |
| revoked | boolean | |
| created_at | timestamp | |

### signing_keys
| Column | Type | Notes |
|--------|------|-------|
| kid | varchar | PK, included in JWT header + JWKS |
| private_key | text | PEM-encoded RSA-2048 |
| public_key | text | PEM-encoded, served via JWKS |
| created_at | timestamp | |
| active | boolean | One active key at a time |

## OAuth Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/.well-known/openid-configuration` | Public | OIDC discovery |
| GET | `/.well-known/jwks.json` | Public | RSA public key |
| GET | `/oauth/authorize` | Public | Renders login page |
| POST | `/oauth/token` | Client auth | Code exchange + refresh |
| POST | `/oauth/revoke` | Public | Invalidate token |
| GET | `/oauth/userinfo` | Bearer | User profile claims |

### Authorization Code Flow
1. App redirects to `/oauth/authorize?client_id=...&redirect_uri=...&scope=...&state=...&response_type=code` (+ optional `code_challenge`)
2. FeauxAuth renders login form (Thymeleaf)
3. User enters email + password
4. Success → redirect to `redirect_uri?code=...&state=...`
5. App POSTs to `/oauth/token` with code + client credentials (or code_verifier for PKCE)
6. Response: `access_token` (JWT), `refresh_token` (opaque), `id_token` (if openid), `expires_in`

### Token Claims (access_token)
`iss`, `sub` (email), `aud` (client_id), `exp`, `iat`, `jti`, `email`, `name`, `scope`

### ID Token
Same as access token but includes `nonce` if provided. Issued when `openid` scope requested.

## Admin REST API

All endpoints under `/api/admin/**`, protected by HTTP Basic.

| Method | Path | Description |
|--------|------|-------------|
| GET/POST | `/api/admin/clients` | List / create clients |
| GET/PUT/DELETE | `/api/admin/clients/{id}` | CRUD single client |
| POST | `/api/admin/clients/{id}/reset-secret` | Regenerate client secret |
| GET/POST | `/api/admin/users` | List / create users |
| GET/PUT/DELETE | `/api/admin/users/{id}` | CRUD single user |
| POST | `/api/admin/users/{id}/reset-password` | Generate new password |
| GET | `/api/admin/tokens` | List active tokens (paginated) |
| POST | `/api/admin/tokens/{jti}/revoke` | Revoke single token |
| POST | `/api/admin/tokens/revoke-user/{userId}` | Revoke all for user |
| GET | `/api/admin/dashboard/stats` | Counts + key fingerprint |
| POST | `/api/admin/inspector` | Decode/verify pasted JWT |

## Admin UI (React SPA)

React 18 + Vite + Tailwind CSS, built via `frontend-maven-plugin` during `mvn package`. Output to `src/main/resources/static/admin/`.

### Pages
- **Login** — collects admin username/password, stores base64 credentials in sessionStorage
- **Dashboard** — stat cards (clients, users, active tokens, key fingerprint) + recent tokens table
- **OAuth Clients** — table + add/edit form, copy-to-clipboard on secret creation
- **Users** — table + add/edit form, reset password
- **Active Tokens** — paginated table with revoke actions
- **Token Inspector** — paste JWT, see decoded header/payload/signature verification

### SPA Routing
Spring Boot returns `index.html` for any `/admin/**` path not matching a static asset. React Router handles client-side navigation.

## Security Model

| What | How |
|------|-----|
| Client secrets | BCrypt hashed |
| User passwords | BCrypt hashed |
| JWT signatures | RS256, RSA-2048 |
| Admin UI/API | HTTP Basic (env vars) |
| Redirect URIs | Exact match only |
| Auth codes | Single-use, 120s TTL |

### Intentionally not secured (lab use)
- No rate limiting
- No MFA
- No token binding
- No audit log
- No TLS enforcement (CF handles termination)
- Refresh tokens reused, not rotated
- Single admin account

## CF Deployment

```yaml
applications:
  - name: feauxauth
    memory: 512M
    disk_quota: 512M
    instances: 1
    buildpacks:
      - java_buildpack
    path: target/feauxauth-1.0.0.jar
    health-check-type: http
    health-check-http-endpoint: /.well-known/openid-configuration
    services:
      - feauxauth-db
    env:
      ADMIN_USERNAME: admin
      ADMIN_PASSWORD: changeme
      JBP_CONFIG_OPEN_JDK_JRE: '{ jre: { version: 21.+ } }'
      SPRING_PROFILES_ACTIVE: cloud
```

## Project Structure

```
feauxauth/
├── manifest.yml
├── pom.xml
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   ├── index.html
│   └── src/
│       ├── App.jsx
│       ├── api/client.js
│       ├── pages/
│       │   ├── Login.jsx
│       │   ├── Dashboard.jsx
│       │   ├── Clients.jsx
│       │   ├── ClientForm.jsx
│       │   ├── Users.jsx
│       │   ├── UserForm.jsx
│       │   ├── Tokens.jsx
│       │   └── Inspector.jsx
│       └── components/
│           ├── Layout.jsx
│           ├── StatCard.jsx
│           └── DataTable.jsx
├── src/main/
│   ├── java/com/baskettecase/feauxauth/
│   │   ├── FeauxAuthApplication.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   ├── AppConfig.java
│   │   │   └── WebConfig.java
│   │   ├── controller/
│   │   │   ├── AuthorizeController.java
│   │   │   ├── TokenController.java
│   │   │   ├── UserInfoController.java
│   │   │   ├── RevocationController.java
│   │   │   ├── WellKnownController.java
│   │   │   └── api/
│   │   │       ├── AdminClientApi.java
│   │   │       ├── AdminUserApi.java
│   │   │       ├── AdminTokenApi.java
│   │   │       ├── AdminDashboardApi.java
│   │   │       └── AdminInspectorApi.java
│   │   ├── service/
│   │   │   ├── TokenService.java
│   │   │   ├── AuthCodeService.java
│   │   │   ├── PkceService.java
│   │   │   ├── ClientService.java
│   │   │   └── UserService.java
│   │   ├── model/
│   │   │   ├── OAuthClient.java
│   │   │   ├── OAuthUser.java
│   │   │   ├── AuthCode.java
│   │   │   ├── AccessToken.java
│   │   │   ├── RefreshToken.java
│   │   │   └── SigningKey.java
│   │   └── repository/
│   │       ├── ClientRepository.java
│   │       ├── UserRepository.java
│   │       ├── AuthCodeRepository.java
│   │       ├── AccessTokenRepository.java
│   │       ├── RefreshTokenRepository.java
│   │       └── SigningKeyRepository.java
│   └── resources/
│       ├── application.yml
│       ├── application-cloud.yml
│       ├── db/migration/
│       │   └── V1__create_schema.sql
│       └── templates/oauth/
│           ├── login.html
│           └── error.html
└── src/test/java/com/baskettecase/feauxauth/
    ├── TokenServiceTest.java
    ├── PkceServiceTest.java
    └── AuthFlowIntegrationTest.java
```

## Configuration

### application.yml (default — H2)
```yaml
spring:
  datasource:
    url: jdbc:h2:file:./feauxauth;AUTO_SERVER=TRUE
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true

feauxauth:
  issuer: ${FEAUXAUTH_ISSUER:http://localhost:8080}
  admin:
    username: ${ADMIN_USERNAME:admin}
    password: ${ADMIN_PASSWORD:feauxauth}
```

### application-cloud.yml (Postgres via CF service binding)
```yaml
spring:
  datasource:
    # Auto-configured from VCAP_SERVICES by java-cfenv (dependency in pom.xml)
```

### Additional dependency for CF
`io.pivotal.cfenv:java-cfenv-boot` — auto-configures Spring datasource from CF service bindings.

## Out of Scope (v1.0)

- Client Credentials grant
- Device Authorization grant
- Implicit grant
- Multi-node / clustered deployment
- JWT key rotation
- External LDAP/SAML federation
- Per-scope consent screens
- Email-based password reset
- RBAC on admin UI
- Metrics / Prometheus
- Open Questions OQ-1 through OQ-5
