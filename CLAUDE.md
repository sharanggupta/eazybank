# Claude Context: EazyBank Microservices

This document provides comprehensive context for Claude to understand the EazyBank codebase, its architecture, patterns, and conventions.

## Project Overview

EazyBank is a microservices-based banking application demonstrating production-grade patterns with Spring Boot and Kubernetes.

### Services

| Service | Port | Directory | Database | Purpose |
|---------|------|-----------|----------|---------|
| **Customer Gateway** | 8000 | `customer-gateway/` | None | API Gateway, orchestration, resilience |
| **Account** | 8080 | `account/` | accountdb | Customer account management |
| **Card** | 9000 | `card/` | carddb | Credit card management |
| **Loan** | 8090 | `loan/` | loandb | Loan management |

### Architecture

```
                    +-------------------+
    Clients ------> |  Customer Gateway |  (NodePort - external)
                    |     :8000         |
                    +--------+----------+
                             |
            +----------------+----------------+
            |                |                |
            v                v                v
     +----------+     +----------+     +----------+
     | Account  |     |   Card   |     |   Loan   |  (ClusterIP - internal)
     |  :8080   |     |  :9000   |     |  :8090   |
     +----+-----+     +----+-----+     +----+-----+
          |                |                |
          v                v                v
     [accountdb]      [carddb]         [loandb]    (PostgreSQL 17)
```

---

## Technology Stack

### Core

- **Java**: 21 (with virtual threads enabled)
- **Spring Boot**: 4.0.x
- **Spring Cloud**: 2025.1.0
- **Build**: Maven with Google Jib for containers

### Reactive Stack (All Services)

- **WebFlux**: Non-blocking web framework
- **R2DBC**: Reactive database driver (not JPA/JDBC)
- **Project Reactor**: Mono/Flux reactive streams
- **WebClient**: Reactive HTTP client

### Gateway-Specific

- **Spring Cloud Gateway**: WebFlux-based routing
- **Resilience4j**: Circuit breaker, rate limiting
- **AOP**: Write Gate pattern enforcement

### Testing

- **JUnit 5**: Test framework
- **Testcontainers**: PostgreSQL containers for integration tests
- **WebTestClient**: Reactive HTTP testing
- **WireMock**: External service mocking (gateway tests)

---

## Code Patterns & Principles

### Clean Code Principles (Uncle Bob)

**1. Single Responsibility**
- Each class has one reason to change
- Services handle business logic only
- Controllers handle HTTP concerns only
- Mappers handle transformations only

**2. Open/Closed**
- Interfaces for services allow extension
- Circuit breaker configs externalized
- Validation via annotations, not hardcoded

**3. Dependency Inversion**
- All dependencies injected via constructor
- Services depend on interfaces, not implementations
- Repository interfaces for data access

**4. Don't Repeat Yourself (DRY)**
- `BaseEntity` for audit fields
- `@ValidMobileNumber` custom annotation
- Reusable Helm chart for all services
- Shared workflow for CI/CD

**5. Keep It Simple (KISS)**
- Static mapper methods, no MapStruct
- Direct field validation, no complex rules
- Clear error messages in exceptions

### Naming Conventions

```
Controllers:     {Entity}Controller.java
Services:        {Entity}Service.java (interface)
                 {Entity}ServiceImpl.java (implementation)
Repositories:    {Entity}Repository.java
DTOs:            {Entity}Dto.java
Entities:        {Entity}.java
Mappers:         {Entity}Mapper.java
Exceptions:      {Condition}Exception.java
```

### Package Structure

```
dev.sharanggupta.{service}/
├── {Service}Application.java     # Main class with @SpringBootApplication
├── annotation/                   # Custom validation annotations
├── aspect/                       # AOP aspects (gateway only)
├── client/                       # HTTP clients (gateway only)
├── config/                       # Spring @Configuration classes
├── controller/                   # REST controllers
├── dto/                          # Data Transfer Objects
├── entity/                       # R2DBC entities
├── exception/                    # Custom exceptions + GlobalExceptionHandler
├── mapper/                       # Static mapper classes
├── repository/                   # ReactiveCrudRepository interfaces
└── service/                      # Business logic (interface + impl)
```

