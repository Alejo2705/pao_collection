package com.proyecto.pedidos.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class AuthController {
    @GetMapping("/auth/csrf")
    public ResponseEntity<Map<String, String>> csrf(CsrfToken csrf) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of(
                "token", csrf.getToken(), "headerName", csrf.getHeaderName(),
                "parameterName", csrf.getParameterName()));
    }
}
