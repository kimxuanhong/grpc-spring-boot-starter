package com.xhk.grpc.spring.annotation;

import org.springframework.context.annotation.ComponentScan;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Annotation để bật auto scan và khởi động gRPC server trong Spring Boot
// Đánh dấu trên class cấu hình chính (Application) để enable server
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ComponentScan(basePackages = {"com.xhk.grpc.spring.server", "com.xhk.grpc.spring.config"})
public @interface EnableGrpcServer {
}

