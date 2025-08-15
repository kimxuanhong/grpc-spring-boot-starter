package com.xhk.grpc.spring.interceptor;


import io.grpc.*;

public class GrpcUtils {
    public static ClientInterceptor convert(GrpcMiddleware middleware) {
        return new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

                return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
                    private Metadata requestHeaders;

                    @Override
                    public void start(Listener<RespT> responseListener, Metadata headers) {
                        this.requestHeaders = headers;
                        super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                            @Override
                            public void onMessage(RespT message) {
                                middleware.afterResponse(method.getFullMethodName(), requestHeaders, message);
                                super.onMessage(message);
                            }
                        }, headers);
                    }

                    @Override
                    public void sendMessage(ReqT message) {
                        middleware.beforeRequest(method.getFullMethodName(), requestHeaders, message);
                        super.sendMessage(message);
                    }
                };
            }
        };
    }


    public static ServerInterceptor convert(GrpcServerMiddleware middleware) {
        return new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                String methodName = call.getMethodDescriptor().getFullMethodName();
                ServerCall<ReqT, RespT> loggingCall = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
                    @Override
                    public void sendMessage(RespT message) {
                        middleware.afterResponse(methodName, headers, message);
                        super.sendMessage(message);
                    }
                };
                ServerCall.Listener<ReqT> listener = next.startCall(loggingCall, headers);
                return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) {
                    @Override
                    public void onMessage(ReqT message) {
                        middleware.beforeRequest(methodName, headers, message);
                        super.onMessage(message);
                    }
                };
            }
        };
    }
}