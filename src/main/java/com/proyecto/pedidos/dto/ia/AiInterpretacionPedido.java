package com.proyecto.pedidos.dto.ia;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public class AiInterpretacionPedido {

    @JsonPropertyDescription(
            "Una de estas intenciones: CREAR_PEDIDO, CONSULTA_PRODUCTO u OTRO."
    )
    public String intencion;

    @JsonPropertyDescription(
            "True cuando el mensaje es ambiguo, hay un producto sin coincidencia clara "
            + "o hace falta preguntarle algo al cliente."
    )
    public boolean requiereRevision;

    @JsonPropertyDescription(
            "Pregunta corta de aclaración para el cliente. "
            + "Si no hace falta aclarar, devuelve una cadena vacía."
    )
    public String aclaracion;

    @JsonPropertyDescription(
            "Productos identificados en el mensaje. "
            + "Solo usa IDs existentes en el catálogo proporcionado."
    )
    public List<AiItemPedido> items;
}
