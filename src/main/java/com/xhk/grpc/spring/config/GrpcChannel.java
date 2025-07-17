package com.xhk.grpc.spring.config;

import io.grpc.ClientInterceptor;

import java.util.List;

public class GrpcChannel {
    private String target;
    private boolean plaintext = true;
    private List<ClientInterceptor> interceptors;
    private Long timeoutSeconds;

    // getters/setters
    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public boolean usePlaintext() {
        return plaintext;
    }

    public void setPlaintext(boolean plaintext) {
        this.plaintext = plaintext;
    }

    public List<ClientInterceptor> getInterceptors() {
        return interceptors;
    }

    public void setInterceptors(List<ClientInterceptor> interceptors) {
        this.interceptors = interceptors;
    }

    public Long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
