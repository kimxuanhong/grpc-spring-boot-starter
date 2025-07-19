package com.xhk.grpc.spring.interceptor;

import io.grpc.*;

// Interceptor client gRPC để thêm/chỉnh sửa header cho mỗi request
// Dùng cho mục đích auth, trace, custom header...
public abstract class HeaderClientInterceptor implements ClientInterceptor {
    /**
     * Subclasses must implement this to modify the headers as needed.
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