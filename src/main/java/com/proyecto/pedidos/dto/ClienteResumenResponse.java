package com.proyecto.pedidos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClienteResumenResponse {

    private Long id;
    private String nombres;
    private String documento;
}