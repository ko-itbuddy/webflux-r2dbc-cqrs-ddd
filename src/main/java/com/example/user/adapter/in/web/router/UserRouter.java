package com.example.user.adapter.in.web.router;

import com.example.user.adapter.in.web.handler.UserHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class UserRouter {
    @Bean
    public RouterFunction<ServerResponse> userUrls(UserHandler handler) {
        return route(POST("/api/users/register"), handler::register);
    }
}
