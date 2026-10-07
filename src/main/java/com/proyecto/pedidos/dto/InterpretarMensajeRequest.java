package com.proyecto.pedidos.dto;

import lombok.Data;

@Data
public class InterpretarMensajeRequest {

    private Long clienteId;
    private String mensaje;
}
