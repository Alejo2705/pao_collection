package com.proyecto.pedidos.controller;

import com.proyecto.pedidos.dto.instagram.InstagramAccountResponse;
import com.proyecto.pedidos.exception.ServicioInstagramException;
import com.proyecto.pedidos.service.InstagramAccountService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/instagram/cuenta")
public class InstagramAccountController {
    private final InstagramAccountService service;

    public InstagramAccountController(InstagramAccountService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<InstagramAccountResponse> cuenta() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.obtenerCuenta());
    }

    @ExceptionHandler(ServicioInstagramException.class)
    public ResponseEntity<Map<String, String>> error(ServicioInstagramException ex) {
        return ResponseEntity.status(503).cacheControl(CacheControl.noStore())
                .body(Map.of("mensaje", ex.getMessage()));
    }
}