---

## DTO Patterns

### Immutable Records with Builders

```java
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomerProfile(
    String name,
    String email,
    String mobileNumber,
    AccountInfo account,
    CardInfo card,      // Optional - omitted if null
    LoanInfo loan       // Optional - omitted if null
) {}
```

### Request DTOs with Validation

```java
@Getter
public class CustomerDto {
    @NotEmpty(message = "Name is required")
    @Size(min = 5, max = 30, message = "Name must be 5-30 characters")
    private final String name;

    @NotEmpty @Email
    private final String email;

    @Pattern(regexp = "^\\d{10}$", message = "Mobile must be 10 digits")
    private final String mobileNumber;

    @JsonCreator
    @Builder(toBuilder = true)
    public CustomerDto(...) { ... }
}
```

### Custom Validation Annotation

```java
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Pattern(regexp = "^\\d{10}$", message = "Mobile number must be 10 digits")
@Constraint(validatedBy = {})
public @interface ValidMobileNumber {
    String message() default "Mobile number must be 10 digits";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

---

## Controller Patterns

### RESTful Endpoints (Account, Card, Loan)

```java
@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@AllArgsConstructor
public class AccountController {

    @PostMapping
    public Mono<ResponseEntity<ResponseDto>> createAccount(
            @Valid @RequestBody CustomerDto customerDto) {
        return accountService.createAccount(customerDto)
            .then(Mono.just(ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto("201", "Account created successfully"))));
    }

    @GetMapping("/{mobileNumber}")
    public Mono<ResponseEntity<CustomerDto>> fetchAccount(
            @PathVariable @ValidMobileNumber String mobileNumber) {
        return accountService.fetchAccount(mobileNumber)
            .map(ResponseEntity::ok);
    }

    @PutMapping
    public Mono<ResponseEntity<ResponseDto>> updateAccount(
            @Valid @RequestBody CustomerDto customerDto) {
        return accountService.updateAccount(customerDto)
            .then(Mono.just(ResponseEntity.ok(
                new ResponseDto("200", "Account updated successfully"))));
    }

    @DeleteMapping("/{mobileNumber}")
    public Mono<ResponseEntity<ResponseDto>> deleteAccount(
            @PathVariable @ValidMobileNumber String mobileNumber) {
        return accountService.deleteAccount(mobileNumber)
            .then(Mono.just(ResponseEntity.ok(
                new ResponseDto("200", "Account deleted successfully"))));
    }
}
```

### Gateway Controller with Write Protection

```java
@RestController
@RequestMapping("/api/customer")
@Validated
@AllArgsConstructor
public class CustomerController {

    @PostMapping("/onboard")
    @ProtectedWrite  // Blocked if any circuit breaker is open
    public Mono<ResponseEntity<ResponseDto>> onboardCustomer(
            @Valid @RequestBody CustomerAccount request) {
        return customerService.onboardCustomer(request)
            .then(Mono.just(ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto("201", "Customer onboarded successfully"))));
    }

    @GetMapping("/details")
    public Mono<ResponseEntity<CustomerDetails>> getCustomerDetails(
            @RequestParam @ValidMobileNumber String mobileNumber) {
        return customerService.getCustomerDetails(mobileNumber)
            .map(ResponseEntity::ok);
    }
}
```

---

## Service Patterns

### Reactive Service Implementation

```java
@Service
@AllArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    @Override
    public Mono<Void> createAccount(CustomerDto customerDto) {
        return validateCustomerDoesNotExist(customerDto.getMobileNumber())
            .then(Mono.defer(() -> {
                Customer customer = CustomerMapper.mapToEntity(customerDto);
                return customerRepository.save(customer);
            }))
            .flatMap(savedCustomer -> {
                Account account = createNewAccount(savedCustomer);
                return accountRepository.save(account);
            })
            .then();
    }

    private Mono<Void> validateCustomerDoesNotExist(String mobileNumber) {
        return customerRepository.findByMobileNumber(mobileNumber)
            .flatMap(existing -> Mono.error(
                new CustomerAlreadyExistsException(
                    "Customer already exists with mobile: " + mobileNumber)))
            .then();
    }
}
```

### Key Reactive Operators

```java
// Chain operations
.then()                    // Ignore result, continue
.flatMap()                 // Async transformation
.map()                     // Sync transformation

