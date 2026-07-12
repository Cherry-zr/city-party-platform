package com.cityparty.database;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseSchemaContractTest {

    @Test
    void signupTableKeepsUniqueActivityUserConstraint() throws IOException {
        String schema = new String(
                getClass().getResourceAsStream("/schema.sql").readAllBytes(),
                StandardCharsets.UTF_8
        ).toLowerCase();

        assertThat(schema).contains("unique key uk_signup_activity_user");
        assertThat(schema).contains("activity_id, user_id");
    }
}
