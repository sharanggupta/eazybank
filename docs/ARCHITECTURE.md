# Architecture & Code Patterns

Design patterns, code conventions, and architectural principles used in EazyBank.

---

## Core Principles

EazyBank follows **Uncle Bob's Clean Code principles**:

1. **Single Responsibility** - Each class has one reason to change
2. **Open/Closed** - Open for extension, closed for modification
3. **Dependency Inversion** - Depend on abstractions, not implementations
4. **Don't Repeat Yourself (DRY)** - Avoid code duplication
5. **Keep It Simple (KISS)** - Simple is better than clever

---

## Technology Stack

- **Java 25** - Latest LTS version with virtual threads enabled
- **Spring Boot 4.0.x** - Application framework
- **Spring WebFlux** - Non-blocking web framework (all services)
- **R2DBC** - Reactive database driver (NOT JPA/JDBC)
- **Project Reactor** - Mono/Flux reactive streams
- **Resilience4j** - Circuit breakers and rate limiting
- **PostgreSQL 17** - Relational database

---

## Project Structure

```
eazybank/
├── customer-gateway/         # API Gateway
│   ├── src/main/java/
│   │   └── .../gateway/
│   │       ├── controller/       # REST controllers
│   │       ├── service/          # Business logic (interface + impl)
│   │       ├── client/           # Downstream service clients
│   │       ├── dto/              # Data Transfer Objects
│   │       ├── exception/        # Exception handlers
│   │       ├── aspect/           # AOP aspects (Write Gate pattern)
│   │       └── config/           # Spring configuration
│   └── src/test/java/
│
├── account/                   # Account microservice
│   ├── src/main/java/
│   │   └── .../account/
│   │       ├── controller/
│   │       ├── service/
│   │       ├── dto/
│   │       ├── entity/           # R2DBC entities (NOT JPA)
│   │       ├── repository/       # Data access
│   │       └── mapper/           # DTO ↔ Entity mapping
│   └── src/test/java/
│
├── card/                      # Card microservice (same structure)
├── loan/                      # Loan microservice (same structure)
│
├── deploy/
│   ├── dev/                   # Docker Compose (local)
│   └── helm/                  # Kubernetes (staging/prod)
│
└── docs/                      # Documentation
```

---

## Code Patterns

### 1. Services (Business Logic)

**Pattern**: Interface + Implementation

```java
// Service interface
public interface AccountService {
    Mono<Void> createAccount(CustomerDto customerDto);
    Mono<CustomerDto> fetchAccount(String mobileNumber);
}

// Implementation
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {
    private final CustomerRepository customerRepository;

    @Override
    public Mono<Void> createAccount(CustomerDto customerDto) {
        return validateCustomerDoesNotExist(customerDto.getMobileNumber())
            .flatMap(__ -> saveToDB(customerDto))
            .then();
    }

    private Mono<Void> validateCustomerDoesNotExist(String mobileNumber) {
        return customerRepository.findByMobileNumber(mobileNumber)
            .flatMap(existing -> Mono.error(
                new CustomerAlreadyExistsException("Customer already exists")))
            .then();
    }
}
```

**Key Points**:
- Interface for testability and polymorphism
- Constructor injection for all dependencies
- Reactive return types (Mono/Flux, NOT blocking)

### 2. DTOs (Data Transfer Objects)

**Pattern**: Immutable records with builders

```java
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomerProfile(
    String name,
    String email,
    String mobileNumber,
    AccountInfo account,
    CardInfo card,           // Optional - omitted from JSON if null
    LoanInfo loan            // Optional - omitted from JSON if null
) {}
```

**Request DTOs with validation**:

```java
@Getter
public class CustomerDto {
    @NotEmpty(message = "Name is required")
    @Size(min = 5, max = 30, message = "Name must be 5-30 characters")
    private final String name;

    @NotEmpty @Email
    private final String email;

    @ValidMobileNumber  // Custom annotation
    private final String mobileNumber;

    @JsonCreator
    @Builder(toBuilder = true)
    public CustomerDto(...) { ... }
}
```

**Custom Validation Annotation**:

```java
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Pattern(regexp = "^\\d{10}$", message = "Mobile number must be 10 digits")
public @interface ValidMobileNumber {
    String message() default "Mobile number must be 10 digits";
}
```

**Key Points**:
- Immutable (records or final fields)
- Validation via annotations on request DTOs
- `@JsonInclude(NON_NULL)` to omit null fields from JSON
- Builders for easier construction in tests

### 3. Controllers (HTTP Entry Points)

