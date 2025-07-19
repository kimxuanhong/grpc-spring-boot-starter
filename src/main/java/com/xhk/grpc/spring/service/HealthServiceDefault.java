package com.xhk.grpc.spring.service;

import com.xhk.grpc.spring.server.GrpcServerRunner;

// Implement mặc định cho HealthService, dùng khi không custom logic riêng
public class HealthServiceDefault extends HealthService implements HealthServiceType{

    public HealthServiceDefault(GrpcServerRunner grpcServerRunner) {
        super(grpcServerRunner);
    }
}