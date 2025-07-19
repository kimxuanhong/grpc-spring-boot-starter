package com.xhk.grpc.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Annotation dùng để đánh dấu một field sẽ được inject GrpcStubProxy cho client gRPC
// Thuộc tính 'service' là tên bean cấu hình channel (GrpcChannelConfig)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GrpcProxy {
    // Tên bean cấu hình channel (GrpcChannelConfig) để lấy endpoint
    String service();
}
