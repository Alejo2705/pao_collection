package com.proyecto.pedidos.dto;

import lombok.Data;

import java.util.List;

@Data
public class CrearPedidoRequest {

    private Long clienteId;
    private List<ItemPedidoRequest> items;
}