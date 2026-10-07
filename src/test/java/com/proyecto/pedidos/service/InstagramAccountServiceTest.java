package com.proyecto.pedidos.service;

import com.proyecto.pedidos.controller.InstagramAccountController;
import com.proyecto.pedidos.exception.ServicioInstagramException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InstagramAccountServiceTest {
    private MockRestServiceServer meta;
    private RestClient client;
    private InstagramAccountService service;
    private final String url = "https://graph.instagram.com/v26.0/123456?fields=id,username,name,profile_picture_url";

    @BeforeEach
    void setup() {
        var builder = RestClient.builder().baseUrl("https://graph.instagram.com");
        meta = MockRestServiceServer.bindTo(builder).build();
        client = builder.build();
        service = new InstagramAccountService(client, "test-secret", "123456", "v26.0");
    }

    @Test
    void consultaMetaYEntregaSoloLosCamposDelPerfil() throws Exception {
        meta.expect(requestTo(url)).andExpect(method(HttpMethod.GET))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("Authorization", "Bearer test-secret"))
                .andRespond(withSuccess("""
                        {"id":"123456","username":"cuenta_prueba","name":"Cuenta de prueba",
                        "profile_picture_url":"https://example.com/foto.jpg","access_token":"no-exponer"}
                        """, MediaType.APPLICATION_JSON));
        MockMvcBuilders.standaloneSetup(new InstagramAccountController(service)).build()
                .perform(get("/api/instagram/cuenta"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.id").value("123456"))
                .andExpect(jsonPath("$.username").value("cuenta_prueba"))
                .andExpect(jsonPath("$.name").value("Cuenta de prueba"))
                .andExpect(jsonPath("$.profile_picture_url").value("https://example.com/foto.jpg"))
                .andExpect(jsonPath("$.access_token").doesNotExist());
        meta.verify();
    }

    @Test
    void rechazaConfiguracionIncompletaSinConsultarMeta() {
        assertThrows(ServicioInstagramException.class,
                () -> new InstagramAccountService(client, "", "123456", "v26.0").obtenerCuenta());
        assertThrows(ServicioInstagramException.class,
                () -> new InstagramAccountService(client, "test-secret", "bad/id", "v26.0").obtenerCuenta());
        assertThrows(ServicioInstagramException.class,
                () -> new InstagramAccountService(client, "test-secret", "123456", "bad-version").obtenerCuenta());
        meta.verify();
    }

    @Test
    void errorDeMetaNoFiltraTokenNiCuerpoNiCausa() throws Exception {
        meta.expect(requestTo(url)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"message\":\"test-secret\",\"code\":190}}"));
        var result = MockMvcBuilders.standaloneSetup(new InstagramAccountController(service)).build()
                .perform(get("/api/instagram/cuenta")).andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Cache-Control", "no-store")).andReturn();
        assertFalse(result.getResponse().getContentAsString().contains("test-secret"));
        meta.verify();
    }

    @Test
    void manejaLimiteTemporal() {
        meta.expect(requestTo(url)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        var ex = assertThrows(ServicioInstagramException.class, service::obtenerCuenta);
        assertTrue(ex.getMessage().contains("limitó"));
        assertNull(ex.getCause());
    }

    @Test
    void manejaFalloDeRedSinCausaExterna() {
        meta.expect(requestTo(url)).andRespond(withException(new IOException("test-secret")));
        var ex = assertThrows(ServicioInstagramException.class, service::obtenerCuenta);
        assertFalse(ex.getMessage().contains("test-secret"));
        assertNull(ex.getCause());
    }

    @Test
    void noAceptaRespuestaIncompletaComoCuentaConectada() {
        meta.expect(requestTo(url)).andRespond(withSuccess("{\"id\":\"123456\"}", MediaType.APPLICATION_JSON));
        assertThrows(ServicioInstagramException.class, service::obtenerCuenta);
    }

    @Test
    void fotoYNombreSonOpcionalesSiMetaNoLosDevuelve() {
        meta.expect(requestTo(url)).andRespond(withSuccess(
                "{\"id\":\"123456\",\"username\":\"cuenta_prueba\"}", MediaType.APPLICATION_JSON));
        var cuenta = service.obtenerCuenta();
        assertNull(cuenta.name());
        assertNull(cuenta.profilePictureUrl());
    }

    @Test
    void respetaVersionConfigurada() {
        meta.expect(requestTo(url.replace("v26.0", "v25.0")))
                .andRespond(withSuccess("{\"id\":\"123456\",\"username\":\"prueba\"}", MediaType.APPLICATION_JSON));
        new InstagramAccountService(client, "test-secret", "123456", "v25.0").obtenerCuenta();
        meta.verify();
    }
}
