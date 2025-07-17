package com.xhk.grpc.spring.injector;


import com.xhk.grpc.spring.middleware.Middleware;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

public class MiddlewareChainBuilder {
    public static List<Middleware> build(Class<? extends Middleware>[] middlewareClasses, ApplicationContext ctx) throws Exception {
        List<Middleware> chain = new ArrayList<>();

        if (middlewareClasses != null) {
            for (Class<? extends Middleware> clazz : middlewareClasses) {
                Middleware instance = resolveMiddleware(clazz, ctx);
                if (instance != null) chain.add(instance);
            }
        }
        return chain;
    }

    private static Middleware resolveMiddleware(Class<? extends Middleware> clazz, ApplicationContext ctx) throws Exception {
        try {
            return ctx.getBean(clazz);
        } catch (Exception e) {
            return clazz.getDeclaredConstructor().newInstance();
        }
    }
}
