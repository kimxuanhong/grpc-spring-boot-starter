package com.xhk.grpc.spring.interceptor;

import io.grpc.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Interceptor server gRPC để log thông tin request/response
// Dùng cho mục đích audit, debug, trace...
public class ServerLoggingInterceptor implements ServerInterceptor {
    private static final Logger logger = LogManager.getLogger(ServerLoggingInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        logger.info("gRPC IN: method={}, headers={}", call.getMethodDescriptor().getFullMethodName(), headers);

        // Wrap ServerCall để log response
        ServerCall<ReqT, RespT> loggingCall = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
            @Override
            public void sendMessage(RespT message) {
                logger.info("gRPC Response: {}", message);
                super.sendMessage(message);
            }
        };

        ServerCall.Listener<ReqT> listener = next.startCall(loggingCall, headers);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) {
            @Override
            public void onMessage(ReqT message) {
                logger.info("gRPC Request: {}", message);
                super.onMessage(message);
            }

            @Override
            public void onComplete() {
                logger.info("gRPC OUT: method={}", call.getMethodDescriptor().getFullMethodName());
                super.onComplete();
            }
        };
    }
} 