# Agent Guidelines for webflux-reactive-hibernate

## Build, Test, and Coverage Commands

```bash
# Build
./gradlew build                 # Assemble and test
./gradlew clean                 # Delete build directory
./gradlew compileJava           # Compile main source only
./gradlew bootRun               # Run Spring Boot application

# Test Execution
./gradlew test                  # Run full test suite
./gradlew check                 # Run all checks (includes tests)
./gradlew test --tests OrderTest                          # Run single test class
./gradlew test --tests OrderTest#shouldCreateOrderWithItems    # Run specific test method
./gradlew test --tests "*OrderTest"                        # Run all test classes matching pattern

# Code Coverage
./gradlew jacocoTestReport      # Generate coverage report (XML + HTML)
./gradlew jacocoTestCoverageVerification  # Verify coverage metrics

# Architecture Tests
./gradlew test --tests ArchitectureTest  # Validate hexagonal architecture rules
```

## Hexagonal Architecture Rules

**STRICT LAYER DEPENDENCIES** (enforced by ArchUnit):
- Domain: **NO dependencies** on any other layer (pure business logic)
- Application: May **only** depend on Domain
- Infrastructure: May **only** depend on Domain and Application

**Package Rules**:
- Domain MUST NOT depend on: Spring Framework, R2dbc, Reactor, WebFlux, Infrastructure
- Application MUST NOT depend on Infrastructure (use ports/adapters pattern)
- All layers must be free of cycle dependencies

## Code Style Guidelines

### Package Structure
```
com.example.order.domain          - Domain layer (entities, value objects, events)
com.example.order.application    - Application layer (use cases, ports, handlers)
com.example.order.infrastructure - Infrastructure layer (web, persistence, messaging)
```

### Import Ordering (blank line between groups)
```java
// 1. Third-party imports (frameworks, libraries)
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.annotation.JsonProperty;

// 2. Internal imports (project packages)
import com.example.order.domain.order.entity.Order;
import com.example.order.application.port.out.DomainEventPublisher;

// 3. Java standard library
import java.util.List;
import java.util.UUID;
import java.time.Instant;
```

### Naming Conventions
- **Classes**: PascalCase (`Order`, `OrderItem`, `Money`)
- **Test Classes**: ClassName + `Test` suffix (`OrderTest`, `EmailTest`)
- **Methods**: camelCase, descriptive (`createOrder`, `applyDiscount`, `getCustomerEmail`)
- **Variables**: camelCase (`customerId`, `totalAmount`)
- **Constants**: UPPER_SNAKE_CASE (`EMAIL_PATTERN`, `MAX_QUANTITY`)
- **Error Codes**: `PREFIX_001`, `PREFIX_002` (e.g., `ORDER_001`)

### Type Usage Patterns

**Value Objects** (final class, immutable):
```java
public final class Email {
    private final String value;
    private Email(String value) { this.value = value; }
    public static Email of(String email) { /* validation */ }
    public String getValue() { return value; }
}
```

**Domain Entities** (rich domain model):
```java
public class Order {
    private final String id;
    private OrderStatus status;  // Mutations only via explicit methods
    public static Order create(...) { /* factory method */ }
    public void confirm() { /* business rule validation */ }
}
```

**Records** (DTOs, events, commands):
```java
public record OrderCreatedEvent(String orderId, String email, Instant createdAt) { }
public record CreateOrderCommand(String customerId, String email, List<OrderItem> items) { }
```

### Error Handling
```java
// Domain business rules - use BusinessException with error code
throw new BusinessException("ORDER_001", "Order must contain at least one item");

// Invalid input validation - use IllegalArgumentException
throw new IllegalArgumentException("Invalid email format: " + email);

// Null checks
Objects.requireNonNull(value, "Value cannot be null");
```

### Testing Patterns
```java
class OrderTest {
    @Test
    void shouldDoSomething() {  // Positive case
        // Arrange - Act - Assert
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void shouldThrowExceptionWhenInvalid() {  // Negative case
        assertThatThrownBy(() -> order.confirm())
            .isInstanceOf(BusinessException.class)
            .matches(ex -> ((BusinessException) ex).getErrorCode().equals("ORDER_004"));
    }
}
```

**Test Naming**: `should` + expected outcome + `when` + condition (optional)
- `shouldCreateOrderWithItems()`
- `shouldThrowExceptionWhenCreatingOrderWithoutItems()`
- `shouldApplyDiscountToPendingOrder()`

### Domain Rules
- **Immutability**: Use `final` for fields that never change after construction
- **Encapsulation**: Collections returned as unmodifiable (`Collections.unmodifiableList`)
- **Rich Objects**: Business logic belongs in domain entities, not services
- **Domain Events**: Use `registerEvent()` / `getDomainEvents()` / `clearDomainEvents()` pattern
- **Factory Methods**: Use static factory methods (`Order.create()`, `Email.of()`) instead of `new`

### Reactive Programming
- Use `Mono<T>` for single async operations
- Use `Flux<T>` for multiple async operations
- Use `flatMap()` for sequential async operations
- Use `.as(transactionalOperator::transactional)` for reactive transactions

### Gradle Configuration Notes
- Java 17 source compatibility
- JaCoCo coverage uses custom exclusions (config package, entities)
- Compiler args include `-parameters` for better reflection support