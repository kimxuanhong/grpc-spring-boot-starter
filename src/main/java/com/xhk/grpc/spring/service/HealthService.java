package com.xhk.grpc.spring.service;


import com.xhk.grpc.spring.proto.proto.HealthCheckRequest;
import com.xhk.grpc.spring.proto.proto.HealthCheckResponse;
import com.xhk.grpc.spring.proto.proto.HealthGrpc;
import com.xhk.grpc.spring.server.GrpcServerRunner;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Service cơ sở cho health check gRPC (liveness, readiness, terminate)
// Có thể kế thừa để custom logic kiểm tra sức khỏe microservice
public abstract class HealthService extends HealthGrpc.HealthImplBase implements HealthServiceType {
    protected final Logger logger = LogManager.getLogger(this.getClass());
    private final GrpcServerRunner grpcServerRunner;

    public HealthService(GrpcServerRunner grpcServerRunner) {
        this.grpcServerRunner = grpcServerRunner;
    }

    @Override
    public void liveness(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        logger.info("Received liveness check request");
        boolean isChecked = livenessCheck();
        HealthCheckResponse.ServingStatus status = grpcServerRunner.isServerRunning() && isChecked ?
                HealthCheckResponse.ServingStatus.SERVING :
                HealthCheckResponse.ServingStatus.NOT_SERVING;
        HealthCheckResponse response = HealthCheckResponse.newBuilder()
                .setStatus(status)
                .build();
        logger.info("Liveness check result: {} (customCheck={})", status, isChecked);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    protected boolean livenessCheck() {
        logger.debug("Default livenessCheck called, always returns true. Override for custom logic.");
        return true;
    }

    @Override
    public void readiness(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        logger.info("Received readiness check request");
        boolean dbReady = connectionCheck();
        HealthCheckResponse.ServingStatus status = grpcServerRunner.isServerReady() && dbReady ?
                HealthCheckResponse.ServingStatus.SERVING :
                HealthCheckResponse.ServingStatus.NOT_SERVING;
        HealthCheckResponse response = HealthCheckResponse.newBuilder()
                .setStatus(status)
                .build();
        logger.info("Readiness check result: {} (connectionCheck={})", status, dbReady);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    protected boolean connectionCheck() {
        logger.debug("Default connectionCheck called, always returns true. Override for DB/cache check.");
        return true;
    }

    @Override
    public void terminate(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        logger.warn("Received terminate request, shutting down server");
        HealthCheckResponse response = HealthCheckResponse.newBuilder()
                .setStatus(HealthCheckResponse.ServingStatus.TERMINATING)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        try {
            shutdownGraceful();
        } catch (Exception e) {
            logger.error("Error during graceful shutdown: {}", e.getMessage(), e);
        }
        grpcServerRunner.shutdown();
    }

    protected void shutdownGraceful() {
        logger.debug("Default shutdownGraceful called. Override to cleanup resources.");
    }
}