package com.proyecto.pedidos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.pedidos.dto.MensajeConversacion;
import com.proyecto.pedidos.dto.ia.*;
import com.proyecto.pedidos.model.*;
import com.proyecto.pedidos.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Model outputs are simulated; context, catalog validation and quote lifecycle are real. */
class WebChatContextoTest {
    final WebChatRepository chats = mock(WebChatRepository.class);
    final ProductoRepository productos = mock(ProductoRepository.class);
    final ClienteRepository clientes = mock(ClienteRepository.class);
    final PedidoService pedidos = mock(PedidoService.class);
    final WebChat chat = new WebChat();
    final Deque<AiInterpretacionPedido> respuestas = new ArrayDeque<>();
    final List<List<MensajeConversacion>> contextos = new ArrayList<>();
    final List<String> mensajes = new ArrayList<>();
    final InterpretacionPedidoService ia = new InterpretacionPedidoService(null, productos, clientes, "test") {
        @Override AiInterpretacionPedido consultarModelo(String mensaje, List<Producto> catalogo,
                                                         List<MensajeConversacion> historial) {
            mensajes.add(mensaje);contextos.add(historial);return respuestas.removeFirst();
        }
    };
    final WebChatService service = new WebChatService(chats, ia, pedidos, clientes, productos, new ObjectMapper());
    WebChatContextoTest() {
        chat.setId("12345678-1234-1234-1234-123456789012");
        when(chats.bloquear(chat.getId())).thenReturn(Optional.of(chat));
        when(productos.findAll()).thenReturn(List.of(
                producto(1,"PULSERA 7 CHAKRAS","25"), producto(2,"PULSERA 7 CHAKRAS","30"),
                producto(3,"COLLAR ACERO OSO","49"), producto(4,"PULSERA OJO TIGRE HILO","35")));
    }
    Producto producto(long id, String nombre, String precio) {
        Producto p = new Producto();p.setId(id);p.setNombre("INTERNO-"+id);p.setDescripcion(nombre);
        p.setStock(5);p.setPrecio(new BigDecimal(precio));p.setActivo(true);return p;
    }
    AiInterpretacionPedido resultado(String intencion, long id, int cantidad) {
        AiInterpretacionPedido ai = new AiInterpretacionPedido();ai.intencion=intencion;ai.aclaracion="";
        AiItemPedido item=new AiItemPedido();item.productoId=id;item.cantidad=cantidad;item.confianza=.99;
        ai.items=List.of(item);return ai;
    }
    WebChatService.Estado decir(String texto, String intencion, long id, int cantidad) {
        respuestas.add(resultado(intencion,id,cantidad));return service.mensaje(chat.getId(),texto);
    }
    String ultima(WebChatService.Estado estado) {return estado.mensajes().get(estado.mensajes().size()-1).texto();}

    @Test void consultaNuevaNoHeredaAmbiguedadDelProductoAnterior() {
        var anterior=decir("Quiero una pulsera 7 chakras","CREAR_PEDIDO",0,1);
        assertTrue(ultima(anterior).contains("varios modelos"));assertFalse(anterior.puedeConfirmar());
        var nuevo=decir("¿Cuánto cuesta el collar del osito?","CONSULTA_PRODUCTO",3,1);
        assertTrue(ultima(nuevo).contains("S/ 49.00"));assertFalse(ultima(nuevo).contains("chakras"));
        assertFalse(ultima(nuevo).contains("varios modelos"));assertFalse(nuevo.puedeConfirmar());
        assertEquals("¿Cuánto cuesta el collar del osito?",mensajes.get(1));
        assertEquals(2,contextos.get(1).size());
        verifyNoInteractions(clientes,pedidos);
    }

    @Test void reemplazarProductoNoArrastraItemsNiCantidadDeLaPropuestaAnterior() {
        decir("Quiero dos collares de osito","CREAR_PEDIDO",3,2);
        var nuevo=decir("Mejor una pulsera ojo de tigre","CREAR_PEDIDO",4,1);
        assertTrue(nuevo.puedeConfirmar());assertEquals(1,nuevo.propuesta().size());
        assertEquals(4L,nuevo.propuesta().get(0).productoId());
        assertEquals(1,nuevo.propuesta().get(0).cantidad());
        assertEquals(new BigDecimal("35"),nuevo.propuesta().get(0).subtotal());
        assertFalse(ultima(nuevo).contains("osito"));
        verifyNoInteractions(clientes,pedidos);
    }

    @ParameterizedTest @ValueSource(strings={"No", "No, gracias", "Ya no lo quiero", "No me interesa", "Olvídalo"})
    void rechazoQuitaPropuestaYNoRetomaTemaDescartado(String rechazo) {
        decir("Quiero dos collares de osito","CREAR_PEDIDO",3,2);
        int llamadas=mensajes.size();
        var descartado=service.mensaje(chat.getId(),rechazo);
        assertFalse(descartado.puedeConfirmar());assertTrue(descartado.propuesta().isEmpty());
        assertEquals(llamadas,mensajes.size());assertTrue(ultima(descartado).contains("dejamos ese producto de lado"));
        assertThrows(ResponseStatusException.class,()->service.confirmar(chat.getId(),"Prueba","999888777",true));
        var nuevo=decir("¿Tienen pulsera ojo de tigre?","CONSULTA_PRODUCTO",4,1);
        assertTrue(contextos.get(1).stream().noneMatch(l->l.texto().contains("osito")));
        assertTrue(ultima(nuevo).contains("ojo tigre"));assertEquals(6,nuevo.mensajes().size());
        verifyNoInteractions(clientes,pedidos);
    }

    @Test void rechazoYCambioEnMismoMensajeDescartaContextoAnterior() {
        decir("Quiero pulsera 7 chakras","CREAR_PEDIDO",0,1);
        var nuevo=decir("No me interesa, mejor el collar del osito","CONSULTA_PRODUCTO",3,1);
        assertTrue(contextos.get(1).isEmpty());assertTrue(ultima(nuevo).contains("S/ 49.00"));
        decir("¿Y cuánto está?","CONSULTA_PRODUCTO",3,1);
        assertTrue(contextos.get(2).stream().noneMatch(l->l.texto().contains("7 chakras")));
    }

    @Test void referenciaConservaContextoDelProductoActual() {
        decir("¿Tienes el collar del osito?","CONSULTA_PRODUCTO",3,1);
        var propuesta=decir("Entonces quiero dos de esos","CREAR_PEDIDO",3,2);
        assertEquals(2,contextos.get(1).size());assertTrue(propuesta.puedeConfirmar());
        assertEquals(3L,propuesta.propuesta().get(0).productoId());
        assertEquals(new BigDecimal("98"),propuesta.propuesta().get(0).subtotal());
    }

    @Test void mensajeActualEHistorialViajanSeparadosAlModelo() throws Exception {
        String entrada=ia.construirEntrada("¿Tienen el collar del osito?",productos.findAll(),List.of(
                new MensajeConversacion("cliente","Quiero una pulsera 7 chakras"),
                new MensajeConversacion("asistente","¿Cuál modelo de pulsera prefieres?")));
        var json=new ObjectMapper().readTree(entrada);
        assertEquals("¿Tienen el collar del osito?",json.get("mensajeCliente").asText());
        assertEquals(2,json.get("historialConversacion").size());
        assertEquals("asistente",json.get("historialConversacion").get(1).get("rol").asText());
    }
}
