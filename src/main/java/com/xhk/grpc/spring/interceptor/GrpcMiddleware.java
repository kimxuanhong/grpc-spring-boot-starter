package com.xhk.grpc.spring.interceptor;

import io.grpc.Metadata;

public interface GrpcMiddleware {
    void beforeRequest(String methodName, Metadata headers, Object request);
    void afterResponse(String methodName, Metadata headers, Object response);
}