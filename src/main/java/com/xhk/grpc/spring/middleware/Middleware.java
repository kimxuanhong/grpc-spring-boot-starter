package com.xhk.grpc.spring.middleware;

// Interface cho middleware xử lý request/response gRPC client/server
// Implement interface này để custom logic (logging, auth, retry...)
public interface Middleware {
    // Hàm xử lý middleware, nhận context chứa request/response
    <ReqT, RespT> void invoke(MiddlewareContext<ReqT, RespT> context);
} 