package com.xhk.grpc.spring.injector;

import com.xhk.grpc.spring.annotation.GrpcProxy;
import io.grpc.stub.AbstractStub;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;

// BeanPostProcessor tự động inject GrpcStubProxy vào các field dùng @GrpcProxy
// Đảm bảo đúng kiểu, đúng generic, và inject đúng stub manager
@Component
@DependsOn("grpcStubManager")
public class GrpcProxyInjector implements BeanPostProcessor {

    // Quản lý lifecycle channel/stub
    private final GrpcStubManager stubManager;

    public GrpcProxyInjector(GrpcStubManager stubManager) {
        this.stubManager = stubManager;
    }

    /**
     * Trước khi khởi tạo bean, kiểm tra và inject GrpcStubProxy vào các field có @GrpcProxy
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        for (Field field : bean.getClass().getDeclaredFields()) {
            GrpcProxy annotation = field.getAnnotation(GrpcProxy.class);
            if (annotation == null) continue;

            // Kiểm tra đúng kiểu GrpcStubProxy
            if (!GrpcStubProxy.class.isAssignableFrom(field.getType())) {
                throw new IllegalArgumentException("@GrpcProxy only supports SafeGrpcCaller<T>");
            }

            // Kiểm tra generic đúng kiểu stub
            if (!(field.getGenericType() instanceof ParameterizedType type)) {
                throw new IllegalArgumentException("SafeGrpcCaller must be parameterized");
            }

            Class<?> stubClass = (Class<?>) type.getActualTypeArguments()[0];
            if (!AbstractStub.class.isAssignableFrom(stubClass)) {
                throw new IllegalArgumentException("Stub must extend AbstractStub");
            }

            @SuppressWarnings("unchecked")
            Class<? extends AbstractStub<?>> castedStubClass = (Class<? extends AbstractStub<?>>) stubClass;
            // Tạo GrpcStubProxy và inject vào field
            GrpcStubProxy<?> caller = new GrpcStubProxy<>(
                    annotation.service(),
                    castedStubClass,
                    stubManager
            );

            field.setAccessible(true);
            try {
                field.set(bean, caller);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to inject SafeGrpcCaller for " + field.getName(), e);
            }
        }

        return bean;
    }
}

