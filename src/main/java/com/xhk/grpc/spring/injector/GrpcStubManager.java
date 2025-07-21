package com.xhk.grpc.spring.injector;

import com.xhk.grpc.spring.config.GrpcChannelConfig;
import io.grpc.ClientInterceptor;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Quản lý lifecycle của các gRPC stub và channel, đảm bảo channel luôn healthy, tự động set timeout cho stub nếu có cấu hình
@Component(value = "grpcStubManager")
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class GrpcStubManager implements DisposableBean, ApplicationContextAware {

    // Logger cho debug, log trạng thái channel
    private static final Logger logger = LogManager.getLogger(GrpcStubManager.class);

    // Spring context để lấy bean cấu hình channel
    private ApplicationContext context;

    // Cache các ManagedChannel theo tên cấu hình (beanName)
    private final Map<String, ManagedChannel> channelCache = new ConcurrentHashMap<>();
    // Executor để monitor trạng thái channel định kỳ
    private final ScheduledExecutorService monitorExecutor = Executors.newSingleThreadScheduledExecutor();

    // Khởi tạo, bắt đầu monitor channel
    public GrpcStubManager() {
        monitorExecutor.scheduleAtFixedRate(this::monitorChannels, 10, 10, TimeUnit.SECONDS);
        logger.info("GrpcStubManager initialized with background monitoring.");
    }

    /**
     * Lấy stub cho một service cụ thể, tự động set timeout nếu có cấu hình
     *
     * @param stubClass          class của stub (BlockingStub, FutureStub...)
     * @param channelCfgBeanName tên bean cấu hình channel
     * @return stub đã được set timeout nếu có
     */
    @SuppressWarnings("unchecked")
    public <T> T getStub(Class<T> stubClass, String channelCfgBeanName) {
        GrpcChannelConfig config = context.getBean(channelCfgBeanName, GrpcChannelConfig.class);
        ManagedChannel channel = getOrCreateHealthyChannel(channelCfgBeanName, config);

        try {
            Object stub = GrpcStubCreator.create(stubClass, channel);
            return (T) stub;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create gRPC stub for " + stubClass.getSimpleName(), e);
        }
    }

    /**
     * Lấy hoặc tạo mới ManagedChannel, đảm bảo channel luôn healthy
     */
    private ManagedChannel getOrCreateHealthyChannel(String beanName, GrpcChannelConfig config) {
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

    /**
     * Monitor trạng thái các channel, tự động shutdown và xóa channel lỗi
     */
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

    /**
     * Shutdown channel và xóa khỏi cache
     */
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

    /**
     * Thủ công refresh channel (shutdown và tạo lại ở lần gọi tiếp theo)
     */
    public void refreshChannel(String beanName) {
        ManagedChannel channel = channelCache.remove(beanName);
        if (channel != null && !channel.isShutdown()) {
            channel.shutdownNow();
            logger.info("Channel '{}' manually refreshed (shutdown + removed)", beanName);
        }
    }

    /**
     * Đóng toàn bộ channel khi shutdown ứng dụng
     */
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

    /**
     * Inject Spring context
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.context = applicationContext;
    }
}
