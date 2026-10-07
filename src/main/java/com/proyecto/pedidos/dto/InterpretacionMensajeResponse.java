package com.proyecto.pedidos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class InterpretacionMensajeResponse {

    private Long clienteId;
    private String clienteNombre;
    private String mensajeOriginal;
    private String intencion;
    private List<ItemInterpretadoResponse> items;
    private BigDecimal totalEstimado;
    private boolean requiereRevision;
    private List<String> observaciones;
    private String respuestaSugerida;
}
