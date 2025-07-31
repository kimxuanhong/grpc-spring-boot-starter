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

@Component
public class GrpcClientInjector implements BeanPostProcessor {
    private static final Logger logger = LogManager.getLogger(GrpcClientInjector.class);
    private final ApplicationContext ctx;

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
        String serviceName = annotation.service();

        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new GrpcClientInjectionException("Service name cannot be null or empty for field: " + field.getName());
        }

        // Validate that the ManagedChannel bean exists
        if (!ctx.containsBean(serviceName)) {
            throw new GrpcClientInjectionException(
                    "ManagedChannel bean with name '" + serviceName + "' not found. " +
                            "Available beans: " + String.join(", ", ctx.getBeanDefinitionNames()));
        }

        ManagedChannel channel = ctx.getBean(serviceName, ManagedChannel.class);

        // Validate channel state
        if (channel.isShutdown()) {
            logger.warn("ManagedChannel '{}' is shutdown", serviceName);
        }

        Object stub = GrpcStubCreator.create(field.getType(), channel);

        if (stub == null) {
            throw new GrpcClientInjectionException("Failed to create stub for type: " + field.getType().getName());
        }

        field.setAccessible(true);
        field.set(bean, stub);

        logger.debug("Successfully injected gRPC client for field {} with service {}", field.getName(), serviceName);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    // Custom exception for better error handling
    public static class GrpcClientInjectionException extends RuntimeException {
        public GrpcClientInjectionException(String message) {
            super(message);
        }

        public GrpcClientInjectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

