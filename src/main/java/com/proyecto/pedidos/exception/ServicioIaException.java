package com.proyecto.pedidos.exception;

public class ServicioIaException extends RuntimeException {

    public ServicioIaException(String mensaje) {
        super(mensaje);
    }

    public ServicioIaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
