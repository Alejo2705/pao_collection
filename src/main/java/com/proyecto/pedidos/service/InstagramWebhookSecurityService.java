package com.proyecto.pedidos.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class InstagramWebhookSecurityService {

    private final String appSecret;

    public InstagramWebhookSecurityService(
            @Value("${instagram.app-secret:}") String appSecret) {
        this.appSecret = appSecret;
    }

    public boolean validarFirma(String rawBody, String signatureHeader) {

        if (appSecret == null || appSecret.isBlank()) {
            return false;
        }

        if (signatureHeader == null
                || !signatureHeader.startsWith("sha256=")) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(new SecretKeySpec(
                    appSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));

            byte[] digest = mac.doFinal(
                    rawBody.getBytes(StandardCharsets.UTF_8)
            );

            String calculada = "sha256="
                    + HexFormat.of().formatHex(digest);

            return MessageDigest.isEqual(
                    calculada.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8)
            );

        } catch (Exception ex) {
            return false;
        }
    }
}
