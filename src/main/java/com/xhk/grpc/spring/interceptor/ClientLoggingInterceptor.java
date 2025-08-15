package com.xhk.grpc.spring.interceptor;

import io.grpc.Metadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientLoggingInterceptor implements GrpcMiddleware {
    private static final Logger logger = LogManager.getLogger(ClientLoggingInterceptor.class);

    @Override
    public void beforeRequest(String methodName, Metadata headers, Object request) {
        logger.info("[gRPC] IN: method={}, headers={}, request={}", methodName, headers, request);
    }

    @Override
    public void afterResponse(String methodName, Metadata headers, Object response) {
        logger.info("[gRPC] OUT: method={}, headers={}, response={}", methodName, headers, response);
    }
}