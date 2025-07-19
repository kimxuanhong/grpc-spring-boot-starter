package com.xhk.grpc.spring.config;

import io.grpc.ClientInterceptor;

import java.util.List;

// Cấu hình channel cho gRPC client, bao gồm target, bảo mật, interceptor và timeout
public class GrpcChannelConfig {
    // Địa chỉ endpoint gRPC server (ví dụ: localhost:9090)
    private String target;
    // Có sử dụng plaintext (không mã hóa SSL/TLS) hay không
    private boolean plaintext = true;
    // Danh sách interceptor cho client (dùng cho logging, auth, v.v.)
    private List<ClientInterceptor> interceptors;
    // Thời gian timeout mặc định cho mọi request qua channel này (đơn vị: giây)
    // Nếu không set, mặc định là 45 giây
    private Long timeoutSeconds = 45L; // Mặc định 45 giây

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
        return timeoutSeconds != null ? timeoutSeconds : 45L;
    }

    public void setTimeoutSeconds(Long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
