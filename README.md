# HOW TO RUN THE SERVICE

## Requirements

- Java 26 (JDK)
- No separate Gradle installation is required — the project includes the Gradle Wrapper.

## Run the application (verified on Windows 11)

- Clone the repository and run the Spring Boot application using the Gradle Wrapper.

- Go to the root of the application ('aggregator' directory)
- Execute commands described below to run via bootRun or via jar invocation
  - it might take a while to build it and run for the 1st time as all the dependencies must be fetched etc. But once you see the following log in the terminal _Started AggregatorApplication in..._ you are ready to go and send requests.

### Linux / macOS

#### Run the application with Gradle

```
./gradlew bootRun
```

#### Run the tests and build the executable JAR:

```
./gradlew clean build
```

The generated JAR will be available under: build/libs/

#### Run the packaged JAR

```
java -jar build/libs/aggregator-0.0.1-SNAPSHOT.jar
```

The service will be available at: http://localhost:8080

#### Send an example request

```
curl -u aggregator:aggregator123 http://localhost:8080/aggregator/aggregate/product-info/123?customerId=CUST-001
```

### Windows

#### Run the application with Gradle

```
.\gradlew.bat bootRun
```

#### Run the tests and build the executable JAR:

```
.\gradlew.bat clean build
```

The generated JAR will be available under: build\libs\

#### Run the packaged JAR

```
java -jar build\libs\aggregator-0.0.1-SNAPSHOT.jar
```

The service will be available at: http://localhost:8080

#### Send an example request

```
curl.exe -u aggregator:aggregator123 http://localhost:8080/aggregator/aggregate/product-info/123?customerId=CUST-001
```

# KEY DESIGN DECISIONS AND TRADE-OFFS

## Technology Stack

- Java 26
- Spring Boot 4.1.1
- Gradle

## Key Components/Solutions

### Providers

- Providers are intentionally not called "services" because, in the current implementation, they are local mock implementations responsible for providing simulated upstream data.

- Mock providers inherit from a common base class that provides functionality for simulating latency, failures and timeouts.

- All providers implement dedicated interfaces (e.g. `MockPricingProvider implements PricingProvider`).
  This creates an abstraction boundary between the aggregator and the actual provider implementation.

  If real upstream integrations are introduced later, new implementations can be created to handle real data fetching, HTTP communication, response mapping and error handling without changing the `AggregatorService`.

  The `AggregatorService` depends only on provider interfaces rather than concrete implementations. Spring profiles could then be used to instantiate and inject different implementations for different environments, for example mock implementations locally and real implementations in production.

### AggregatorService

- Responsible for gathering and combining data from multiple sources.

- The Catalog request is treated as required and is performed synchronously because the aggregation cannot continue without the product itself.

- Other requests are executed asynchronously using virtual threads, which is sufficient for the scope of this solution and provides efficient concurrency for the simulated I/O-bound operations.

- The aggregator is intentionally not concerned with the specific reason for an optional provider failure. Its primary concern is the resulting availability of the requested data.

- Responses are simplified and unified. If an optional upstream fails (everything except Catalog), the corresponding part of the response is returned as `null` rather than exposing different error messages or failure types to the client.

### Virtual Threads and CompletableFuture

- Virtual threads are used to execute independent optional upstream calls concurrently.

  The aggregation is I/O-bound rather than CPU-bound, so virtual threads are a good fit:
  they allow multiple potentially slow upstream operations to wait concurrently without
  requiring a large pool of platform threads.

- `CompletableFuture` is used to represent and compose the asynchronous operations.

  It allows the aggregator to start independent calls concurrently, wait for their results,
  and handle dependencies between calls where necessary (e.g. Pricing depends on Customer).

- Concurrent execution also reduces the overall request latency.

  With a fully synchronous implementation, the aggregation would have to wait for each
  upstream call sequentially, making the total latency approximately the sum of the
  individual calls. With independent calls executed concurrently, the total latency is
  closer to the duration of the slowest parallel operation rather than the sum of all
  operations.

- This approach was chosen over creating a dedicated thread pool for each provider because
  virtual threads are lightweight and keep the concurrency model relatively simple.

- For the current scope, the default virtual-thread executor is sufficient. In a production
  system with many concurrent requests, executor configuration and per-upstream concurrency
  limits would need to be evaluated and potentially made configurable.

### Exceptions

