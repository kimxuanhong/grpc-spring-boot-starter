package com.xhk.grpc.spring.middleware;

import java.util.Map;

// Context truyền vào middleware, cho phép truy cập/chỉnh sửa request, response, header
// Được sử dụng trong method invoke của Middleware
public interface MiddlewareContext<ReqT, RespT> {
    ReqT getRequest();
    void setRequest(ReqT request);
    RespT getResponse();
    void setResponse(RespT response);
    Map<String, String> getHeaders();
    void setHeader(String key, String value);
    void next();
    boolean isChainStopped();
} 