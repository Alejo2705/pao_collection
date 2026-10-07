package com.proyecto.pedidos.dto;

import com.proyecto.pedidos.model.EstadoPedido;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class PedidoResponse {

    private Long id;
    private ClienteResumenResponse cliente;
    private LocalDateTime fecha;
    private EstadoPedido estado;
    private BigDecimal total;
    private List<DetallePedidoResponse> detalles;
}
