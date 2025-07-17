package com.xhk.grpc.spring.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "grpc")
public class GrpcProperties {
    private Server server;
    private Map<String, Client> clients;

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

    public static class Server {
        private int port;

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static class Client {
        private String address;
        private int port;
        private Map<String, Object> headers;

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
    }
}
