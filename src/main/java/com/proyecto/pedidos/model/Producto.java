package com.proyecto.pedidos.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "productos")
@Data
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 300)
    private String descripcion;

    @Column(length = 50)
    private String categoria;

    @Column(length = 80)
    private String material;

    @Column(length = 50)
    private String color;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer stock;

    private Boolean activo = true;
}
