package com.proyecto.pedidos.dto;

/** Context supplied separately from the customer's current request. */
public record MensajeConversacion(String rol, String texto) {}
