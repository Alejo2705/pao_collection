package com.proyecto.pedidos.dto.ia;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class AiItemPedido {

    @JsonPropertyDescription(
            "ID exacto del producto del catálogo proporcionado. "
            + "Usa 0 si no existe una coincidencia suficientemente clara."
    )
    public long productoId;

    @JsonPropertyDescription(
            "Cantidad solicitada por el cliente. Si identifica claramente el producto "
            + "pero no indica cantidad, usa 1."
    )
    public int cantidad;

    @JsonPropertyDescription(
            "Confianza de la asociación entre 0.0 y 1.0."
    )
    public double confianza;

    @JsonPropertyDescription(
            "Fragmento breve del mensaje que justificó la detección."
    )
    public String textoDetectado;
}
