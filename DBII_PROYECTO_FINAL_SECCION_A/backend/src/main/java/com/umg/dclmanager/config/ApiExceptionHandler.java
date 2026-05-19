package com.umg.dclmanager.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> err(Exception e) {
        return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage() == null ? "Error interno" : e.getMessage())
        );
    }
}