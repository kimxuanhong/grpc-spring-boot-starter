package com.xhk.grpc.spring.injector;

import com.xhk.grpc.spring.config.GrpcChannel;
import io.grpc.ClientInterceptor;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class GrpcStubManager implements DisposableBean, ApplicationContextAware {

    private static final Logger logger = LogManager.getLogger(GrpcStubManager.class);

    private ApplicationContext context;

    private final Map<String, ManagedChannel> channelCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService monitorExecutor = Executors.newSingleThreadScheduledExecutor();

    public GrpcStubManager() {
        monitorExecutor.scheduleAtFixedRate(this::monitorChannels, 10, 10, TimeUnit.SECONDS);
        logger.info("GrpcStubManager initialized with background monitoring.");
    }

    @SuppressWarnings("unchecked")
    public <T> T getStub(Class<T> stubClass, String channelCfgBeanName) {
        GrpcChannel config = context.getBean(channelCfgBeanName, GrpcChannel.class);
        ManagedChannel channel = getOrCreateHealthyChannel(channelCfgBeanName, config);

        try {
            Object stub = StubCreator.create(stubClass, channel);
            return (T) stub;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create gRPC stub for " + stubClass.getSimpleName(), e);
        }
    }

    private ManagedChannel getOrCreateHealthyChannel(String beanName, GrpcChannel config) {
        ManagedChannel existing = channelCache.get(beanName);

        if (existing != null && !existing.isShutdown() && !existing.isTerminated()) {
            ConnectivityState state = existing.getState(false);
            if (state != ConnectivityState.SHUTDOWN && state != ConnectivityState.TRANSIENT_FAILURE) {
                return existing;
            }

            logger.warn("Channel for '{}' is in bad state ({}), recreating...", beanName, state);
            shutdownChannel(beanName, existing);
        }

        ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forTarget(config.getTarget());

        if (config.usePlaintext()) {
            builder.usePlaintext();
        }

        List<ClientInterceptor> interceptors = config.getInterceptors();
        if (interceptors != null && !interceptors.isEmpty()) {
            builder.intercept(interceptors.toArray(new ClientInterceptor[0]));
        }

        ManagedChannel newChannel = builder.build();
        channelCache.put(beanName, newChannel);
        logger.info("Created new ManagedChannel for '{}': {}", beanName, config.getTarget());
        return newChannel;
    }

    private void monitorChannels() {
        for (Map.Entry<String, ManagedChannel> entry : channelCache.entrySet()) {
            String name = entry.getKey();
            ManagedChannel channel = entry.getValue();

            if (channel.isShutdown() || channel.isTerminated()) {
                logger.info("Detected channel '{}' has been shut down → removing", name);
                channelCache.remove(name);
                continue;
            }

            ConnectivityState state = channel.getState(false);
            if (state == ConnectivityState.TRANSIENT_FAILURE || state == ConnectivityState.SHUTDOWN) {
                logger.warn("Channel '{}' in bad state ({}), shutting down and removing", name, state);
                shutdownChannel(name, channel);
            }
        }
    }

    private void shutdownChannel(String beanName, ManagedChannel channel) {
        try {
            if (!channel.isShutdown()) {
                channel.shutdownNow();
            }
        } catch (Exception ex) {
            logger.debug("Error shutting down channel '{}': {}", beanName, ex.getMessage());
        } finally {
            channelCache.remove(beanName);
            logger.info("Channel '{}' shutdown complete and removed from cache.", beanName);
        }
    }

    public void refreshChannel(String beanName) {
        ManagedChannel channel = channelCache.remove(beanName);
        if (channel != null && !channel.isShutdown()) {
            channel.shutdownNow();
            logger.info("Channel '{}' manually refreshed (shutdown + removed)", beanName);
        }
    }

    @Override
    public void destroy() {
        logger.info("GrpcStubManager shutting down...");

        monitorExecutor.shutdownNow();

        for (Map.Entry<String, ManagedChannel> entry : channelCache.entrySet()) {
            String name = entry.getKey();
            ManagedChannel channel = entry.getValue();

            try {
                if (!channel.isShutdown()) {
                    channel.shutdownNow();
                }
            } catch (Exception ex) {
                logger.debug("Error during shutdown of '{}': {}", name, ex.getMessage());
            }
        }

        channelCache.clear();
        logger.info("All channels shut down and cache cleared.");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.context = applicationContext;
    }
}
