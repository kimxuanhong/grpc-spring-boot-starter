package com.xhk.grpc.spring.injector;

import com.xhk.grpc.spring.annotation.GrpcClient;
import com.xhk.grpc.spring.middleware.Middleware;
import com.xhk.grpc.spring.middleware.MiddlewareChainBuilder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Proxy;
import java.util.List;

// FactoryBean tạo proxy cho interface client gRPC, inject stub và middleware
public class GrpcClientFactoryBean<T> implements FactoryBean<T> {

    // Interface client gRPC
    private final Class<T> grpcInterface;
    // Annotation cấu hình client
    private final GrpcClient annotation;

    @Autowired
    private GrpcStubManager stubManager;

    @Autowired
    private ApplicationContext ctx;

    public GrpcClientFactoryBean(Class<T> grpcInterface, GrpcClient annotation) {
        this.grpcInterface = grpcInterface;
        this.annotation = annotation;
    }

    /**
     * Tạo proxy cho interface client, inject stub và middleware chain
     */
    @Override
    public T getObject() {
        try {
            Object stub = stubManager.getStub(annotation.stub(), annotation.service());
            List<Middleware> middlewares = MiddlewareChainBuilder.build(annotation.middlewares(), ctx);
            return create(grpcInterface, stub, middlewares);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create gRPC stub for " + annotation.stub().getSimpleName(), e);
        }
    }

    /**
     * Tạo proxy instance với middleware chain
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> grpcInterface, Object stub, List<Middleware> middlewares) {
        return (T) Proxy.newProxyInstance(
                grpcInterface.getClassLoader(),
                new Class[]{grpcInterface},
                new GrpcClientInvocationHandler(stub, middlewares)
        );
    }

    @Override
    public Class<?> getObjectType() {
        return grpcInterface;
    }
}
