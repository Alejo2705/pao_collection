package com.proyecto.pedidos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ProductoResumenResponse {

    private Long id;
    private String nombre;
    private BigDecimal precio;
}
