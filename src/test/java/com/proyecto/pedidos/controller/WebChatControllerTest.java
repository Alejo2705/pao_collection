package com.proyecto.pedidos.controller;
import com.proyecto.pedidos.config.SecurityConfig;
import com.proyecto.pedidos.service.WebChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@WebMvcTest(controllers=WebChatController.class)
@Import(SecurityConfig.class)
class WebChatControllerTest {
 @Autowired MockMvc mvc;
 @MockBean WebChatService service;
 WebChatService.Estado empty=new WebChatService.Estado(List.of(),List.of(),null,false);
 @Test void publicSessionDoesNotExposeAnotherVisitor()throws Exception {
  when(service.estado(anyString())).thenReturn(empty);
  var a=mvc.perform(get("/api/chat/sesion")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andReturn();
  var b=mvc.perform(get("/api/chat/sesion")).andExpect(status().isOk()).andReturn();
  assertNotEquals(a.getRequest().getSession().getAttribute("webChatId"),b.getRequest().getSession().getAttribute("webChatId"));
 }
 @Test void publicPostRequiresCsrfAndAdminStaysPrivate()throws Exception {
  mvc.perform(post("/api/chat/mensajes").contentType("application/json").content("{\"mensaje\":\"hola\"}")).andExpect(status().isForbidden());
  mvc.perform(get("/api/chat/admin/conversaciones")).andExpect(status().isUnauthorized());
  verifyNoInteractions(service);
 }
 @Test void clientCannotChooseSessionOrSupplyOrderItems()throws Exception {
  MockHttpSession session=new MockHttpSession();session.setAttribute("webChatId","server-session");
  when(service.confirmar(anyString(),anyString(),anyString(),anyBoolean())).thenReturn(empty);
  mvc.perform(post("/api/chat/confirmar").session(session).with(csrf()).contentType("application/json")
    .content("{\"nombre\":\"Prueba\",\"telefono\":\"999888777\",\"consentimiento\":true,\"sessionId\":\"victim\",\"items\":[],\"total\":0}"))
    .andExpect(status().isOk());
  verify(service).confirmar("server-session","Prueba","999888777",true);
 }
 @Test void sessionBudgetReturns429()throws Exception {
  MockHttpSession session=new MockHttpSession();when(service.mensaje(anyString(),anyString())).thenReturn(empty);
  for(int i=0;i<8;i++)mvc.perform(post("/api/chat/mensajes").session(session).with(csrf()).contentType("application/json").content("{\"mensaje\":\"hola\"}")).andExpect(status().isOk());
  mvc.perform(post("/api/chat/mensajes").session(session).with(csrf()).contentType("application/json").content("{\"mensaje\":\"hola\"}")).andExpect(status().isTooManyRequests());
 }
}
