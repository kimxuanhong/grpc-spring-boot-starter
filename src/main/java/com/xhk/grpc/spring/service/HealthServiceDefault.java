package com.xhk.grpc.spring.service;

import com.xhk.grpc.spring.server.GrpcServerRunner;

public class HealthServiceDefault extends HealthService implements HealthServiceType{

    public HealthServiceDefault(GrpcServerRunner grpcServerRunner) {
        super(grpcServerRunner);
    }

    protected boolean livenessCheck() {
        logger.debug("Default livenessCheck called, always returns true. Override for custom logic.");
        return true;
    }

    protected boolean connectionCheck() {
        logger.debug("Default connectionCheck called, always returns true. Override for DB/cache check.");
        return true;
    }


    protected void shutdownGraceful() {
        logger.debug("Default shutdownGraceful called. Override to cleanup resources.");
    }
}