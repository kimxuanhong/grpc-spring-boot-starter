
package com.xhk.grpc.spring.annotation;

import com.xhk.grpc.spring.middleware.Middleware;
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
public @interface GrpcClient {
    String channelConfig();
    Class<?> stub();
    Class<? extends Middleware>[] middlewares() default {};
}