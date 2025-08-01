# grpc-spring-boot-starter

Một framework nhẹ, mở rộng cho tích hợp gRPC vào Spring Boot với các tính năng nâng cao.

## Giới thiệu

**grpc-spring-boot-starter** giúp tích hợp gRPC vào ứng dụng Spring Boot một cách tự động, chuẩn hóa, dễ mở rộng và vận hành. Framework cung cấp:

- ✅ Đăng ký và quản lý gRPC server tự động qua annotation
- ✅ Inject gRPC client interface hoặc proxy qua annotation
- ✅ Health check API chuẩn (liveness, readiness, terminate) cho microservice
- ✅ Cấu hình tập trung, dễ mở rộng cho nhiều microservice
- ✅ Hỗ trợ interceptor/middleware cho cả client và server
- ✅ Auto-configuration cho Spring Boot
- ✅ Validation và error handling nâng cao
- ✅ Performance metrics và logging chi tiết
- ✅ Graceful shutdown với timeout configurable

## Cài đặt

Thêm vào `pom.xml`:

```xml
<dependency>
    <groupId>com.xhk</groupId>
    <artifactId>grpc-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Hướng dẫn sử dụng

### 1. Định nghĩa service gRPC (proto)

Tạo file `.proto` định nghĩa API, ví dụ:

```proto
syntax = "proto3";

option java_multiple_files = true;
option java_package = "com.example.grpc";
option java_outer_classname = "GreeterProto";

service Greeter {
  rpc SayHello (HelloRequest) returns (HelloReply);
  rpc SayHelloStream (HelloRequest) returns (stream HelloReply);
}

message HelloRequest {
  string name = 1;
}

message HelloReply {
  string message = 1;
}
```

Sinh code Java từ proto như thông thường.

### 2. Cấu hình server

#### Auto-configuration (Khuyến nghị)
Framework sẽ tự động cấu hình khi detect `@EnableGrpcServer`:

```java
import com.xhk.grpc.spring.annotation.EnableGrpcServer;

@EnableGrpcServer
@SpringBootApplication
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}
```

#### Cấu hình thủ công
```java
@Configuration
public class GrpcConfig {
    
    @Bean
    public GrpcServerRunner grpcServerRunner(ApplicationContext context, GrpcProperties properties) {
        return new GrpcServerRunner(context, properties);
    }
}
```

#### Cấu hình trong application.yml:

```yaml
grpc:
  server:
    port: 9090
    enabled: true  # Có thể disable server
  clients:
    user-service:
      address: localhost
      port: 9091
      keepAliveTimeMinutes: 1
      keepAliveTimeoutSeconds: 30
      keepAliveWithoutCalls: true
      idleTimeoutMinutes: 5
      maxRetryAttempts: 5
      usePlaintext: true
      headers:
        api-key: "your-api-key"
        user-agent: "grpc-client"
```

### 3. Implement service

Tạo class kế thừa từ service stub và annotate `@GrpcController`:

```java
import com.xhk.grpc.spring.annotation.GrpcController;
import com.xhk.grpc.spring.interceptor.ServerLoggingInterceptor;

@GrpcController(interceptors = {ServerLoggingInterceptor.class})
public class GreeterService extends GreeterGrpc.GreeterImplBase {
    
    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        try {
            HelloReply reply = HelloReply.newBuilder()
                .setMessage("Hello " + request.getName())
                .build();
            
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Error processing request")
                .withCause(e)
                .asRuntimeException());
        }
    }
}
```

### 4. Khai báo cấu hình client (ManagedChannel)

```java
import com.xhk.grpc.spring.annotation.EnableGrpcClients;

@EnableGrpcClients
@SpringBootApplication
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}
```

```java
@Configuration
public class GrpcClientConfig {
    
    @Bean("userService")
    public ManagedChannel userServiceChannel(GrpcProperties properties) {
        GrpcProperties.Client clientConfig = properties.getClients().get("user-service");
        
        return ManagedChannelBuilder
            .forAddress(clientConfig.getAddress(), clientConfig.getPort())
            .keepAliveTime(clientConfig.getKeepAliveTimeMinutes(), TimeUnit.MINUTES)
            .keepAliveTimeout(clientConfig.getKeepAliveTimeoutSeconds(), TimeUnit.SECONDS)
            .keepAliveWithoutCalls(clientConfig.isKeepAliveWithoutCalls())
            .idleTimeout(clientConfig.getIdleTimeoutMinutes(), TimeUnit.MINUTES)
            .enableRetry()
            .maxRetryAttempts(clientConfig.getMaxRetryAttempts())
            .usePlaintext(clientConfig.isUsePlaintext())
            .intercept(new ClientLoggingInterceptor())
            .build();
    }
}
```

### 5. Sử dụng client

#### a) Direct stub injection
```java
@Service
public class UserService {
    
