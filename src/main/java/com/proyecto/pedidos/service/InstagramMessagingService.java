package com.proyecto.pedidos.service;

import com.proyecto.pedidos.exception.ServicioInstagramException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class InstagramMessagingService {

    private final RestClient restClient;
    private final String accessToken;
    private final String instagramUserId;
    private final String graphVersion;

    public InstagramMessagingService(
            RestClient.Builder restClientBuilder,
            @Value("${instagram.access-token:}") String accessToken,
            @Value("${instagram.user-id:}") String instagramUserId,
            @Value("${instagram.graph-version:v26.0}") String graphVersion) {

        this.restClient = restClientBuilder
                .baseUrl("https://graph.instagram.com")
                .build();

        this.accessToken = accessToken;
        this.instagramUserId = instagramUserId;
        this.graphVersion = graphVersion;
    }

    public void enviarTexto(String instagramScopedId, String mensaje) {

        validarConfiguracion();

        Map<String, Object> recipient = new LinkedHashMap<>();
        recipient.put("id", instagramScopedId);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("text", mensaje);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("recipient", recipient);
        body.put("message", message);

        try {
            restClient.post()
                    .uri("/{version}/{igUserId}/messages",
                            graphVersion,
                            instagramUserId)
                    .headers(headers ->
                            headers.setBearerAuth(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception ex) {
            throw new ServicioInstagramException(
                    "No se pudo enviar el mensaje por Instagram.",
                    ex
            );
        }
    }

    private void validarConfiguracion() {

        if (accessToken == null || accessToken.isBlank()) {
            throw new ServicioInstagramException(
                    "INSTAGRAM_ACCESS_TOKEN no está configurado."
            );
        }

        if (instagramUserId == null || instagramUserId.isBlank()) {
            throw new ServicioInstagramException(
                    "INSTAGRAM_USER_ID no está configurado."
            );
        }
    }
}