// Error handling
.switchIfEmpty()           // Fallback for empty
.onErrorResume()           // Recover from error

// Composition
Mono.zip(a, b, c)          // Parallel execution
Mono.defer(() -> ...)      // Lazy evaluation

// Terminal
.subscribe()               // Start the pipeline
.block()                   // Block (avoid in reactive code)
```

---

## Gateway Resilience Patterns

### Write Gate Pattern

Prevents partial writes when any downstream service is degraded.

**Annotation:**
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtectedWrite {}
```

**Aspect:**
```java
@Aspect
@Component
@RequiredArgsConstructor
public class WriteGateAspect {

    private final WriteGate writeGate;

    @Around("@annotation(ProtectedWrite)")
    public Object enforceWriteGate(ProceedingJoinPoint joinPoint) throws Throwable {
        return writeGate.checkWriteAllowed()
            .then(Mono.defer(() -> {
                try {
                    return (Mono<?>) joinPoint.proceed();
                } catch (Throwable e) {
                    return Mono.error(e);
                }
            }));
    }
}
```

**Gate Implementation:**
```java
@Service
@AllArgsConstructor
public class WriteGateImpl implements WriteGate {

    private final CircuitBreakerRegistry registry;

    @Override
    public Mono<Void> checkWriteAllowed() {
        return Mono.defer(() -> {
            Optional<String> openCB = findOpenCircuitBreaker();
            if (openCB.isPresent()) {
                return Mono.error(new ServiceUnavailableException(
                    "Writes blocked: " + openCB.get() + " circuit breaker is open"));
            }
            return Mono.empty();
        });
    }

    private Optional<String> findOpenCircuitBreaker() {
        return registry.getAllCircuitBreakers().stream()
            .filter(cb -> cb.getState() == CircuitBreaker.State.OPEN
                       || cb.getState() == CircuitBreaker.State.HALF_OPEN
                       || cb.getState() == CircuitBreaker.State.FORCED_OPEN)
            .map(CircuitBreaker::getName)
            .findFirst();
    }
}
```

### Circuit Breaker Configuration

```yaml
# application.yaml (production defaults)
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        ignore-exceptions:
          - WebClientResponseException$BadRequest
          - WebClientResponseException$NotFound

# application-dev.yaml (aggressive for testing)
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 2
        minimum-number-of-calls: 1
        failure-rate-threshold: 100
        wait-duration-in-open-state: 5s
```

### Graceful Degradation

Card and Loan services have fallbacks - data omitted on failure:

```java
@CircuitBreaker(name = "card_service", fallbackMethod = "fallbackFetchCard")
public Mono<CardDto> fetchCard(String mobileNumber) {
    return cardClient.fetchCard(mobileNumber);
}

private Mono<CardDto> fallbackFetchCard(String mobileNumber, Throwable t) {
    log.warn("Card service unavailable for mobile: {}", mobileNumber);
    return Mono.empty();  // Results in null field, omitted from JSON
}
```

---

## Repository Patterns

### R2DBC Repository

```java
public interface CustomerRepository extends ReactiveCrudRepository<Customer, Long> {
    Mono<Customer> findByMobileNumber(String mobileNumber);
}

public interface AccountRepository extends ReactiveCrudRepository<Account, Long> {
    Mono<Account> findByCustomerId(Long customerId);
    Mono<Account> findByAccountNumber(String accountNumber);
    Mono<Void> deleteByCustomerId(Long customerId);
}
```

---

## Entity Patterns

### Base Entity (Audit Fields)

```java
@Getter @Setter @ToString
public abstract class BaseEntity {
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
```

