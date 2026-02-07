package com.example.common.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final boolean success;
    private final int status;
    private final T data;
    private final ApiError error;

    public static <T> ApiResponse<T> success(int status, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(status)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String errorCode, String devMessage) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(status)
                .error(new ApiError(errorCode, devMessage))
                .build();
    }

    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiError {
        private final String code;
        private final String message; // This will be null in production

        public ApiError(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
