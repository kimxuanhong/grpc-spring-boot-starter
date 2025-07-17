
package com.xhk.grpc.spring.annotation;

import org.springframework.stereotype.Component;
import java.lang.annotation.*;
import io.grpc.ServerInterceptor;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface GrpcController {
    Class<? extends ServerInterceptor>[] interceptors() default {};
}
