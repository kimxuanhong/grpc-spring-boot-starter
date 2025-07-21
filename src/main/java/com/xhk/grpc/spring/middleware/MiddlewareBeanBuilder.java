package com.xhk.grpc.spring.middleware;


import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Dùng để tạo danh sách middleware từ annotation hoặc cấu hình.
 */
public class MiddlewareBeanBuilder {

    /**
     * Build danh sách middleware theo functional-style
     */
    public static List<Middleware<Context<Object, Object>>> build(Class<?>[] middlewareClasses, ApplicationContext ctx) {
        List<Middleware<Context<Object, Object>>> result = new ArrayList<>();

        for (Class<?> clazz : middlewareClasses) {
            Object instance = getOrCreateInstance(clazz, ctx);

            if (!(instance instanceof Middleware)) {
                throw new IllegalArgumentException("Class " + clazz.getName() + " does not implement Middleware interface");
            }

            // Ép kiểu an toàn (nếu bạn chắc middleware đúng generic Context<?, ?>)
            @SuppressWarnings("unchecked")
            Middleware<Context<Object, Object>> middleware = (Middleware<Context<Object, Object>>) instance;

            result.add(middleware);
        }

        return result;
    }

    // Lấy từ Spring context hoặc khởi tạo mới
    private static Object getOrCreateInstance(Class<?> clazz, ApplicationContext ctx) {
        try {
            return ctx.getBean(clazz); // ưu tiên lấy từ Spring context
        } catch (Exception e) {
            try {
                return clazz.getDeclaredConstructor().newInstance(); // nếu không có bean, tạo mới
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot instantiate middleware: " + clazz.getName(), ex);
            }
        }
    }
}
