package org.example.RideApp;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DriverIntegrationTest extends AbstractIntegrationTest {

    @Test
    void registerCreatesAvailableDriverAndNeverReturnsPassword() {
        Resp response = post("/api/drivers/register", registration("Bola", "bola@example.com"));

        assertThat(response.status()).isEqualTo(201);
        assertThat(response.dataString("name")).isEqualTo("Bola");
        assertThat(response.dataMap().get("available")).isEqualTo(true);
        assertThat(response.dataMap()).doesNotContainKey("password");
        assertThat(driverRepository.findByEmail("bola@example.com").orElseThrow().getPassword())
                .isNotEqualTo("secret123");
    }

    @Test
    void duplicateEmailIsRejected() {
        post("/api/drivers/register", registration("Bola", "bola@example.com"));

        assertThat(post("/api/drivers/register", registration("Bola 2", "bola@example.com")).status()).isEqualTo(409);
        assertThat(driverRepository.count()).isEqualTo(1);
    }

    @Test
    void sameEmailMayBeUsedForPassengerAndDriver() {
        assertThat(post("/api/passengers/register", registration("Sam", "sam@example.com")).status()).isEqualTo(201);
        assertThat(post("/api/drivers/register", registration("Sam", "sam@example.com")).status()).isEqualTo(201);
    }

    @Test
    void registerRejectsMissingPhoneNumber() {
        Map<String, Object> body = new HashMap<>(registration("Bola", "bola@example.com"));
        body.remove("phoneNumber");

        Resp response = post("/api/drivers/register", body);

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.data()).isEqualTo("PhoneNumber is required");
    }

    @Test
    void loginAndLogout() {
        String id = post("/api/drivers/register", registration("Bola", "bola@example.com")).dataString("id");

        assertThat(post("/api/drivers/login", Map.of("email", "bola@example.com", "password", "bad-password"))
                .status()).isEqualTo(401);
        assertThat(post("/api/drivers/login", Map.of("email", "bola@example.com", "password", "secret123"))
                .status()).isEqualTo(200);
        assertThat(driverRepository.findById(id).orElseThrow().isLoggedIn()).isTrue();

        assertThat(post("/api/drivers/logout", Map.of("userId", id)).status()).isEqualTo(200);
        assertThat(driverRepository.findById(id).orElseThrow().isLoggedIn()).isFalse();
    }

    @Test
    void driverCanToggleAvailabilityOnlyWhenLoggedIn() {
        String id = post("/api/drivers/register", registration("Bola", "bola@example.com")).dataString("id");
        assertThat(patch("/api/drivers/" + id + "/availability", Map.of("available", false)).status()).isEqualTo(401);

        post("/api/drivers/login", Map.of("email", "bola@example.com", "password", "secret123"));
        Resp off = patch("/api/drivers/" + id + "/availability", Map.of("available", false));

        assertThat(off.status()).isEqualTo(200);
        assertThat(off.dataMap().get("available")).isEqualTo(false);
        assertThat(driverRepository.findById(id).orElseThrow().isAvailable()).isFalse();
        assertThat(patch("/api/drivers/" + id + "/availability", Map.of()).status()).isEqualTo(400);
    }
}
