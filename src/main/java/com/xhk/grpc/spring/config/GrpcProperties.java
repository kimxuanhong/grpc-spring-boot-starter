package com.xhk.grpc.spring.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "grpc")
@Validated
public class GrpcProperties {

    @NotNull
    private Server server = new Server();

    private Map<String, Client> clients = new HashMap<>();

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public Map<String, Client> getClients() {
        return clients;
    }

    public void setClients(Map<String, Client> clients) {
        this.clients = clients;
    }

    @Validated
    public static class Server {
        @Min(value = 1, message = "Port must be greater than 0")
        @Min(value = 65535, message = "Port must be less than 65536")
        private int port = 9090;

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    @Validated
    public static class Client {
        @NotBlank(message = "Address cannot be blank")
        private String address = "localhost";

        @Min(value = 1, message = "Port must be greater than 0")
        @Min(value = 65535, message = "Port must be less than 65536")
        private int port = 9090;

        private Map<String, Object> headers;

        // Connection settings
        private int keepAliveTimeMinutes = 1;
        private int keepAliveTimeoutSeconds = 30;
        private boolean keepAliveWithoutCalls = true;
        private int idleTimeoutMinutes = 5;
        private int maxRetryAttempts = 5;
        private boolean usePlaintext = true;
        private boolean enableDebug = false;
        private boolean enableRetry = false;

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public Map<String, Object> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, Object> headers) {
            this.headers = headers;
        }

        public int getKeepAliveTimeMinutes() {
            return keepAliveTimeMinutes;
        }

        public void setKeepAliveTimeMinutes(int keepAliveTimeMinutes) {
            this.keepAliveTimeMinutes = keepAliveTimeMinutes;
        }

        public int getKeepAliveTimeoutSeconds() {
            return keepAliveTimeoutSeconds;
        }

        public void setKeepAliveTimeoutSeconds(int keepAliveTimeoutSeconds) {
            this.keepAliveTimeoutSeconds = keepAliveTimeoutSeconds;
        }

        public boolean isKeepAliveWithoutCalls() {
            return keepAliveWithoutCalls;
        }

        public void setKeepAliveWithoutCalls(boolean keepAliveWithoutCalls) {
            this.keepAliveWithoutCalls = keepAliveWithoutCalls;
        }

        public int getIdleTimeoutMinutes() {
            return idleTimeoutMinutes;
        }

        public void setIdleTimeoutMinutes(int idleTimeoutMinutes) {
            this.idleTimeoutMinutes = idleTimeoutMinutes;
        }

        public int getMaxRetryAttempts() {
            return maxRetryAttempts;
        }

        public void setMaxRetryAttempts(int maxRetryAttempts) {
            this.maxRetryAttempts = maxRetryAttempts;
        }

        public boolean isUsePlaintext() {
            return usePlaintext;
        }

        public void setUsePlaintext(boolean usePlaintext) {
            this.usePlaintext = usePlaintext;
        }

        public boolean isEnableDebug() {
            return enableDebug;
        }

        public void setEnableDebug(boolean enableDebug) {
            this.enableDebug = enableDebug;
        }

        public boolean isEnableRetry() {
            return enableRetry;
        }

        public void setEnableRetry(boolean enableRetry) {
            this.enableRetry = enableRetry;
        }
    }
}
