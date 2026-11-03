package com.proyecto.pedidos.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.pedidos.dto.*;
import com.proyecto.pedidos.model.*;
import com.proyecto.pedidos.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties={"spring.jpa.hibernate.ddl-auto=create-drop"})
@Import({WebChatService.class,PedidoService.class,WebChatPersistenceTest.JsonConfig.class})
class WebChatPersistenceTest {
 @TestConfiguration static class JsonConfig{@Bean ObjectMapper json(){return new ObjectMapper();}}
 @Autowired WebChatService service;
 @Autowired ProductoRepository productos;
 @Autowired PedidoRepository pedidos;
 @Autowired ClienteRepository clientes;
 @MockBean InterpretacionPedidoService ia;
 @Test void confirmaEnBaseTemporalDescuentaUnaVezYPersisteHistorial(){
  Producto p=new Producto();p.setNombre("PUL-TEST");p.setDescripcion("Pulsera ojo tigre hilo");p.setStock(5);p.setPrecio(new BigDecimal("35.00"));p=productos.saveAndFlush(p);
  var r=new InterpretacionMensajeResponse(null,"Visitante","Quiero dos","CREAR_PEDIDO",List.of(new ItemInterpretadoResponse(p.getId(),p.getDescripcion(),"","","",2,p.getPrecio(),5,new BigDecimal("70.00"),.99)),new BigDecimal("70.00"),false,List.of(),"Dos pulseras por S/ 70.");
  when(ia.interpretarPublico(anyString())).thenReturn(r);
  String id=UUID.randomUUID().toString();assertTrue(service.mensaje(id,"Quiero dos pulseras ojo tigre").puedeConfirmar());
  assertEquals(5,productos.findById(p.getId()).orElseThrow().getStock());assertEquals(0,pedidos.count());assertEquals(0,clientes.count());
  var confirmed=service.confirmar(id,"CLIENTE PRUEBA WEB","999888777",true);
  assertNotNull(confirmed.pedidoId());assertFalse(confirmed.puedeConfirmar());
  assertEquals(confirmed.pedidoId(),service.confirmar(id,"CLIENTE PRUEBA WEB","999888777",true).pedidoId());
  assertEquals(1,pedidos.count());assertEquals(1,clientes.count());assertEquals(3,productos.findById(p.getId()).orElseThrow().getStock());
  assertEquals(3,service.estado(id).mensajes().size());assertEquals(EstadoPedido.PENDIENTE,pedidos.findById(confirmed.pedidoId()).orElseThrow().getEstado());
 }
}
