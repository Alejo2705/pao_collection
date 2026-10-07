package com.proyecto.pedidos.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {

    @Bean
    public OpenAIClient openAIClient() {
        // El SDK oficial lee OPENAI_API_KEY desde las variables de entorno.
        return OpenAIOkHttpClient.fromEnv();
    }
}
