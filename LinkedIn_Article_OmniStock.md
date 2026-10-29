# Building OmniStock: A Production-Grade Distributed Microservices Retail Platform with Spring Boot, PostgreSQL, and JWT Security

*By Vivek | Distributed Systems & Microservices Engineering*

---

### 🚀 The Problem: Fragmented Inventory and Order-to-Stock Disconnect

In modern retail and supply-chain logistics, fragmented inventory tracking is a pervasive challenge. Small-to-medium retail operations frequently suffer from:
1. **Disconnected stock records**: Warehouses managing stock in isolated spreadsheets or notebooks, resulting in stock mismatches.
2. **Delayed stockout visibility**: Inventory shortages identified only *after* an order fails rather than proactively through automated thresholds.
3. **The Order-to-Stock Disconnect**: Order placement decoupled from real-time stock deduction, creating serious risks of overselling and customer dissatisfaction.
4. **Monolithic vulnerability**: Single points of failure where a bug in one component brings down the entire business workflow.

To solve this, I designed and implemented **OmniStock**—a distributed, reactive, enterprise-grade retail microservices ecosystem built using **Spring Boot 3**, **Spring Cloud**, **PostgreSQL**, and **Spring Tool Suite (STS)**.

---

### 🏗️ Architectural Blueprint: The 6 Core Microservices

OmniStock strictly adopts the **Service-Oriented Architecture (SOA)** and **Database-per-Service** design patterns. The entire ecosystem consists of **6 independently deployable microservices**:

```
                              ┌──────────────────────┐
                              │   Client / Browser   │
                              └──────────┬───────────┘
                                         │
                                         ▼
                              ┌──────────────────────┐         ┌──────────────────────┐
                              │     API Gateway      │◄───────►│    Eureka Server     │
                              │      Port 8080       │         │      Port 8761       │
                              │  (JWT Filter Check)  │         │  (Service Registry)  │
                              └──────────┬───────────┘         └──────────────────────┘
                                         │
         ┌───────────────────────────────┼───────────────────────────────┐
         │                               │                               │
         ▼                               ▼                               ▼
┌──────────────────┐            ┌──────────────────┐            ┌──────────────────┐
│   Auth Service   │            │ Product Service  │            │Inventory Service │
│    Port 8084     │            │    Port 8081     │            │    Port 8082     │
│ (BCrypt + JWT)   │            │   (Catalog CRUD) │            │ (Stock Tracking) │
└────────┬─────────┘            └────────┬─────────┘            └────────┬─────────┘
         │                               │                               ▲
         ▼                               ▼                               │ Feign Call:
┌──────────────────┐            ┌──────────────────┐                     │ Deduct Stock
│    PostgreSQL    │            │    PostgreSQL    │                     │
│    (auth_db)     │            │   (product_db)   │            ┌────────┴─────────┐
└──────────────────┘            └──────────────────┘            │  Order Service   │
                                                                │    Port 8083     │
                                                                │(Order Lifecycle) │
                                                                └────────┬─────────┘
                                                                         │
                                                                         ▼
                                                                ┌──────────────────┐
                                                                │    PostgreSQL    │
                                                                │    (order_db)    │
                                                                └──────────────────┘
```

1. **Eureka Server (Port 8761)**: Dynamic service registry enabling automated heartbeat monitoring, client-side discovery, and seamless zero-configuration inter-service resolution.
2. **Spring Cloud API Gateway (Port 8080)**: High-throughput, non-blocking reactive gateway providing single-entry routing, request transformation, CORS management, and centralized security enforcement.
3. **Auth Service (Port 8084)**: Identity provider managing user registration, credential hashing with **BCrypt**, and cryptographically signed stateless **HMAC-SHA256 JWT** tokens.
4. **Product Service (Port 8081)**: Manages product catalog entities (name, SKU, unit price, category, stock metadata) exposing clean RESTful CRUD endpoints.
5. **Inventory Service (Port 8082)**: Real-time stock level tracker featuring automated threshold alarms to alert managers before inventory exhausts.
6. **Order Service (Port 8083)**: Manages customer order lifecycles, orchestrating synchronous verification and stock deductions via declarative OpenFeign communication.

---

### 🛡️ End-to-End Security: BCrypt Password Hashing & Gateway JWT Validation

