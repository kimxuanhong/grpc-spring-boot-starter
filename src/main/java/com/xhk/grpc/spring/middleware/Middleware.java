package com.xhk.grpc.spring.middleware;

// Interface cho middleware xử lý request/response gRPC client/server
// Implement interface này để custom logic (logging, auth, retry...)
@FunctionalInterface
public interface Middleware<CtxT> {
    Handler<CtxT> apply(Handler<CtxT> next);
}