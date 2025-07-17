package com.xhk.grpc.spring.middleware;

public interface Middleware {
    <ReqT, RespT> void invoke(MiddlewareContext<ReqT, RespT> context);
} 