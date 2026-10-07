package com.proyecto.pedidos.service;

import com.proyecto.pedidos.dto.instagram.InstagramAccountResponse;
import com.proyecto.pedidos.exception.ServicioInstagramException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;

@Service
public class InstagramAccountService {
    private final RestClient restClient;
    private final String accessToken;
    private final String instagramUserId;
    private final String graphVersion;

    @Autowired
    public InstagramAccountService(
            RestClient.Builder builder,
            @Value("${instagram.access-token:}") String accessToken,
            @Value("${instagram.user-id:}") String instagramUserId,
            @Value("${instagram.graph-version:v26.0}") String graphVersion) {
        this(crearCliente(builder), accessToken, instagramUserId, graphVersion);
    }

    private static RestClient crearCliente(RestClient.Builder builder) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        return builder.clone().baseUrl("https://graph.instagram.com")
                .requestFactory(factory).build();
    }

    InstagramAccountService(RestClient restClient, String accessToken, String instagramUserId, String graphVersion) {
        this.restClient = restClient;
        this.accessToken = accessToken == null ? "" : accessToken.trim();
        this.instagramUserId = instagramUserId == null ? "" : instagramUserId.trim();
        this.graphVersion = graphVersion == null ? "" : graphVersion.trim();
    }

    public InstagramAccountResponse obtenerCuenta() {
        if (accessToken.isBlank() || instagramUserId.isBlank()) {
            throw new ServicioInstagramException(
                    "Configura INSTAGRAM_ACCESS_TOKEN e INSTAGRAM_USER_ID para consultar la cuenta.");
        }
        if (!instagramUserId.matches("[0-9]+") || !graphVersion.matches("v[0-9]+\\.[0-9]+")) {
            throw new ServicioInstagramException("Revisa el ID de Instagram y la versión de Graph API configurados.");
        }
        try {
            var cuenta = restClient.get()
                    .uri("/{version}/{id}?fields=id,username,name,profile_picture_url",
                            graphVersion, instagramUserId)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve().body(InstagramAccountResponse.class);
            if (cuenta == null || cuenta.id() == null || !cuenta.id().matches("[0-9]+")
                    || cuenta.username() == null || cuenta.username().isBlank()) {
                throw new ServicioInstagramException("Meta no devolvió un perfil válido. Intenta actualizar la cuenta.");
            }
            return cuenta;
        } catch (RestClientResponseException ex) {
            // Nunca propagar el cuerpo, los encabezados o la causa recibidos de Meta.
            if (ex.getStatusCode().value() == 429) {
                throw new ServicioInstagramException("Meta limitó temporalmente las consultas. Intenta de nuevo más tarde.");
            }
            throw new ServicioInstagramException(
                    "No se pudo verificar la cuenta con Meta. Revisa el token, el ID y el permiso instagram_business_basic.");
        } catch (RestClientException ex) {
            throw new ServicioInstagramException("No se pudo consultar el perfil de Instagram. Intenta de nuevo más tarde.");
        }
    }
}