### R2DBC Entity

```java
@Table("customer")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder(toBuilder = true)
public class Customer extends BaseEntity {
    @Id
    @Column("customer_id")
    private Long customerId;

    @Column("name")
    private String name;

    @Column("email")
    private String email;

    @Column("mobile_number")
    private String mobileNumber;
}
```

---

## Exception Handling

### Custom Exceptions

```java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName, String fieldName, String fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}

public class CustomerAlreadyExistsException extends RuntimeException {
    public CustomerAlreadyExistsException(String message) {
        super(message);
    }
}

public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}
```

### Global Exception Handler

```java
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponseDto>> handleResourceNotFound(
            ServerWebExchange exchange, ResourceNotFoundException ex) {
        return buildErrorResponse(exchange, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public Mono<ResponseEntity<ErrorResponseDto>> handleDuplicate(
            ServerWebExchange exchange, CustomerAlreadyExistsException ex) {
        return buildErrorResponse(exchange, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Map<String, String>>> handleValidation(
            WebExchangeBindException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid"
            ));
        return Mono.just(ResponseEntity.badRequest().body(errors));
    }

    private Mono<ResponseEntity<ErrorResponseDto>> buildErrorResponse(
            ServerWebExchange exchange, HttpStatus status, String message) {
        return Mono.just(ResponseEntity.status(status).body(
            new ErrorResponseDto(
                exchange.getRequest().getPath().value(),
                status.toString(),
                message,
                LocalDateTime.now()
            )));
    }
}
```

---

## Testing Patterns

### Test-Driven Development (TDD) Approach

1. **Write failing test first**
2. **Write minimal code to pass**
3. **Refactor while keeping tests green**

### Integration Test with Testcontainers

```java
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountEndToEndTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
        .withDatabaseName("testdb");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
            "r2dbc:postgresql://" + postgres.getHost() + ":" +
            postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Autowired
    WebTestClient client;

    @Test
    @DisplayName("Should create account successfully")
    void shouldCreateAccount() {
        CustomerDto request = CustomerDto.builder()
            .name("John Doe")
            .email("john@example.com")
            .mobileNumber("1234567890")
            .build();

        client.post()
            .uri("/api")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.statusCode").isEqualTo("201");
    }

    @Test
    @DisplayName("Should return 404 for non-existent account")
    void shouldReturn404ForNonExistent() {
        client.get()
            .uri("/api/9999999999")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Should reject duplicate mobile number")
    void shouldRejectDuplicate() {
        // First create succeeds
        createAccount("1111111111");

        // Second with same mobile fails
        CustomerDto duplicate = CustomerDto.builder()
            .name("Another")
            .email("another@example.com")
            .mobileNumber("1111111111")
            .build();

        client.post()
            .uri("/api")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(duplicate)
            .exchange()
            .expectStatus().isBadRequest();
    }
}
```

### Test Categories

1. **Happy Path**: Normal successful operations
2. **Validation Errors**: Invalid input handling
3. **Not Found**: Missing resource scenarios
4. **Duplicates**: Unique constraint violations
5. **Edge Cases**: Boundary conditions

---

## Configuration

### Application Properties

```yaml
server:
  port: 8080

spring:
  webflux:
    base-path: /account
  application:
    name: account
  r2dbc:
    url: ${SPRING_R2DBC_URL:r2dbc:postgresql://localhost:5432/accountdb}
    username: ${SPRING_R2DBC_USERNAME:postgres}
    password: ${SPRING_R2DBC_PASSWORD:postgres}
    pool:
      initial-size: 10
      max-size: 20
      max-idle-time: 30m
  sql:
    init:
      mode: always

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

### Type-Safe Configuration

```java
@ConfigurationProperties(prefix = "services")
public record ServiceProperties(
    String accountUrl,
    String cardUrl,
    String loanUrl
) {}
```

---

## Database Schema

### Account Service

```sql
CREATE TABLE customer (
    customer_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    mobile_number VARCHAR(20) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(20) NOT NULL,
    updated_at TIMESTAMP,
    updated_by VARCHAR(20)
);

