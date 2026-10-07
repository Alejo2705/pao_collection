package com.proyecto.pedidos.exception;

public class ServicioInstagramException extends RuntimeException {

    public ServicioInstagramException(String mensaje) {
        super(mensaje);
    }

    public ServicioInstagramException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
