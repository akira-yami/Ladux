package org.akira.ladux.shared.config;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class JacksonConfig {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    @Bean
    public JsonMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule("LaduxTimeModule");

            module.addSerializer(Instant.class, new ValueSerializer<>() {
                @Override
                public void serialize(Instant value, JsonGenerator gen, SerializationContext context)
                        throws JacksonException {
                    gen.writeString(DATE_TIME_FORMATTER.format(value.atZone(APP_ZONE)));
                }
            });

            module.addDeserializer(Instant.class, new ValueDeserializer<>() {
                @Override
                public Instant deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
                    String value = parser.getString();
                    if (value == null || value.isBlank()) {
                        return null;
                    }
                    try {
                        return LocalDateTime.parse(value.trim(), DATE_TIME_FORMATTER)
                                .atZone(APP_ZONE)
                                .toInstant();
                    } catch (DateTimeParseException ignored) {
                        return Instant.parse(value.trim());
                    }
                }
            });

            module.addSerializer(LocalDateTime.class, new ValueSerializer<>() {
                @Override
                public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext context)
                        throws JacksonException {
                    gen.writeString(DATE_TIME_FORMATTER.format(value));
                }
            });

            module.addDeserializer(LocalDateTime.class, new ValueDeserializer<>() {
                @Override
                public LocalDateTime deserialize(JsonParser parser, DeserializationContext context)
                        throws JacksonException {
                    return LocalDateTime.parse(parser.getString().trim(), DATE_TIME_FORMATTER);
                }
            });

            builder.addModule(module);
        };
    }
}
