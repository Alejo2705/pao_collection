package com.proyecto.pedidos.controller;

import com.proyecto.pedidos.dto.instagram.ContactoInstagramResponse;
import com.proyecto.pedidos.dto.instagram.ConversacionInstagramResponse;
import com.proyecto.pedidos.dto.instagram.InstagramStatusResponse;
import com.proyecto.pedidos.exception.RecursoNoEncontradoException;
import com.proyecto.pedidos.model.Cliente;
import com.proyecto.pedidos.model.ContactoInstagram;
import com.proyecto.pedidos.model.ConversacionInstagram;
import com.proyecto.pedidos.repository.ClienteRepository;
import com.proyecto.pedidos.repository.ContactoInstagramRepository;
import com.proyecto.pedidos.repository.ConversacionInstagramRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instagram")
public class InstagramAdminController {

    private final ContactoInstagramRepository contactoRepository;
    private final ConversacionInstagramRepository conversacionRepository;
    private final ClienteRepository clienteRepository;

    private final String accessToken;
    private final String instagramUserId;
    private final String appSecret;
    private final String verifyToken;
    private final String graphVersion;

    public InstagramAdminController(
            ContactoInstagramRepository contactoRepository,
            ConversacionInstagramRepository conversacionRepository,
            ClienteRepository clienteRepository,
            @Value("${instagram.access-token:}") String accessToken,
            @Value("${instagram.user-id:}") String instagramUserId,
            @Value("${instagram.app-secret:}") String appSecret,
            @Value("${instagram.verify-token:}") String verifyToken,
            @Value("${instagram.graph-version:v26.0}") String graphVersion) {

        this.contactoRepository = contactoRepository;
        this.conversacionRepository = conversacionRepository;
        this.clienteRepository = clienteRepository;
        this.accessToken = accessToken;
        this.instagramUserId = instagramUserId;
        this.appSecret = appSecret;
        this.verifyToken = verifyToken;
        this.graphVersion = graphVersion;
    }

    @GetMapping("/status")
    public InstagramStatusResponse status() {

        boolean tokenOk = noVacio(accessToken);
        boolean userIdOk = noVacio(instagramUserId);
        boolean secretOk = noVacio(appSecret);
        boolean verifyOk = noVacio(verifyToken);

        return new InstagramStatusResponse(
                tokenOk && userIdOk && secretOk && verifyOk,
                tokenOk,
                userIdOk,
                secretOk,
                verifyOk,
                graphVersion,
                "/api/instagram/webhook"
        );
    }

    @GetMapping("/contactos")
    public List<ContactoInstagramResponse> contactos() {

        return contactoRepository
                .findAllByOrderByCreadoEnDesc()
                .stream()
                .map(this::convertirContacto)
                .toList();
    }

    @PatchMapping("/contactos/{contactoId}/vincular")
    public ContactoInstagramResponse vincular(
            @PathVariable Long contactoId,
            @RequestParam Long clienteId) {

        ContactoInstagram contacto =
                contactoRepository.findById(contactoId)
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                        "Contacto de Instagram no encontrado con id: "
                                        + contactoId
                                )
                        );

        Cliente cliente =
                clienteRepository.findById(clienteId)
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                        "Cliente no encontrado con id: "
                                        + clienteId
                                )
                        );

        contacto.setCliente(cliente);
        contactoRepository.save(contacto);

        return convertirContacto(contacto);
    }

    @DeleteMapping("/contactos/{contactoId}/vinculo")
    public ContactoInstagramResponse desvincular(
            @PathVariable Long contactoId) {

        ContactoInstagram contacto =
                contactoRepository.findById(contactoId)
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                        "Contacto de Instagram no encontrado con id: "
                                        + contactoId
                                )
                        );

        contacto.setCliente(null);
        contactoRepository.save(contacto);

        return convertirContacto(contacto);
    }

    @GetMapping("/conversaciones")
    public List<ConversacionInstagramResponse> conversaciones() {

        return conversacionRepository
                .findTop100ByOrderByFechaDesc()
                .stream()
                .map(this::convertirConversacion)
                .toList();
    }

    private ContactoInstagramResponse convertirContacto(
            ContactoInstagram c) {

        Long clienteId =
                c.getCliente() == null
                        ? null
                        : c.getCliente().getId();

        String clienteNombre =
                c.getCliente() == null
                        ? null
                        : c.getCliente().getNombres();

        return new ContactoInstagramResponse(
                c.getId(),
                c.getInstagramScopedId(),
                c.getNombreReferencia(),
                clienteId,
                clienteNombre,
                c.getActivo(),
                c.getCreadoEn()
        );
    }

    private ConversacionInstagramResponse convertirConversacion(
            ConversacionInstagram c) {

        ContactoInstagram contacto = c.getContacto();

        Long clienteId =
                contacto.getCliente() == null
                        ? null
                        : contacto.getCliente().getId();

        String clienteNombre =
                contacto.getCliente() == null
                        ? null
                        : contacto.getCliente().getNombres();

        return new ConversacionInstagramResponse(
                c.getId(),
                contacto.getId(),
                contacto.getInstagramScopedId(),
                clienteId,
                clienteNombre,
                c.getMensajeEntrada(),
                c.getRespuestaSistema(),
                c.getIntencion(),
                c.getRequiereRevision(),
                c.getEstado().name(),
                c.getPedidoId(),
                c.getFecha()
        );
    }

    private boolean noVacio(String valor) {
        return valor != null && !valor.isBlank();
    }
}
