package com.proyecto.pedidos.dto.instagram;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ConversacionInstagramResponse {

    private Long id;
    private Long contactoId;
    private String instagramScopedId;
    private Long clienteId;
    private String clienteNombre;
    private String mensajeEntrada;
    private String respuestaSistema;
    private String intencion;
    private Boolean requiereRevision;
    private String estado;
    private Long pedidoId;
    private LocalDateTime fecha;
}
