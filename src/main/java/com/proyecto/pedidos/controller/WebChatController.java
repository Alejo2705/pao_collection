package com.proyecto.pedidos.controller;
import com.proyecto.pedidos.service.WebChatService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import java.util.UUID;

@RestController
public class WebChatController {
    private final WebChatService service;
    public WebChatController(WebChatService service){this.service=service;}
    public record Mensaje(String mensaje){}
    public record Confirmacion(String nombre,String telefono,boolean consentimiento){}
    private String id(HttpSession s){String id=(String)s.getAttribute("webChatId");if(id==null){id=UUID.randomUUID().toString();s.setAttribute("webChatId",id);}return id;}
    private ResponseEntity<WebChatService.Estado> respuesta(WebChatService.Estado e){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(e);}
    @GetMapping("/api/chat/sesion") public ResponseEntity<WebChatService.Estado> estado(HttpSession s){synchronized(s){return respuesta(service.estado(id(s)));}}
    @PostMapping("/api/chat/mensajes") public ResponseEntity<WebChatService.Estado> mensaje(@RequestBody Mensaje m,HttpSession s){synchronized(s){return respuesta(service.mensaje(id(s),m.mensaje()));}}
    @PostMapping("/api/chat/confirmar") public ResponseEntity<WebChatService.Estado> confirmar(@RequestBody Confirmacion c,HttpSession s){synchronized(s){return respuesta(service.confirmar(id(s),c.nombre(),c.telefono(),c.consentimiento()));}}
    @PostMapping("/api/chat/nueva") public ResponseEntity<WebChatService.Estado> nueva(HttpSession s){synchronized(s){s.setAttribute("webChatId",UUID.randomUUID().toString());return respuesta(service.estado(id(s)));}}
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<java.util.Map<String,String>> error(org.springframework.web.server.ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(java.util.Map.of("detail",e.getReason()==null?"No se pudo completar la solicitud":e.getReason()));}
    @GetMapping("/api/chat/admin/conversaciones") public Object recientes(){return service.recientes();}
}
