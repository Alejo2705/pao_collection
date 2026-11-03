package com.proyecto.pedidos.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity @Table(name="web_chats") @Data
public class WebChat {
    @Id @Column(length=36) private String id;
    private LocalDateTime fecha = LocalDateTime.now(java.time.ZoneOffset.UTC);
    private LocalDateTime actualizado = LocalDateTime.now(java.time.ZoneOffset.UTC);
    @Lob @Column(columnDefinition="LONGTEXT") private String historial = "[]";
    @Lob @Column(columnDefinition="LONGTEXT") private String propuesta;
    private LocalDateTime vencePropuesta;
    private Long pedidoId;
    @Column(length=100) private String nombre;
    @Column(length=20) private String telefono;
    private LocalDateTime consentimiento;
}