CREATE TABLE account (
    account_id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL REFERENCES customer(customer_id) ON DELETE CASCADE,
    account_type VARCHAR(100) NOT NULL,
    branch_address VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(20) NOT NULL,
    updated_at TIMESTAMP,
    updated_by VARCHAR(20)
);
```

### Card Service

```sql
CREATE TABLE card (
    card_id BIGSERIAL PRIMARY KEY,
    card_number VARCHAR(16) UNIQUE NOT NULL,
    mobile_number VARCHAR(15) NOT NULL UNIQUE,
    card_type VARCHAR(100) NOT NULL,
    total_limit INT NOT NULL,
    amount_used INT NOT NULL,
    available_amount INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(20) NOT NULL,
    updated_at TIMESTAMP,
    updated_by VARCHAR(20)
);
```

### Loan Service

```sql
CREATE TABLE loan (
    loan_id BIGSERIAL PRIMARY KEY,
    loan_number VARCHAR(12) UNIQUE NOT NULL,
    mobile_number VARCHAR(15) NOT NULL UNIQUE,
    loan_type VARCHAR(100) NOT NULL,
    total_loan INT NOT NULL,
    amount_paid INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(20) NOT NULL,
    updated_at TIMESTAMP,
    updated_by VARCHAR(20)
);
```

---

## Key Design Decisions

### Why Reactive?

1. **Scalability**: Non-blocking I/O handles more concurrent connections
2. **Resource efficiency**: Fewer threads needed
3. **Composition**: Reactor operators for complex async flows
4. **Gateway fit**: Perfect for aggregating multiple service calls

### Why R2DBC over JPA?

1. **Consistency**: Full reactive stack end-to-end
2. **No blocking**: JPA uses JDBC which blocks threads
3. **Performance**: Better throughput under load

### Why Write Gate?

1. **Data integrity**: Prevent partial writes across services
2. **Fail fast**: Immediately reject when degraded
3. **User experience**: Clear error message vs cryptic failures

### Why Separate Databases?

1. **Service isolation**: Each service owns its data
2. **Independent scaling**: Scale databases separately
3. **Failure isolation**: One DB failure doesn't cascade

---

## Development Guidelines

### Before Making Changes

1. Read relevant service README
2. Run existing tests: `./mvnw test`
3. Understand the reactive patterns in use

### When Adding Features

1. Start with a failing test (TDD)
2. Follow existing patterns in the service
3. Use immutable DTOs with builders
4. Add validation annotations
5. Handle errors with custom exceptions

### When Fixing Bugs

1. Write a test that reproduces the bug
2. Fix the bug
3. Verify the test passes
4. Check no other tests broke

### Code Review Checklist

- [ ] Tests written and passing
- [ ] Follows existing patterns
- [ ] No hardcoded values (use config)
- [ ] Proper error handling
- [ ] DTOs are immutable
- [ ] Validation on inputs
- [ ] Logging at appropriate levels

---

## File Locations Quick Reference

| What | Where |
|------|-------|
| Main README | `README.md` |
| Documentation Index | `docs/README.md` |
| Gateway docs | `customer-gateway/README.md` |
| API examples | `deploy/dev/api-examples.md` |
| Resilience testing | `deploy/dev/resilience-testing.md` |
| Config reference | `docs/configuration-reference.md` |
| Docker Compose | `deploy/dev/docker-compose.yml` |
| Build script | `deploy/dev/build-images.sh` |
| Helm charts | `deploy/helm/` |
| CI/CD workflows | `.github/workflows/` |
| Versioning guide | `.github/VERSIONING.md` |

---

## Common Commands

```bash
# Build all images locally
cd deploy/dev && ./build-images.sh

# Start everything
docker compose up -d

# Run tests for a service
cd account && ./mvnw test

# Check circuit breaker status
curl http://localhost:8000/actuator/circuitbreakers

# View gateway logs
docker compose logs -f gateway

# Stop everything
docker compose down
```
