package com.xhk.grpc.spring.injector;

import com.xhk.grpc.spring.annotation.GrpcProxy;
import io.grpc.stub.AbstractStub;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;

@Component
public class GrpcProxyInjector implements BeanPostProcessor {

    private final GrpcStubManager stubManager;

    public GrpcProxyInjector(GrpcStubManager stubManager) {
        this.stubManager = stubManager;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        for (Field field : bean.getClass().getDeclaredFields()) {
            GrpcProxy annotation = field.getAnnotation(GrpcProxy.class);
            if (annotation == null) continue;

            if (!SafeGrpcCaller.class.isAssignableFrom(field.getType())) {
                throw new IllegalArgumentException("@GrpcProxy only supports SafeGrpcCaller<T>");
            }

            if (!(field.getGenericType() instanceof ParameterizedType type)) {
                throw new IllegalArgumentException("SafeGrpcCaller must be parameterized");
            }

            Class<?> stubClass = (Class<?>) type.getActualTypeArguments()[0];
            if (!AbstractStub.class.isAssignableFrom(stubClass)) {
                throw new IllegalArgumentException("Stub must extend AbstractStub");
            }

            SafeGrpcCaller<?> caller = new SafeGrpcCaller<>(
                    annotation.service(),
                    stubClass,
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

