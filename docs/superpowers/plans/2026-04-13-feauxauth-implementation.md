# FeauxAuth Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a CF-deployable OAuth 2.0 / OIDC authorization server for lab/demo environments with a React admin UI.

**Architecture:** Spring Boot 3.x backend implementing OAuth 2.0 Authorization Code flow (with PKCE) and OIDC. H2 for local dev, Postgres on CF via java-cfenv. React/Vite SPA for admin, Thymeleaf for OAuth login page. Core-out build order: scaffold → crypto → entities → OAuth endpoints → security → admin API + UI → tests.

**Tech Stack:** Java 21, Spring Boot 3.x, Maven, H2/Postgres, Flyway, nimbus-jose-jwt, React 18, Vite, Tailwind CSS

---

## File Map

### Project Root
- `pom.xml` — Maven build with Spring Boot, H2, Postgres, Flyway, nimbus-jose-jwt, Lombok, java-cfenv, frontend-maven-plugin
- `manifest.yml` — CF deployment descriptor
- `.gitignore` — Java/Maven/Node ignores

### Backend: `src/main/java/com/baskettecase/feauxauth/`
- `FeauxAuthApplication.java` — Spring Boot main class
- `config/SecurityConfig.java` — Split security filter chains
- `config/AppConfig.java` — Keypair initialization, app properties binding
- `config/WebConfig.java` — SPA fallback routing for `/admin/**`
- `model/OAuthClient.java` — JPA entity
- `model/OAuthUser.java` — JPA entity
- `model/AuthCode.java` — JPA entity
- `model/AccessToken.java` — JPA entity
- `model/RefreshToken.java` — JPA entity
- `model/SigningKey.java` — JPA entity
- `repository/ClientRepository.java` — Spring Data JPA
- `repository/UserRepository.java` — Spring Data JPA
- `repository/AuthCodeRepository.java` — Spring Data JPA
- `repository/AccessTokenRepository.java` — Spring Data JPA
- `repository/RefreshTokenRepository.java` — Spring Data JPA
- `repository/SigningKeyRepository.java` — Spring Data JPA
- `service/KeyService.java` — RSA keypair generation and retrieval
- `service/TokenService.java` — JWT mint/verify/revoke
- `service/AuthCodeService.java` — Auth code generation and exchange
- `service/PkceService.java` — PKCE S256 verification
- `service/ClientService.java` — Client CRUD operations
- `service/UserService.java` — User CRUD operations
- `controller/WellKnownController.java` — OIDC discovery + JWKS
- `controller/AuthorizeController.java` — GET /oauth/authorize + login POST
- `controller/TokenController.java` — POST /oauth/token
- `controller/UserInfoController.java` — GET /oauth/userinfo
- `controller/RevocationController.java` — POST /oauth/revoke
- `controller/api/AdminClientApi.java` — Client CRUD REST
- `controller/api/AdminUserApi.java` — User CRUD REST
- `controller/api/AdminTokenApi.java` — Token list/revoke REST
- `controller/api/AdminDashboardApi.java` — Stats REST
- `controller/api/AdminInspectorApi.java` — JWT decode/verify REST

### Resources: `src/main/resources/`
- `application.yml` — Default profile (H2)
- `application-cloud.yml` — Cloud profile (Postgres)
- `db/migration/V1__create_schema.sql` — All tables
- `templates/oauth/login.html` — Thymeleaf login page
- `templates/oauth/error.html` — Thymeleaf error page

### Frontend: `frontend/`
- `package.json` — React, Vite, Tailwind dependencies
- `vite.config.js` — Vite config with proxy for dev
- `tailwind.config.js` — Tailwind config
- `postcss.config.js` — PostCSS for Tailwind
- `index.html` — SPA entry point
- `src/main.jsx` — React entry
- `src/App.jsx` — Router setup
- `src/api/client.js` — HTTP Basic fetch wrapper
- `src/components/Layout.jsx` — Nav shell
- `src/components/StatCard.jsx` — Dashboard stat card
- `src/components/DataTable.jsx` — Reusable table
- `src/pages/Login.jsx` — Admin login
- `src/pages/Dashboard.jsx` — Dashboard
- `src/pages/Clients.jsx` — Client list
- `src/pages/ClientForm.jsx` — Client add/edit
- `src/pages/Users.jsx` — User list
- `src/pages/UserForm.jsx` — User add/edit
- `src/pages/Tokens.jsx` — Active tokens
- `src/pages/Inspector.jsx` — Token inspector

### Tests: `src/test/java/com/baskettecase/feauxauth/`
- `service/PkceServiceTest.java` — PKCE verification unit tests
- `service/TokenServiceTest.java` — JWT mint/verify unit tests
- `AuthFlowIntegrationTest.java` — End-to-end OAuth flow tests

---

## Task 1: Project Scaffold

**Files:**
- Create: `pom.xml`
- Create: `.gitignore`
- Create: `manifest.yml`
- Create: `src/main/java/com/baskettecase/feauxauth/FeauxAuthApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-cloud.yml`
- Create: `src/main/resources/db/migration/V1__create_schema.sql`

- [ ] **Step 1: Create `.gitignore`**

```gitignore
# Maven
target/
!.mvn/wrapper/maven-wrapper.jar

# IDE
.idea/
*.iml
.vscode/
.project
.classpath
.settings/

# H2
*.db
*.db.mv.db
*.db.trace.db

# OS
.DS_Store
Thumbs.db

# Node
frontend/node_modules/
frontend/dist/

# Env
.env
```

- [ ] **Step 2: Create `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.4</version>
        <relativePath/>
    </parent>

    <groupId>com.baskettecase</groupId>
    <artifactId>feauxauth</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    <name>FeauxAuth</name>
    <description>Lightweight OAuth 2.0 / OIDC Authorization Server for lab and demo environments</description>

    <properties>
        <java.version>21</java.version>
        <nimbus.version>9.37.3</nimbus.version>
        <cfenv.version>3.2.0</cfenv.version>
        <frontend-maven-plugin.version>1.15.1</frontend-maven-plugin.version>
        <node.version>v20.11.1</node.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- CF Environment -->
        <dependency>
            <groupId>io.pivotal.cfenv</groupId>
            <artifactId>java-cfenv-boot</artifactId>
            <version>${cfenv.version}</version>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>com.nimbusds</groupId>
            <artifactId>nimbus-jose-jwt</artifactId>
            <version>${nimbus.version}</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <finalName>feauxauth-${project.version}</finalName>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>

            <!-- Frontend build -->
            <plugin>
                <groupId>com.github.eirslett</groupId>
                <artifactId>frontend-maven-plugin</artifactId>
                <version>${frontend-maven-plugin.version}</version>
                <configuration>
                    <workingDirectory>frontend</workingDirectory>
                </configuration>
                <executions>
                    <execution>
                        <id>install-node</id>
                        <goals><goal>install-node-and-npm</goal></goals>
                        <configuration>
                            <nodeVersion>${node.version}</nodeVersion>
                        </configuration>
                    </execution>
                    <execution>
                        <id>npm-install</id>
                        <goals><goal>npm</goal></goals>
                        <configuration>
                            <arguments>install</arguments>
                        </configuration>
                    </execution>
                    <execution>
                        <id>npm-build</id>
                        <goals><goal>npm</goal></goals>
                        <configuration>
                            <arguments>run build</arguments>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: Create `manifest.yml`**

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

- [ ] **Step 4: Create `FeauxAuthApplication.java`**

```java
package com.baskettecase.feauxauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FeauxAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(FeauxAuthApplication.class, args);
    }
}
```

- [ ] **Step 5: Create `application.yml`**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:file:./feauxauth;AUTO_SERVER=TRUE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
  flyway:
    enabled: true
    locations: classpath:db/migration
  thymeleaf:
    cache: false

feauxauth:
  issuer: ${FEAUXAUTH_ISSUER:http://localhost:8080}
  admin:
    username: ${ADMIN_USERNAME:admin}
    password: ${ADMIN_PASSWORD:feauxauth}
```

- [ ] **Step 6: Create `application-cloud.yml`**

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    locations: classpath:db/migration
```

- [ ] **Step 7: Create `V1__create_schema.sql`**

This migration must be compatible with both H2 and PostgreSQL.

```sql
-- Signing keys
CREATE TABLE signing_keys (
    kid VARCHAR(255) NOT NULL PRIMARY KEY,
    private_key TEXT NOT NULL,
    public_key TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- OAuth clients
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

-- OAuth users
CREATE TABLE oauth_users (
    id UUID NOT NULL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

-- Authorization codes
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

-- Access tokens (tracking for revocation)
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

-- Refresh tokens
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
```

- [ ] **Step 8: Verify the app starts**

```bash
cd /Users/dbbaskette/Projects/FeauxAuth
# We need the frontend scaffold first for Maven to work, so create a minimal placeholder
mkdir -p frontend
echo '{"name":"feauxauth-admin","version":"1.0.0","scripts":{"build":"echo placeholder"}}' > frontend/package.json
mvn clean compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add .gitignore pom.xml manifest.yml src/ frontend/package.json
git commit -m "feat: project scaffold with Maven, Spring Boot, Flyway schema"
```

---

## Task 2: JPA Entities

**Files:**
- Create: `src/main/java/com/baskettecase/feauxauth/model/SigningKey.java`
- Create: `src/main/java/com/baskettecase/feauxauth/model/OAuthClient.java`
- Create: `src/main/java/com/baskettecase/feauxauth/model/OAuthUser.java`
- Create: `src/main/java/com/baskettecase/feauxauth/model/AuthCode.java`
- Create: `src/main/java/com/baskettecase/feauxauth/model/AccessToken.java`
- Create: `src/main/java/com/baskettecase/feauxauth/model/RefreshToken.java`

- [ ] **Step 1: Create `SigningKey.java`**

```java
package com.baskettecase.feauxauth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "signing_keys")
@Getter @Setter
public class SigningKey {
    @Id
    private String kid;

    @Column(name = "private_key", nullable = false, columnDefinition = "TEXT")
    private String privateKey;

    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean active = true;
}
```

- [ ] **Step 2: Create `OAuthClient.java`**

```java
package com.baskettecase.feauxauth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "oauth_clients")
@Getter @Setter
public class OAuthClient {
    @Id
    private UUID id;

    @Column(name = "client_id", nullable = false, unique = true)
    private String clientId;

    @Column(name = "client_secret_hash", nullable = false)
    private String clientSecretHash;

    @Column(nullable = false)
    private String name;

    @Column(name = "redirect_uris", nullable = false, columnDefinition = "TEXT")
    private String redirectUris;

    @Column(name = "allowed_scopes", nullable = false)
    private String allowedScopes = "openid profile email";

    @Column(name = "access_token_ttl", nullable = false)
    private int accessTokenTtl = 3600;

    @Column(name = "refresh_token_ttl", nullable = false)
    private int refreshTokenTtl = 2592000;

    @Column(name = "require_pkce", nullable = false)
    private boolean requirePkce = false;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 3: Create `OAuthUser.java`**

```java
package com.baskettecase.feauxauth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "oauth_users")
@Getter @Setter
public class OAuthUser {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 4: Create `AuthCode.java`**

```java
package com.baskettecase.feauxauth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth_codes")
@Getter @Setter
public class AuthCode {
    @Id
    private String code;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "redirect_uri", nullable = false)
    private String redirectUri;

    @Column(nullable = false)
    private String scope;

