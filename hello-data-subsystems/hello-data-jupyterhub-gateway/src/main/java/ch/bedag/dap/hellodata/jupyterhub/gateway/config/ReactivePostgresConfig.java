package ch.bedag.dap.hellodata.jupyterhub.gateway.config;

import ch.bedag.dap.hellodata.jupyterhub.gateway.entities.Permissions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Configuration
@RequiredArgsConstructor
public class ReactivePostgresConfig {

    private final ObjectMapper objectMapper;

    @Bean
    public R2dbcCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new JsonToPermissionsNodeConverter(objectMapper));
        converters.add(new PermissionsToJsonConverter(objectMapper));
        return R2dbcCustomConversions.of(PostgresDialect.INSTANCE, converters);
    }

    @ReadingConverter
    @RequiredArgsConstructor
    static class JsonToPermissionsNodeConverter implements Converter<Json, Permissions> {

        private final ObjectMapper objectMapper;

        @Override
        public Permissions convert(Json json) {
            try {
                return objectMapper.readValue(json.asString().getBytes(StandardCharsets.UTF_8), Permissions.class);
            } catch (IOException e) {
                log.error("Problem while parsing JSON: {}", json, e);
                throw new RuntimeException("Problem while parsing JSON", e); //NOSONAR
            }
        }
    }

    @WritingConverter
    @RequiredArgsConstructor
    static class PermissionsToJsonConverter implements Converter<Permissions, Json> {

        private final ObjectMapper objectMapper;

        @Override
        public Json convert(Permissions source) {
            try {
                return Json.of(objectMapper.writeValueAsString(source));
            } catch (JsonProcessingException e) {
                log.error("Error occurred while serializing map to JSON: {}", source, e);
            }
            return Json.of("");
        }
    }
}
