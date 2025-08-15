package com.xhk.grpc.spring.injector;

import com.xhk.grpc.spring.config.GrpcProperties;
import com.xhk.grpc.spring.interceptor.ClientLoggingInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class GrpcClientAutoConfig implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private GrpcProperties properties;

    @Override
    public void setEnvironment(Environment environment) {
        Binder binder = Binder.get(environment);
        this.properties = binder.bind("grpc", GrpcProperties.class).get();
    }


    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        properties.getClients().forEach((name, clientConfig) -> {
            if (!registry.containsBeanDefinition(name)) {
                registry.registerBeanDefinition(name,
                        BeanDefinitionBuilder.genericBeanDefinition(ManagedChannel.class, () -> {
                            ManagedChannelBuilder<?> builder = ManagedChannelBuilder
                                    .forAddress(clientConfig.getAddress(), clientConfig.getPort())
                                    .keepAliveTime(clientConfig.getKeepAliveTimeMinutes(), TimeUnit.MINUTES)
                                    .keepAliveTimeout(clientConfig.getKeepAliveTimeoutSeconds(), TimeUnit.SECONDS)
                                    .keepAliveWithoutCalls(clientConfig.isKeepAliveWithoutCalls())
                                    .idleTimeout(clientConfig.getIdleTimeoutMinutes(), TimeUnit.MINUTES);

                            if (clientConfig.isEnableRetry()) {
                                builder.enableRetry().maxRetryAttempts(clientConfig.getMaxRetryAttempts());
                            }
                            if (clientConfig.isUsePlaintext()) {
                                builder.usePlaintext();
                            }
                            if (clientConfig.isEnableDebug()) {
                                builder.intercept(new ClientLoggingInterceptor());
                            }

                            return builder.build();
                        }).getBeanDefinition()
                );
            }
        });
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    }
}
