package com.xhk.grpc.spring.interceptor;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;


public abstract class HeaderClientInterceptor implements ClientInterceptor {
    /**
     * Subclasses must implement this to modify the headers as needed.
     *
     * @param headers The gRPC metadata headers to modify.
     */
    protected abstract void applyHeaders(Metadata headers);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                applyHeaders(headers);
                super.start(responseListener, headers);
            }
        };
    }
} 