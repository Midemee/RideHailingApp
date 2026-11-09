package org.example.RideApp;

import org.example.RideApp.data.models.Passenger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PassengerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerCreatesPassengerAndNeverReturnsPassword() {
        Resp response = post("/api/passengers/register", registration("Ada", "ada@example.com"));

        assertThat(response.status()).isEqualTo(201);
        assertThat(response.successful()).isTrue();
        assertThat(response.dataString("name")).isEqualTo("Ada");
        assertThat(response.dataString("email")).isEqualTo("ada@example.com");
        assertThat(response.dataMap()).doesNotContainKey("password");
        assertThat(passengerRepository.count()).isEqualTo(1);
    }

    @Test
    void passwordIsStoredHashed() {
        post("/api/passengers/register", registration("Ada", "ada@example.com"));

        Passenger stored = passengerRepository.findByEmail("ada@example.com").orElseThrow();
        assertThat(stored.getPassword()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", stored.getPassword())).isTrue();
    }

    @Test
    void emailIsNormalizedAndDuplicatesAreRejected() {
        post("/api/passengers/register", registration("Ada", "ada@example.com"));

        Resp duplicate = post("/api/passengers/register", registration("Ada Two", "  ADA@Example.com "));

        assertThat(duplicate.status()).isEqualTo(409);
        assertThat(duplicate.successful()).isFalse();
        assertThat(duplicate.data()).isEqualTo("Email already exists");
        assertThat(passengerRepository.count()).isEqualTo(1);
    }

    @Test
    void registerRejectsMissingAndInvalidFields() {
        Map<String, Object> noName = new HashMap<>(registration("x", "a@example.com"));
        noName.remove("name");
        Map<String, Object> badEmail = registration("Ada", "not-an-email");
        Map<String, Object> shortPassword = new HashMap<>(registration("Ada", "b@example.com"));
        shortPassword.put("password", "123");

        assertThat(post("/api/passengers/register", noName).data()).isEqualTo("Name is required");
        assertThat(post("/api/passengers/register", badEmail).data()).isEqualTo("Email is not valid");
        assertThat(post("/api/passengers/register", shortPassword).status()).isEqualTo(400);
        assertThat(passengerRepository.count()).isZero();
    }

    @Test
    void malformedJsonReturnsBadRequest() {
        Resp response = post("/api/passengers/register", "this is not json {");

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.successful()).isFalse();
    }

    @Test
    void loginWithCorrectCredentialsMarksPassengerLoggedIn() {
        String id = post("/api/passengers/register", registration("Ada", "ada@example.com")).dataString("id");

        Resp response = post("/api/passengers/login", Map.of("email", "ADA@example.com", "password", "secret123"));

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.dataString("id")).isEqualTo(id);
        assertThat(response.dataString("message")).isEqualTo("Login successful");
        assertThat(passengerRepository.findById(id).orElseThrow().isLoggedIn()).isTrue();
    }

    @Test
    void loginWithWrongPasswordOrUnknownEmailIsUnauthorized() {
        post("/api/passengers/register", registration("Ada", "ada@example.com"));

        Resp wrongPassword = post("/api/passengers/login", Map.of("email", "ada@example.com", "password", "wrong-one"));
        Resp unknownEmail = post("/api/passengers/login", Map.of("email", "nobody@example.com", "password", "secret123"));

        assertThat(wrongPassword.status()).isEqualTo(401);
        assertThat(unknownEmail.status()).isEqualTo(401);
        assertThat(wrongPassword.data()).isEqualTo(unknownEmail.data()); // does not reveal which one was wrong
    }

    @Test
    void logoutClearsLoggedInFlag() {
        String id = loggedInPassenger("Ada", "ada@example.com");

        Resp response = post("/api/passengers/logout", Map.of("userId", id));

        assertThat(response.status()).isEqualTo(200);
        assertThat(passengerRepository.findById(id).orElseThrow().isLoggedIn()).isFalse();
    }

    @Test
    void logoutOfUnknownPassengerIsNotFound() {
        assertThat(post("/api/passengers/logout", Map.of("userId", "does-not-exist")).status()).isEqualTo(404);
    }

    @Test
    void rideOperationsRequireLogin() {
        String id = post("/api/passengers/register", registration("Ada", "ada@example.com")).dataString("id");

        Resp response = requestRide(id);

        assertThat(response.status()).isEqualTo(401);
        assertThat(rideRepository.count()).isZero();
    }
}
