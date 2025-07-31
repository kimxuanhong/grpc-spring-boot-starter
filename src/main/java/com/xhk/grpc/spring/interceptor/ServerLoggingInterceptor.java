package com.xhk.grpc.spring.interceptor;

import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicLong;

public class ServerLoggingInterceptor implements ServerInterceptor {
    private static final Logger logger = LogManager.getLogger(ServerLoggingInterceptor.class);
    private static final AtomicLong requestCounter = new AtomicLong(0);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        
        long requestId = requestCounter.incrementAndGet();
        String methodName = call.getMethodDescriptor().getFullMethodName();
        long startTime = System.currentTimeMillis();
        
        logger.info("gRPC IN [{}]: method={}, headers={}", requestId, methodName, headers);

        // Wrap ServerCall để log response và performance
        ServerCall<ReqT, RespT> loggingCall = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
            @Override
            public void sendMessage(RespT message) {
                logger.info("gRPC Response [{}]: {}", requestId, message);
                super.sendMessage(message);
            }

            @Override
            public void close(Status status, Metadata trailers) {
                long duration = System.currentTimeMillis() - startTime;
                if (status.isOk()) {
                    logger.info("gRPC OUT [{}]: method={}, duration={}ms", requestId, methodName, duration);
                } else {
                    logger.error("gRPC ERROR [{}]: method={}, status={}, duration={}ms", 
                               requestId, methodName, status, duration);
                }
                super.close(status, trailers);
            }
        };

        ServerCall.Listener<ReqT> listener = next.startCall(loggingCall, headers);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) {
            @Override
            public void onMessage(ReqT message) {
                logger.info("gRPC Request [{}]: {}", requestId, message);
                super.onMessage(message);
            }

            @Override
            public void onComplete() {
                logger.debug("gRPC Request completed [{}]: method={}", requestId, methodName);
                super.onComplete();
            }

            @Override
            public void onCancel() {
                logger.warn("gRPC Request cancelled [{}]: method={}", requestId, methodName);
                super.onCancel();
            }

            @Override
            public void onReady() {
                logger.debug("gRPC Request ready [{}]: method={}", requestId, methodName);
                super.onReady();
            }
        };
    }
} 