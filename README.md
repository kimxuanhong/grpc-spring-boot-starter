# grpc-spring-boot-starter

A lightweight, extensible gRPC framework for Spring Boot applications.

## Giới thiệu

**grpc-spring-boot-starter** là một framework giúp tích hợp gRPC vào các ứng dụng Spring Boot một cách dễ dàng, chuẩn hóa và mở rộng. Framework này cung cấp các thành phần core như:
- Đăng ký và quản lý gRPC server tự động.
- Inject gRPC client stub vào các bean qua annotation.
- Health check API chuẩn (liveness, readiness, terminate) cho microservice.
- Cấu hình tập trung, dễ dàng mở rộng cho nhiều microservice.

## Ưu điểm
- **Tích hợp Spring Boot chuẩn:** Sử dụng annotation, DI, cấu hình yml quen thuộc.
- **Tự động hóa:** Đăng ký service, inject client hoàn toàn tự động.
- **Health check chuẩn cloud-native:** Sẵn sàng cho Kubernetes, Docker, CI/CD.
- **Dễ mở rộng:** Chỉ cần kế thừa các class base để viết business logic riêng.
- **Logging chi tiết:** Sử dụng log4j, dễ vận hành và debug.
- **Tách biệt framework và app:** Dễ tái sử dụng cho nhiều dự án, nhiều team.

## Cài đặt

Thêm vào `pom.xml` của project Spring Boot:
```xml
<dependency>
    <groupId>com.xhk</groupId>
    <artifactId>grpc-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Cách sử dụng cơ bản

### 1. Định nghĩa service gRPC (proto)
Tạo file `.proto` định nghĩa API, ví dụ:
```proto
service Greeter {
  rpc SayHello (HelloRequest) returns (HelloReply);
}
```
Sinh code Java từ proto như thông thường.

### 2. Implement service
Tạo class kế thừa từ `GreeterGrpc.GreeterImplBase` và annotate `@GrpcController`:
```java
import com.xhk.grpc.spring.annotation.GrpcController;

@GrpcController
public class GreeterService extends GreeterGrpc.GreeterImplBase {
    // Implement các method
}
```

### 3. Inject gRPC client qua field (chuẩn)
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
- Mỗi field sẽ được inject một instance stub riêng biệt.
- **Lưu ý:** Các stub được generate từ gRPC Java luôn là `final`, không thể kế thừa. Không thể tạo subclass stub để làm bean.

### 4. Nếu muốn dùng chung stub như bean (singleton)
**Cách đúng:** Dùng `@GrpcClient` trên field trong class `@Configuration`, sau đó trả về field này trong method `@Bean`:
```java
import com.xhk.grpc.spring.annotation.GrpcClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcStubConfig {
    @GrpcClient(
        value = "greeter",
        stub = GreeterGrpc.GreeterBlockingStub.class,
        interceptors = {ClientLoggingInterceptor.class}
    )
    private GreeterGrpc.GreeterBlockingStub stub;

    @Bean
    public GreeterGrpc.GreeterBlockingStub greeterStub() {
        return stub;
    }
}
```
- Sau đó ở bất kỳ đâu:
```java
@Autowired
private GreeterGrpc.GreeterBlockingStub greeterStub;
```
- **Lưu ý:** Không thể dùng `@GrpcClient` trên tham số method `@Bean`.

## Sử dụng Interceptor cho Client & Server

### Cho client
- Truyền vào trường `interceptors` của `@GrpcClient` (field).
- Có thể là Spring bean hoặc class thường (phải có constructor mặc định).

### Cho server
- Truyền vào trường `interceptors` của `@GrpcController`.
- Có thể là Spring bean hoặc class thường.

## Ví dụ cấu hình annotation

```java
@GrpcController(interceptors = {ServerLoggingInterceptor.class})
public class MyGrpcService extends MyServiceGrpc.MyServiceImplBase {}

@GrpcClient(
    value = "myClient",
    stub = MyServiceGrpc.MyServiceBlockingStub.class,
    interceptors = {ClientLoggingInterceptor.class}
)
private MyServiceGrpc.MyServiceBlockingStub stub;
```

## Lưu ý
- Interceptor có thể là Spring bean (inject dependencies) hoặc class thường (phải có constructor mặc định).
- Stub gRPC Java luôn là final, không thể kế thừa. Không thể tạo subclass stub để làm bean.
- Nếu muốn dùng chung stub, hãy tạo bean thủ công như hướng dẫn ở trên.
- Không thể dùng `@GrpcClient` trên tham số method `@Bean`.

### 5. Health check
Kế thừa `HealthService` để custom logic health check nếu cần:
```java
public class MyHealthService extends HealthService {
    @Override
    protected boolean connectionCheck() {
        // Kiểm tra DB, cache, ...
        return true;
    }
}
```

### 6. Cấu hình
Cấu hình port, client/server trong `application.yml`:
```yaml
grpc:
  server:
    port: 9090
  clients:
    greeter:
      address: localhost
      port: 9090
```

## Đóng góp & phát triển
- Fork, PR, hoặc liên hệ tác giả để đóng góp thêm tính năng.
- Có thể publish lên Maven repo nội bộ để dùng chung cho nhiều team.

---

**grpc-spring-boot-starter** giúp bạn xây dựng microservice gRPC với Spring Boot nhanh chóng, chuẩn hóa, dễ mở rộng và vận hành! 