    @Column(name = "code_challenge")
    private String codeChallenge;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;
}
```

- [ ] **Step 5: Create `AccessToken.java`**

```java
package com.baskettecase.feauxauth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "access_tokens")
@Getter @Setter
public class AccessToken {
    @Id
    private String jti;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String scope;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

- [ ] **Step 6: Create `RefreshToken.java`**

```java
package com.baskettecase.feauxauth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter @Setter
public class RefreshToken {
    @Id
    private String token;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String scope;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/baskettecase/feauxauth/model/
git commit -m "feat: add JPA entities for all data model tables"
```

---

## Task 3: Repositories

**Files:**
- Create: `src/main/java/com/baskettecase/feauxauth/repository/SigningKeyRepository.java`
- Create: `src/main/java/com/baskettecase/feauxauth/repository/ClientRepository.java`
- Create: `src/main/java/com/baskettecase/feauxauth/repository/UserRepository.java`
- Create: `src/main/java/com/baskettecase/feauxauth/repository/AuthCodeRepository.java`
- Create: `src/main/java/com/baskettecase/feauxauth/repository/AccessTokenRepository.java`
- Create: `src/main/java/com/baskettecase/feauxauth/repository/RefreshTokenRepository.java`

- [ ] **Step 1: Create all repositories**

```java
// SigningKeyRepository.java
package com.baskettecase.feauxauth.repository;

import com.baskettecase.feauxauth.model.SigningKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SigningKeyRepository extends JpaRepository<SigningKey, String> {
    Optional<SigningKey> findByActiveTrue();
}
```

```java
// ClientRepository.java
package com.baskettecase.feauxauth.repository;

import com.baskettecase.feauxauth.model.OAuthClient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<OAuthClient, UUID> {
    Optional<OAuthClient> findByClientId(String clientId);
    boolean existsByClientId(String clientId);
}
```

```java
// UserRepository.java
package com.baskettecase.feauxauth.repository;

import com.baskettecase.feauxauth.model.OAuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<OAuthUser, UUID> {
    Optional<OAuthUser> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

```java
// AuthCodeRepository.java
package com.baskettecase.feauxauth.repository;

import com.baskettecase.feauxauth.model.AuthCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthCodeRepository extends JpaRepository<AuthCode, String> {
}
```

```java
// AccessTokenRepository.java
package com.baskettecase.feauxauth.repository;

import com.baskettecase.feauxauth.model.AccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AccessTokenRepository extends JpaRepository<AccessToken, String> {
    Page<AccessToken> findByExpiresAtAfterOrderByCreatedAtDesc(LocalDateTime now, Pageable pageable);
    List<AccessToken> findByUserIdAndRevokedFalse(UUID userId);
    long countByExpiresAtAfterAndRevokedFalse(LocalDateTime now);
}
```

```java
// RefreshTokenRepository.java
package com.baskettecase.feauxauth.repository;

import com.baskettecase.feauxauth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    List<RefreshToken> findByUserIdAndRevokedFalse(UUID userId);
}
```

- [ ] **Step 2: Verify compilation**

```bash
mvn compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/baskettecase/feauxauth/repository/
git commit -m "feat: add Spring Data JPA repositories"
```

---

## Task 4: PKCE Service + Tests

**Files:**
- Create: `src/main/java/com/baskettecase/feauxauth/service/PkceService.java`
- Create: `src/test/java/com/baskettecase/feauxauth/service/PkceServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.baskettecase.feauxauth.service;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import static org.assertj.core.api.Assertions.*;

class PkceServiceTest {

    private final PkceService pkceService = new PkceService();

    @Test
    void verifyChallenge_validS256_returnsTrue() throws Exception {
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        // SHA-256 of verifier, base64url-encoded (no padding)
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        assertThat(pkceService.verifyChallenge(verifier, challenge)).isTrue();
    }

    @Test
    void verifyChallenge_wrongVerifier_returnsFalse() throws Exception {
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        assertThat(pkceService.verifyChallenge("wrong-verifier", challenge)).isFalse();
    }

