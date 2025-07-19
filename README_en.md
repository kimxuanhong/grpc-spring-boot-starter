# grpc-spring-boot-starter

A lightweight, extensible gRPC framework for Spring Boot applications.

## Introduction

**grpc-spring-boot-starter** helps you integrate gRPC into your Spring Boot applications automatically, in a standardized and extensible way. The framework provides:
- Automatic registration and management of the gRPC server via annotation.
- Injecting gRPC client interfaces or proxies via annotation.
- Standard health check APIs (liveness, readiness, terminate) for microservices.
- Centralized configuration, easy to extend for multiple microservices.
- Support for interceptors/middleware for both client and server.

## Installation

Add to your project's `pom.xml`:
```xml
<dependency>
    <groupId>com.xhk</groupId>
    <artifactId>grpc-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Usage Guide

### 1. Define gRPC service (proto)
Create a `.proto` file for your API, for example:
```proto
service Greeter {
  rpc SayHello (HelloRequest) returns (HelloReply);
}
```
Generate Java code from proto as usual.

### 2. Server setup

- Annotate your main configuration class with `@EnableGrpcServer` to enable auto scan and start the server:
```java
import com.xhk.grpc.spring.annotation.EnableGrpcServer;

@EnableGrpcServer
@SpringBootApplication
public class MyApp { ... }
```

- Configure the server port in `application.yml`:
```yaml
grpc:
  server:
    port: 9090
```

### 3. Implement service

Create a class extending `GreeterGrpc.GreeterImplBase` and annotate with `@GrpcController`:
```java
import com.xhk.grpc.spring.annotation.GrpcController;

@GrpcController(interceptors = {ServerLoggingInterceptor.class})
public class GreeterService extends GreeterGrpc.GreeterImplBase {
    // Implement your methods
}
```
- You can pass interceptors to the annotation for logging, auth, etc.

### 4. Client configuration (GrpcChannelConfig)

You **do not need** to create a separate GrpcChannelConfig bean for each stub. Multiple clients (stubs) can share a single GrpcChannelConfig bean if they connect to the same service/endpoint. Just name the bean appropriately and reference that name in your client annotations.

**Example:**
```java
import com.xhk.grpc.spring.config.GrpcChannelConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {
    @Bean("userServiceChannelConfig")
    public GrpcChannelConfig userServiceConfig() {
        GrpcChannelConfig config = new GrpcChannelConfig();
        config.setTarget("localhost:9090");
        config.setPlaintext(true);
        config.setTimeoutSeconds(5L);
        return config;
    }
    // Other clients can share this bean
}
```

```java
@GrpcClient(service = "userServiceChannelConfig", stub = UserServiceGrpc.UserServiceBlockingStub.class)
public interface UserApi { ... }

@GrpcClient(service = "userServiceChannelConfig", stub = AnotherGrpc.AnotherBlockingStub.class)
public interface AnotherApi { ... }
```

### 5. Using the client

#### a) Interface client (proxy, method mapping)
- Define an interface with the `@GrpcClient` annotation, specifying the correct service name (matching the GrpcChannelConfig bean):
```java
import com.xhk.grpc.spring.annotation.GrpcClient;
import com.xhk.grpc.spring.annotation.GrpcMethod;

@GrpcClient(service = "userServiceChannelConfig", stub = UserServiceGrpc.UserServiceBlockingStub.class)
public interface UserApi {
    @GrpcMethod("getUser")
    UserReply getUser(UserRequest req);
}
```
- Enable auto scan for client interfaces:
```java
import com.xhk.grpc.spring.annotation.EnableGrpcClients;

@EnableGrpcClients(basePackages = "com.example.grpc")
@SpringBootApplication
public class MyApp { ... }
```
- Inject the client interface:
```java
@Autowired
private UserApi userApi;
```
- **Advantages:** Clean code, easy to mock/test, clear separation of logic.
- **Disadvantages:** Must map method names correctly, cannot use all stub overloads.

#### b) Using GrpcCaller (functional proxy, advanced error control)

```java
import com.xhk.grpc.spring.annotation.GrpcProxy;
import com.xhk.grpc.spring.injector.GrpcStubProxy;

public class MyService {
    @GrpcProxy(service = "userServiceChannelConfig")
    private GrpcStubProxy<UserServiceGrpc.UserServiceBlockingStub> userCaller;

    public String call() {
        return userCaller.call(stub -> stub.getUser(...));
    }
}
```
- **Advantages:** Fine-grained control over retry, fallback, channel refresh, clear code.
- **Disadvantages:** More verbose, must use lambda.

### 6. Health check

Extend `HealthService` to customize health check logic:
```java
public class MyHealthService extends HealthService {
    @Override
    protected boolean connectionCheck() {
        // Check DB, cache, ...
        return true;
    }
}
```
- Three APIs available: liveness, readiness, terminate.

### 7. Middleware/Interceptor

- Client: pass to the `middlewares` field of `@GrpcClient`.
- Server: pass to the `interceptors` field of `@GrpcController`.
- Can be a Spring bean or a regular class (must have a default constructor).

### 8. Comparison of client creation methods

| Client creation method  | Advantages                              | Disadvantages                             |
|------------------------|------------------------------------------|-------------------------------------------|
| Interface proxy        | Clean code, easy to mock/test, separation| Must map method, cannot use all overloads |
| GrpcCaller (proxy)     | Fine-grained error/retry/fallback control| Verbose, must use lambda                  |

## Contribution & Development

- Fork, PR, or contact the author to contribute features.
- Can be published to a private Maven repo for team-wide use.

---

**grpc-spring-boot-starter** helps you build gRPC microservices with Spring Boot quickly, in a standardized, extensible, and operationally friendly way! 