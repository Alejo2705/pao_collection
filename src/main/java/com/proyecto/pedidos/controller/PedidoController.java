package com.proyecto.pedidos.controller;

import com.proyecto.pedidos.dto.CrearPedidoRequest;
import com.proyecto.pedidos.dto.PedidoResponse;
import com.proyecto.pedidos.model.EstadoPedido;
import com.proyecto.pedidos.model.Pedido;
import com.proyecto.pedidos.service.PedidoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @GetMapping
    public List<PedidoResponse> listar() {
        return service.listarResponses();
    }

    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable Long id) {
        return service.obtenerResponsePorId(id);
    }

    @PostMapping
    public PedidoResponse crear(
            @RequestBody CrearPedidoRequest request) {

        Pedido pedido = service.crear(request);

        return service.convertirAResponse(pedido);
    }

    @PatchMapping("/{id}/estado")
    public PedidoResponse cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoPedido estado) {

        Pedido pedido = service.cambiarEstado(id, estado);

        return service.convertirAResponse(pedido);
    }
}