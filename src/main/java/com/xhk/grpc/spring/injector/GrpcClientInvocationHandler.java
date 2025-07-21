package com.xhk.grpc.spring.injector;

import com.xhk.grpc.spring.annotation.GrpcMethod;
import com.xhk.grpc.spring.middleware.Context;
import com.xhk.grpc.spring.middleware.Handler;
import com.xhk.grpc.spring.middleware.Middleware;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

// Functional middleware proxy handler cho gRPC client
public class GrpcClientInvocationHandler implements InvocationHandler {
    private final Object stub;
    private final List<Middleware<Context<Object, Object>>> middlewares;

    public GrpcClientInvocationHandler(Object stub, List<Middleware<Context<Object, Object>>> middlewares) {
        this.stub = Objects.requireNonNull(stub, "Stub instance must not be null");
        this.middlewares = middlewares != null ? middlewares : List.of();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        validateMethodArgs(method, args);

        String grpcMethodName = resolveGrpcMethodName(method);
        Object request = args[0];

        // Tạo context
        Context<Object, Object> context = new Context<>(request, stub);

        // Tìm method gRPC thực tế (trước khi handler)
        Method grpcMethod = findGrpcMethod(stub.getClass(), grpcMethodName, request);

        // Final handler thực sự gọi gRPC stub
        Handler<Context<Object, Object>> finalHandler = ctx -> {
            Object modifiedRequest = Objects.requireNonNull(ctx.getRequest(), "Request is null");
            Object response = grpcMethod.invoke(ctx.getStub(), modifiedRequest);
            ctx.setResponse(response);
        };

        // Build chain từ middleware
        Handler<Context<Object, Object>> chain = buildChain(middlewares, finalHandler);

        // Chạy middleware chain
        chain.handle(context);

        // Lấy response từ context
        return context.getResponse();
    }


    private void validateMethodArgs(Method method, Object[] args) {
        if (args == null || args.length != 1) {
            throw new IllegalArgumentException("Method '" + method.getName() + "' must have exactly one argument");
        }
        if (args[0] == null) {
            throw new IllegalArgumentException("Request argument must not be null");
        }
    }

    private String resolveGrpcMethodName(Method method) {
        GrpcMethod annotation = method.getAnnotation(GrpcMethod.class);
        return annotation != null ? annotation.value() : method.getName();
    }

    private Method findGrpcMethod(Class<?> stubClass, String methodName, Object param) throws NoSuchMethodException {
        for (Method m : stubClass.getMethods()) {
            if (m.getName().equals(methodName)
                    && m.getParameterCount() == 1
                    && m.getParameterTypes()[0].isAssignableFrom(param.getClass())) {
                return m;
            }
        }
        throw new NoSuchMethodException("gRPC stub has no method named '" + methodName +
                "' accepting parameter of type: " + param.getClass().getName());
    }

    public static <T> Handler<T> buildChain(List<Middleware<T>> middlewares, Handler<T> finalHandler) {
        Handler<T> handler = finalHandler;
        for (int i = middlewares.size() - 1; i >= 0; i--) {
            handler = middlewares.get(i).apply(handler);
        }
        return handler;
    }

}
