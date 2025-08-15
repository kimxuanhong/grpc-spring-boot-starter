package com.xhk.grpc.spring.server;

import com.xhk.grpc.spring.annotation.GrpcController;
import com.xhk.grpc.spring.config.GrpcProperties;
import com.xhk.grpc.spring.service.HealthServiceDefault;
import com.xhk.grpc.spring.service.HealthServiceType;
import io.grpc.*;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class GrpcServerRunner implements SmartLifecycle {

    private static final Logger logger = LogManager.getLogger(GrpcServerRunner.class);
    private static final int GRACEFUL_SHUTDOWN_TIMEOUT_SECONDS = 30;

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
                } else {
                    logger.warn("Bean {} is annotated with @GrpcController but does not implement BindableService",
                            bean.getClass().getSimpleName());
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

            printRegisteredServices();

            // Giữ server sống bằng luồng riêng (non-daemon)
            Thread awaitThread = new Thread(() -> {
                try {
                    server.awaitTermination();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.info("gRPC server await thread interrupted");
                }
            }, "grpc-server-await-thread");
            awaitThread.setDaemon(false); // giữ JVM sống
            awaitThread.start();

        } catch (IOException e) {
            logger.error("Failed to start gRPC server on port {}", grpcProperties.getServer().getPort(), e);
            throw new GrpcServerStartupException("Failed to start gRPC server", e);
        } catch (Exception e) {
            logger.error("Unexpected error during gRPC server startup", e);
            throw new GrpcServerStartupException("Unexpected error during gRPC server startup", e);
        }
    }

    @Override
    public void stop() {
        if (server != null && !server.isShutdown()) {
            try {
                logger.info("Initiating graceful shutdown of gRPC server");
                server.shutdown();

                // Wait for graceful shutdown
                if (!server.awaitTermination(GRACEFUL_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    logger.warn("gRPC server did not terminate gracefully within {} seconds, forcing shutdown",
                            GRACEFUL_SHUTDOWN_TIMEOUT_SECONDS);
                    server.shutdownNow();

                    if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                        logger.error("gRPC server did not terminate");
                    }
                }
                logger.info("gRPC server shutdown completed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted during gRPC server shutdown", e);
                server.shutdownNow();
            }
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
                logger.debug("Resolved interceptor from Spring context: {}", classes[i].getSimpleName());
            } catch (Exception e) {
                try {
                    interceptors[i] = classes[i].getDeclaredConstructor().newInstance();
                    logger.info("Created interceptor by newInstance: {}", classes[i].getSimpleName());
                } catch (Exception ex) {
                    logger.error("Cannot instantiate interceptor: {}", classes[i], ex);
                    throw new GrpcServerStartupException("Cannot instantiate interceptor: " + classes[i], ex);
                }
            }
        }

        return interceptors;
    }

    // Custom exception for better error handling
    public static class GrpcServerStartupException extends RuntimeException {
        public GrpcServerStartupException(String message) {
            super(message);
        }

        public GrpcServerStartupException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public void printRegisteredServices() {
        if (server == null) {
            logger.warn("gRPC server is not started yet.");
            return;
        }

        logger.info("📋 Registered gRPC services and methods:");
        for (ServerServiceDefinition serviceDef : server.getServices()) {
            String serviceName = serviceDef.getServiceDescriptor().getName();
            logger.info("  • {}", serviceName);

            serviceDef.getMethods().forEach(method -> {
                String methodName = method.getMethodDescriptor().getFullMethodName();
                logger.info("    └─ {}", methodName);
            });
        }
    }
}