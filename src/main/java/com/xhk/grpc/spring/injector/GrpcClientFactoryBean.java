package com.xhk.grpc.spring.injector;

import com.xhk.grpc.spring.annotation.GrpcClient;
import com.xhk.grpc.spring.middleware.Middleware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Proxy;
import java.util.List;

public class GrpcClientFactoryBean<T> implements FactoryBean<T> {

    private final Class<T> grpcInterface;
    private final GrpcClient annotation;

    @Autowired
    private GrpcStubManager stubManager;

    @Autowired
    private ApplicationContext ctx;

    public GrpcClientFactoryBean(Class<T> grpcInterface, GrpcClient annotation) {
        this.grpcInterface = grpcInterface;
        this.annotation = annotation;
    }

    @Override
    public T getObject() {
        try {
            Object stub = stubManager.getStub(annotation.stub(), annotation.channelConfig());
            List<Middleware> middlewares = MiddlewareChainBuilder.build(annotation.middlewares(), ctx);
            return create(grpcInterface, stub, middlewares);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create gRPC stub for " + annotation.stub().getSimpleName(), e);
        }
    }


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
