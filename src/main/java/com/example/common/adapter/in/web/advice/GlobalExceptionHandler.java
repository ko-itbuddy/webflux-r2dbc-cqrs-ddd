package com.example.common.adapter.in.web.advice;

import com.example.common.adapter.in.web.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleAllExceptions(Exception ex) {
        log.error("Unhandled exception occurred", ex);
        
        boolean isDev = !activeProfile.equalsIgnoreCase("prod");
        String message = isDev ? ex.getMessage() : null;
        
        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "SERVER_ERROR",
                message
        );
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleBadRequest(IllegalArgumentException ex) {
        boolean isDev = !activeProfile.equalsIgnoreCase("prod");
        
        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_INPUT",
                isDev ? ex.getMessage() : null
        );
        
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
    }
}
