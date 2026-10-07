package com.proyecto.pedidos.dto.instagram;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ContactoInstagramResponse {

    private Long id;
    private String instagramScopedId;
    private String nombreReferencia;
    private Long clienteId;
    private String clienteNombre;
    private Boolean activo;
    private LocalDateTime creadoEn;
}
