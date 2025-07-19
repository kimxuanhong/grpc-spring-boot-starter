# grpc-spring-boot-starter

Một framework nhẹ, mở rộng cho tích hợp gRPC vào Spring Boot.

## Giới thiệu

**grpc-spring-boot-starter** giúp tích hợp gRPC vào ứng dụng Spring Boot một cách tự động, chuẩn hóa, dễ mở rộng và vận hành. Framework cung cấp:
- Đăng ký và quản lý gRPC server tự động qua annotation.
- Inject gRPC client interface hoặc proxy qua annotation.
- Health check API chuẩn (liveness, readiness, terminate) cho microservice.
- Cấu hình tập trung, dễ mở rộng cho nhiều microservice.
- Hỗ trợ interceptor/middleware cho cả client và server.

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
service Greeter {
  rpc SayHello (HelloRequest) returns (HelloReply);
}
```
Sinh code Java từ proto như thông thường.

### 2. Cấu hình server

- Annotate class cấu hình chính với `@EnableGrpcServer` để bật auto scan và khởi động server:
```java
import com.xhk.grpc.spring.annotation.EnableGrpcServer;

@EnableGrpcServer
@SpringBootApplication
public class MyApp { ... }
```

- Cấu hình port server trong `application.yml`:
```yaml
grpc:
  server:
    port: 9090
```

### 3. Implement service

Tạo class kế thừa từ `GreeterGrpc.GreeterImplBase` và annotate `@GrpcController`:
```java
import com.xhk.grpc.spring.annotation.GrpcController;

@GrpcController(interceptors = {ServerLoggingInterceptor.class})
public class GreeterService extends GreeterGrpc.GreeterImplBase {
    // Implement các method
}
```
- Có thể truyền interceptor vào annotation để log, auth, v.v.

### 4. Khai báo cấu hình client (GrpcChannelConfig)

Bạn **không bắt buộc** phải tạo một bean GrpcChannelConfig riêng cho từng stub. Nhiều client (stub) có thể dùng chung một bean GrpcChannelConfig nếu cùng kết nối tới một service/endpoint. Chỉ cần đặt tên bean phù hợp và tham chiếu đúng tên đó trong các annotation client.

**Ví dụ:**
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
    // Các client khác có thể dùng chung bean này
}
```

```java
@GrpcClient(service = "userServiceChannelConfig", stub = UserServiceGrpc.UserServiceBlockingStub.class)
public interface UserApi { ... }

@GrpcClient(service = "userServiceChannelConfig", stub = AnotherGrpc.AnotherBlockingStub.class)
public interface AnotherApi { ... }
```

### 5. Sử dụng client

#### a) Interface client (proxy, mapping method)
- Định nghĩa interface với annotation `@GrpcClient`, chỉ định đúng tên service (trùng tên bean GrpcChannelConfig):
```java
import com.xhk.grpc.spring.annotation.GrpcClient;
import com.xhk.grpc.spring.annotation.GrpcMethod;

@GrpcClient(service = "userServiceChannelConfig", stub = UserServiceGrpc.UserServiceBlockingStub.class)
public interface UserApi {
    @GrpcMethod("getUser")
    UserReply getUser(UserRequest req);
}
```
- Kích hoạt auto scan interface client:
```java
import com.xhk.grpc.spring.annotation.EnableGrpcClients;

@EnableGrpcClients(basePackages = "com.example.grpc")
@SpringBootApplication
public class MyApp { ... }
```
- Inject interface client:
```java
@Autowired
private UserApi userApi;
```
- **Ưu điểm:** code clean, dễ mock/test, tách biệt logic.
- **Nhược điểm:** phải mapping method đúng tên, không tận dụng được hết overload stub gốc.

#### b) Sử dụng GrpcStubProxy (proxy kiểu functional, kiểm soát lỗi tốt)
```java
import com.xhk.grpc.spring.annotation.GrpcProxy;
import com.xhk.grpc.spring.injector.GrpcStubProxy;

public class MyService {
    @GrpcProxy(service = "userServiceChannelConfig")
    private GrpcStubProxy<UserServiceGrpc.UserServiceBlockingStub> userProxy;

    public String call() {
        return userProxy.call(stub -> stub.getUser(...));
    }
}
```
- **Ưu điểm:** kiểm soát retry, fallback, refresh channel, code rõ ràng.
- **Nhược điểm:** verbose hơn, phải truyền lambda.

#### c) Sử dụng trực tiếp GrpcStubManager (lấy stub thủ công)
```java
import com.xhk.grpc.spring.injector.GrpcStubManager;
import org.springframework.beans.factory.annotation.Autowired;

public class MyService {
    @Autowired
    private GrpcStubManager stubManager;

    public String call() {
        UserServiceGrpc.UserServiceBlockingStub stub =
            stubManager.getStub(UserServiceGrpc.UserServiceBlockingStub.class, "userServiceChannelConfig");
        return stub.getUser(...);
    }
}
```
- **Ưu điểm:** Chủ động, linh hoạt, không cần annotation, có thể dùng cho các trường hợp động/phức tạp.
- **Nhược điểm:** Không tự động inject, phải tự quản lý stub, không có middleware/proxy, code verbose hơn.

### 6. Health check

Kế thừa `HealthService` để custom logic health check:
```java
public class MyHealthService extends HealthService {
    @Override
    protected boolean connectionCheck() {
        // Kiểm tra DB, cache, ...
        return true;
    }
}
```
- Có sẵn 3 API: liveness, readiness, terminate.

### 7. Middleware/Interceptor

- Client: truyền vào `middlewares` của `@GrpcClient`.
- Server: truyền vào `interceptors` của `@GrpcController`.
- Có thể là Spring bean hoặc class thường (phải có constructor mặc định).

### 8. Tổng kết ưu nhược điểm các cách tạo client

| Cách tạo client             | Ưu điểm                                         | Nhược điểm                                 |
|----------------------------|-------------------------------------------------|--------------------------------------------|
| Interface proxy            | Code clean, dễ mock/test, tách biệt              | Phải mapping method, không overload stub   |
| GrpcStubProxy (proxy)      | Kiểm soát lỗi, retry, fallback tốt               | Verbose, phải truyền lambda                |
| GrpcStubManager (thủ công) | Chủ động, linh hoạt, không cần annotation        | Không tự inject, không middleware/proxy    |

## Đóng góp & phát triển

- Fork, PR, hoặc liên hệ tác giả để đóng góp thêm tính năng.
- Có thể publish lên Maven repo nội bộ để dùng chung cho nhiều team.

---

**grpc-spring-boot-starter** giúp bạn xây dựng microservice gRPC với Spring Boot nhanh chóng, chuẩn hóa, dễ mở rộng và vận hành! 