package com.xhk.grpc.spring.injector;


import com.xhk.grpc.spring.annotation.EnableGrpcClients;
import com.xhk.grpc.spring.annotation.GrpcClient;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.Map;

// ImportBeanDefinitionRegistrar để auto scan và đăng ký các interface client gRPC
// Được kích hoạt qua @EnableGrpcClients, tự động tìm và đăng ký proxy cho các interface có @GrpcClient
public class GrpcClientsRegistrar implements ImportBeanDefinitionRegistrar {

    /**
     * Đăng ký bean proxy cho các interface client gRPC trong các package chỉ định
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        // Lấy danh sách package cần scan
        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(EnableGrpcClients.class.getName());
        String[] basePackages = (String[]) attributes.get("basePackages");
        if (basePackages.length == 0) {
            String className = importingClassMetadata.getClassName();
            String basePackage = ClassUtils.getPackageName(className);
            basePackages = new String[]{basePackage};
        }

        // Tìm các interface có @GrpcClient
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isInterface();
            }
        };
        scanner.addIncludeFilter(new AnnotationTypeFilter(GrpcClient.class));

        for (String basePackage : basePackages) {
            for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
                try {
                    Class<?> clazz = Class.forName(candidate.getBeanClassName());
                    GrpcClient grpcClient = clazz.getAnnotation(GrpcClient.class);

                    // Đăng ký bean proxy cho interface client
                    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(GrpcClientFactoryBean.class);
                    builder.addConstructorArgValue(clazz);
                    builder.addConstructorArgValue(grpcClient);

                    registry.registerBeanDefinition(clazz.getSimpleName(), builder.getBeanDefinition());
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
