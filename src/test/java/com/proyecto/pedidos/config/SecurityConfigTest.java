package com.proyecto.pedidos.config;

import com.proyecto.pedidos.controller.AuthController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, properties = {
        "ADMIN_USERNAME=test-admin", "ADMIN_PASSWORD=only-for-tests-12345" })
@Import({SecurityConfig.class, SecurityConfigTest.Probe.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SecurityConfigTest {
    @Autowired MockMvc mvc;

    @RestController
    static class Probe {
        @GetMapping("/api/probe") String getProbe() { return "private"; }
        @PostMapping("/api/probe") String postProbe() { return "private"; }
        @PostMapping("/api/instagram/webhook") String webhook() { return "public-filter-only"; }
    }

    @Test @Order(1)
    void anonymousApiIsDeniedAndDashboardRedirects() throws Exception {
        mvc.perform(get("/api/probe")).andExpect(status().isUnauthorized());
        mvc.perform(get("/")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login.html"));
        mvc.perform(get("/app.js")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/instagram/status")).andExpect(status().isUnauthorized());
    }

    @Test @Order(2)
    void publicPagesAndCsrfRemainAccessible() throws Exception {
        mvc.perform(get("/login.html")).andExpect(status().isOk());
        mvc.perform(get("/politica-privacidad.html")).andExpect(status().isOk());
        mvc.perform(get("/eliminacion-datos.html")).andExpect(status().isOk());
        mvc.perform(get("/auth/csrf")).andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test @Order(3)
    void correctLoginCreatesSessionAndLogoutRevokesIt() throws Exception {
        var result = mvc.perform(post("/login").with(csrf()).param("username", "test-admin")
                        .param("password", "only-for-tests-12345"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/")).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get("/api/probe").session(session)).andExpect(status().isOk());
        mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/login.html?logout"));
        assertTrue(session.isInvalid());
    }

    @Test @Order(4)
    void wrongPasswordDoesNotAuthenticate() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("username", "test-admin").param("password", "wrong"))
                .andExpect(redirectedUrl("/login.html?error"));
    }

    @Test @Order(5)
    void mutationsLoginAndLogoutRequireCsrf() throws Exception {
        mvc.perform(post("/api/probe").with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/probe").with(user("test-admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(post("/login")).andExpect(status().isForbidden());
        mvc.perform(post("/logout").with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test @Order(6)
    void onlyExactWebhookPostBypassesBrowserCsrf() throws Exception {
        mvc.perform(post("/api/instagram/webhook")).andExpect(status().isOk());
        mvc.perform(post("/api/instagram/webhook/other")).andExpect(status().isForbidden());
    }

    @Test @Order(7)
    void missingPasswordHasNoDefaultAccount() {
        var users = new SecurityConfig().adminUsers("admin", "");
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> users.loadUserByUsername("admin"));
        assertThrows(IllegalArgumentException.class,
                () -> new SecurityConfig().adminUsers("admin", "short"));
    }

    @Test @Order(8)
    void excessiveLoginsAreLimited() throws Exception {
        for (int i = 0; i < 10; i++) {
            mvc.perform(post("/login").with(csrf()).param("username", "unknown").param("password", "wrong"));
        }
        mvc.perform(post("/login").with(csrf()).param("username", "unknown").param("password", "wrong"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "60"));
    }
}
