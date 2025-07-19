package com.xhk.grpc.spring.annotation;

import com.xhk.grpc.spring.injector.GrpcClientsRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Annotation để bật auto scan và inject các interface client gRPC
// Đánh dấu trên class Application, chỉ định package cần scan
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(GrpcClientsRegistrar.class)
public @interface EnableGrpcClients {
    // Danh sách package cần scan để tìm interface client
    String[] basePackages() default {};
}