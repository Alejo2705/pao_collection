package com.proyecto.pedidos.dto;

import lombok.Data;

@Data
public class ItemPedidoRequest {

    private Long productoId;
    private Integer cantidad;
}