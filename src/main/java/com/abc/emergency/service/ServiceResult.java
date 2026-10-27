package com.abc.emergency.service;

public class ServiceResult<T> {

    public enum Status {
        SUCCESS,
        VALIDATION_ERROR,
        NOT_FOUND,
        CONFLICT,
        DATABASE_ERROR
    }

    private final Status status;
    private final String message;
    private final T data;

    private ServiceResult(Status status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static <T> ServiceResult<T> success(T data) {
        return new ServiceResult<>(Status.SUCCESS, null, data);
    }

    public static <T> ServiceResult<T> success(String message, T data) {
        return new ServiceResult<>(Status.SUCCESS, message, data);
    }

    public static <T> ServiceResult<T> validationError(String message) {
        return new ServiceResult<>(Status.VALIDATION_ERROR, message, null);
    }

    public static <T> ServiceResult<T> notFound(String message) {
        return new ServiceResult<>(Status.NOT_FOUND, message, null);
    }

    public static <T> ServiceResult<T> conflict(String message) {
        return new ServiceResult<>(Status.CONFLICT, message, null);
    }

    public static <T> ServiceResult<T> databaseError(String message) {
        return new ServiceResult<>(Status.DATABASE_ERROR, message, null);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
