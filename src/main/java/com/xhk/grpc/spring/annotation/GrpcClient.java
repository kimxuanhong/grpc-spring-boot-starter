
package com.xhk.grpc.spring.annotation;

import com.xhk.grpc.spring.middleware.Middleware;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Annotation để đánh dấu interface client gRPC, cho phép auto inject proxy
// Thuộc tính 'service' là tên bean cấu hình channel
// Thuộc tính 'stub' là class stub gốc (BlockingStub, FutureStub...)
// Thuộc tính 'middlewares' là danh sách middleware áp dụng cho client này
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface GrpcClient {
    String service(); // Tên bean cấu hình channel
    Class<?> stub(); // Class stub gốc
    Class<? extends Middleware>[] middlewares() default {};
}