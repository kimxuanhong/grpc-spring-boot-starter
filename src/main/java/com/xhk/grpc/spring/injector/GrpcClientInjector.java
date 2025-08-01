package com.xhk.grpc.spring.injector;

import com.xhk.grpc.spring.annotation.GrpcClient;
import io.grpc.ManagedChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GrpcClientInjector implements BeanPostProcessor {
    private static final Logger logger = LogManager.getLogger(GrpcClientInjector.class);
    private final ApplicationContext ctx;
    private final Map<StubKey, Object> stubCache = new ConcurrentHashMap<>();

    public GrpcClientInjector(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();

        for (Field field : beanClass.getDeclaredFields()) {
            GrpcClient annotation = field.getAnnotation(GrpcClient.class);
            if (annotation != null) {
                try {
                    injectGrpcClient(bean, field, annotation);
                } catch (Exception e) {
                    logger.error("Failed to inject gRPC client for field {} in bean {}", field.getName(), beanName, e);
                    throw new GrpcClientInjectionException("Failed to inject gRPC client for field " + field.getName() + " in bean " + beanName, e);
                }
            }
        }
        return bean;
    }

    private void injectGrpcClient(Object bean, Field field, GrpcClient annotation) throws Exception {
        Class<?> stubType = field.getType();
        String serviceName = annotation.service();

        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new GrpcClientInjectionException("Service name cannot be null or empty for field: " + field.getName());
        }

        StubKey key = new StubKey(stubType, serviceName);

        // Nếu đã có trong cache thì dùng lại
        if (stubCache.containsKey(key)) {
            Object cachedStub = stubCache.get(key);
            field.setAccessible(true);
            field.set(bean, cachedStub);
            logger.debug("Reused cached gRPC client for field {} with type {} and service {}", field.getName(), stubType.getName(), serviceName);
            return;
        }

        if (!ctx.containsBean(serviceName)) {
            throw new GrpcClientInjectionException(
                    "ManagedChannel bean with name '" + serviceName + "' not found. " +
                            "Available beans: " + String.join(", ", ctx.getBeanDefinitionNames()));
        }

        ManagedChannel channel = ctx.getBean(serviceName, ManagedChannel.class);
        if (channel.isShutdown()) {
            logger.warn("ManagedChannel '{}' is shutdown", serviceName);
        }

        Object stub = GrpcStubCreator.create(stubType, channel);
        if (stub == null) {
            throw new GrpcClientInjectionException("Failed to create stub for type: " + stubType.getName());
        }

        // Cache stub
        stubCache.put(key, stub);

        field.setAccessible(true);
        field.set(bean, stub);
        logger.debug("Created and injected gRPC client for field {} with service {}", field.getName(), serviceName);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    public static class GrpcClientInjectionException extends RuntimeException {
        public GrpcClientInjectionException(String message) {
            super(message);
        }

        public GrpcClientInjectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // Helper key class for caching by (Stub class + Service name)
    private record StubKey(Class<?> stubType, String serviceName) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StubKey that)) return false;
            return stubType.equals(that.stubType) && serviceName.equals(that.serviceName);
        }
    }
}
