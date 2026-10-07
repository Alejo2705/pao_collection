package com.proyecto.pedidos.dto.instagram;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InstagramStatusResponse {

    private boolean configurado;
    private boolean accessTokenConfigurado;
    private boolean instagramUserIdConfigurado;
    private boolean appSecretConfigurado;
    private boolean verifyTokenConfigurado;
    private String graphVersion;
    private String webhookPath;
}
