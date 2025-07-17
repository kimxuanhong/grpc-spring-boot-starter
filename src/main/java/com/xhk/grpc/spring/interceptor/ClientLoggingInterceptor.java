package com.xhk.grpc.spring.interceptor;

import io.grpc.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientLoggingInterceptor implements ClientInterceptor {
    private static final Logger logger = LogManager.getLogger(ClientLoggingInterceptor.class);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        logger.info("gRPC OUT: method={}", method.getFullMethodName());
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                logger.info("gRPC OUT: headers={}", headers);
                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                    @Override
                    public void onMessage(RespT message) {
                        logger.info("gRPC IN: response={}", message);
                        super.onMessage(message);
                    }
                }, headers);
            }

            @Override
            public void sendMessage(ReqT message) {
                logger.info("gRPC OUT: request={}", message);
                super.sendMessage(message);
            }
        };
    }
} 