**Pattern**: Reactive handlers with validation

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
                .body(new ResponseDto("201", "Account created"))));
    }

    @GetMapping("/{mobileNumber}")
    public Mono<ResponseEntity<CustomerDto>> fetchAccount(
            @PathVariable @ValidMobileNumber String mobileNumber) {
        return accountService.fetchAccount(mobileNumber)
            .map(ResponseEntity::ok);
    }
}
```

**Key Points**:
- Reactive return types (Mono/Flux)
- `@Valid` for input validation
- Consistent HTTP status codes (201 for creation, 200 for success)
- Error handling via GlobalExceptionHandler

### 4. Repositories (Data Access)

**Pattern**: R2DBC reactive repositories (NOT JPA)

```java
public interface CustomerRepository extends ReactiveCrudRepository<Customer, Long> {
    Mono<Customer> findByMobileNumber(String mobileNumber);
    Mono<Void> deleteByMobileNumber(String mobileNumber);
}
```

**Why R2DBC, not JPA?**
- Fully reactive (non-blocking I/O)
- No ORM overhead
- Better for microservices
- Simpler for Spring Boot 4.0

### 5. Entities (Database Models)

**Pattern**: R2DBC entities with audit fields

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

// Base entity with audit fields
@Getter @Setter
public abstract class BaseEntity {
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
```

**Key Points**:
- Use `@Table` and `@Column` annotations
- Inherit from `BaseEntity` for audit fields
- `@Id` for primary key
- Use records for simple value objects

### 6. Mappers (DTO ↔ Entity Conversion)

**Pattern**: Static mapper methods (NOT MapStruct)

```java
public class CustomerMapper {
    public static Customer mapToEntity(CustomerDto dto) {
        return Customer.builder()
            .name(dto.getName())
            .email(dto.getEmail())
            .mobileNumber(dto.getMobileNumber())
            .build();
    }

    public static CustomerDto mapToDto(Customer entity) {
        return CustomerDto.builder()
            .name(entity.getName())
            .email(entity.getEmail())
            .mobileNumber(entity.getMobileNumber())
            .build();
    }
}
```

**Why static methods, not MapStruct?**
- Explicit, readable mapping
- No annotation processing overhead
- Easy to debug
- Good for microservices with few entities

### 7. Exception Handling

**Pattern**: Custom exceptions + GlobalExceptionHandler

```java
// Custom exceptions
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, String field, String value) {
        super(String.format("%s not found with %s: '%s'", resource, field, value));
    }
}

// Global handler
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponseDto>> handleNotFound(
            ServerWebExchange exchange, ResourceNotFoundException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            new ErrorResponseDto(
                exchange.getRequest().getPath().value(),
                "404",
                ex.getMessage(),
                LocalDateTime.now()
            )));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Map<String, String>>> handleValidationError(
            WebExchangeBindException ex) {
        return Mono.just(ResponseEntity.badRequest().body(
            ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                    FieldError::getField,
                    fe -> fe.getDefaultMessage() != null ?
                        fe.getDefaultMessage() : "Invalid"
                ))
        ));
    }
}
```

**Key Points**:
- Specific exception types for different errors
- Centralized error handling via `@ControllerAdvice`
- Consistent error response format

---

## Reactive Programming Patterns

### Common Operators

```java
// Chain operations
.then()                    // Ignore result, continue
.flatMap()                 // Async transformation
.map()                     // Sync transformation

// Handle errors
.switchIfEmpty()           // Fallback for empty Mono
.onErrorResume()           // Recover from error with another Mono

// Composition
Mono.zip(a, b, c)          // Parallel execution, wait for all
Mono.first(a, b)           // Race, use first to complete

// Terminal
.subscribe()               // Start the pipeline
.block()                   // Block (avoid in reactive code!)
```

### Example: Service Implementation

```java
public Mono<CustomerDetails> getCustomerDetails(String mobileNumber) {
    return Mono.defer(() ->
        Mono.zip(
            customerRepository.findByMobileNumber(mobileNumber)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException(...))),
            cardService.fetchCard(mobileNumber)
                .onErrorResume(e -> Mono.empty()),  // Graceful degradation
            loanService.fetchLoan(mobileNumber)
                .onErrorResume(e -> Mono.empty())
        )
        .map(tuple -> new CustomerDetails(
            tuple.getT1(),
            tuple.getT2().orElse(null),
            tuple.getT3().orElse(null)
        ))
    );
}
```

---

## Gateway-Specific Patterns

### Write Gate Pattern

Prevents writes when any downstream service is unavailable.

**Annotation**:
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtectedWrite {}
```

**Usage**:
```java
@PostMapping("/api/customer/onboard")
@ProtectedWrite  // Checks if all services are healthy
public Mono<ResponseEntity<ResponseDto>> onboardCustomer(...) {
    // Only executes if WriteGate.checkWriteAllowed() passes
}
```

**Implementation**:
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

### Circuit Breaker Pattern

```java
@Service
@RequiredArgsConstructor
public class CardServiceClient {

    @CircuitBreaker(name = "card_service", fallbackMethod = "fallbackFetchCard")
    public Mono<CardDto> fetchCard(String mobileNumber) {
        return webClient.get()
            .uri("/api/" + mobileNumber)
            .retrieve()
            .bodyToMono(CardDto.class);
    }

