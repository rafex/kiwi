# System Design

## Overview
Kiwi is a lightweight HTTP backend with RSQL filtering capabilities, designed for resource-based APIs with complex query requirements. It provides a hexagonal architecture implementation with Jetty 12 and Java 25.

## Components

```mermaid
graph LR
    Client[HTTP Client] -->|REST/JSON| API[Jetty HTTP Server]
    API --> Router[ResourceHandler Router]
    Router --> Hello[HelloResource]
    Router --> Auth[AuthResource]
    Router --> Object[ObjectResource]
    Router --> Location[LocationResource]
    
    Hello --> HelloService[HelloService]
    Auth --> AuthService[AuthService]
    Object --> ObjectService[ObjectService]
    Location --> LocationService[LocationService]
    
    HelloService --> HelloRepo[HelloRepository]
    AuthService --> UserRepo[UserRepository]
    AuthService --> RoleRepo[RoleRepository]
    ObjectService --> ObjectRepo[ObjectRepository]
    LocationService --> LocationRepo[LocationRepository]
    
    HelloRepo --> DB[(PostgreSQL)]
    UserRepo --> DB
    RoleRepo --> DB
    ObjectRepo --> DB
    LocationRepo --> DB
    
    Config[Configuration] --> API
    Config --> Services
    Config --> Repositories
    
    QueryParser[RSQL Parser] --> ObjectRepo
    QuerySpec[QuerySpec Builder] --> ObjectRepo
```

## Key Flows

### Object Search with RSQL Filtering

```mermaid
sequenceDiagram
    participant C as Client
    participant J as Jetty
    participant RH as ResourceHandler
    participant OS as ObjectService
    participant OR as ObjectRepository
    participant QP as QuerySpec Builder
    participant DB as PostgreSQL

    C->>J: GET /object/search?q=status==active&tags=football&limit=10&sort=-createdAt
    J->>RH: Route to ObjectResource
    RH->>OS: search(QuerySpec)
    OS->>QP: buildQuerySpec(params)
    QP->>QP: Parse RSQL (status==active)
    QP->>QP: Merge query params (tags=football)
    QP->>QP: Apply pagination (limit=10, offset=0)
    QP->>QP: Apply sorting (-createdAt)
    QP->>OR: search(querySpec)
    OR->>OR: Generate safe SQL with PreparedStatements
    OR->>DB: Execute query
    DB-->>OR: ResultSet
    OR-->>OS: List<SearchItem>
    OS-->>RH: JSON response
    RH-->>J: HTTP 200 OK
    J-->>C: JSON array
```

### Authentication Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant A as AuthResource
    participant AS as AuthService
    participant UR as UserRepository
    participant PH as PasswordHasher
    participant DB as PostgreSQL

    C->>A: POST /auth/login {username, password}
    A->>AS: authenticate(username, password)
    AS->>UR: findByUsername(username)
    UR->>DB: SELECT * FROM users WHERE username = ?
    DB-->>UR: User record
    UR-->>AS: Optional<User>
    
    alt User found and active
        AS->>PH: verify(password, storedHash)
        PH-->>AS: boolean valid
        alt Password valid
            AS-->>A: AuthResult.ok(userId, username, roles)
            A-->>C: 200 OK + JWT token
        else Password invalid
            AS-->>A: AuthResult.bad("bad_credentials")
            A-->>C: 401 Unauthorized
        end
    else User not found or inactive
        AS-->>A: AuthResult.bad("bad_credentials")
        A-->>C: 401 Unauthorized
    end
```

### Database Migration Flow

```mermaid
sequenceDiagram
    participant M as Makefile
    participant F as Flyway
    participant DB as PostgreSQL

    M->>F: db-migrate target
    F->>DB: Connect with FLYWAY_URL
    F->>DB: Check schema_version table
    F->>DB: Apply V1__init_schema.sql
    F->>DB: Apply V2__api_functions.sql
    F->>DB: Apply V3__security_definer.sql
    F->>DB: Apply remaining migrations
    DB-->>F: Success
    F-->>M: Migration complete
```

## Scalability Notes
- **Stateless architecture**: All state in PostgreSQL, session tokens in JWTs
- **Connection pooling**: HikariCP for database connections
- **Query optimization**: RSQL parsing happens in-memory, SQL generation uses indexes
- **Container-ready**: Docker images with multi-stage builds
- **Helm charts**: Kubernetes deployment templates in `helm/kiwi-backend/`

## Security Model
- **Password hashing**: PBKDF2-HMAC-SHA256 with configurable iterations
- **JWT tokens**: Stateless authentication with configurable expiration
- **Field whitelisting**: All dynamic query fields must be explicitly allowed
- **PreparedStatements**: SQL injection protection
- **Input validation**: All parameters validated before processing
- **Role-based access**: PostgreSQL roles with least privilege