- Exceptions are intentionally implemented as unchecked (`RuntimeException`) exceptions.

  This applies both to infrastructure-related failures (e.g. upstream unavailable or interrupted)
  and domain-related failures (e.g. product or customer not found).

  The main reason is to avoid forcing checked exceptions through every layer of the application.
  Providers are responsible for detecting and translating low-level failures, while the
  `AggregatorService` decides how those failures affect the final response.

  Checked exceptions could be introduced if the application later requires callers to explicitly
  handle specific failure conditions or if exception handling becomes part of a strict API contract.
  For the current scope, unchecked exceptions keep the provider and aggregation code simpler
  and avoid unnecessary `throws` declarations and exception propagation.

## Trade-offs

### Returning `null` for unavailable optional data

- Returning `null` when optional data cannot be obtained is a deliberate interpretation of the requirements.

- `null` provides a simple representation of a missing or unavailable value. From the end client's perspective, the specific reason why pricing or availability is missing is not relevant to the current API contract. What matters is that the data is unavailable.

- This also simplifies frontend handling because the client only needs to handle one consistent state instead of interpreting different statuses and error messages.

- If more granular information becomes necessary, the response contract could be extended, for example:

```json
{
  "pricing": {
    "status": "UNAVAILABLE"
  }
}
```

This was intentionally not introduced because it would increase the API contract.

### Localization intentionally omitted

- Implementing full localization support (resource bundles, fallback logic, locale validation and testing) would add significant complexity without improving the core aggregator logic.

- Given the project guidance: _We value focused, well-reasoned solutions over feature-completeness._
  the implementation prioritizes concurrency, partial responses, pricing personalization and clean architecture.

- Localization can be added later without requiring changes to the core aggregation design.

### Dynamic warehouse selection intentionally omitted

- The aggregator currently returns warehouse information exactly as provided by the Availability Service.

- A more advanced design could dynamically select the optimal warehouse based on:
  - customer segment,
  - geographic proximity,
  - stock distribution,
  - delivery-time optimization

- Implementing such logic would require additional data structures, cross-service relationships and business rules.
- To maintain focus and avoid unnecessary complexity, dynamic warehouse selection was intentionally omitted.

### Security layer intentionally minimal

- The project uses simple Basic Authentication with in-memory credentials.
- This is sufficient for the scope of the assignment and keeps the security layer lightweight and easy to reason about.
- A more production-oriented setup could include:
  - credentials or secrets stored in a dedicated secret-management solution such as HashiCorp Vault,
  - secret rotation,
  - token-based authentication such as JWT or OAuth2,
  - environment-specific security configuration.

- Implementing these features would require additional infrastructure and integration work that goes beyond the base requirements.

- Therefore, the security layer was intentionally kept minimal while remaining easy to extend in the future.

### Configuration intentionally simplified

- The project currently uses a single application.yaml.
- In a real production system, configuration such as credentials, upstream URLs, timeouts and resilience policies should typically differ between environments:
  - local development,
  - CI/CD,
  - test/staging,
  - production

- For the sake of clarity and simplicity, the current implementation intentionally keeps the configuration in a single file. This makes the project easier to run and review while keeping the configuration straightforward.

## WHAT COULD BE DONE DIFFERENTLY WITH MORE TIME

### GO instead Java

Just heard that Go is also a great way to build microservices, with less memory footprint, requirements compared to Java/Spring Boot etc. My stack is Java/Python/ a bit Rust if we talk about Backend. I've had no opportunity to work in Go so far thus decided to use Java.

### DECLARATIVE PROVIDERS

- With a larger number of upstream services, I would move from explicitly managing each CompletableFuture inside aggregate() to a more declarative provider model. Each provider could declare what data it produces and which other data it depends on, while a generic aggregation engine would resolve these dependencies and execute independent providers in parallel. This would allow adding a new provider, such as RelatedProductsProvider, without modifying the aggregate() method. For the current solution this would be unnecessary complexity, but it would be a natural evolution if the number of upstreams grew significantly.

- One possible implementation would be to inject a List<AggregationProvider<?>> into the aggregator. Each provider would expose its key and declare any dependencies it requires (like Pricing requires Customer in one scenario), while a generic aggregation engine would execute providers with no dependencies in parallel and resolve dependent providers once their prerequisites are available. The results could be stored in an AggregationContext or result registry and passed to the final response mapper. This would make adding a new provider mostly a matter of implementing the interface and registering the bean, without changing the aggregation flow itself.

