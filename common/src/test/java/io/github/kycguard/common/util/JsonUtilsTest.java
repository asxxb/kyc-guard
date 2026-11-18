package io.github.kycguard.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JsonUtilsTest {

    @Test
    @DisplayName("Should provide configured singleton mapper")
    void shouldProvideConfiguredMapper() {
        assertThat(JsonUtils.mapper()).isNotNull();
    }

    @Test
    @DisplayName("Should handle serialization errors gracefully")
    void shouldHandleSerializationErrors() {
        Object selfReferencing = new Object() {
            public Object getSelf() {
                return this;
            }
        };

        assertThatThrownBy(() -> JsonUtils.toJson(selfReferencing))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to serialize object to JSON");
    }

    @Test
    @DisplayName("Should handle deserialization errors gracefully")
    void shouldHandleDeserializationErrors() {
        String invalidJson = "{ invalid-json }";

        assertThatThrownBy(() -> JsonUtils.fromJson(invalidJson, Map.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to deserialize JSON to Map");
    }

    @Test
    @DisplayName("Should serialize and deserialize Instant as ISO-8601 string")
    void shouldSerializeInstantAsIso8601() {
        Instant now = Instant.parse("2026-11-18T12:34:56.789Z");
        Map<String, Instant> data = Map.of("time", now);

        String json = JsonUtils.toJson(data);
        assertThat(json).contains("\"time\":\"2026-11-18T12:34:56.789Z\"");
    }
}
