package com.xhk.grpc.spring.injector;



import com.xhk.grpc.spring.annotation.GrpcMethod;
import com.xhk.grpc.spring.middleware.Middleware;
import com.xhk.grpc.spring.middleware.MiddlewareChain;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

// InvocationHandler cho proxy interface client gRPC
// Thực thi middleware chain, mapping method, gọi stub thực tế
public class GrpcClientInvocationHandler implements InvocationHandler {
    // Stub gốc (BlockingStub, FutureStub...)
    private final Object stub;
    // Danh sách middleware áp dụng cho client
    private final List<Middleware> middlewares;

    public GrpcClientInvocationHandler(Object stub, List<Middleware> middlewares) {
        this.stub = Objects.requireNonNull(stub, "Stub instance must not be null");
        this.middlewares = middlewares != null ? middlewares : List.of();
    }

    /**
     * Xử lý gọi method trên interface client:
     * - Thực thi middleware chain (trước request)
     * - Mapping tên method (GrpcMethod)
     * - Gọi method thực tế trên stub
     * - Truyền response lại cho middleware (nếu cần)
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        validateMethodArgs(method, args);

        String grpcMethodName = resolveGrpcMethodName(method);
        Object request = args[0];

        // Middleware chain: xử lý request trước khi gọi stub
        MiddlewareChain<Object, Object> chain = new MiddlewareChain<>(middlewares, request);
        chain.execute();
        Object modifiedRequest = Objects.requireNonNull(chain.getRequest(), "Request after middleware chain is null");

        // Gọi method thực tế trên stub
        Method grpcMethod = findGrpcMethod(stub.getClass(), grpcMethodName, modifiedRequest);
        Object response = grpcMethod.invoke(stub, modifiedRequest);
        chain.setResponse(response);
        return response;
    }

    // Kiểm tra tham số method hợp lệ
    private void validateMethodArgs(Method method, Object[] args) {
        if (args == null || args.length != 1) {
            throw new IllegalArgumentException("Method '" + method.getName() + "' must have exactly one argument");
        }
        if (args[0] == null) {
            throw new IllegalArgumentException("Request argument must not be null");
        }
    }

    // Lấy tên method gRPC thực tế (nếu có annotation GrpcMethod)
    private String resolveGrpcMethodName(Method method) {
        GrpcMethod annotation = method.getAnnotation(GrpcMethod.class);
        return annotation != null ? annotation.value() : method.getName();
    }

    // Tìm method thực tế trên stub
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
}