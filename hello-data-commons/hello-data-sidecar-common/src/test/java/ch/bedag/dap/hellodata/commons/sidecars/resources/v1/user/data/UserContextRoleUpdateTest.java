package ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserContextRoleUpdateTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testDeserializeUserContextRoleUpdate() {
        String jsonPayload = """
                {
                    "email": "some_username@example.com",
                    "username": "some_username",
                    "active": true,
                    "contextRoles": [
                        {"contextKey": "some_key", "parentContextKey": null, "roleName": "NONE"},
                        {"contextKey": "some_key1", "parentContextKey": null, "roleName": "DATA_DOMAIN_VIEWER"}
                    ]
                }
                """;

        try {
            UserContextRoleUpdate result = objectMapper.readValue(jsonPayload, UserContextRoleUpdate.class);
            assertNotNull(result);
            assertEquals("some_username@example.com", result.getEmail());
            assertEquals("some_username", result.getUsername());
            assertNotNull(result.getContextRoles());
        } catch (JsonProcessingException e) {
            fail("Deserialization failed: " + e.getMessage());
        }
    }
}