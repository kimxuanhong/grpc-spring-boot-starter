package com.xhk.grpc.spring.injector;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SafeGrpcCaller<T> {

    private final String service;
    private final Class<T> stubClass;
    private final GrpcStubManager stubManager;

    public SafeGrpcCaller(String service, Class<T> stubClass, GrpcStubManager stubManager) {
        this.service = service;
        this.stubClass = stubClass;
        this.stubManager = stubManager;
    }

    public <R> R call(Function<T, R> fn) {
        try {
            T stub = stubManager.getStub(stubClass, service);
            return fn.apply(stub);
        } catch (StatusRuntimeException ex) {
            if (ex.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                stubManager.refreshChannel(service);
                T stub = stubManager.getStub(stubClass, service);
                return fn.apply(stub);
            }
            throw ex;
        }
    }

    public void callVoid(Consumer<T> fn) {
        call(t -> {
            fn.accept(t);
            return null;
        });
    }

    public <R> R call(Function<T, R> fn, Supplier<R> fallback) {
        try {
            return call(fn);
        } catch (Exception ex) {
            return fallback.get();
        }
    }

    public void callVoid(Consumer<T> fn, Runnable fallback) {
        try {
            call(t -> {
                fn.accept(t);
                return null;
            });
        } catch (Exception ex) {
            fallback.run();
        }
    }
}


