package com.xhk.grpc.spring.injector;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.AbstractStub;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

// Proxy cho stub gRPC, cho phép gọi hàm trên stub với logic retry, refresh channel khi gặp lỗi
// Được inject qua @GrpcProxy để sử dụng client gRPC dạng functional/lambda
public class GrpcStubProxy<T extends AbstractStub<?>> {
    // Tên bean cấu hình channel (GrpcChannelConfig)
    private String service;
    // Class stub gốc (BlockingStub, FutureStub...)
    private Class<T> stubClass;
    // Quản lý lifecycle channel/stub
    private GrpcStubManager stubManager;

    public GrpcStubProxy() {
    }

    // Khởi tạo proxy với thông tin channel, stub, manager
    public GrpcStubProxy(String service, Class<T> stubClass, GrpcStubManager stubManager) {
        this.service = service;
        this.stubClass = stubClass;
        this.stubManager = stubManager;
    }

    /**
     * Gọi hàm trên stub, tự động retry nếu gặp lỗi UNAVAILABLE (channel lỗi)
     *
     * @param fn lambda nhận stub và trả về kết quả
     * @return kết quả trả về từ stub
     */
    public <R> R call(Function<T, R> fn) {
        try {
            T stub = stubManager.getStub(stubClass, service);
            return fn.apply(stub);
        } catch (StatusRuntimeException ex) {
            if (ex.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                // Nếu channel lỗi, refresh và thử lại
                stubManager.refreshChannel(service);
                T stub = stubManager.getStub(stubClass, service);
                return fn.apply(stub);
            }
            throw ex;
        }
    }

    /**
     * Gọi hàm void trên stub
     */
    public void callVoid(Consumer<T> fn) {
        call(t -> {
            fn.accept(t);
            return null;
        });
    }

    /**
     * Gọi hàm trên stub, có fallback khi gặp lỗi
     */
    public <R> R call(Function<T, R> fn, Supplier<R> fallback) {
        try {
            return call(fn);
        } catch (Exception ex) {
            return fallback.get();
        }
    }

    /**
     * Gọi hàm void trên stub, có fallback khi gặp lỗi
     */
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



