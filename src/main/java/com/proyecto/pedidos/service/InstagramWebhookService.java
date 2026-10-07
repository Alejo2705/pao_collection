package com.proyecto.pedidos.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.pedidos.dto.CrearPedidoRequest;
import com.proyecto.pedidos.dto.InterpretacionMensajeResponse;
import com.proyecto.pedidos.dto.InterpretarMensajeRequest;
import com.proyecto.pedidos.dto.ItemPedidoRequest;
import com.proyecto.pedidos.model.*;
import com.proyecto.pedidos.repository.ContactoInstagramRepository;
import com.proyecto.pedidos.repository.ConversacionInstagramRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InstagramWebhookService {

    private final ObjectMapper objectMapper;
    private final ContactoInstagramRepository contactoRepository;
    private final ConversacionInstagramRepository conversacionRepository;
    private final InterpretacionPedidoService interpretacionPedidoService;
    private final PedidoService pedidoService;
    private final InstagramMessagingService messagingService;

    public InstagramWebhookService(
            ObjectMapper objectMapper,
            ContactoInstagramRepository contactoRepository,
            ConversacionInstagramRepository conversacionRepository,
            InterpretacionPedidoService interpretacionPedidoService,
            PedidoService pedidoService,
            InstagramMessagingService messagingService) {

        this.objectMapper = objectMapper;
        this.contactoRepository = contactoRepository;
        this.conversacionRepository = conversacionRepository;
        this.interpretacionPedidoService = interpretacionPedidoService;
        this.pedidoService = pedidoService;
        this.messagingService = messagingService;
    }

    @Async
    public void procesarAsync(String rawBody) {

        try {
            JsonNode root = objectMapper.readTree(rawBody);

            if (!"instagram".equals(root.path("object").asText())) {
                return;
            }

            for (JsonNode entry : root.path("entry")) {

                /*
                 * Formato actual que Meta está enviando en tu prueba:
                 *
                 * entry[]
                 *   -> changes[]
                 *      -> field = "messages"
                 *      -> value
                 *         -> sender
                 *         -> recipient
                 *         -> message
                 */
                for (JsonNode change : entry.path("changes")) {

                    if (!"messages".equals(change.path("field").asText())) {
                        continue;
                    }

                    JsonNode value = change.path("value");

                    if (!value.isMissingNode() && !value.isNull()) {
                        procesarEvento(value);
                    }
                }

                /*
                 * Compatibilidad adicional por si Meta entrega un evento
                 * con el formato entry[].messaging[].
                 */
                for (JsonNode evento : entry.path("messaging")) {
                    procesarEvento(evento);
                }
            }

        } catch (Exception ex) {
            System.err.println(
                    "[INSTAGRAM] Error leyendo el webhook: "
                    + ex.getMessage()
            );
            ex.printStackTrace();
        }
    }

    private void procesarEvento(JsonNode evento) {

        JsonNode message = evento.path("message");

        if (message.isMissingNode() || message.isNull()) {
            return;
        }

        if (message.path("is_echo").asBoolean(false)
                || message.path("is_self").asBoolean(false)) {
            return;
        }

        String igsid = evento.path("sender").path("id").asText("");
        String messageId = message.path("mid").asText("");

        if (igsid.isBlank() || messageId.isBlank()) {
            return;
        }

        /*
         * El botón "Enviar al servidor" de Meta usa valores sintéticos
         * como random_mid/random_text. Lo reconocemos para validar que
         * el POST llegó sin crear un contacto falso ni intentar responderle.
         */
        if ("random_mid".equals(messageId)) {
            System.out.println(
                    "[INSTAGRAM] Evento de prueba de Meta recibido correctamente."
            );
            return;
        }

        if (conversacionRepository
                .existsByInstagramMessageId(messageId)) {

            System.out.println(
                    "[INSTAGRAM] Mensaje duplicado ignorado: "
                    + messageId
            );
            return;
        }

        ContactoInstagram contacto =
                obtenerOCrearContacto(igsid);

        String texto = message.path("text").asText("").trim();

        if (texto.isBlank()) {
            procesarMensajeNoTextual(
                    contacto,
                    messageId
            );
            return;
        }

        System.out.println(
                "[INSTAGRAM] Mensaje recibido de "
                + igsid
                + ": "
                + texto
        );

        ConversacionInstagram conversacion =
                nuevaConversacion(
                        contacto,
                        messageId,
                        texto
                );

        conversacionRepository.save(conversacion);

        try {
            if (esConfirmacion(texto)) {
                confirmarPedido(conversacion);
                return;
            }

            if (esCancelacion(texto)) {
                cancelarPropuesta(conversacion);
                return;
            }

            if (contacto.getCliente() == null) {

                String respuesta =
                        "¡Hola! Recibimos tu mensaje en Pao Collection 💗 "
                        + "Para procesar pedidos desde Instagram, primero debemos "
                        + "vincular tu cuenta con un cliente en nuestro sistema. "
                        + "Un asesor realizará esta vinculación y luego podrás "
                        + "reenviar tu pedido.";

                conversacion.setEstado(
                        EstadoConversacionInstagram.REQUIERE_VINCULACION
                );
                conversacion.setRequiereRevision(true);
                conversacion.setRespuestaSistema(respuesta);
                conversacionRepository.save(conversacion);

                messagingService.enviarTexto(
                        contacto.getInstagramScopedId(),
                        respuesta
                );
                return;
            }

            InterpretarMensajeRequest request =
                    new InterpretarMensajeRequest();

            request.setClienteId(
                    contacto.getCliente().getId()
            );
            request.setMensaje(texto);

            InterpretacionMensajeResponse interpretacion =
                    interpretacionPedidoService.interpretar(request);

            conversacion.setIntencion(
                    interpretacion.getIntencion()
            );
            conversacion.setRequiereRevision(
                    interpretacion.isRequiereRevision()
            );

            if (interpretacion.isRequiereRevision()
                    || !"CREAR_PEDIDO".equals(
                            interpretacion.getIntencion())
                    || interpretacion.getItems() == null
                    || interpretacion.getItems().isEmpty()) {

                conversacion.setEstado(
                        EstadoConversacionInstagram.REQUIERE_REVISION
                );

                conversacion.setRespuestaSistema(
                        interpretacion.getRespuestaSugerida()
                );

                conversacionRepository.save(conversacion);

                messagingService.enviarTexto(
                        contacto.getInstagramScopedId(),
                        interpretacion.getRespuestaSugerida()
                );
                return;
            }

            CrearPedidoRequest propuesta =
                    convertirAPropuesta(
                            interpretacion,
                            contacto.getCliente().getId()
                    );

            conversacion.setPropuestaJson(
                    objectMapper.writeValueAsString(propuesta)
            );
            conversacion.setEstado(
                    EstadoConversacionInstagram.PENDIENTE_CONFIRMACION
            );

            String respuesta =
                    construirResumenConfirmacion(interpretacion);

            conversacion.setRespuestaSistema(respuesta);
            conversacionRepository.save(conversacion);

            messagingService.enviarTexto(
                    contacto.getInstagramScopedId(),
                    respuesta
            );

        } catch (Exception ex) {

            conversacion.setEstado(
                    EstadoConversacionInstagram.ERROR
            );
            conversacion.setRespuestaSistema(
                    "No pude procesar tu mensaje en este momento."
            );
            conversacionRepository.save(conversacion);

            System.err.println(
                    "[INSTAGRAM] Error procesando mensaje: "
                    + ex.getMessage()
            );
            ex.printStackTrace();
        }
    }

    private void confirmarPedido(
            ConversacionInstagram mensajeConfirmacion) {

        ContactoInstagram contacto =
                mensajeConfirmacion.getContacto();

        Optional<ConversacionInstagram> pendienteOpt =
                conversacionRepository
                        .findFirstByContactoAndEstadoOrderByFechaDesc(
                                contacto,
                                EstadoConversacionInstagram
                                        .PENDIENTE_CONFIRMACION
                        );

        if (pendienteOpt.isEmpty()) {

            String respuesta =
                    "No encuentro un pedido pendiente de confirmación. "
                    + "Envíame primero los productos que deseas comprar.";

            mensajeConfirmacion.setEstado(
                    EstadoConversacionInstagram.IGNORADO
            );
            mensajeConfirmacion.setRespuestaSistema(respuesta);
            conversacionRepository.save(mensajeConfirmacion);

            messagingService.enviarTexto(
                    contacto.getInstagramScopedId(),
                    respuesta
            );
            return;
        }

        ConversacionInstagram pendiente = pendienteOpt.get();

        try {
            CrearPedidoRequest request =
                    objectMapper.readValue(
                            pendiente.getPropuestaJson(),
                            CrearPedidoRequest.class
                    );

            Pedido pedido = pedidoService.crear(request);

            pendiente.setEstado(
                    EstadoConversacionInstagram.CONFIRMADO
            );
            pendiente.setPedidoId(pedido.getId());
            conversacionRepository.save(pendiente);

            String respuesta =
                    "¡Pedido confirmado! 💗 Tu pedido #"
                    + pedido.getId()
                    + " fue registrado por S/ "
                    + pedido.getTotal()
                    + ". Gracias por comprar en Pao Collection.";

            mensajeConfirmacion.setEstado(
                    EstadoConversacionInstagram.CONFIRMADO
            );
            mensajeConfirmacion.setPedidoId(pedido.getId());
            mensajeConfirmacion.setRespuestaSistema(respuesta);
            conversacionRepository.save(mensajeConfirmacion);

            messagingService.enviarTexto(
                    contacto.getInstagramScopedId(),
                    respuesta
            );

        } catch (Exception ex) {

            String respuesta =
                    "No pude confirmar el pedido. "
                    + "Es posible que el stock haya cambiado. "
                    + "Vuelve a enviarme tu solicitud.";

            mensajeConfirmacion.setEstado(
                    EstadoConversacionInstagram.ERROR
            );
            mensajeConfirmacion.setRespuestaSistema(respuesta);
            conversacionRepository.save(mensajeConfirmacion);

            messagingService.enviarTexto(
                    contacto.getInstagramScopedId(),
                    respuesta
            );

            ex.printStackTrace();
        }
    }

    private void cancelarPropuesta(
            ConversacionInstagram mensajeCancelacion) {

        ContactoInstagram contacto =
                mensajeCancelacion.getContacto();

        Optional<ConversacionInstagram> pendienteOpt =
                conversacionRepository
                        .findFirstByContactoAndEstadoOrderByFechaDesc(
                                contacto,
                                EstadoConversacionInstagram
                                        .PENDIENTE_CONFIRMACION
                        );

        pendienteOpt.ifPresent(pendiente -> {
            pendiente.setEstado(
                    EstadoConversacionInstagram.CANCELADO
            );
            conversacionRepository.save(pendiente);
        });

        String respuesta =
                "Solicitud cancelada. Puedes enviarme un nuevo pedido cuando quieras.";

        mensajeCancelacion.setEstado(
                EstadoConversacionInstagram.CANCELADO
        );
        mensajeCancelacion.setRespuestaSistema(respuesta);
        conversacionRepository.save(mensajeCancelacion);

        messagingService.enviarTexto(
                contacto.getInstagramScopedId(),
                respuesta
        );
    }

    private void procesarMensajeNoTextual(
            ContactoInstagram contacto,
            String messageId) {

        ConversacionInstagram conversacion =
                nuevaConversacion(
                        contacto,
                        messageId,
                        "[Mensaje no textual]"
                );

        conversacion.setEstado(
                EstadoConversacionInstagram.IGNORADO
        );

        String respuesta =
                "Por ahora puedo procesar pedidos escritos en texto. "
                + "Cuéntame qué joyas deseas comprar.";

        conversacion.setRespuestaSistema(respuesta);
        conversacionRepository.save(conversacion);

        messagingService.enviarTexto(
                contacto.getInstagramScopedId(),
                respuesta
        );
    }

    private ContactoInstagram obtenerOCrearContacto(
            String igsid) {

        return contactoRepository
                .findByInstagramScopedId(igsid)
                .orElseGet(() -> {

                    ContactoInstagram nuevo =
                            new ContactoInstagram();

                    nuevo.setInstagramScopedId(igsid);
                    nuevo.setNombreReferencia(
                            "Instagram " + abreviar(igsid)
                    );
                    nuevo.setActivo(true);

                    return contactoRepository.save(nuevo);
                });
    }

    private ConversacionInstagram nuevaConversacion(
            ContactoInstagram contacto,
            String messageId,
            String texto) {

        ConversacionInstagram conversacion =
                new ConversacionInstagram();

        conversacion.setInstagramMessageId(messageId);
        conversacion.setContacto(contacto);
        conversacion.setMensajeEntrada(texto);
        conversacion.setEstado(
                EstadoConversacionInstagram.RECIBIDO
        );
        conversacion.setRequiereRevision(false);
        conversacion.setFecha(LocalDateTime.now());

        return conversacion;
    }

    private CrearPedidoRequest convertirAPropuesta(
            InterpretacionMensajeResponse interpretacion,
            Long clienteId) {

        CrearPedidoRequest request =
                new CrearPedidoRequest();

        request.setClienteId(clienteId);

        List<ItemPedidoRequest> items =
                new ArrayList<>();

        interpretacion.getItems().forEach(item -> {
            ItemPedidoRequest detalle =
                    new ItemPedidoRequest();

            detalle.setProductoId(
                    item.getProductoId()
            );
            detalle.setCantidad(
                    item.getCantidad()
            );

            items.add(detalle);
        });

        request.setItems(items);
        return request;
    }

    private String construirResumenConfirmacion(
            InterpretacionMensajeResponse interpretacion) {

        String items = interpretacion.getItems()
                .stream()
                .map(item ->
                        item.getCantidad()
                        + " x "
                        + item.getNombreProducto()
                )
                .collect(Collectors.joining(", "));

        return "Identifiqué: "
                + items
                + ". Total estimado: S/ "
                + interpretacion.getTotalEstimado()
                + ". Responde CONFIRMAR para registrar tu pedido "
                + "o CANCELAR para descartarlo.";
    }

    private boolean esConfirmacion(String texto) {

        String valor = normalizarComando(texto);

        return valor.equals("CONFIRMAR")
                || valor.equals("CONFIRMO")
                || valor.equals("SI CONFIRMO")
                || valor.equals("CONFIRMAR PEDIDO");
    }

    private boolean esCancelacion(String texto) {

        String valor = normalizarComando(texto);

        return valor.equals("CANCELAR")
                || valor.equals("CANCELO")
                || valor.equals("CANCELAR PEDIDO");
    }

    private String normalizarComando(String valor) {

        String sinAcentos = Normalizer
                .normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return sinAcentos
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String abreviar(String igsid) {

        if (igsid.length() <= 8) {
            return igsid;
        }

        return igsid.substring(0, 4)
                + "..."
                + igsid.substring(igsid.length() - 4);
    }
}
