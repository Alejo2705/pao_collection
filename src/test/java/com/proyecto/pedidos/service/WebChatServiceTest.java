package com.proyecto.pedidos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.pedidos.dto.*;
import com.proyecto.pedidos.model.*;
import com.proyecto.pedidos.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebChatServiceTest {
    final WebChatRepository chats=mock(WebChatRepository.class);
    final InterpretacionPedidoService ia=mock(InterpretacionPedidoService.class);
    final PedidoService pedidos=mock(PedidoService.class);
    final ClienteRepository clientes=mock(ClienteRepository.class);
    final ProductoRepository productos=mock(ProductoRepository.class);
    final WebChatService service=new WebChatService(chats,ia,pedidos,clientes,productos,new ObjectMapper());
    final WebChat chat=new WebChat();
    WebChatServiceTest(){chat.setId("12345678-1234-1234-1234-123456789012");when(chats.bloquear(chat.getId())).thenReturn(Optional.of(chat));}
    InterpretacionMensajeResponse resultado(String intencion,boolean revision){
        return new InterpretacionMensajeResponse(null,"Visitante web","mensaje",intencion,
            List.of(new ItemInterpretadoResponse(1L,"Pulsera ojo tigre hilo","Pulsera","","",2,new BigDecimal("35"),5,new BigDecimal("70"),.99)),new BigDecimal("70"),revision,List.of(),"¡Con gusto! Dos pulseras por S/ 70.");
    }
    void propuesta(){when(ia.interpretarPublico(anyString())).thenReturn(resultado("CREAR_PEDIDO",false));service.mensaje(chat.getId(),"Quiero dos pulseras ojo tigre");}
    Producto producto(){var p=new Producto();p.setId(1L);p.setPrecio(new BigDecimal("35"));p.setStock(5);p.setActivo(true);return p;}
    @Test void consultaNoCreaClientesPedidosNiStock(){
        when(ia.interpretarPublico(anyString())).thenReturn(resultado("CONSULTA_PRODUCTO",false));
        assertFalse(service.mensaje(chat.getId(),"¿Cuánto cuesta?").puedeConfirmar());
        verifyNoInteractions(clientes,pedidos,productos);
    }
    @Test void aclaracionNuncaPermiteConfirmar(){when(ia.interpretarPublico(anyString())).thenReturn(resultado("CREAR_PEDIDO",true));assertFalse(service.mensaje(chat.getId(),"Quiero una pulsera").puedeConfirmar());}
    @Test void confirmaSoloOfertaDelServidorYReintentoNoDuplica(){
        propuesta();when(productos.findByIdForUpdate(1L)).thenReturn(Optional.of(producto()));
        when(clientes.save(any())).thenAnswer(a->{Cliente c=a.getArgument(0);c.setId(9L);return c;});
        Pedido p=new Pedido();p.setId(33L);p.setTotal(new BigDecimal("70"));when(pedidos.crear(any())).thenReturn(p);
        assertEquals(33L,service.confirmar(chat.getId(),"Cliente prueba","999888777",true).pedidoId());
        assertEquals(33L,service.confirmar(chat.getId(),"Cliente prueba","999888777",true).pedidoId());
        verify(pedidos,times(1)).crear(argThat(r->r.getClienteId()==9L && r.getItems().size()==1 && r.getItems().get(0).getProductoId()==1L && r.getItems().get(0).getCantidad()==2));
    }
    @Test void precioModificadoBloqueaSinCrearCliente(){
        propuesta();Producto p=producto();p.setPrecio(new BigDecimal("40"));when(productos.findByIdForUpdate(1L)).thenReturn(Optional.of(p));
        assertThrows(ResponseStatusException.class,()->service.confirmar(chat.getId(),"Prueba","999888777",true));verifyNoInteractions(clientes,pedidos);
    }
    @Test void stockAgotadoBloquea(){propuesta();Producto p=producto();p.setStock(1);when(productos.findByIdForUpdate(1L)).thenReturn(Optional.of(p));assertThrows(ResponseStatusException.class,()->service.confirmar(chat.getId(),"Prueba","999888777",true));verifyNoInteractions(clientes,pedidos);}
    @Test void ofertaVencidaBloquea(){propuesta();chat.setVencePropuesta(LocalDateTime.now().minusSeconds(1));assertThrows(ResponseStatusException.class,()->service.confirmar(chat.getId(),"Prueba","999888777",true));verifyNoInteractions(clientes,pedidos,productos);}
    @Test void requiereConsentimientoYDatosValidos(){propuesta();assertThrows(ResponseStatusException.class,()->service.confirmar(chat.getId(),"Prueba","999888777",false));assertThrows(ResponseStatusException.class,()->service.confirmar(chat.getId(),"P","x",true));verifyNoInteractions(clientes,pedidos,productos);}
    @Test void noPuedeConfirmarOtraSesion(){when(chats.bloquear("ajena")).thenReturn(Optional.empty());assertThrows(ResponseStatusException.class,()->service.confirmar("ajena","Prueba","999888777",true));verifyNoInteractions(clientes,pedidos,productos);}
    @Test void mensajeInvalidoNoInvocaIa(){assertThrows(ResponseStatusException.class,()->service.mensaje(chat.getId()," "));assertThrows(ResponseStatusException.class,()->service.mensaje(chat.getId(),"x".repeat(1001)));verifyNoInteractions(ia);}
    @Test void fechaDelPanelIncluyeOffsetUtcSinAlterarRegistrosExistentes(){
        chat.setActualizado(LocalDateTime.of(2026,11,3,2,25,1));
        when(chats.findTop100ByOrderByActualizadoDesc()).thenReturn(List.of(chat));
        var fecha=(java.time.OffsetDateTime)service.recientes().get(0).get("fecha");
        assertEquals(java.time.ZoneOffset.UTC,fecha.getOffset());
        assertEquals(LocalDateTime.of(2026,11,2,21,25,1),fecha.atZoneSameInstant(java.time.ZoneId.of("America/Lima")).toLocalDateTime());
    }
}
