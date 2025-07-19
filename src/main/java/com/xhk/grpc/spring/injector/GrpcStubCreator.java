package com.xhk.grpc.spring.injector;

import io.grpc.Channel;

import java.lang.reflect.Method;

// Utility tạo stub gRPC từ ManagedChannel, tự động chọn factory method phù hợp (BlockingStub, FutureStub...)
public class GrpcStubCreator {
    /**
     * Tạo instance stub từ class stub và channel
     * @param stubType class stub (BlockingStub, FutureStub...)
     * @param channel ManagedChannel
     * @return instance stub
     */
    public static Object create(Class<?> stubType, Channel channel) throws Exception {
        Method factory = getMethod(stubType);
        return factory.invoke(null, channel);
    }

    /**
     * Lấy method factory phù hợp để tạo stub
     */
    public static Method getMethod(Class<?> stubType) throws Exception {
        Class<?> outerClass = stubType.getEnclosingClass();
        String stubName = stubType.getSimpleName();

        Method factory;
        if (stubName.contains("BlockingStub")) {
            factory = outerClass.getMethod("newBlockingStub", Channel.class);
        } else if (stubName.contains("FutureStub")) {
            factory = outerClass.getMethod("newFutureStub", Channel.class);
        } else {
            factory = outerClass.getMethod("newStub", Channel.class);
        }

        return factory;
    }
}
