package com.xhk.grpc.spring.config;


import com.xhk.grpc.spring.annotation.GrpcClient;
import com.xhk.grpc.spring.injector.GrpcClientFactoryBean;
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

public class GrpcClientsRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(EnableGrpcClients.class.getName());
        String[] basePackages = (String[]) attributes.get("basePackages");
        if (basePackages.length == 0) {
            String className = importingClassMetadata.getClassName();
            String basePackage = ClassUtils.getPackageName(className);
            basePackages = new String[]{basePackage};
        }

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
