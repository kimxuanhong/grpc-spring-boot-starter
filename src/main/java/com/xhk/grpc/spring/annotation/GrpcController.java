
package com.xhk.grpc.spring.annotation;

import org.springframework.stereotype.Component;
import java.lang.annotation.*;
import io.grpc.ServerInterceptor;

// Annotation để đánh dấu một class là gRPC controller (implement service)
// Có thể truyền danh sách interceptor cho server qua thuộc tính 'interceptors'
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface GrpcController {
    // Danh sách interceptor áp dụng cho controller này
    Class<? extends ServerInterceptor>[] interceptors() default {};
}