    @Test
    void generateChallenge_producesValidChallenge() {
        String verifier = "test-verifier-string-with-enough-entropy-1234567890";
        String challenge = pkceService.generateChallenge(verifier);

        assertThat(challenge).isNotBlank();
        assertThat(pkceService.verifyChallenge(verifier, challenge)).isTrue();
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
mvn test -pl . -Dtest=com.baskettecase.feauxauth.service.PkceServiceTest -DfailIfNoTests=false
```

Expected: FAIL (class not found)

- [ ] **Step 3: Implement `PkceService.java`**

```java
package com.baskettecase.feauxauth.service;

import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
public class PkceService {

    public String generateChallenge(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public boolean verifyChallenge(String verifier, String challenge) {
        String computed = generateChallenge(verifier);
        return computed.equals(challenge);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
mvn test -pl . -Dtest=com.baskettecase.feauxauth.service.PkceServiceTest
```

Expected: 3 tests PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/baskettecase/feauxauth/service/PkceService.java \
        src/test/java/com/baskettecase/feauxauth/service/PkceServiceTest.java
git commit -m "feat: add PKCE S256 verification service with tests"
```

---

## Task 5: Key Service

**Files:**
- Create: `src/main/java/com/baskettecase/feauxauth/service/KeyService.java`

- [ ] **Step 1: Create `KeyService.java`**

```java
package com.baskettecase.feauxauth.service;

import com.baskettecase.feauxauth.model.SigningKey;
import com.baskettecase.feauxauth.repository.SigningKeyRepository;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeyService {

    private final SigningKeyRepository signingKeyRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeKeyPair() {
        if (signingKeyRepository.findByActiveTrue().isPresent()) {
            log.info("Active signing key found");
            return;
        }

        log.info("No active signing key found — generating RSA-2048 keypair");
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            String kid = UUID.randomUUID().toString().substring(0, 16);

            SigningKey signingKey = new SigningKey();
            signingKey.setKid(kid);
            signingKey.setPrivateKey(encodePem(keyPair.getPrivate().getEncoded(), "PRIVATE KEY"));
            signingKey.setPublicKey(encodePem(keyPair.getPublic().getEncoded(), "PUBLIC KEY"));
            signingKey.setCreatedAt(LocalDateTime.now());
            signingKey.setActive(true);

            signingKeyRepository.save(signingKey);
            log.info("Generated signing key with kid={}", kid);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA keypair", e);
        }
    }

    public SigningKey getActiveKey() {
        return signingKeyRepository.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("No active signing key"));
    }

    public RSAKey getActiveRSAKey() {
        SigningKey key = getActiveKey();
        try {
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");

            byte[] pubBytes = decodePem(key.getPublicKey());
            RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(
                    new java.security.spec.X509EncodedKeySpec(pubBytes));

            byte[] privBytes = decodePem(key.getPrivateKey());
            RSAPrivateKey privateKey = (RSAPrivateKey) kf.generatePrivate(
                    new java.security.spec.PKCS8EncodedKeySpec(privBytes));

            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(key.getKid())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load RSA key", e);
        }
    }

    public RSAKey getPublicRSAKey() {
        SigningKey key = getActiveKey();
        try {
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
            byte[] pubBytes = decodePem(key.getPublicKey());
            RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(
                    new java.security.spec.X509EncodedKeySpec(pubBytes));

            return new RSAKey.Builder(publicKey)
                    .keyID(key.getKid())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load public RSA key", e);
        }
    }

    private String encodePem(byte[] keyBytes, String type) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyBytes);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----";
    }

    private byte[] decodePem(String pem) {
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
mvn compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/baskettecase/feauxauth/service/KeyService.java
git commit -m "feat: add RSA keypair generation and management service"
```

---

## Task 6: Token Service + Tests

**Files:**
- Create: `src/main/java/com/baskettecase/feauxauth/service/TokenService.java`
- Create: `src/test/java/com/baskettecase/feauxauth/service/TokenServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.baskettecase.feauxauth.service;

import com.baskettecase.feauxauth.model.AccessToken;
import com.baskettecase.feauxauth.model.OAuthClient;
import com.baskettecase.feauxauth.model.OAuthUser;
import com.baskettecase.feauxauth.model.RefreshToken;
import com.baskettecase.feauxauth.repository.AccessTokenRepository;
import com.baskettecase.feauxauth.repository.RefreshTokenRepository;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock private KeyService keyService;
    @Mock private AccessTokenRepository accessTokenRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    private TokenService tokenService;
    private RSAKey rsaKey;

    @BeforeEach
    void setUp() throws Exception {
        rsaKey = new RSAKeyGenerator(2048).keyID("test-kid").generate();
        tokenService = new TokenService(keyService, accessTokenRepository, refreshTokenRepository, "http://localhost:8080");
    }

    @Test
    void mintAccessToken_returnsValidJwt() throws Exception {
        when(keyService.getActiveRSAKey()).thenReturn(rsaKey);
        when(accessTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        OAuthUser user = new OAuthUser();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setDisplayName("Test User");

        OAuthClient client = new OAuthClient();
        client.setClientId("my-app");
        client.setAccessTokenTtl(3600);

        String jwt = tokenService.mintAccessToken(user, client, "openid email");

        SignedJWT parsed = SignedJWT.parse(jwt);
        assertThat(parsed.getJWTClaimsSet().getIssuer()).isEqualTo("http://localhost:8080");
        assertThat(parsed.getJWTClaimsSet().getSubject()).isEqualTo("test@example.com");
        assertThat(parsed.getJWTClaimsSet().getAudience()).contains("my-app");
        assertThat(parsed.getJWTClaimsSet().getClaim("email")).isEqualTo("test@example.com");
        assertThat(parsed.getJWTClaimsSet().getClaim("scope")).isEqualTo("openid email");
    }

    @Test
    void mintRefreshToken_returns256BitToken() throws Exception {
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        OAuthUser user = new OAuthUser();
        user.setId(UUID.randomUUID());

        OAuthClient client = new OAuthClient();
        client.setClientId("my-app");
        client.setRefreshTokenTtl(2592000);

        String token = tokenService.mintRefreshToken(user, client, "openid");

        assertThat(token).isNotBlank();
        assertThat(token.length()).isGreaterThanOrEqualTo(32);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getScope()).isEqualTo("openid");
    }

    @Test
    void verifyAccessToken_validToken_returnsClaims() throws Exception {
        when(keyService.getActiveRSAKey()).thenReturn(rsaKey);
        when(keyService.getPublicRSAKey()).thenReturn(rsaKey.toPublicJWK());
        when(accessTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(accessTokenRepository.findById(any())).thenAnswer(i -> {
            AccessToken at = new AccessToken();
            at.setJti(i.getArgument(0));
            at.setRevoked(false);
            return Optional.of(at);
        });

        OAuthUser user = new OAuthUser();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setDisplayName("Test User");

        OAuthClient client = new OAuthClient();
        client.setClientId("my-app");
        client.setAccessTokenTtl(3600);

        String jwt = tokenService.mintAccessToken(user, client, "openid");
        var claims = tokenService.verifyAccessToken(jwt);

        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo("test@example.com");
    }

    @Test
    void verifyAccessToken_revokedToken_returnsEmpty() throws Exception {
        when(keyService.getActiveRSAKey()).thenReturn(rsaKey);
        when(keyService.getPublicRSAKey()).thenReturn(rsaKey.toPublicJWK());
        when(accessTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(accessTokenRepository.findById(any())).thenAnswer(i -> {
            AccessToken at = new AccessToken();
            at.setJti(i.getArgument(0));
            at.setRevoked(true);
            return Optional.of(at);
        });

        OAuthUser user = new OAuthUser();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setDisplayName("Test User");

        OAuthClient client = new OAuthClient();
        client.setClientId("my-app");
        client.setAccessTokenTtl(3600);

        String jwt = tokenService.mintAccessToken(user, client, "openid");
        var claims = tokenService.verifyAccessToken(jwt);

        assertThat(claims).isEmpty();
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
mvn test -Dtest=com.baskettecase.feauxauth.service.TokenServiceTest -DfailIfNoTests=false
```

Expected: FAIL (class not found)

- [ ] **Step 3: Implement `TokenService.java`**

```java
package com.baskettecase.feauxauth.service;

import com.baskettecase.feauxauth.model.AccessToken;
import com.baskettecase.feauxauth.model.OAuthClient;
import com.baskettecase.feauxauth.model.OAuthUser;
import com.baskettecase.feauxauth.model.RefreshToken;
import com.baskettecase.feauxauth.repository.AccessTokenRepository;
import com.baskettecase.feauxauth.repository.RefreshTokenRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
@Slf4j
public class TokenService {

    private final KeyService keyService;
    private final AccessTokenRepository accessTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final String issuer;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(KeyService keyService,
                        AccessTokenRepository accessTokenRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        @Value("${feauxauth.issuer}") String issuer) {
        this.keyService = keyService;
        this.accessTokenRepository = accessTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.issuer = issuer;
    }

    public String mintAccessToken(OAuthUser user, OAuthClient client, String scope) {
        try {
            RSAKey rsaKey = keyService.getActiveRSAKey();
            String jti = UUID.randomUUID().toString();
            Date now = new Date();
            Date expiry = new Date(now.getTime() + (long) client.getAccessTokenTtl() * 1000);

            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(user.getEmail())
                    .audience(client.getClientId())
                    .expirationTime(expiry)
                    .issueTime(now)
                    .jwtID(jti)
                    .claim("scope", scope);

            if (scope.contains("email") || scope.contains("openid")) {
                claimsBuilder.claim("email", user.getEmail());
            }
            if (scope.contains("profile")) {
                claimsBuilder.claim("name", user.getDisplayName());
            }

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(rsaKey.getKeyID())
                    .type(JOSEObjectType.JWT)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsBuilder.build());
            signedJWT.sign(new RSASSASigner(rsaKey));

            // Record token for revocation tracking
            AccessToken accessToken = new AccessToken();
            accessToken.setJti(jti);
            accessToken.setClientId(client.getClientId());
            accessToken.setUserId(user.getId());
            accessToken.setScope(scope);
            accessToken.setExpiresAt(LocalDateTime.ofInstant(expiry.toInstant(), ZoneOffset.UTC));
            accessToken.setCreatedAt(LocalDateTime.now());
            accessTokenRepository.save(accessToken);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to sign access token", e);
        }
    }

    public String mintIdToken(OAuthUser user, OAuthClient client, String scope, String nonce) {
        try {
            RSAKey rsaKey = keyService.getActiveRSAKey();
            Date now = new Date();
            Date expiry = new Date(now.getTime() + (long) client.getAccessTokenTtl() * 1000);

            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(user.getEmail())
                    .audience(client.getClientId())
                    .expirationTime(expiry)
                    .issueTime(now)
                    .claim("email", user.getEmail())
                    .claim("name", user.getDisplayName());

            if (nonce != null && !nonce.isBlank()) {
                claimsBuilder.claim("nonce", nonce);
            }

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(rsaKey.getKeyID())
                    .type(JOSEObjectType.JWT)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsBuilder.build());
            signedJWT.sign(new RSASSASigner(rsaKey));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to sign ID token", e);
        }
    }

    public String mintRefreshToken(OAuthUser user, OAuthClient client, String scope) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String tokenValue = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(tokenValue);
        refreshToken.setClientId(client.getClientId());
        refreshToken.setUserId(user.getId());
        refreshToken.setScope(scope);
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(client.getRefreshTokenTtl()));
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        return tokenValue;
    }

    public Optional<JWTClaimsSet> verifyAccessToken(String jwt) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(jwt);
            RSAKey publicKey = keyService.getPublicRSAKey();
            if (!signedJWT.verify(new RSASSAVerifier(publicKey))) {
                return Optional.empty();
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            if (claims.getExpirationTime().before(new Date())) {
                return Optional.empty();
            }

            // Check revocation
            String jti = claims.getJWTID();
            Optional<AccessToken> tokenRecord = accessTokenRepository.findById(jti);
            if (tokenRecord.isPresent() && tokenRecord.get().isRevoked()) {
                return Optional.empty();
            }

            return Optional.of(claims);
        } catch (Exception e) {
            log.debug("Token verification failed", e);
            return Optional.empty();
        }
    }

    public JWTClaimsSet decodeWithoutVerification(String jwt) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(jwt);
            return signedJWT.getJWTClaimsSet();
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode JWT", e);
        }
    }

    public boolean revokeToken(String tokenValue) {
        // Try as access token (by JTI)
        Optional<AccessToken> accessToken = accessTokenRepository.findById(tokenValue);
        if (accessToken.isPresent()) {
            accessToken.get().setRevoked(true);
            accessTokenRepository.save(accessToken.get());
            return true;
        }

        // Try parsing as JWT to extract JTI
        try {
            SignedJWT signedJWT = SignedJWT.parse(tokenValue);
            String jti = signedJWT.getJWTClaimsSet().getJWTID();
            Optional<AccessToken> byJti = accessTokenRepository.findById(jti);
            if (byJti.isPresent()) {
                byJti.get().setRevoked(true);
                accessTokenRepository.save(byJti.get());
                return true;
            }
        } catch (Exception ignored) {
            // Not a JWT — try as refresh token
        }

        // Try as refresh token
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findById(tokenValue);
        if (refreshToken.isPresent()) {
            refreshToken.get().setRevoked(true);
            refreshTokenRepository.save(refreshToken.get());
            return true;
        }

        return false;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
mvn test -Dtest=com.baskettecase.feauxauth.service.TokenServiceTest
```

Expected: 4 tests PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/baskettecase/feauxauth/service/TokenService.java \
        src/test/java/com/baskettecase/feauxauth/service/TokenServiceTest.java
git commit -m "feat: add JWT token minting, verification, and revocation service"
```

---

## Task 7: Auth Code Service

**Files:**
- Create: `src/main/java/com/baskettecase/feauxauth/service/AuthCodeService.java`

- [ ] **Step 1: Create `AuthCodeService.java`**

```java
package com.baskettecase.feauxauth.service;

import com.baskettecase.feauxauth.model.AuthCode;
import com.baskettecase.feauxauth.repository.AuthCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthCodeService {

    private final AuthCodeRepository authCodeRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateCode(String clientId, UUID userId, String redirectUri, String scope, String codeChallenge) {
        byte[] codeBytes = new byte[32];
        secureRandom.nextBytes(codeBytes);
        String codeValue = Base64.getUrlEncoder().withoutPadding().encodeToString(codeBytes);

        AuthCode authCode = new AuthCode();
        authCode.setCode(codeValue);
        authCode.setClientId(clientId);
        authCode.setUserId(userId);
        authCode.setRedirectUri(redirectUri);
        authCode.setScope(scope);
        authCode.setCodeChallenge(codeChallenge);
        authCode.setExpiresAt(LocalDateTime.now().plusSeconds(120));
        authCode.setUsed(false);

        authCodeRepository.save(authCode);
        return codeValue;
    }

    public Optional<AuthCode> consumeCode(String code) {
        Optional<AuthCode> authCode = authCodeRepository.findById(code);
        if (authCode.isEmpty()) {
            return Optional.empty();
        }

        AuthCode ac = authCode.get();

        // Single-use check
        if (ac.isUsed()) {
            return Optional.empty();
        }

        // Expiry check
        if (ac.getExpiresAt().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }

        // Mark as used
        ac.setUsed(true);
        authCodeRepository.save(ac);

        return Optional.of(ac);
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
mvn compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/baskettecase/feauxauth/service/AuthCodeService.java
git commit -m "feat: add authorization code generation and exchange service"
```

---

## Task 8: Client and User Services

**Files:**
- Create: `src/main/java/com/baskettecase/feauxauth/service/ClientService.java`
- Create: `src/main/java/com/baskettecase/feauxauth/service/UserService.java`

- [ ] **Step 1: Create `ClientService.java`**

```java
package com.baskettecase.feauxauth.service;

import com.baskettecase.feauxauth.model.OAuthClient;
import com.baskettecase.feauxauth.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public List<OAuthClient> findAll() {
        return clientRepository.findAll();
    }

    public Optional<OAuthClient> findById(UUID id) {
        return clientRepository.findById(id);
    }

    public Optional<OAuthClient> findByClientId(String clientId) {
        return clientRepository.findByClientId(clientId);
    }

    /**
     * Creates a new client. Returns a map with "client" and "plainSecret" keys.
     * The plain secret is only available at creation time.
     */
    public Map<String, Object> create(String name, String clientId, String redirectUris,
                                       String allowedScopes, int accessTokenTtl, int refreshTokenTtl,
                                       boolean requirePkce) {
        String plainSecret = generateSecret();

        OAuthClient client = new OAuthClient();
        client.setName(name);
        client.setClientId(clientId);
        client.setClientSecretHash(passwordEncoder.encode(plainSecret));
        client.setRedirectUris(redirectUris);
        client.setAllowedScopes(allowedScopes);
        client.setAccessTokenTtl(accessTokenTtl);
        client.setRefreshTokenTtl(refreshTokenTtl);
        client.setRequirePkce(requirePkce);

        clientRepository.save(client);

        Map<String, Object> result = new HashMap<>();
        result.put("client", client);
        result.put("plainSecret", plainSecret);
        return result;
    }

    public OAuthClient update(OAuthClient client) {
        return clientRepository.save(client);
    }

    public void delete(UUID id) {
        clientRepository.deleteById(id);
    }

    public String resetSecret(UUID id) {
        OAuthClient client = clientRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Client not found"));
        String plainSecret = generateSecret();
        client.setClientSecretHash(passwordEncoder.encode(plainSecret));
        clientRepository.save(client);
        return plainSecret;
    }

    public boolean verifySecret(OAuthClient client, String plainSecret) {
        return passwordEncoder.matches(plainSecret, client.getClientSecretHash());
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
```

- [ ] **Step 2: Create `UserService.java`**

```java
package com.baskettecase.feauxauth.service;

import com.baskettecase.feauxauth.model.OAuthUser;
import com.baskettecase.feauxauth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public List<OAuthUser> findAll() {
        return userRepository.findAll();
    }

    public Optional<OAuthUser> findById(UUID id) {
        return userRepository.findById(id);
    }

    public Optional<OAuthUser> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public OAuthUser create(String email, String displayName, String password) {
        OAuthUser user = new OAuthUser();
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    public OAuthUser update(OAuthUser user) {
        return userRepository.save(user);
    }

    public void delete(UUID id) {
        userRepository.deleteById(id);
    }

    public String resetPassword(UUID id) {
        OAuthUser user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        String newPassword = generatePassword();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return newPassword;
    }

    public boolean verifyPassword(OAuthUser user, String password) {
        return passwordEncoder.matches(password, user.getPasswordHash());
    }

    public void recordLogin(OAuthUser user) {
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private String generatePassword() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
mvn compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/baskettecase/feauxauth/service/ClientService.java \
        src/main/java/com/baskettecase/feauxauth/service/UserService.java
git commit -m "feat: add client and user CRUD services"
```

---

## Task 9: App Config

**Files:**
- Create: `src/main/java/com/baskettecase/feauxauth/config/AppConfig.java`

- [ ] **Step 1: Create `AppConfig.java`**

```java
package com.baskettecase.feauxauth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "feauxauth")
@Getter @Setter
public class AppConfig {

    private String issuer = "http://localhost:8080";

    private Admin admin = new Admin();

    @Getter @Setter
    public static class Admin {
        private String username = "admin";
        private String password = "feauxauth";
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/baskettecase/feauxauth/config/AppConfig.java
git commit -m "feat: add application configuration properties binding"
```

---

## Task 10: Security Config

**Files:**
- Create: `src/main/java/com/baskettecase/feauxauth/config/SecurityConfig.java`

- [ ] **Step 1: Create `SecurityConfig.java`**

```java
package com.baskettecase.feauxauth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AppConfig appConfig;

    @Bean
    public UserDetailsService adminUserDetailsService() {
        var admin = User.withUsername(appConfig.getAdmin().getUsername())
                .password("{noop}" + appConfig.getAdmin().getPassword())
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminApiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/admin/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMIN"))
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain oauthFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/.well-known/**").permitAll()
                        .requestMatchers("/oauth/authorize").permitAll()
                        .requestMatchers("/oauth/token").permitAll()
                        .requestMatchers("/oauth/revoke").permitAll()
                        .requestMatchers("/oauth/userinfo").permitAll()
                        .requestMatchers("/admin/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().permitAll()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        return http.build();
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
mvn compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/baskettecase/feauxauth/config/SecurityConfig.java
git commit -m "feat: add split security config for admin and OAuth paths"
```

---

## Task 11: Well-Known Endpoints

**Files:**
- Create: `src/main/java/com/baskettecase/feauxauth/controller/WellKnownController.java`

- [ ] **Step 1: Create `WellKnownController.java`**

```java
package com.baskettecase.feauxauth.controller;

import com.baskettecase.feauxauth.config.AppConfig;
import com.baskettecase.feauxauth.service.KeyService;
import com.nimbusds.jose.jwk.JWKSet;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WellKnownController {

    private final AppConfig appConfig;
    private final KeyService keyService;

    @GetMapping(value = "/.well-known/openid-configuration", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> openIdConfiguration() {
        String issuer = appConfig.getIssuer();
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("issuer", issuer);
        config.put("authorization_endpoint", issuer + "/oauth/authorize");
        config.put("token_endpoint", issuer + "/oauth/token");
        config.put("userinfo_endpoint", issuer + "/oauth/userinfo");
        config.put("revocation_endpoint", issuer + "/oauth/revoke");
        config.put("jwks_uri", issuer + "/.well-known/jwks.json");
        config.put("response_types_supported", List.of("code"));
        config.put("grant_types_supported", List.of("authorization_code", "refresh_token"));
        config.put("subject_types_supported", List.of("public"));
        config.put("id_token_signing_alg_values_supported", List.of("RS256"));
        config.put("scopes_supported", List.of("openid", "profile", "email", "offline_access"));
        config.put("token_endpoint_auth_methods_supported", List.of("client_secret_post", "client_secret_basic"));
        config.put("code_challenge_methods_supported", List.of("S256"));
        return config;
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String jwks() {
        JWKSet jwkSet = new JWKSet(keyService.getPublicRSAKey());
        return jwkSet.toJSONObject().toString();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/baskettecase/feauxauth/controller/WellKnownController.java
git commit -m "feat: add OIDC discovery and JWKS endpoints"
```

---

## Task 12: Authorization Endpoint + Login Page

**Files:**
- Create: `src/main/java/com/baskettecase/feauxauth/controller/AuthorizeController.java`
- Create: `src/main/resources/templates/oauth/login.html`
- Create: `src/main/resources/templates/oauth/error.html`

- [ ] **Step 1: Create `AuthorizeController.java`**

```java
package com.baskettecase.feauxauth.controller;

import com.baskettecase.feauxauth.model.OAuthClient;
import com.baskettecase.feauxauth.model.OAuthUser;
import com.baskettecase.feauxauth.service.AuthCodeService;
import com.baskettecase.feauxauth.service.ClientService;
import com.baskettecase.feauxauth.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class AuthorizeController {

    private final ClientService clientService;
    private final UserService userService;
    private final AuthCodeService authCodeService;

    @GetMapping("/oauth/authorize")
    public String authorize(
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("response_type") String responseType,
            @RequestParam(value = "scope", defaultValue = "openid") String scope,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "code_challenge", required = false) String codeChallenge,
            @RequestParam(value = "code_challenge_method", required = false) String codeChallengeMethod,
            @RequestParam(value = "nonce", required = false) String nonce,
            HttpSession session,
            Model model) {

        // Validate response_type
        if (!"code".equals(responseType)) {
            model.addAttribute("error", "unsupported_response_type");
            model.addAttribute("errorDescription", "Only response_type=code is supported");
            return "oauth/error";
        }

        // Validate client
        Optional<OAuthClient> clientOpt = clientService.findByClientId(clientId);
        if (clientOpt.isEmpty() || !clientOpt.get().isEnabled()) {
            model.addAttribute("error", "invalid_client");
            model.addAttribute("errorDescription", "Unknown or disabled client");
            return "oauth/error";
        }

        OAuthClient client = clientOpt.get();

        // Validate redirect_uri
        boolean validRedirect = Arrays.stream(client.getRedirectUris().split("\\n"))
                .map(String::trim)
                .anyMatch(uri -> uri.equals(redirectUri));
        if (!validRedirect) {
            model.addAttribute("error", "invalid_redirect_uri");
            model.addAttribute("errorDescription", "Redirect URI not registered for this client");
            return "oauth/error";
        }

        // Check PKCE requirement
        if (client.isRequirePkce() && (codeChallenge == null || codeChallenge.isBlank())) {
            model.addAttribute("error", "invalid_request");
            model.addAttribute("errorDescription", "This client requires PKCE (code_challenge)");
            return "oauth/error";
        }

        // Store authorize request in session
        session.setAttribute("auth_client_id", clientId);
        session.setAttribute("auth_redirect_uri", redirectUri);
        session.setAttribute("auth_scope", scope);
        session.setAttribute("auth_state", state);
        session.setAttribute("auth_code_challenge", codeChallenge);
        session.setAttribute("auth_nonce", nonce);

        model.addAttribute("clientName", client.getName());
        model.addAttribute("scope", scope);
        return "oauth/login";
    }

    @PostMapping("/oauth/authorize")
    public String login(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            HttpSession session,
            Model model) {

        String clientId = (String) session.getAttribute("auth_client_id");
        String redirectUri = (String) session.getAttribute("auth_redirect_uri");
        String scope = (String) session.getAttribute("auth_scope");
        String state = (String) session.getAttribute("auth_state");
        String codeChallenge = (String) session.getAttribute("auth_code_challenge");

        if (clientId == null || redirectUri == null) {
            model.addAttribute("error", "invalid_request");
            model.addAttribute("errorDescription", "Session expired. Please start the login flow again.");
            return "oauth/error";
        }

        // Authenticate user
        Optional<OAuthUser> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty() || !userOpt.get().isEnabled() || !userService.verifyPassword(userOpt.get(), password)) {
            model.addAttribute("clientName", clientId);
            model.addAttribute("scope", scope);
            model.addAttribute("loginError", "Invalid email or password");
            return "oauth/login";
        }

        OAuthUser user = userOpt.get();
        userService.recordLogin(user);

        // Generate auth code
        String code = authCodeService.generateCode(clientId, user.getId(), redirectUri, scope, codeChallenge);

        // Clear session attributes
        session.removeAttribute("auth_client_id");
        session.removeAttribute("auth_redirect_uri");
        session.removeAttribute("auth_scope");
        session.removeAttribute("auth_state");
        session.removeAttribute("auth_code_challenge");
        session.removeAttribute("auth_nonce");

        // Redirect back to app
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", code);
        if (state != null) {
            builder.queryParam("state", state);
        }

        return "redirect:" + builder.toUriString();
    }
}
```

- [ ] **Step 2: Create `login.html`**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sign In — FeauxAuth</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="min-h-screen bg-gray-900 flex items-center justify-center px-4">
    <div class="w-full max-w-md">
        <div class="bg-gray-800 rounded-2xl shadow-xl p-8">
            <div class="text-center mb-8">
                <h1 class="text-2xl font-bold text-white">FeauxAuth</h1>
                <p class="text-gray-400 mt-2">Sign in to continue to
                    <span class="text-indigo-400" th:text="${clientName}">App</span>
                </p>
                <p class="text-gray-500 text-sm mt-1">Requesting scopes:
                    <span class="text-gray-400" th:text="${scope}">openid</span>
                </p>
            </div>

            <div th:if="${loginError}" class="bg-red-900/50 border border-red-700 text-red-300 px-4 py-3 rounded-lg mb-6">
                <span th:text="${loginError}">Error</span>
            </div>

            <form method="post" th:action="@{/oauth/authorize}" class="space-y-6">
                <div>
                    <label for="email" class="block text-sm font-medium text-gray-300 mb-2">Email</label>
                    <input type="email" id="email" name="email" required
                           class="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                           placeholder="user@example.com">
                </div>
                <div>
                    <label for="password" class="block text-sm font-medium text-gray-300 mb-2">Password</label>
                    <input type="password" id="password" name="password" required
                           class="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                           placeholder="••••••••">
                </div>
                <button type="submit"
                        class="w-full py-3 px-4 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 focus:ring-offset-gray-800">
                    Sign In
                </button>
            </form>

            <div class="mt-6 text-center">
                <p class="text-xs text-gray-500">⚠ FeauxAuth — Lab / Demo use only. Not for production.</p>
            </div>
        </div>
    </div>
</body>
</html>
```

- [ ] **Step 3: Create `error.html`**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Error — FeauxAuth</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="min-h-screen bg-gray-900 flex items-center justify-center px-4">
    <div class="w-full max-w-md">
        <div class="bg-gray-800 rounded-2xl shadow-xl p-8 text-center">
            <div class="text-red-400 text-5xl mb-4">⚠</div>
            <h1 class="text-xl font-bold text-white mb-2">OAuth Error</h1>
            <p class="text-red-400 font-mono text-sm mb-2" th:text="${error}">error_code</p>
            <p class="text-gray-400" th:text="${errorDescription}">Something went wrong.</p>
            <div class="mt-6">
                <p class="text-xs text-gray-500">FeauxAuth — Lab / Demo use only</p>
            </div>
        </div>
    </div>
</body>
</html>
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/baskettecase/feauxauth/controller/AuthorizeController.java \
        src/main/resources/templates/oauth/
git commit -m "feat: add authorization endpoint with login page"
```

---

## Task 13: Token Endpoint

**Files:**
- Create: `src/main/java/com/baskettecase/feauxauth/controller/TokenController.java`

- [ ] **Step 1: Create `TokenController.java`**

```java
package com.baskettecase.feauxauth.controller;

import com.baskettecase.feauxauth.model.AuthCode;
import com.baskettecase.feauxauth.model.OAuthClient;
import com.baskettecase.feauxauth.model.OAuthUser;
import com.baskettecase.feauxauth.model.RefreshToken;
import com.baskettecase.feauxauth.repository.RefreshTokenRepository;
import com.baskettecase.feauxauth.repository.UserRepository;
import com.baskettecase.feauxauth.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class TokenController {

    private final AuthCodeService authCodeService;
    private final TokenService tokenService;
    private final ClientService clientService;
    private final PkceService pkceService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @PostMapping(value = "/oauth/token", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam("client_id") String clientId,
            @RequestParam(value = "client_secret", required = false) String clientSecret,
            @RequestParam(value = "code_verifier", required = false) String codeVerifier,
            @RequestParam(value = "refresh_token", required = false) String refreshTokenValue) {

        // Validate client
        Optional<OAuthClient> clientOpt = clientService.findByClientId(clientId);
        if (clientOpt.isEmpty() || !clientOpt.get().isEnabled()) {
            return errorResponse("invalid_client", "Unknown or disabled client");
        }
        OAuthClient client = clientOpt.get();

        // Client authentication: secret or PKCE
        boolean isPkce = codeVerifier != null && !codeVerifier.isBlank();
        if (!isPkce) {
            if (clientSecret == null || !clientService.verifySecret(client, clientSecret)) {
                return errorResponse("invalid_client", "Invalid client credentials");
            }
        }

        if ("authorization_code".equals(grantType)) {
            return handleAuthorizationCode(client, code, redirectUri, codeVerifier);
        } else if ("refresh_token".equals(grantType)) {
            return handleRefreshToken(client, refreshTokenValue);
        } else {
            return errorResponse("unsupported_grant_type", "Supported: authorization_code, refresh_token");
        }
    }

    private ResponseEntity<?> handleAuthorizationCode(OAuthClient client, String code, String redirectUri, String codeVerifier) {
        if (code == null || code.isBlank()) {
            return errorResponse("invalid_request", "Missing code parameter");
        }

        Optional<AuthCode> authCodeOpt = authCodeService.consumeCode(code);
        if (authCodeOpt.isEmpty()) {
            return errorResponse("invalid_grant", "Invalid, expired, or already-used authorization code");
        }

        AuthCode authCode = authCodeOpt.get();

        // Validate client matches
        if (!authCode.getClientId().equals(client.getClientId())) {
            return errorResponse("invalid_grant", "Code was not issued to this client");
        }

        // Validate redirect_uri matches
        if (!authCode.getRedirectUri().equals(redirectUri)) {
            return errorResponse("invalid_grant", "redirect_uri mismatch");
        }

        // PKCE verification
        if (authCode.getCodeChallenge() != null) {
            if (codeVerifier == null || !pkceService.verifyChallenge(codeVerifier, authCode.getCodeChallenge())) {
                return errorResponse("invalid_grant", "PKCE verification failed");
            }
        }

        // Load user
        Optional<OAuthUser> userOpt = userRepository.findById(authCode.getUserId());
        if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
            return errorResponse("invalid_grant", "User not found or disabled");
        }
        OAuthUser user = userOpt.get();

        // Mint tokens
        String accessToken = tokenService.mintAccessToken(user, client, authCode.getScope());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", accessToken);
        response.put("token_type", "Bearer");
        response.put("expires_in", client.getAccessTokenTtl());

        // Refresh token (if TTL > 0 and offline_access or any scope really)
        if (client.getRefreshTokenTtl() > 0) {
            String refreshToken = tokenService.mintRefreshToken(user, client, authCode.getScope());
            response.put("refresh_token", refreshToken);
        }

        // ID token (if openid scope)
        if (authCode.getScope().contains("openid")) {
            // Retrieve nonce from the original request — stored in session, but we don't have session here
            // The nonce was not stored in auth_codes. For lab use, we omit nonce in id_token.
            String idToken = tokenService.mintIdToken(user, client, authCode.getScope(), null);
            response.put("id_token", idToken);
        }

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> handleRefreshToken(OAuthClient client, String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return errorResponse("invalid_request", "Missing refresh_token parameter");
        }

        Optional<RefreshToken> rtOpt = refreshTokenRepository.findById(refreshTokenValue);
        if (rtOpt.isEmpty()) {
            return errorResponse("invalid_grant", "Invalid refresh token");
        }

        RefreshToken rt = rtOpt.get();

        if (rt.isRevoked()) {
            return errorResponse("invalid_grant", "Refresh token has been revoked");
        }

        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            return errorResponse("invalid_grant", "Refresh token has expired");
        }

        if (!rt.getClientId().equals(client.getClientId())) {
            return errorResponse("invalid_grant", "Refresh token was not issued to this client");
        }

        // Load user
        Optional<OAuthUser> userOpt = userRepository.findById(rt.getUserId());
        if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
            return errorResponse("invalid_grant", "User not found or disabled");
        }
        OAuthUser user = userOpt.get();

        // Mint new access token
        String accessToken = tokenService.mintAccessToken(user, client, rt.getScope());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", accessToken);
        response.put("token_type", "Bearer");
        response.put("expires_in", client.getAccessTokenTtl());
        response.put("refresh_token", refreshTokenValue); // Reuse same refresh token

        if (rt.getScope().contains("openid")) {
            String idToken = tokenService.mintIdToken(user, client, rt.getScope(), null);
            response.put("id_token", idToken);
        }

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, String>> errorResponse(String error, String description) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("error_description", description);
        return ResponseEntity.badRequest().body(body);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/baskettecase/feauxauth/controller/TokenController.java
git commit -m "feat: add token endpoint with auth code and refresh grant support"
```

---

## Task 14: UserInfo and Revocation Endpoints

**Files:**
- Create: `src/main/java/com/baskettecase/feauxauth/controller/UserInfoController.java`
- Create: `src/main/java/com/baskettecase/feauxauth/controller/RevocationController.java`

- [ ] **Step 1: Create `UserInfoController.java`**

```java
package com.baskettecase.feauxauth.controller;

import com.baskettecase.feauxauth.model.OAuthUser;
import com.baskettecase.feauxauth.service.TokenService;
import com.baskettecase.feauxauth.service.UserService;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class UserInfoController {

    private final TokenService tokenService;
    private final UserService userService;

    @GetMapping(value = "/oauth/userinfo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> userInfo(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token", "error_description", "Missing or invalid Bearer token"));
        }

        String token = authHeader.substring(7);
        Optional<JWTClaimsSet> claimsOpt = tokenService.verifyAccessToken(token);
        if (claimsOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token", "error_description", "Token is invalid, expired, or revoked"));
        }

        JWTClaimsSet claims = claimsOpt.get();
        String email = claims.getSubject();
        String scope = (String) claims.getClaim("scope");

        Optional<OAuthUser> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token", "error_description", "User not found or disabled"));
        }

        OAuthUser user = userOpt.get();

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("sub", user.getEmail());

        if (scope != null && (scope.contains("email") || scope.contains("openid"))) {
            profile.put("email", user.getEmail());
        }
        if (scope != null && scope.contains("profile")) {
            profile.put("name", user.getDisplayName());
        }

        return ResponseEntity.ok(profile);
    }
}
```

- [ ] **Step 2: Create `RevocationController.java`**

```java
package com.baskettecase.feauxauth.controller;

import com.baskettecase.feauxauth.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RevocationController {

    private final TokenService tokenService;

    @PostMapping("/oauth/revoke")
    public ResponseEntity<Void> revoke(@RequestParam("token") String token) {
        tokenService.revokeToken(token);
        // RFC 7009: always return 200, even if token was not found
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/baskettecase/feauxauth/controller/UserInfoController.java \
        src/main/java/com/baskettecase/feauxauth/controller/RevocationController.java
git commit -m "feat: add userinfo and token revocation endpoints"
```

---

## Task 15: SPA Fallback Routing

**Files:**
- Create: `src/main/java/com/baskettecase/feauxauth/config/WebConfig.java`

- [ ] **Step 1: Create `WebConfig.java`**

```java
package com.baskettecase.feauxauth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/admin/**")
                .addResourceLocations("classpath:/static/admin/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        return requested.exists() && requested.isReadable()
                                ? requested
                                : new ClassPathResource("/static/admin/index.html");
                    }
                });
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/baskettecase/feauxauth/config/WebConfig.java
git commit -m "feat: add SPA fallback routing for admin UI"
```

---

## Task 16: Admin REST API — Dashboard and Inspector

**Files:**
- Create: `src/main/java/com/baskettecase/feauxauth/controller/api/AdminDashboardApi.java`
- Create: `src/main/java/com/baskettecase/feauxauth/controller/api/AdminInspectorApi.java`

- [ ] **Step 1: Create `AdminDashboardApi.java`**

```java
package com.baskettecase.feauxauth.controller.api;

import com.baskettecase.feauxauth.repository.AccessTokenRepository;
import com.baskettecase.feauxauth.repository.ClientRepository;
import com.baskettecase.feauxauth.repository.UserRepository;
import com.baskettecase.feauxauth.service.KeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardApi {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final AccessTokenRepository accessTokenRepository;
    private final KeyService keyService;

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalClients", clientRepository.count());
        stats.put("totalUsers", userRepository.count());
        stats.put("activeTokens", accessTokenRepository.countByExpiresAtAfterAndRevokedFalse(LocalDateTime.now()));
        stats.put("signingKeyId", keyService.getActiveKey().getKid());
        stats.put("recentTokens", accessTokenRepository.findByExpiresAtAfterOrderByCreatedAtDesc(
                LocalDateTime.now().minusDays(7), PageRequest.of(0, 10)).getContent());
        return stats;
    }
}
```

- [ ] **Step 2: Create `AdminInspectorApi.java`**

```java
package com.baskettecase.feauxauth.controller.api;

import com.baskettecase.feauxauth.model.AccessToken;
import com.baskettecase.feauxauth.repository.AccessTokenRepository;
import com.baskettecase.feauxauth.service.KeyService;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/inspector")
@RequiredArgsConstructor
public class AdminInspectorApi {

    private final KeyService keyService;
    private final AccessTokenRepository accessTokenRepository;

    @PostMapping
    public ResponseEntity<?> inspect(@RequestBody Map<String, String> body) {
        String jwt = body.get("token");
        if (jwt == null || jwt.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing token"));
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(jwt);

            // Header
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", signedJWT.getHeader().getAlgorithm().getName());
            header.put("kid", signedJWT.getHeader().getKeyID());
            header.put("typ", signedJWT.getHeader().getType() != null ? signedJWT.getHeader().getType().toString() : null);

            // Payload
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Map<String, Object> payload = claims.toJSONObject();

            // Signature verification
            boolean signatureValid;
            try {
                signatureValid = signedJWT.verify(new RSASSAVerifier(keyService.getPublicRSAKey()));
            } catch (Exception e) {
                signatureValid = false;
            }

            // Expiry check
            boolean expired = claims.getExpirationTime() != null && claims.getExpirationTime().before(new Date());

            // Revocation check
            String revocationStatus = "unknown";
            String jti = claims.getJWTID();
            if (jti != null) {
                Optional<AccessToken> at = accessTokenRepository.findById(jti);
                if (at.isPresent()) {
                    revocationStatus = at.get().isRevoked() ? "revoked" : "active";
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("header", header);
            result.put("payload", payload);
            result.put("signatureValid", signatureValid);
            result.put("expired", expired);
            result.put("revocationStatus", revocationStatus);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid JWT: " + e.getMessage()));
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/baskettecase/feauxauth/controller/api/AdminDashboardApi.java \
        src/main/java/com/baskettecase/feauxauth/controller/api/AdminInspectorApi.java
git commit -m "feat: add admin dashboard stats and token inspector API"
```

---

## Task 17: Admin REST API — Clients

**Files:**
- Create: `src/main/java/com/baskettecase/feauxauth/controller/api/AdminClientApi.java`

- [ ] **Step 1: Create `AdminClientApi.java`**

```java
package com.baskettecase.feauxauth.controller.api;

import com.baskettecase.feauxauth.model.OAuthClient;
import com.baskettecase.feauxauth.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/clients")
@RequiredArgsConstructor
public class AdminClientApi {

    private final ClientService clientService;

    @GetMapping
    public List<OAuthClient> list() {
        return clientService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OAuthClient> get(@PathVariable UUID id) {
        return clientService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String clientId = (String) body.get("clientId");
        String redirectUris = (String) body.get("redirectUris");
        String allowedScopes = (String) body.getOrDefault("allowedScopes", "openid profile email");
        int accessTokenTtl = ((Number) body.getOrDefault("accessTokenTtl", 3600)).intValue();
        int refreshTokenTtl = ((Number) body.getOrDefault("refreshTokenTtl", 2592000)).intValue();
        boolean requirePkce = (Boolean) body.getOrDefault("requirePkce", false);

        Map<String, Object> result = clientService.create(name, clientId, redirectUris,
                allowedScopes, accessTokenTtl, refreshTokenTtl, requirePkce);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("client", result.get("client"));
        response.put("clientSecret", result.get("plainSecret"));

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        Optional<OAuthClient> existing = clientService.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        OAuthClient client = existing.get();
        if (body.containsKey("name")) client.setName((String) body.get("name"));
        if (body.containsKey("redirectUris")) client.setRedirectUris((String) body.get("redirectUris"));
        if (body.containsKey("allowedScopes")) client.setAllowedScopes((String) body.get("allowedScopes"));
        if (body.containsKey("accessTokenTtl")) client.setAccessTokenTtl(((Number) body.get("accessTokenTtl")).intValue());
        if (body.containsKey("refreshTokenTtl")) client.setRefreshTokenTtl(((Number) body.get("refreshTokenTtl")).intValue());
        if (body.containsKey("requirePkce")) client.setRequirePkce((Boolean) body.get("requirePkce"));
        if (body.containsKey("enabled")) client.setEnabled((Boolean) body.get("enabled"));

        return ResponseEntity.ok(clientService.update(client));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reset-secret")
    public ResponseEntity<?> resetSecret(@PathVariable UUID id) {
        String newSecret = clientService.resetSecret(id);
        return ResponseEntity.ok(Map.of("clientSecret", newSecret));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/baskettecase/feauxauth/controller/api/AdminClientApi.java
git commit -m "feat: add admin client CRUD REST API"
```

---

## Task 18: Admin REST API — Users and Tokens

**Files:**
- Create: `src/main/java/com/baskettecase/feauxauth/controller/api/AdminUserApi.java`
- Create: `src/main/java/com/baskettecase/feauxauth/controller/api/AdminTokenApi.java`

- [ ] **Step 1: Create `AdminUserApi.java`**

```java
package com.baskettecase.feauxauth.controller.api;

import com.baskettecase.feauxauth.model.OAuthUser;
import com.baskettecase.feauxauth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserApi {

    private final UserService userService;

    @GetMapping
    public List<OAuthUser> list() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OAuthUser> get(@PathVariable UUID id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<OAuthUser> create(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String displayName = body.get("displayName");
        String password = body.get("password");
        OAuthUser user = userService.create(email, displayName, password);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        Optional<OAuthUser> existing = userService.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        OAuthUser user = existing.get();
        if (body.containsKey("email")) user.setEmail((String) body.get("email"));
        if (body.containsKey("displayName")) user.setDisplayName((String) body.get("displayName"));
        if (body.containsKey("enabled")) user.setEnabled((Boolean) body.get("enabled"));

        return ResponseEntity.ok(userService.update(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable UUID id) {
        String newPassword = userService.resetPassword(id);
        return ResponseEntity.ok(Map.of("password", newPassword));
    }
}
```

- [ ] **Step 2: Create `AdminTokenApi.java`**

```java
package com.baskettecase.feauxauth.controller.api;

import com.baskettecase.feauxauth.model.AccessToken;
import com.baskettecase.feauxauth.model.RefreshToken;
import com.baskettecase.feauxauth.repository.AccessTokenRepository;
import com.baskettecase.feauxauth.repository.RefreshTokenRepository;
import com.baskettecase.feauxauth.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/tokens")
@RequiredArgsConstructor
public class AdminTokenApi {

    private final AccessTokenRepository accessTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;

    @GetMapping
    public Page<AccessToken> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return accessTokenRepository.findByExpiresAtAfterOrderByCreatedAtDesc(
                LocalDateTime.now(), PageRequest.of(page, size));
    }

    @PostMapping("/{jti}/revoke")
    public ResponseEntity<Void> revoke(@PathVariable String jti) {
        tokenService.revokeToken(jti);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/revoke-user/{userId}")
    public ResponseEntity<Void> revokeAllForUser(@PathVariable UUID userId) {
        List<AccessToken> accessTokens = accessTokenRepository.findByUserIdAndRevokedFalse(userId);
        for (AccessToken at : accessTokens) {
            at.setRevoked(true);
            accessTokenRepository.save(at);
        }

        List<RefreshToken> refreshTokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        for (RefreshToken rt : refreshTokens) {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        }

        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/baskettecase/feauxauth/controller/api/AdminUserApi.java \
        src/main/java/com/baskettecase/feauxauth/controller/api/AdminTokenApi.java
git commit -m "feat: add admin user and token management REST APIs"
```

---

## Task 19: React Frontend Scaffold

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.js`
- Create: `frontend/tailwind.config.js`
- Create: `frontend/postcss.config.js`
- Create: `frontend/index.html`
- Create: `frontend/src/main.jsx`
- Create: `frontend/src/index.css`
- Create: `frontend/src/App.jsx`
- Create: `frontend/src/api/client.js`

- [ ] **Step 1: Create `frontend/package.json`**

```json
{
  "name": "feauxauth-admin",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "react-router-dom": "^6.26.2"
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^4.3.4",
    "autoprefixer": "^10.4.20",
    "postcss": "^8.4.49",
    "tailwindcss": "^3.4.17",
    "vite": "^6.0.7"
  }
}
```

- [ ] **Step 2: Create `frontend/vite.config.js`**

```js
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  base: '/admin/',
  build: {
    outDir: '../src/main/resources/static/admin',
    emptyOutDir: true,
  },
  server: {
    port: 3000,
    proxy: {
      '/api': 'http://localhost:8080',
      '/oauth': 'http://localhost:8080',
      '/.well-known': 'http://localhost:8080',
    },
  },
})
```

- [ ] **Step 3: Create `frontend/tailwind.config.js`**

```js
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {},
  },
  plugins: [],
}
```

- [ ] **Step 4: Create `frontend/postcss.config.js`**

```js
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
}
```

- [ ] **Step 5: Create `frontend/index.html`**

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FeauxAuth Admin</title>
</head>
<body class="bg-gray-900 text-white">
    <div id="root"></div>
    <script type="module" src="/src/main.jsx"></script>
</body>
</html>
```

- [ ] **Step 6: Create `frontend/src/index.css`**

```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

- [ ] **Step 7: Create `frontend/src/main.jsx`**

```jsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter basename="/admin">
      <App />
    </BrowserRouter>
  </React.StrictMode>
)
```

- [ ] **Step 8: Create `frontend/src/api/client.js`**

```js
const getAuth = () => sessionStorage.getItem('feauxauth_credentials')

export const api = {
  async fetch(url, options = {}) {
    const auth = getAuth()
    if (!auth) {
      window.location.href = '/admin/login'
      throw new Error('Not authenticated')
    }

    const response = await fetch(url, {
      ...options,
      headers: {
        'Authorization': `Basic ${auth}`,
        'Content-Type': 'application/json',
        ...options.headers,
      },
    })

    if (response.status === 401) {
      sessionStorage.removeItem('feauxauth_credentials')
      window.location.href = '/admin/login'
      throw new Error('Unauthorized')
    }

    return response
  },

  async get(url) {
    const res = await this.fetch(url)
    return res.json()
  },

  async post(url, body) {
    const res = await this.fetch(url, {
      method: 'POST',
      body: JSON.stringify(body),
    })
    return res.json()
  },

  async put(url, body) {
    const res = await this.fetch(url, {
      method: 'PUT',
      body: JSON.stringify(body),
    })
    return res.json()
  },

  async del(url) {
    return this.fetch(url, { method: 'DELETE' })
  },

  login(username, password) {
    const credentials = btoa(`${username}:${password}`)
    sessionStorage.setItem('feauxauth_credentials', credentials)
  },

  logout() {
    sessionStorage.removeItem('feauxauth_credentials')
  },

  isAuthenticated() {
    return !!getAuth()
  },
}
```

- [ ] **Step 9: Create `frontend/src/App.jsx`**

```jsx
import { Routes, Route, Navigate } from 'react-router-dom'
import { api } from './api/client'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Clients from './pages/Clients'
import ClientForm from './pages/ClientForm'
import Users from './pages/Users'
import UserForm from './pages/UserForm'
import Tokens from './pages/Tokens'
import Inspector from './pages/Inspector'

function ProtectedRoute({ children }) {
  if (!api.isAuthenticated()) {
    return <Navigate to="/login" replace />
  }
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route index element={<Dashboard />} />
        <Route path="clients" element={<Clients />} />
        <Route path="clients/new" element={<ClientForm />} />
        <Route path="clients/:id/edit" element={<ClientForm />} />
        <Route path="users" element={<Users />} />
        <Route path="users/new" element={<UserForm />} />
        <Route path="users/:id/edit" element={<UserForm />} />
        <Route path="tokens" element={<Tokens />} />
        <Route path="inspector" element={<Inspector />} />
      </Route>
    </Routes>
  )
}
```

- [ ] **Step 10: Commit**

```bash
git add frontend/
git commit -m "feat: scaffold React/Vite frontend with routing and API client"
```

---

## Task 20: React Components — Layout, StatCard, DataTable

**Files:**
- Create: `frontend/src/components/Layout.jsx`
- Create: `frontend/src/components/StatCard.jsx`
- Create: `frontend/src/components/DataTable.jsx`

- [ ] **Step 1: Create `frontend/src/components/Layout.jsx`**

```jsx
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { api } from '../api/client'

const navItems = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/clients', label: 'OAuth Clients' },
  { to: '/users', label: 'Users' },
  { to: '/tokens', label: 'Active Tokens' },
  { to: '/inspector', label: 'Token Inspector' },
]

export default function Layout() {
  const navigate = useNavigate()

  const handleLogout = () => {
    api.logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-gray-900">
      <nav className="bg-gray-800 border-b border-gray-700">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center space-x-8">
              <span className="text-xl font-bold text-white">FeauxAuth</span>
              <div className="flex space-x-1">
                {navItems.map(item => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    end={item.end}
                    className={({ isActive }) =>
                      `px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                        isActive
                          ? 'bg-gray-900 text-white'
                          : 'text-gray-300 hover:bg-gray-700 hover:text-white'
                      }`
                    }
                  >
                    {item.label}
                  </NavLink>
                ))}
              </div>
            </div>
            <button
              onClick={handleLogout}
              className="text-gray-300 hover:text-white text-sm font-medium transition-colors"
            >
              Logout
            </button>
          </div>
        </div>
      </nav>
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>
    </div>
  )
}
```

- [ ] **Step 2: Create `frontend/src/components/StatCard.jsx`**

```jsx
export default function StatCard({ label, value }) {
  return (
    <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
      <p className="text-sm font-medium text-gray-400">{label}</p>
      <p className="mt-2 text-3xl font-bold text-white">{value}</p>
    </div>
  )
}
```

- [ ] **Step 3: Create `frontend/src/components/DataTable.jsx`**

```jsx
export default function DataTable({ columns, data, actions }) {
  return (
    <div className="overflow-x-auto rounded-xl border border-gray-700">
      <table className="min-w-full divide-y divide-gray-700">
        <thead className="bg-gray-800">
          <tr>
            {columns.map(col => (
              <th key={col.key} className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                {col.label}
              </th>
            ))}
            {actions && (
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-400 uppercase tracking-wider">
                Actions
              </th>
            )}
          </tr>
        </thead>
        <tbody className="bg-gray-800/50 divide-y divide-gray-700">
          {data.map((row, i) => (
            <tr key={row.id || i} className="hover:bg-gray-700/50 transition-colors">
              {columns.map(col => (
                <td key={col.key} className="px-6 py-4 whitespace-nowrap text-sm text-gray-300">
                  {col.render ? col.render(row[col.key], row) : String(row[col.key] ?? '')}
                </td>
              ))}
              {actions && (
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm">
                  {actions(row)}
                </td>
              )}
            </tr>
          ))}
          {data.length === 0 && (
            <tr>
              <td colSpan={columns.length + (actions ? 1 : 0)} className="px-6 py-8 text-center text-gray-500">
                No data
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/components/
git commit -m "feat: add Layout, StatCard, and DataTable components"
```

---

## Task 21: React Pages — Login and Dashboard

**Files:**
- Create: `frontend/src/pages/Login.jsx`
- Create: `frontend/src/pages/Dashboard.jsx`

- [ ] **Step 1: Create `frontend/src/pages/Login.jsx`**

```jsx
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'

export default function Login() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    api.login(username, password)

    try {
      const res = await api.fetch('/api/admin/dashboard/stats')
      if (res.ok) {
        navigate('/')
      } else {
        api.logout()
        setError('Invalid credentials')
      }
    } catch {
      setError('Invalid credentials')
    }
  }

  return (
    <div className="min-h-screen bg-gray-900 flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        <div className="bg-gray-800 rounded-2xl shadow-xl p-8">
          <div className="text-center mb-8">
            <h1 className="text-2xl font-bold text-white">FeauxAuth</h1>
            <p className="text-gray-400 mt-2">Admin Console</p>
          </div>

          {error && (
            <div className="bg-red-900/50 border border-red-700 text-red-300 px-4 py-3 rounded-lg mb-6">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Username</label>
              <input
                type="text"
                value={username}
                onChange={e => setUsername(e.target.value)}
                required
                className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                placeholder="admin"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Password</label>
              <input
                type="password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                required
                className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <button
              type="submit"
              className="w-full py-3 px-4 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              Sign In
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Create `frontend/src/pages/Dashboard.jsx`**

```jsx
import { useState, useEffect } from 'react'
import { api } from '../api/client'
import StatCard from '../components/StatCard'
import DataTable from '../components/DataTable'

const tokenColumns = [
  { key: 'jti', label: 'Token ID', render: v => v?.substring(0, 8) + '...' },
  { key: 'clientId', label: 'Client' },
  { key: 'userId', label: 'User', render: v => v?.substring(0, 8) + '...' },
  { key: 'scope', label: 'Scopes' },
  { key: 'createdAt', label: 'Issued', render: v => new Date(v).toLocaleString() },
  { key: 'expiresAt', label: 'Expires', render: v => new Date(v).toLocaleString() },
  { key: 'revoked', label: 'Revoked', render: v => v ? 'Yes' : 'No' },
]

export default function Dashboard() {
  const [stats, setStats] = useState(null)

  useEffect(() => {
    api.get('/api/admin/dashboard/stats').then(setStats)
  }, [])

  if (!stats) return <div className="text-gray-400">Loading...</div>

  return (
    <div>
      <h1 className="text-2xl font-bold text-white mb-6">Dashboard</h1>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <StatCard label="Total Clients" value={stats.totalClients} />
        <StatCard label="Total Users" value={stats.totalUsers} />
        <StatCard label="Active Tokens" value={stats.activeTokens} />
        <StatCard label="Signing Key" value={stats.signingKeyId} />
      </div>

      <h2 className="text-lg font-semibold text-white mb-4">Recent Tokens</h2>
      <DataTable columns={tokenColumns} data={stats.recentTokens || []} />
    </div>
  )
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/Login.jsx frontend/src/pages/Dashboard.jsx
git commit -m "feat: add admin login and dashboard pages"
```

---

## Task 22: React Pages — Clients

**Files:**
- Create: `frontend/src/pages/Clients.jsx`
- Create: `frontend/src/pages/ClientForm.jsx`

- [ ] **Step 1: Create `frontend/src/pages/Clients.jsx`**

```jsx
import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import DataTable from '../components/DataTable'

const columns = [
  { key: 'clientId', label: 'Client ID' },
  { key: 'name', label: 'Name' },
  { key: 'allowedScopes', label: 'Scopes' },
  { key: 'enabled', label: 'Enabled', render: v => v ? 'Yes' : 'No' },
  { key: 'accessTokenTtl', label: 'Token TTL', render: v => `${v}s` },
]

export default function Clients() {
  const [clients, setClients] = useState([])

  useEffect(() => {
    api.get('/api/admin/clients').then(setClients)
  }, [])

  const handleDelete = async (id) => {
    if (!confirm('Delete this client?')) return
    await api.del(`/api/admin/clients/${id}`)
    setClients(clients.filter(c => c.id !== id))
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-white">OAuth Clients</h1>
        <Link
          to="/clients/new"
          className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium transition-colors"
        >
          Add Client
        </Link>
      </div>

      <DataTable
        columns={columns}
        data={clients}
        actions={(row) => (
          <div className="space-x-3">
            <Link to={`/clients/${row.id}/edit`} className="text-indigo-400 hover:text-indigo-300">Edit</Link>
            <button onClick={() => handleDelete(row.id)} className="text-red-400 hover:text-red-300">Delete</button>
          </div>
        )}
      />
    </div>
  )
}
```

- [ ] **Step 2: Create `frontend/src/pages/ClientForm.jsx`**

```jsx
import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'

export default function ClientForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const isEdit = !!id

  const [form, setForm] = useState({
    name: '',
    clientId: '',
    redirectUris: '',
    allowedScopes: 'openid profile email',
    accessTokenTtl: 3600,
    refreshTokenTtl: 2592000,
    requirePkce: false,
    enabled: true,
  })
  const [secret, setSecret] = useState(null)

  useEffect(() => {
    if (isEdit) {
      api.get(`/api/admin/clients/${id}`).then(data => {
        setForm({
          name: data.name,
          clientId: data.clientId,
          redirectUris: data.redirectUris,
          allowedScopes: data.allowedScopes,
          accessTokenTtl: data.accessTokenTtl,
          refreshTokenTtl: data.refreshTokenTtl,
          requirePkce: data.requirePkce,
          enabled: data.enabled,
        })
      })
    }
  }, [id, isEdit])

  const handleNameChange = (name) => {
    setForm(f => ({
      ...f,
      name,
      clientId: isEdit ? f.clientId : name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, ''),
    }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (isEdit) {
      await api.put(`/api/admin/clients/${id}`, form)
      navigate('/clients')
    } else {
      const result = await api.post('/api/admin/clients', form)
      setSecret(result.clientSecret)
    }
  }

  const handleResetSecret = async () => {
    const result = await api.post(`/api/admin/clients/${id}/reset-secret`)
    setSecret(result.clientSecret)
  }

  if (secret) {
    return (
      <div className="max-w-2xl">
        <div className="bg-green-900/50 border border-green-700 rounded-xl p-6">
          <h2 className="text-lg font-bold text-green-300 mb-4">Client Created Successfully</h2>
          <p className="text-gray-300 mb-2">Copy the client secret now — it won't be shown again.</p>
          <div className="space-y-3">
            <div>
              <label className="text-sm text-gray-400">Client ID</label>
              <div className="font-mono bg-gray-800 px-4 py-2 rounded-lg text-white">{form.clientId}</div>
            </div>
            <div>
              <label className="text-sm text-gray-400">Client Secret</label>
              <div className="font-mono bg-gray-800 px-4 py-2 rounded-lg text-white break-all">{secret}</div>
            </div>
            <button
              onClick={() => navigator.clipboard.writeText(secret)}
              className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm transition-colors"
            >
              Copy Secret
            </button>
            <button
              onClick={() => navigate('/clients')}
              className="ml-3 px-4 py-2 bg-gray-600 hover:bg-gray-500 text-white rounded-lg text-sm transition-colors"
            >
              Done
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-bold text-white mb-6">{isEdit ? 'Edit Client' : 'Add Client'}</h1>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">Client Name</label>
          <input type="text" value={form.name} onChange={e => handleNameChange(e.target.value)} required
                 className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500" />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">Client ID</label>
          <input type="text" value={form.clientId} onChange={e => setForm(f => ({ ...f, clientId: e.target.value }))}
                 required disabled={isEdit}
                 className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:opacity-50" />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">Redirect URIs (one per line)</label>
          <textarea value={form.redirectUris} onChange={e => setForm(f => ({ ...f, redirectUris: e.target.value }))}
                    required rows={3}
                    className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500" />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">Allowed Scopes (space-separated)</label>
          <input type="text" value={form.allowedScopes} onChange={e => setForm(f => ({ ...f, allowedScopes: e.target.value }))} required
                 className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500" />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">Access Token TTL (seconds)</label>
            <input type="number" value={form.accessTokenTtl} onChange={e => setForm(f => ({ ...f, accessTokenTtl: parseInt(e.target.value) }))}
                   className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">Refresh Token TTL (seconds)</label>
            <input type="number" value={form.refreshTokenTtl} onChange={e => setForm(f => ({ ...f, refreshTokenTtl: parseInt(e.target.value) }))}
                   className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          </div>
        </div>

        <div className="flex items-center space-x-6">
          <label className="flex items-center space-x-2">
            <input type="checkbox" checked={form.requirePkce} onChange={e => setForm(f => ({ ...f, requirePkce: e.target.checked }))}
                   className="rounded bg-gray-700 border-gray-600 text-indigo-500 focus:ring-indigo-500" />
            <span className="text-sm text-gray-300">Require PKCE</span>
          </label>
          <label className="flex items-center space-x-2">
            <input type="checkbox" checked={form.enabled} onChange={e => setForm(f => ({ ...f, enabled: e.target.checked }))}
                   className="rounded bg-gray-700 border-gray-600 text-indigo-500 focus:ring-indigo-500" />
            <span className="text-sm text-gray-300">Enabled</span>
          </label>
        </div>

        <div className="flex space-x-3">
          <button type="submit"
                  className="px-6 py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg transition-colors">
            {isEdit ? 'Save Changes' : 'Create Client'}
          </button>
          {isEdit && (
            <button type="button" onClick={handleResetSecret}
                    className="px-6 py-3 bg-yellow-600 hover:bg-yellow-700 text-white font-medium rounded-lg transition-colors">
              Reset Secret
            </button>
          )}
          <button type="button" onClick={() => navigate('/clients')}
                  className="px-6 py-3 bg-gray-600 hover:bg-gray-500 text-white font-medium rounded-lg transition-colors">
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/Clients.jsx frontend/src/pages/ClientForm.jsx
git commit -m "feat: add client list and form pages"
```

---

## Task 23: React Pages — Users

**Files:**
- Create: `frontend/src/pages/Users.jsx`
- Create: `frontend/src/pages/UserForm.jsx`

- [ ] **Step 1: Create `frontend/src/pages/Users.jsx`**

```jsx
import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import DataTable from '../components/DataTable'

const columns = [
  { key: 'email', label: 'Email' },
  { key: 'displayName', label: 'Display Name' },
  { key: 'enabled', label: 'Enabled', render: v => v ? 'Yes' : 'No' },
  { key: 'lastLoginAt', label: 'Last Login', render: v => v ? new Date(v).toLocaleString() : 'Never' },
]

export default function Users() {
  const [users, setUsers] = useState([])

  useEffect(() => {
    api.get('/api/admin/users').then(setUsers)
  }, [])

  const handleDelete = async (id) => {
    if (!confirm('Delete this user?')) return
    await api.del(`/api/admin/users/${id}`)
    setUsers(users.filter(u => u.id !== id))
  }

  const handleResetPassword = async (id) => {
    const result = await api.post(`/api/admin/users/${id}/reset-password`)
    alert(`New password: ${result.password}\n\nCopy it now — it won't be shown again.`)
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-white">Users</h1>
        <Link
          to="/users/new"
          className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium transition-colors"
        >
          Add User
        </Link>
      </div>

      <DataTable
        columns={columns}
        data={users}
        actions={(row) => (
          <div className="space-x-3">
            <Link to={`/users/${row.id}/edit`} className="text-indigo-400 hover:text-indigo-300">Edit</Link>
            <button onClick={() => handleResetPassword(row.id)} className="text-yellow-400 hover:text-yellow-300">Reset Pwd</button>
            <button onClick={() => handleDelete(row.id)} className="text-red-400 hover:text-red-300">Delete</button>
          </div>
        )}
      />
    </div>
  )
}
```

- [ ] **Step 2: Create `frontend/src/pages/UserForm.jsx`**

```jsx
import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'

export default function UserForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const isEdit = !!id

  const [form, setForm] = useState({
    email: '',
    displayName: '',
    password: '',
    enabled: true,
  })

  useEffect(() => {
    if (isEdit) {
      api.get(`/api/admin/users/${id}`).then(data => {
        setForm({
          email: data.email,
          displayName: data.displayName,
          password: '',
          enabled: data.enabled,
        })
      })
    }
  }, [id, isEdit])

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (isEdit) {
      const { password, ...updateData } = form
      await api.put(`/api/admin/users/${id}`, updateData)
    } else {
      await api.post('/api/admin/users', form)
    }
    navigate('/users')
  }

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-bold text-white mb-6">{isEdit ? 'Edit User' : 'Add User'}</h1>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">Email</label>
          <input type="email" value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))}
                 required
                 className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500" />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">Display Name</label>
          <input type="text" value={form.displayName} onChange={e => setForm(f => ({ ...f, displayName: e.target.value }))}
                 required
                 className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500" />
        </div>

        {!isEdit && (
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">Password (min 8 characters)</label>
            <input type="password" value={form.password} onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
                   required minLength={8}
                   className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          </div>
        )}

        <label className="flex items-center space-x-2">
          <input type="checkbox" checked={form.enabled} onChange={e => setForm(f => ({ ...f, enabled: e.target.checked }))}
                 className="rounded bg-gray-700 border-gray-600 text-indigo-500 focus:ring-indigo-500" />
          <span className="text-sm text-gray-300">Enabled</span>
        </label>

        <div className="flex space-x-3">
          <button type="submit"
                  className="px-6 py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg transition-colors">
            {isEdit ? 'Save Changes' : 'Create User'}
          </button>
          <button type="button" onClick={() => navigate('/users')}
                  className="px-6 py-3 bg-gray-600 hover:bg-gray-500 text-white font-medium rounded-lg transition-colors">
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/Users.jsx frontend/src/pages/UserForm.jsx
git commit -m "feat: add user list and form pages"
```

---

## Task 24: React Pages — Tokens and Inspector

**Files:**
- Create: `frontend/src/pages/Tokens.jsx`
- Create: `frontend/src/pages/Inspector.jsx`

- [ ] **Step 1: Create `frontend/src/pages/Tokens.jsx`**

```jsx
import { useState, useEffect } from 'react'
import { api } from '../api/client'
import DataTable from '../components/DataTable'

const columns = [
  { key: 'jti', label: 'Token ID', render: v => v?.substring(0, 8) + '...' },
  { key: 'clientId', label: 'Client' },
  { key: 'userId', label: 'User', render: v => v?.substring(0, 8) + '...' },
  { key: 'scope', label: 'Scopes' },
  { key: 'createdAt', label: 'Issued', render: v => new Date(v).toLocaleString() },
  { key: 'expiresAt', label: 'Expires', render: v => new Date(v).toLocaleString() },
  { key: 'revoked', label: 'Status', render: v => v ? 'Revoked' : 'Active' },
]

export default function Tokens() {
  const [tokens, setTokens] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const loadTokens = async (p) => {
    const data = await api.get(`/api/admin/tokens?page=${p}&size=20`)
    setTokens(data.content || [])
    setTotalPages(data.totalPages || 0)
    setPage(p)
  }

  useEffect(() => { loadTokens(0) }, [])

  const handleRevoke = async (jti) => {
    await api.post(`/api/admin/tokens/${jti}/revoke`)
    loadTokens(page)
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-white mb-6">Active Tokens</h1>

      <DataTable
        columns={columns}
        data={tokens}
        actions={(row) => (
          !row.revoked && (
            <button onClick={() => handleRevoke(row.jti)} className="text-red-400 hover:text-red-300">
              Revoke
            </button>
          )
        )}
      />

      {totalPages > 1 && (
        <div className="flex justify-center space-x-2 mt-4">
          <button disabled={page === 0} onClick={() => loadTokens(page - 1)}
                  className="px-3 py-1 bg-gray-700 text-white rounded disabled:opacity-50">Prev</button>
          <span className="text-gray-400 py-1">Page {page + 1} of {totalPages}</span>
          <button disabled={page >= totalPages - 1} onClick={() => loadTokens(page + 1)}
                  className="px-3 py-1 bg-gray-700 text-white rounded disabled:opacity-50">Next</button>
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 2: Create `frontend/src/pages/Inspector.jsx`**

```jsx
import { useState } from 'react'
import { api } from '../api/client'

export default function Inspector() {
  const [token, setToken] = useState('')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')

  const handleInspect = async (e) => {
    e.preventDefault()
    setError('')
    setResult(null)

    try {
      const data = await api.post('/api/admin/inspector', { token })
      if (data.error) {
        setError(data.error)
      } else {
        setResult(data)
      }
    } catch (err) {
      setError('Failed to inspect token')
    }
  }

  return (
    <div className="max-w-4xl">
      <h1 className="text-2xl font-bold text-white mb-6">Token Inspector</h1>

      <form onSubmit={handleInspect} className="mb-8">
        <textarea
          value={token}
          onChange={e => setToken(e.target.value)}
          rows={4}
          placeholder="Paste a JWT here..."
          className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white font-mono text-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
        <button type="submit"
                className="mt-3 px-6 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg transition-colors">
          Inspect
        </button>
      </form>

      {error && (
        <div className="bg-red-900/50 border border-red-700 text-red-300 px-4 py-3 rounded-lg mb-6">{error}</div>
      )}

      {result && (
        <div className="space-y-6">
          <div>
            <h2 className="text-lg font-semibold text-white mb-2">Header</h2>
            <pre className="bg-gray-800 border border-gray-700 rounded-lg p-4 text-sm text-green-400 overflow-x-auto">
              {JSON.stringify(result.header, null, 2)}
            </pre>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-white mb-2">Payload</h2>
            <pre className="bg-gray-800 border border-gray-700 rounded-lg p-4 text-sm text-blue-400 overflow-x-auto">
              {JSON.stringify(result.payload, null, 2)}
            </pre>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div className={`rounded-lg p-4 border ${result.signatureValid ? 'bg-green-900/30 border-green-700' : 'bg-red-900/30 border-red-700'}`}>
              <p className="text-sm text-gray-400">Signature</p>
              <p className={`text-lg font-bold ${result.signatureValid ? 'text-green-400' : 'text-red-400'}`}>
                {result.signatureValid ? 'Valid' : 'Invalid'}
              </p>
            </div>
            <div className={`rounded-lg p-4 border ${result.expired ? 'bg-red-900/30 border-red-700' : 'bg-green-900/30 border-green-700'}`}>
              <p className="text-sm text-gray-400">Expiry</p>
              <p className={`text-lg font-bold ${result.expired ? 'text-red-400' : 'text-green-400'}`}>
                {result.expired ? 'Expired' : 'Valid'}
              </p>
            </div>
            <div className={`rounded-lg p-4 border ${
              result.revocationStatus === 'revoked' ? 'bg-red-900/30 border-red-700' :
              result.revocationStatus === 'active' ? 'bg-green-900/30 border-green-700' :
              'bg-gray-800 border-gray-700'
            }`}>
              <p className="text-sm text-gray-400">Revocation</p>
              <p className={`text-lg font-bold ${
                result.revocationStatus === 'revoked' ? 'text-red-400' :
                result.revocationStatus === 'active' ? 'text-green-400' :
                'text-gray-400'
              }`}>
                {result.revocationStatus === 'revoked' ? 'Revoked' :
                 result.revocationStatus === 'active' ? 'Active' : 'Unknown'}
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/Tokens.jsx frontend/src/pages/Inspector.jsx
git commit -m "feat: add token management and inspector pages"
```

---

## Task 25: Integration Test

**Files:**
- Create: `src/test/java/com/baskettecase/feauxauth/AuthFlowIntegrationTest.java`

- [ ] **Step 1: Create `AuthFlowIntegrationTest.java`**

```java
package com.baskettecase.feauxauth;

import com.baskettecase.feauxauth.model.OAuthClient;
import com.baskettecase.feauxauth.model.OAuthUser;
import com.baskettecase.feauxauth.repository.ClientRepository;
import com.baskettecase.feauxauth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ClientRepository clientRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ObjectMapper objectMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        clientRepository.deleteAll();
        userRepository.deleteAll();

        OAuthClient client = new OAuthClient();
        client.setId(UUID.randomUUID());
        client.setClientId("test-app");
        client.setClientSecretHash(encoder.encode("test-secret"));
        client.setName("Test App");
        client.setRedirectUris("http://localhost:3000/callback");
        client.setAllowedScopes("openid profile email");
        clientRepository.save(client);

        OAuthUser user = new OAuthUser();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.com");
        user.setDisplayName("Test User");
        user.setPasswordHash(encoder.encode("password123"));
        userRepository.save(user);
    }

    @Test
    void discoveryEndpoint_returnsValidConfig() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").exists())
                .andExpect(jsonPath("$.authorization_endpoint").exists())
                .andExpect(jsonPath("$.token_endpoint").exists())
                .andExpect(jsonPath("$.jwks_uri").exists());
    }

    @Test
    void jwksEndpoint_returnsPublicKey() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"));
    }

    @Test
    void fullAuthCodeFlow() throws Exception {
        // Step 1: GET /oauth/authorize — should render login page
        MvcResult authorizeResult = mockMvc.perform(get("/oauth/authorize")
                        .param("client_id", "test-app")
                        .param("redirect_uri", "http://localhost:3000/callback")
                        .param("response_type", "code")
                        .param("scope", "openid email")
                        .param("state", "xyz"))
                .andExpect(status().isOk())
                .andReturn();

        String sessionId = authorizeResult.getRequest().getSession().getId();

        // Step 2: POST /oauth/authorize — login
        MvcResult loginResult = mockMvc.perform(post("/oauth/authorize")
                        .session((org.springframework.mock.web.MockHttpSession) authorizeResult.getRequest().getSession())
                        .param("email", "user@test.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String location = loginResult.getResponse().getRedirectedUrl();
        assertThat(location).startsWith("http://localhost:3000/callback");
        assertThat(location).contains("code=");
        assertThat(location).contains("state=xyz");

        // Extract code
        String code = extractParam(location, "code");

        // Step 3: POST /oauth/token — exchange code
        MvcResult tokenResult = mockMvc.perform(post("/oauth/token")
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", "http://localhost:3000/callback")
                        .param("client_id", "test-app")
                        .param("client_secret", "test-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.refresh_token").exists())
                .andExpect(jsonPath("$.id_token").exists())
                .andReturn();

        Map<String, Object> tokenResponse = objectMapper.readValue(
                tokenResult.getResponse().getContentAsString(), Map.class);
        String accessToken = (String) tokenResponse.get("access_token");
        String refreshToken = (String) tokenResponse.get("refresh_token");

        // Step 4: GET /oauth/userinfo
        mockMvc.perform(get("/oauth/userinfo")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value("user@test.com"))
                .andExpect(jsonPath("$.email").value("user@test.com"));

        // Step 5: Refresh token
        mockMvc.perform(post("/oauth/token")
                        .param("grant_type", "refresh_token")
                        .param("refresh_token", refreshToken)
                        .param("client_id", "test-app")
                        .param("client_secret", "test-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists());

        // Step 6: Revoke token
        mockMvc.perform(post("/oauth/revoke")
                        .param("token", accessToken))
                .andExpect(status().isOk());

        // Step 7: Verify revoked token is rejected
        mockMvc.perform(get("/oauth/userinfo")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authCodeFlow_invalidClient_returnsError() throws Exception {
        mockMvc.perform(get("/oauth/authorize")
                        .param("client_id", "nonexistent")
                        .param("redirect_uri", "http://localhost:3000/callback")
                        .param("response_type", "code")
                        .param("scope", "openid"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("invalid_client")));
    }

    @Test
    void authCodeReuse_returnsError() throws Exception {
        // Get auth code
        MvcResult authorizeResult = mockMvc.perform(get("/oauth/authorize")
                        .param("client_id", "test-app")
                        .param("redirect_uri", "http://localhost:3000/callback")
                        .param("response_type", "code")
                        .param("scope", "openid")
                        .param("state", "abc"))
                .andReturn();

        MvcResult loginResult = mockMvc.perform(post("/oauth/authorize")
                        .session((org.springframework.mock.web.MockHttpSession) authorizeResult.getRequest().getSession())
                        .param("email", "user@test.com")
                        .param("password", "password123"))
                .andReturn();

        String code = extractParam(loginResult.getResponse().getRedirectedUrl(), "code");

        // First use — success
        mockMvc.perform(post("/oauth/token")
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", "http://localhost:3000/callback")
                        .param("client_id", "test-app")
                        .param("client_secret", "test-secret"))
                .andExpect(status().isOk());

        // Second use — fail
        mockMvc.perform(post("/oauth/token")
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", "http://localhost:3000/callback")
                        .param("client_id", "test-app")
                        .param("client_secret", "test-secret"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }

    private String extractParam(String url, String param) {
        String query = url.substring(url.indexOf('?') + 1);
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv[0].equals(param)) return kv[1];
        }
        throw new IllegalArgumentException("Param not found: " + param);
    }
}
```

- [ ] **Step 2: Run tests**

```bash
mvn test
```

Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/baskettecase/feauxauth/AuthFlowIntegrationTest.java
git commit -m "feat: add integration tests for full OAuth flow"
```

---

## Task 26: Full Build and Verify

- [ ] **Step 1: Install frontend dependencies and build**

```bash
cd frontend && npm install && cd ..
```

- [ ] **Step 2: Full Maven build**

```bash
mvn clean package
```

Expected: BUILD SUCCESS, `target/feauxauth-1.0.0.jar` exists

- [ ] **Step 3: Smoke test — start the app**

```bash
java -jar target/feauxauth-1.0.0.jar &
sleep 5
curl -s http://localhost:8080/.well-known/openid-configuration | python3 -m json.tool
kill %1
```

Expected: OIDC discovery JSON with all endpoints

- [ ] **Step 4: Commit any remaining changes**

```bash
git add -A && git status
# If there are changes:
git commit -m "chore: finalize build configuration"
```