- For a significantly larger number of microservices, an integration framework such as Apache Camel could also be considered. Camel provides routing, parallel processing, error handling and aggregation patterns out of the box, which could reduce the amount of custom orchestration code in the microservices directly. This orchestration code would then be part of some higher level (and no, it's not easy, trivial if we have many microservices). For the current scope introducing such a framework would add unnecessary complexity, so a lightweight provider-based aggregation model is sufficient.

### Environment-specific configuration

- The application could use separate Spring profiles and configuration files, for example:
  - application-local.yaml
  - application-test.yaml
  - application-prod.yaml

Profiles could also determine which provider implementations are instantiated. For example, mock providers could be used locally while real provider implementations would be enabled in production.

### Production credentials and secrets

- Credentials and other sensitive configuration could be provided through environment variables or a dedicated secret-management solution such as HashiCorp Vault instead of being stored directly in application configuration.

### Externalized upstream configuration

- The current simulation keeps latency, timeout and failure assumptions in code.
- In a production implementation, these values should be externalized using Spring Boot configuration properties.
  This would allow each environment to configure different:
  - upstream URLs,
  - timeouts,
  - retry policies,
  - connection limits,
  - concurrency limits,
  - resilience settings

  These values could then be changed without modifying application code.

### Request/correlation ID propagation and distributed tracing

- In the current implementation, providers are local mock implementations, so there are no actual external service calls.
- In a production implementation, providers would communicate with external services. A request or correlation ID could then be propagated through the system to associate an incoming aggregator request with the corresponding upstream operations.
- Distributed tracing could provide a more complete view by correlating the incoming request with parallel calls to Catalog, Pricing, Availability and Customer services.
- This would make it easier to identify which upstream contributed to increased latency or caused a partial response.

### Logs/Observability

- Current solution has no real logging, for production-ready apps logs are a must in the key parts
- Micrometer/Prometheus to see some metrics related to our key flows, logic etc

### Caching

- Frequently requested and relatively stable data, such as catalog information, could be cached to reduce upstream traffic and improve latency.
- Different data types would require different TTLs. Pricing and availability would require more careful cache strategies because they are more dynamic, and pricing may depend on customer-specific information.

### Resilience and concurrency protection

- The current implementation handles upstream timeouts and failures but does not introduce production-oriented resilience mechanisms such as retries or per-upstream concurrency limits.
- Selective retries could be introduced for transient failures where retrying is safe and meaningful.
- Per-upstream concurrency limits could also be introduced if real external services have limited capacity. This would prevent one slow or overloaded upstream from consuming an excessive amount of application capacity.

### Integration testing with real dependencies

- The current provider implementations are local mocks, so Testcontainers would not provide significant additional value at this stage.
- Once real provider implementations are introduced, Testcontainers could be used to test integrations with real infrastructure such as containerized HTTP dependencies.

### Concurrency and load testing

- Additional tests could verify the behavior of the aggregator under load

### Test data (tests and mocks) and fixture refactoring

- Some hardcoded test data could be centralized and reused more consistently, for example through shared constants, enums or fixture builders. This would reduce duplication and make the test suite easier to maintain as the number of test scenarios grows. And yes, AI could help but free AI solutions are not as good as commercial ones and this task requires passing bigger context (to have consistency across many files/modules). Thus the tests were generated step by step without full necessary context so the final state is not ideal.

### Localization

- Localization could be implemented using the IETF BCP 47 standard together with Spring's message resolution facilities, locale fallback rules and dedicated tests.

### Dynamic warehouse selection

- The Availability integration could be extended with business logic for selecting an optimal warehouse based on customer location, stock distribution, expected delivery time and other business constraints.

# ANSWER TO THE DESIGN QUESTION

## DESIGN QUESTION

_Option A: The Assortment team wants to add a 'Related Products' service (200ms latency, 90% reliability)._
_How would your design accommodate this? Should it be required or optional?_

## ANSWER (see also the [Declarative Providers](#declarative-providers) section above):

I would add a RelatedProductsProvider interface and implementation and
execute it asynchronously alongside the other optional providers.

I would treat it as optional: failure or timeout would result in
relatedProducts = null, while the core product response would still be
returned. This fits the existing aggregation model and prevents a
relatively slow (200ms) and less reliable (90%) upstream from blocking
the whole request.
