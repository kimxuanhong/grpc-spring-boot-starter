package com.xhk.grpc.spring.server;

import com.xhk.grpc.spring.annotation.GrpcController;
import com.xhk.grpc.spring.config.GrpcProperties;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GrpcServerRunner implements CommandLineRunner {

    private static final Logger logger = LogManager.getLogger(GrpcServerRunner.class);

    private final ApplicationContext context;
    private final GrpcProperties grpcProperties;
    private Server server;

    public GrpcServerRunner(ApplicationContext context, GrpcProperties grpcProperties) {
        this.context = context;
        this.grpcProperties = grpcProperties;
    }

    @Override
    public void run(String... args) throws Exception {
        int port = grpcProperties.getServer().getPort();
        NettyServerBuilder builder = NettyServerBuilder.forPort(port);

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

        if (registeredServices == 0) {
            logger.warn("No gRPC services found with @GrpcController annotation.");
        }

        server = builder.build().start();
        logger.info("gRPC server started on port {}", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (server != null) server.shutdown();
            logger.info("gRPC server shutdown hook triggered");
        }));

        server.awaitTermination();
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

    public void shutdown() {
        if (server != null) {
            server.shutdown();
            logger.warn("gRPC server is shutting down by external request");
        }
    }

    public boolean isServerRunning() {
        return server != null && !server.isShutdown();
    }

    public boolean isServerReady() {
        return server != null && !server.isShutdown() && !server.isTerminated();
    }
}
