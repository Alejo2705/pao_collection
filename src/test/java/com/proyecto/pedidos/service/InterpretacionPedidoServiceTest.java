package com.proyecto.pedidos.service;

import com.proyecto.pedidos.dto.*;
import com.proyecto.pedidos.dto.ia.*;
import com.proyecto.pedidos.model.*;
import com.proyecto.pedidos.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InterpretacionPedidoServiceTest {
    private final ProductoRepository productos = mock(ProductoRepository.class);
    private final ClienteRepository clientes = mock(ClienteRepository.class);

    private Producto producto(long id, String codigo, String descripcion, String precio) {
        Producto p = new Producto();
        p.setId(id); p.setNombre(codigo); p.setDescripcion(descripcion);
        p.setPrecio(new BigDecimal(precio)); p.setStock(5); p.setActivo(true);
        return p;
    }

    private InterpretacionMensajeResponse interpretar(String mensaje, String intencion,
            long id, int cantidad, List<Producto> catalogo) {
        return interpretar(mensaje, intencion, id, cantidad, catalogo, "");
    }

    private InterpretacionMensajeResponse interpretar(String mensaje, String intencion,
            long id, int cantidad, List<Producto> catalogo, String aclaracion) {
        Cliente cliente = new Cliente(); cliente.setId(1L); cliente.setNombres("Cliente prueba");
        when(clientes.findById(1L)).thenReturn(Optional.of(cliente));
        when(productos.findAll()).thenReturn(catalogo);
        AiItemPedido item = new AiItemPedido(); item.productoId = id;
        item.cantidad = cantidad; item.confianza = .99; item.textoDetectado = mensaje;
        AiInterpretacionPedido ai = new AiInterpretacionPedido();
        ai.intencion = intencion; ai.items = List.of(item); ai.aclaracion = aclaracion;
        InterpretacionPedidoService service = new InterpretacionPedidoService(null, productos, clientes, "test") {
            @Override AiInterpretacionPedido consultarModelo(String texto, List<Producto> lista) {
                return ai;
            }
        };
        InterpretarMensajeRequest request = new InterpretarMensajeRequest();
        request.setClienteId(1L); request.setMensaje(mensaje);
        return service.interpretar(request);
    }

    @Test void catalogoIncluyeDescripcionNombreComercialPrecioYStock() throws Exception {
        var service = new InterpretacionPedidoService(null, productos, clientes, "test");
        String entrada = service.construirEntrada("Hola, tienes el collar del osito?",
                List.of(producto(1, "COLL", "COLLAR ACERO OSO", "49")));
        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(entrada);
        var p = json.get("catalogoAutorizado").get(0);
        assertEquals("COLLAR ACERO OSO", p.get("descripcion").asText());
        assertEquals("COLLAR ACERO OSO", p.get("nombreComercial").asText());
        assertEquals("COLL", p.get("codigoInterno").asText());
        assertEquals(49, p.get("precio").asInt()); assertEquals(5, p.get("stock").asInt());
    }

    @Test void pedidoUsaNombreComercialYCalculaPrecioDesdeCatalogo() {
        var r = interpretar("Me separas dos pulseritas ojo de tigre?", "CREAR_PEDIDO", 1, 2,
                List.of(producto(1, "PULOT", "PULSERA OJO TIGRE HILO", "35")));
        assertFalse(r.isRequiereRevision()); assertEquals(new BigDecimal("70"), r.getTotalEstimado());
        assertEquals("PULSERA OJO TIGRE HILO", r.getItems().get(0).getNombreProducto());
        assertTrue(r.getRespuestaSugerida().contains("pulsera ojo tigre hilo"));
        assertFalse(r.getRespuestaSugerida().contains("PULOT"));
    }

    @Test void consultaDevuelvePrecioSinPedirConfirmarPedido() {
        var r = interpretar("Cuánto está el collar del osito?", "CONSULTA_PRODUCTO", 1, 1,
                List.of(producto(1, "COLL", "COLLAR ACERO OSO", "49")));
        assertEquals("CONSULTA_PRODUCTO", r.getIntencion()); assertFalse(r.isRequiereRevision());
        assertTrue(r.getRespuestaSugerida().contains("S/ 49.00"));
        assertFalse(r.getRespuestaSugerida().contains("confirmar tu pedido"));
        verify(productos, never()).save(any());
    }

    @Test void descripcionDuplicadaNuncaSeleccionaModeloAunqueIAEsteSegura() {
        var r = interpretar("Quiero una pulsera 7 chakras", "CREAR_PEDIDO", 1, 1,
                List.of(producto(1, "7CHK", "PULSERA 7 CHAKRAS", "25"),
                        producto(2, "P7CH", "pulsera 7 chákrás", "30")));
        assertTrue(r.isRequiereRevision()); assertTrue(r.getItems().isEmpty());
        assertTrue(r.getRespuestaSugerida().contains("S/ 25.00"));
        assertTrue(r.getRespuestaSugerida().contains("S/ 30.00"));
    }

    @Test void stockInsuficienteSigueBloqueado() {
        var r = interpretar("Me das seis pulseras ojo tigre", "CREAR_PEDIDO", 1, 6,
                List.of(producto(1, "PULOT", "PULSERA OJO TIGRE HILO", "35")));
        assertTrue(r.isRequiereRevision());
        assertTrue(r.getObservaciones().stream().anyMatch(t -> t.contains("Stock insuficiente")));
    }

    @Test void aclaracionDeModelosDuplicadosNoExponeIdsAunqueIANoElijaUnProducto() {
        var r = interpretar("Hola, quisiera una pulsera de 7 chakras", "CREAR_PEDIDO", 0, 1,
                List.of(producto(13, "7CHK", "PULSERA 7 CHAKRAS", "25"),
                        producto(16, "P7CH", "PULSERA 7 CHAKRAS", "30")));
        assertTrue(r.isRequiereRevision());
        assertTrue(r.getRespuestaSugerida().contains("S/ 25.00"));
        assertTrue(r.getRespuestaSugerida().contains("S/ 30.00"));
        assertFalse(r.getRespuestaSugerida().contains("producto 13"));
        assertFalse(r.getRespuestaSugerida().contains("7CHK"));
    }

    @Test void productoInventadoNuncaSeConvierteEnPedido() {
        var r = interpretar("Quiero un accesorio", "CREAR_PEDIDO", 999, 1,
                List.of(producto(1, "PULOT", "PULSERA OJO TIGRE HILO", "35")));
        assertTrue(r.isRequiereRevision()); assertTrue(r.getItems().isEmpty());
    }
    @Test void respuestaDeCompraNoAfirmaReservaSinConfirmacion() {
        var r = interpretar("Hola, me separas dos pulseritas?", "CREAR_PEDIDO", 1, 2,
                List.of(producto(1, "PULOT", "PULSERA OJO TIGRE HILO", "35")));
        assertTrue(r.getRespuestaSugerida().contains("Antes de separarlo"));
        assertTrue(r.getRespuestaSugerida().contains("S/ 70.00"));
        assertFalse(r.getRespuestaSugerida().contains("Identifiqué"));
        assertFalse(r.getRespuestaSugerida().contains("Cliente prueba"));
    }

    @Test void consultaDeRegaloNoVuelveAPreguntarSiEsRegalo() {
        var r = interpretar("Hola, cuánto cuesta el collar para un regalo?", "CONSULTA_PRODUCTO", 1, 1,
                List.of(producto(1, "COLL", "COLLAR ACERO OSO", "49")));
        assertTrue(r.getRespuestaSugerida().contains("para tu regalo"));
        assertFalse(r.getRespuestaSugerida().contains("¿Es para ti"));
    }

    @Test void stockInsuficienteOfreceCantidadRealSinPrometerEntrega() {
        var r = interpretar("Quiero seis pulseras", "CREAR_PEDIDO", 1, 6,
                List.of(producto(1, "PULOT", "PULSERA OJO TIGRE HILO", "35")));
        assertTrue(r.getRespuestaSugerida().contains("nos quedan 5 unidades"));
        assertTrue(r.getRespuestaSugerida().contains("cantidad disponible"));
        assertTrue(r.isRequiereRevision());
    }

    @Test void productoAgotadoNoSeOfreceComoDisponible() {
        var p = producto(1, "COLL", "COLLAR ACERO OSO", "49"); p.setStock(0);
        var r = interpretar("Tienes el collar del oso?", "CONSULTA_PRODUCTO", 1, 1, List.of(p));
        assertTrue(r.getRespuestaSugerida().contains("no tenemos unidades disponibles"));
        assertFalse(r.getRespuestaSugerida().contains("sí está disponible"));
    }


    @ParameterizedTest
    @ValueSource(strings={"Envíame una foto del modelo", "¿Puedes adjuntar una imagen?",
            "Manda una captura", "Comparte una fotografía por Instagram", "Adjunta el archivo",
            "Please upload a picture", "Send a photo"})
    void aclaracionDelModeloNoPuedeOfrecerRecepcionDeImagenes(String aclaracion) {
        var r=interpretar("Quiero el accesorio que vi", "CONSULTA_PRODUCTO",0,1,
                List.of(producto(1,"COLL","COLLAR ACERO OSO","49")),aclaracion);
        assertTrue(r.isRequiereRevision());
        assertTrue(r.getRespuestaSugerida().contains("por escrito"));
        assertFalse(r.getRespuestaSugerida().contains(aclaracion));
    }

    @Test void aclaracionUtilPorTextoSeConserva() {
        String pregunta="¿Buscas un collar o una pulsera?";
        var r=interpretar("Busco algo para regalar", "CONSULTA_PRODUCTO",0,1,
                List.of(producto(1,"COLL","COLLAR ACERO OSO","49")),pregunta);
        assertTrue(r.getRespuestaSugerida().contains(pregunta));
    }

    @Test void modelosDuplicadosSeAclaranSoloPorTextoInclusoSiCompartenPrecio() {
        var catalogo=List.of(producto(1,"7CHK","PULSERA 7 CHAKRAS","25"),
                producto(2,"P7CH","PULSERA 7 CHAKRAS","25"));
        for (long id : List.of(0L,1L)) {
            var r=interpretar("Quiero pulsera 7 chakras", "CREAR_PEDIDO",id,1,catalogo);
            assertTrue(r.isRequiereRevision());assertTrue(r.getItems().isEmpty());
            assertTrue(r.getRespuestaSugerida().contains("por escrito"));
            assertFalse(r.getRespuestaSugerida().contains("foto"));
            assertFalse(r.getRespuestaSugerida().contains("imagen"));
        }
    }

    @Test void productoSinCoincidenciaPideDescripcionEscrita() {
        var r=interpretar("Quiero el accesorio que vi", "CREAR_PEDIDO",0,1,
                List.of(producto(1,"COLL","COLLAR ACERO OSO","49")));
        assertTrue(r.getRespuestaSugerida().contains("por escrito"));
        assertFalse(r.getRespuestaSugerida().contains("foto"));
    }
}
