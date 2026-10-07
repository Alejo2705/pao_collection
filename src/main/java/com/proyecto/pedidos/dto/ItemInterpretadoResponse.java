package com.proyecto.pedidos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ItemInterpretadoResponse {

    private Long productoId;
    private String nombreProducto;
    private String categoria;
    private String material;
    private String color;
    private Integer cantidad;
    private BigDecimal precio;
    private Integer stockDisponible;
    private BigDecimal subtotal;
    private Double confianza;
}
