package com.proyecto.pedidos.controller;

import com.proyecto.pedidos.dto.InterpretacionMensajeResponse;
import com.proyecto.pedidos.dto.InterpretarMensajeRequest;
import com.proyecto.pedidos.service.InterpretacionPedidoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ia")
public class InteligenciaController {

    private final InterpretacionPedidoService service;

    public InteligenciaController(InterpretacionPedidoService service) {
        this.service = service;
    }

    @PostMapping("/interpretar")
    public InterpretacionMensajeResponse interpretar(
            @RequestBody InterpretarMensajeRequest request) {

        return service.interpretar(request);
    }
}