A common vulnerability in distributed architectures is fragmented security logic across microservices. In OmniStock:

- **BCrypt Hashing**: The **Auth Service (`8084`)** hashes user credentials with salted BCrypt before persisting them to `auth_db`. Passwords are never stored in plaintext.
- **JWT Issuance**: Upon successful authentication, the Auth Service issues a signed JWT token containing user identity and expiration claims.
- **API Gateway JWT Filter**: An edge filter (`JwtAuthenticationFilter`) intercepts inbound HTTP requests. Public routes (such as `/auth/login`, `/auth/register`, and catalog browsing) bypass authentication. For protected mutations (such as product creation, updates, deletions, and order placement), the Gateway:
  1. Validates the `Authorization: Bearer <token>` header.
  2. Cryptographically validates the signature and expiration timestamp.
  3. Rejects invalid or expired tokens with `401 Unauthorized` before the request can ever reach downstream services.

---

### 🗄️ True Persistence: Dedicated PostgreSQL Databases (Zero H2)

While in-memory databases like H2 are convenient for prototyping, enterprise microservices demand high availability, strict ACID compliance, and data persistence across container restarts.

OmniStock completely eliminates H2 in favor of **PostgreSQL**, establishing dedicated, isolated databases for every service:

| Microservice | Port | Persistence Layer | Dedicated Database | Isolation Scope |
| :--- | :--- | :--- | :--- | :--- |
| **Auth Service** | `8084` | PostgreSQL | `auth_db` | User credentials, roles, salted password hashes |
| **Product Service** | `8081` | PostgreSQL | `product_db` | Product catalog, pricing, SKU mappings |
| **Inventory Service** | `8082` | PostgreSQL | `inventory_db` | Warehouse quantities, low-stock threshold rules |
| **Order Service** | `8083` | PostgreSQL | `order_db` | Order records, line items, fulfillment status |

**Why Database-Per-Service with PostgreSQL?**
- **Strict Bounded Contexts**: No service can execute cross-database joins or mutate another service’s schema.
- **Independent Scaling & Migration**: Database schemas evolve, migrate, and backup independently without cross-team dependencies.
- **Resilience**: A database failover or maintenance cycle in `order_db` does not compromise catalog availability in `product_db`.

---

### ⚡ Declarative Inter-Service Communication & Atomic Deductions

To prevent overselling:
- When a customer triggers `POST /orders`, the **Order Service** issues a synchronous **OpenFeign** REST call to the **Inventory Service**.
- The Inventory Service checks the real-time stock count in `inventory_db`.
- If stock is sufficient (`stock >= orderQuantity`), the Inventory Service decrements the quantity atomically and returns `CONFIRMED`.
- If stock is insufficient, the Inventory Service rejects the deduction, and the Order Service flags the transaction as `REJECTED - Insufficient stock`, safeguarding inventory consistency.

---

### 🧪 Verified CRUD Operations & Swagger UI Testing

All microservice endpoints were tested and verified using **Swagger UI** (`http://localhost:8081/swagger-ui/index.html`):

1. **POST `/products`** → `201 Created` with generated sequential ID.
2. **GET `/products/{id}`** → `200 OK` retrieving the persisted product from PostgreSQL `product_db`.
3. **GET `/products`** → `200 OK` listing catalog items.
4. **PUT `/products/{id}`** → `200 OK` updating product attributes.
5. **DELETE `/products/{id}`** → `204 No Content` / `200 OK` successfully removing the record from PostgreSQL.
6. Subsequent **GET `/products/{id}`** → `404 Not Found` confirming deletion integrity.

---

### 💡 Key Takeaways

1. **Centralized Security at the Gateway**: Decoupling authentication (Auth Service) from edge authorization (API Gateway) ensures zero code duplication across downstream business services.
2. **Production-Ready Persistence**: Moving from ephemeral H2 to production-grade PostgreSQL databases guarantees real transactional reliability.
3. **Observable Service Mesh**: Eureka service registration combined with Spring Cloud Gateway and LoadBalancer enables horizontal scalability with zero hardcoded URLs.

#SpringBoot #Microservices #Java #PostgreSQL #SpringCloud #JWT #SoftwareEngineering #CloudNative #SystemDesign #DistributedSystems