    @GrpcClient("userService")
    private UserServiceGrpc.UserServiceBlockingStub userStub;
    
    @GrpcClient("userService")
    private UserServiceGrpc.UserServiceFutureStub userFutureStub;
    
    public User getUser(String id) {
        GetUserRequest request = GetUserRequest.newBuilder()
            .setId(id)
            .build();
        
        GetUserResponse response = userStub.getUser(request);
        return convertToUser(response);
    }
    
    public CompletableFuture<User> getUserAsync(String id) {
        GetUserRequest request = GetUserRequest.newBuilder()
            .setId(id)
            .build();
        
        return userFutureStub.getUser(request)
            .toCompletableFuture()
            .thenApply(this::convertToUser);
    }
}
```

#### b) Interface-based client (Proxy pattern)
```java
public interface UserServiceClient {
    User getUser(String id);
    CompletableFuture<User> getUserAsync(String id);
}

@Service
public class UserServiceClientImpl implements UserServiceClient {
    
    @GrpcClient("userService")
    private UserServiceGrpc.UserServiceBlockingStub userStub;
    
    @GrpcClient("userService")
    private UserServiceGrpc.UserServiceFutureStub userFutureStub;
    
    @Override
    public User getUser(String id) {
        // Implementation
    }
    
    @Override
    public CompletableFuture<User> getUserAsync(String id) {
        // Implementation
    }
}
```

### 6. Health check

Kế thừa `HealthService` để custom logic health check:

```java
@Component
public class MyHealthService extends HealthService {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    protected boolean livenessCheck() {
        // Kiểm tra basic health
        return true;
    }
    
    @Override
    protected boolean connectionCheck() {
        // Kiểm tra DB, cache, external services
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            logger.error("Database health check failed", e);
            return false;
        }
    }
    
    @Override
    protected void shutdownGraceful() {
        // Cleanup resources
        logger.info("Performing graceful shutdown");
    }
}
```

### 7. Middleware/Interceptor

#### Server Interceptor
```java
@Component
public class AuthInterceptor implements ServerInterceptor {
    
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        
        String token = headers.get(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER));
        
        if (token == null || !isValidToken(token)) {
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid token"), headers);
            return new ServerCall.Listener<ReqT>() {};
        }
        
        return next.startCall(call, headers);
    }
    
    private boolean isValidToken(String token) {
        // Token validation logic
        return token.startsWith("Bearer ");
    }
}
```

#### Client Interceptor
```java
@Component
public class AuthClientInterceptor implements ClientInterceptor {
    
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions)) {
            
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), 
                           "Bearer " + getAuthToken());
                super.start(responseListener, headers);
            }
        };
    }
    
    private String getAuthToken() {
        // Get token from security context or configuration
        return "your-auth-token";
    }
}
```

### 8. Error Handling

Framework cung cấp các exception custom:

```java
try {
    userStub.getUser(request);
} catch (GrpcClientInjectionException e) {
    // Handle client injection errors
} catch (GrpcServerStartupException e) {
    // Handle server startup errors
} catch (StatusRuntimeException e) {
    // Handle gRPC call errors
    switch (e.getStatus().getCode()) {
        case UNAVAILABLE:
            // Service unavailable
            break;
        case DEADLINE_EXCEEDED:
            // Timeout
            break;
        default:
            // Other errors
    }
}
```

## Tính năng nâng cao

### Performance Monitoring
- Request ID tracking
- Response time logging
- Error rate monitoring
- Connection pool metrics

### Security
- TLS/SSL support
- Authentication interceptors
- Authorization headers
- Rate limiting

### Observability
- Structured logging
- Request/response correlation
- Health check endpoints
- Metrics collection

## Đóng góp & phát triển

- Fork, PR, hoặc liên hệ tác giả để đóng góp thêm tính năng
- Có thể publish lên Maven repo nội bộ để dùng chung cho nhiều team

---

**grpc-spring-boot-starter** giúp bạn xây dựng microservice gRPC với Spring Boot nhanh chóng, chuẩn hóa, dễ mở rộng và vận hành! 