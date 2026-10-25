package org.akira.ladux.assistant.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ChatBotConfig {
    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel,
            @org.springframework.beans.factory.annotation.Value("${spring.ai.vectorstore.pgvector.initialize-schema:true}") boolean initializeSchema) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(3072)
                .indexType(PgVectorStore.PgIndexType.NONE)
                .initializeSchema(initializeSchema)
                .build();
    }
}
