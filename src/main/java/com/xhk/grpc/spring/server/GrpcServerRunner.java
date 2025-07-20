package com.xhk.grpc.spring.server;

import com.xhk.grpc.spring.annotation.GrpcController;
import com.xhk.grpc.spring.config.GrpcProperties;
import com.xhk.grpc.spring.service.HealthServiceDefault;
import com.xhk.grpc.spring.service.HealthServiceType;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

// Runner khởi động gRPC server trong Spring Boot, quản lý lifecycle server
// Tự động scan và đăng ký các controller/service gRPC
@Component
public class GrpcServerRunner implements SmartLifecycle {

    private static final Logger logger = LogManager.getLogger(GrpcServerRunner.class);

    private final ApplicationContext context;
    private final GrpcProperties grpcProperties;
    private Server server;
    private boolean running = false;

    public GrpcServerRunner(ApplicationContext context, GrpcProperties grpcProperties) {
        this.context = context;
        this.grpcProperties = grpcProperties;
    }

    @Override
    public void start() {
        try {
            int port = grpcProperties.getServer().getPort();
            NettyServerBuilder builder = NettyServerBuilder.forPort(port);

            // Scan các bean có @GrpcController
            Map<String, Object> grpcControllerBeans = context.getBeansWithAnnotation(GrpcController.class);
            int registeredServices = 0;

            for (Object bean : grpcControllerBeans.values()) {
                if (bean instanceof BindableService service) {
                    GrpcController annotation = bean.getClass().getAnnotation(GrpcController.class);
                    ServerInterceptor[] interceptors = resolveInterceptors(annotation.interceptors());

                    if (interceptors.length > 0) {
                        builder.addService(ServerInterceptors.intercept(service, interceptors));
                        logger.info("Registered gRPC service with interceptors: {} -> {}",
                                bean.getClass().getSimpleName(),
                                annotation.interceptors());
                    } else {
                        builder.addService(service);
                        logger.info("Registered gRPC service: {}", bean.getClass().getSimpleName());
                    }

                    registeredServices++;
                }
            }

            // Default health service nếu không có
            if (context.getBeansOfType(HealthServiceType.class).isEmpty()) {
                builder.addService(new HealthServiceDefault(this));
                logger.info("Registered default HealthService");
            }

            if (registeredServices == 0) {
                logger.warn("No gRPC services found with @GrpcController annotation.");
            }

            server = builder.build().start();
            running = true;

            logger.info("gRPC server started on port {}", port);

            // Giữ server sống bằng luồng riêng (non-daemon)
            Thread awaitThread = new Thread(() -> {
                try {
                    server.awaitTermination();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "grpc-server-await-thread");
            awaitThread.setDaemon(false); // giữ JVM sống
            awaitThread.start();

        } catch (IOException e) {
            throw new RuntimeException("Failed to start gRPC server", e);
        }
    }

    @Override
    public void stop() {
        if (server != null && !server.isShutdown()) {
            server.shutdown();
            logger.info("gRPC server is shutting down");
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE; // Start sau cùng, stop đầu tiên
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    public void shutdown() {
        this.stop();
    }

    public boolean isServerRunning() {
        return server != null && !server.isShutdown();
    }

    public boolean isServerReady() {
        return server != null && !server.isShutdown() && !server.isTerminated();
    }

    private ServerInterceptor[] resolveInterceptors(Class<? extends ServerInterceptor>[] classes) {
        ServerInterceptor[] interceptors = new ServerInterceptor[classes.length];

        for (int i = 0; i < classes.length; i++) {
            try {
                interceptors[i] = context.getBean(classes[i]);
            } catch (Exception e) {
                try {
                    interceptors[i] = classes[i].getDeclaredConstructor().newInstance();
                    logger.info("Created interceptor by newInstance: {}", classes[i].getSimpleName());
                } catch (Exception ex) {
                    throw new RuntimeException("Cannot instantiate interceptor: " + classes[i], ex);
                }
            }
        }

        return interceptors;
    }
}