package com.proyecto.pedidos.dto.instagram;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InstagramAccountResponse(
        String id,
        String username,
        String name,
        @JsonProperty("profile_picture_url") String profilePictureUrl) {
}
