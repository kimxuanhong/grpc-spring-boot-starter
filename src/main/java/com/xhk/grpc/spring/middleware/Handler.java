package com.xhk.grpc.spring.middleware;

@FunctionalInterface
public interface Handler<CtxT> {
    void handle(CtxT ctx) throws Exception;
}
