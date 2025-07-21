package com.xhk.grpc.spring.middleware;

import java.util.HashMap;
import java.util.Map;

// Context truyền vào middleware, cho phép truy cập/chỉnh sửa request, response, header
// Được sử dụng trong method invoke của Middleware
public class Context<ReqT, RespT> {
    private final ReqT request;
    private RespT response;
    private Object stub;
    private final Map<String, String> headers = new HashMap<>();

    public Context(ReqT request, Object stub) {
        this.request = request;
        this.stub = stub;
    }

    public Object getStub() {
        return stub;
    }

    public void setStub(Object stub) {
        this.stub = stub;
    }

    public ReqT getRequest() {
        return request;
    }

    public RespT getResponse() {
        return response;
    }

    public void setResponse(RespT response) {
        this.response = response;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeader(String key, String value) {
        headers.put(key, value);
    }

    public String getHeader(String key) {
        return headers.get(key);
    }
}