    private Mono<CardDto> fallbackFetchCard(String mobileNumber, Throwable t) {
        log.warn("Card service unavailable for: {}", mobileNumber);
        return Mono.empty();  // Graceful degradation
    }
}
```

---

## Testing Patterns

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    @Test
    @DisplayName("Should create account successfully")
    void shouldCreateAccount() {
        CustomerDto dto = CustomerDto.builder()
            .name("John Doe")
            .email("john@example.com")
            .mobileNumber("1234567890")
            .build();

        when(customerRepository.findByMobileNumber(anyString()))
            .thenReturn(Mono.empty());
        when(customerRepository.save(any()))
            .thenReturn(Mono.just(CustomerMapper.mapToEntity(dto)));

        StepVerifier.create(accountService.createAccount(dto))
            .expectComplete()
            .verify();
    }
}
```

### Integration Tests with Testcontainers

```java
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountEndToEndTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
            "r2dbc:postgresql://" + postgres.getHost() + ":" +
            postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName());
    }

    @Autowired
    WebTestClient client;

    @Test
    void shouldCreateAccount() {
        client.post()
            .uri("/api")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(CustomerDto.builder()
                .name("John Doe")
                .email("john@example.com")
                .mobileNumber("1234567890")
                .build())
            .exchange()
            .expectStatus().isCreated();
    }
}
```

---

## Naming Conventions

```
Controllers:     {Entity}Controller.java
Services:        {Entity}Service.java (interface)
                 {Entity}ServiceImpl.java (implementation)
Repositories:    {Entity}Repository.java
DTOs:            {Entity}Dto.java
Entities:        {Entity}.java
Mappers:         {Entity}Mapper.java
Exceptions:      {Condition}Exception.java
Aspects:         {Feature}Aspect.java
```

---

## Design Decisions

### Why Reactive Stack?

✅ **Scalability**: Non-blocking I/O handles more concurrent requests
✅ **Efficiency**: Fewer threads needed
✅ **Composition**: Reactor operators for complex async flows
✅ **Gateway fit**: Perfect for aggregating multiple service calls

### Why R2DBC over JPA?

✅ **Fully reactive**: No blocking JDBC calls
✅ **Simpler**: No ORM complexity
✅ **Consistent**: Same reactive model throughout stack
✅ **Performant**: Less overhead than JPA

### Why Separate Databases?

✅ **Isolation**: Each service owns its data
✅ **Independence**: Scale databases separately
✅ **Resilience**: One DB failure doesn't cascade

### Why Write Gate Pattern?

✅ **Integrity**: Prevent partial writes across services
✅ **Clarity**: Fast failure when degraded
✅ **UX**: Clear error message, not cryptic failure

---

## Best Practices

1. **Always use builders** for creating complex objects
2. **Validate at boundaries** - incoming requests and external APIs only
3. **Return DTOs, not entities** - from controllers
4. **Use static mappers** - for explicit, debuggable conversions
5. **Keep services simple** - let repositories handle queries
6. **Test business logic** - unit tests with mocks
7. **Test integrations** - integration tests with Testcontainers
8. **Log at appropriate levels** - INFO for events, DEBUG for details
9. **Use reactive operators** - chain operations, avoid blocking
10. **Document patterns** - future developers will thank you

---

## Adding New Features

### Checklist

- [ ] Write failing test (TDD)
- [ ] Implement feature using existing patterns
- [ ] Add validation if handling user input
- [ ] Add error handling in GlobalExceptionHandler if needed
- [ ] Update API documentation
- [ ] Update configuration if needed
- [ ] All tests passing

### Example: Add New Endpoint

```java
// 1. Test first
@Test
void shouldFetchAccountByNumber() {
    // Given, When, Then
}

// 2. DTO
@Builder
public record AccountByNumberRequest(String accountNumber) {}

// 3. Service method
public Mono<AccountDto> fetchByAccountNumber(String accountNumber);

// 4. Implementation
public Mono<AccountDto> fetchByAccountNumber(String accountNumber) {
    return accountRepository.findByAccountNumber(accountNumber)
        .switchIfEmpty(Mono.error(
            new ResourceNotFoundException("Account", "number", accountNumber)))
        .map(AccountMapper::toDto);
}

// 5. Controller
@GetMapping("/by-number/{accountNumber}")
public Mono<ResponseEntity<AccountDto>> getByNumber(
        @PathVariable String accountNumber) {
    return accountService.fetchByAccountNumber(accountNumber)
        .map(ResponseEntity::ok);
}
```

---

## Code Review Checklist

- [ ] Tests written and passing
- [ ] Follows existing patterns
- [ ] No hardcoded values (use config)
- [ ] Proper error handling
- [ ] DTOs immutable
- [ ] Validation on inputs
- [ ] Logging at appropriate levels
- [ ] Reactive (no `.block()`)
- [ ] Comments only where non-obvious
- [ ] Javadoc for public APIs

---

## More Information

- **Getting Started**: [GETTING_STARTED.md](GETTING_STARTED.md)
- **API Guide**: [API_GUIDE.md](API_GUIDE.md)
- **Resilience Patterns**: [RESILIENCE.md](RESILIENCE.md)
- **Testing Guide**: See individual service READMEs
- **Configuration**: [CONFIGURATION.md](CONFIGURATION.md)
