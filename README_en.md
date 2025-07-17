# grpc-spring-boot-starter

A lightweight, extensible gRPC framework for Spring Boot applications.

## Introduction

**grpc-spring-boot-starter** is a framework that helps you integrate gRPC into your Spring Boot applications easily, in a standardized and extensible way. It provides core components such as:
- Automatic registration and management of the gRPC server.
- Injecting gRPC client stubs into beans via annotation.
- Standard health check APIs (liveness, readiness, terminate) for microservices.
- Centralized configuration, easy to extend for multiple microservices.

## Advantages
- **Standard Spring Boot integration:** Uses familiar annotations, DI, and yml configuration.
- **Automation:** Service registration and client injection are fully automatic.
- **Cloud-native health check:** Ready for Kubernetes, Docker, CI/CD.
- **Extensible:** Just extend base classes to implement your own business logic.
- **Detailed logging:** Uses log4j for easy operation and debugging.
- **Separation of framework and app:** Easy to reuse across projects and teams.

## Installation

Add to your project's `pom.xml`:
```xml
<dependency>
    <groupId>com.xhk</groupId>
    <artifactId>grpc-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Basic Usage

### 1. Define gRPC service (proto)
Create a `.proto` file for your API, for example:
```proto
service Greeter {
  rpc SayHello (HelloRequest) returns (HelloReply);
}
```
Generate Java code from proto as usual.

### 2. Implement the service
Create a class extending `GreeterGrpc.GreeterImplBase` and annotate with `@GrpcController`:
```java
import com.xhk.grpc.spring.annotation.GrpcController;

@GrpcController
public class GreeterService extends GreeterGrpc.GreeterImplBase {
    // Implement your methods
}
```

### 3. Inject gRPC client

#### a) Inject directly into a field (flexible way)
```java
import com.xhk.grpc.spring.annotation.GrpcClient;

public class MyService {
    @GrpcClient(
        value = "greeter",
        stub = GreeterGrpc.GreeterBlockingStub.class,
        interceptors = {ClientLoggingInterceptor.class}
    )
    private GreeterGrpc.GreeterBlockingStub greeter;
}
```
- Each field will be injected with a separate stub instance.

#### b) Automatically create a bean for a stub subclass (recommended for large microservices)
```java
import com.xhk.grpc.spring.annotation.GrpcClient;
import io.grpc.Channel;

@GrpcClient(
    value = "greeter",
    stub = GreeterGrpc.GreeterBlockingStub.class,
    interceptors = {ClientLoggingInterceptor.class}
)
public class GreeterStubBean extends GreeterGrpc.GreeterBlockingStub {
    public GreeterStubBean(Channel channel) {
        super(channel);
    }
}
```
- This bean will be automatically registered in the Spring context.
- Anywhere you need it, just:
```java
@Autowired
private GreeterStubBean greeterStub;
```

## Using Interceptors for Client & Server

### For client
- Pass to the `interceptors` field of `@GrpcClient` (either on a field or a subclass).
- Can be a Spring bean or a regular class (must have a default constructor).

### For server
- Pass to the `interceptors` field of `@GrpcController`.
- Can be a Spring bean or a regular class.

## Example annotation configuration

```java
@GrpcController(interceptors = {ServerLoggingInterceptor.class})
public class MyGrpcService extends MyServiceGrpc.MyServiceImplBase {}

@GrpcClient(
    value = "myClient",
    stub = MyServiceGrpc.MyServiceBlockingStub.class,
    interceptors = {ClientLoggingInterceptor.class}
)
public class MyStubBean extends MyServiceGrpc.MyServiceBlockingStub {
    public MyStubBean(Channel channel) { super(channel); }
}
```

## Notes
- Interceptors can be Spring beans (with dependencies injected) or regular classes (must have a default constructor).
- If using a stub subclass, just inject it anywhere with `@Autowired`.

### 4. Health check
Extend `HealthService` to customize health check logic if needed:
```java
public class MyHealthService extends HealthService {
    @Override
    protected boolean connectionCheck() {
        // Check DB, cache, etc.
        return true;
    }
}
```

### 5. Configuration
Configure ports, clients, and server in `application.yml`:
```yaml
grpc:
  server:
    port: 9090
  clients:
    greeter:
      address: localhost
      port: 9090
```

## Contribution & Development
- Fork, PR, or contact the author to contribute features.
- Can be published to a private Maven repo for team-wide use.

---

**grpc-spring-boot-starter** helps you build gRPC microservices with Spring Boot quickly, in a standardized, extensible, and operationally friendly way! 