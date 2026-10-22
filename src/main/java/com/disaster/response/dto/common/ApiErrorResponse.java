package com.disaster.response.dto.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.Map;

public class ApiErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant timestamp;
    private int status;
    private String code;
    private String message;
    private String path;
    private Map<String, String> details;

    public ApiErrorResponse() {
        this.timestamp = Instant.now();
    }

    public ApiErrorResponse(int status, String code, String message, String path) {
        this.timestamp = Instant.now();
        this.status = status;
        this.code = code;
        this.message = message;
        this.path = path;
    }

    public ApiErrorResponse(int status, String code, String message, String path, Map<String, String> details) {
        this.timestamp = Instant.now();
        this.status = status;
        this.code = code;
        this.message = message;
        this.path = path;
        this.details = details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }
}
