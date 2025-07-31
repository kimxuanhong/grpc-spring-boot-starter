
package com.xhk.grpc.spring.annotation;

import io.grpc.ServerInterceptor;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface GrpcController {
    Class<? extends ServerInterceptor>[] interceptors() default {};
}
