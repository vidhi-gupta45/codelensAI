package com.codelens.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class EmbeddingConfig {

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        TransformersEmbeddingModel model = new TransformersEmbeddingModel();
        try {
            model.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize TransformersEmbeddingModel", e);
        }
        return model;
    }
}
