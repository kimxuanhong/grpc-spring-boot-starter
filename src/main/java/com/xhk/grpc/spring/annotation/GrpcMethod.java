package com.xhk.grpc.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Annotation dùng để chỉ định tên method gRPC thực tế khi mapping interface client
// Nếu không khai báo, sẽ lấy tên method Java làm tên method gRPC
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GrpcMethod {
    // Tên method gRPC thực tế (nếu khác tên method Java)
    String value();
}
