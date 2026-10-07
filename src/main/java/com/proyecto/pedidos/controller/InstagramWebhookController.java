package com.proyecto.pedidos.controller;

import com.proyecto.pedidos.service.InstagramWebhookSecurityService;
import com.proyecto.pedidos.service.InstagramWebhookService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/instagram/webhook")
public class InstagramWebhookController {

    private final InstagramWebhookService webhookService;
    private final InstagramWebhookSecurityService securityService;
    private final String verifyToken;

    public InstagramWebhookController(
            InstagramWebhookService webhookService,
            InstagramWebhookSecurityService securityService,
            @Value("${instagram.verify-token:}") String verifyToken) {

        this.webhookService = webhookService;
        this.securityService = securityService;
        this.verifyToken = verifyToken;
    }

    @GetMapping
    public ResponseEntity<String> verificar(
            @RequestParam(name = "hub.mode", required = false)
            String mode,
            @RequestParam(name = "hub.verify_token", required = false)
            String token,
            @RequestParam(name = "hub.challenge", required = false)
            String challenge) {

        if ("subscribe".equals(mode)
                && verifyToken != null
                && !verifyToken.isBlank()
                && verifyToken.equals(token)) {

            return ResponseEntity.ok(challenge);
        }

        return ResponseEntity.status(403)
                .body("Verification failed");
    }

    @PostMapping
    public ResponseEntity<String> recibirEvento(
            @RequestBody String rawBody,
            @RequestHeader(
                    name = "X-Hub-Signature-256",
                    required = false)
            String signature) {

        if (!securityService.validarFirma(
                rawBody,
                signature)) {

            return ResponseEntity.status(401)
                    .body("Invalid signature");
        }

        webhookService.procesarAsync(rawBody);

        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}
