package com.proyecto.pedidos.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "conversaciones_instagram",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_conversacion_instagram_message_id",
                        columnNames = "instagram_message_id"
                )
        }
)
@Data
public class ConversacionInstagram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instagram_message_id", nullable = false, length = 255)
    private String instagramMessageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contacto_instagram_id", nullable = false)
    private ContactoInstagram contacto;

    @Column(name = "mensaje_entrada", nullable = false, columnDefinition = "TEXT")
    private String mensajeEntrada;

    @Column(name = "respuesta_sistema", columnDefinition = "TEXT")
    private String respuestaSistema;

    @Column(length = 40)
    private String intencion;

    @Column(name = "requiere_revision")
    private Boolean requiereRevision;

    @Column(name = "propuesta_json", columnDefinition = "TEXT")
    private String propuestaJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EstadoConversacionInstagram estado;

    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    public void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();

        if (fecha == null) {
            fecha = ahora;
        }

        actualizadoEn = ahora;
    }

    @PreUpdate
    public void preUpdate() {
        actualizadoEn = LocalDateTime.now();
    }
}
