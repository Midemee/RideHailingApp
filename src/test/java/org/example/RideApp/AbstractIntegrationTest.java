package org.example.RideApp;

import org.example.RideApp.data.repositories.DriverRepository;
import org.example.RideApp.data.repositories.PassengerRepository;
import org.example.RideApp.data.repositories.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.mongodb.MongoDBContainer;

import java.util.List;
import java.util.Map;

/**
 * Starts the real application on a random port against a real MongoDB running in a Testcontainers container.
 * The container is a JVM-wide singleton shared by every test class, so the Spring context is cached
 * and MongoDB is only started once. Docker must be running to execute these tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    private static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    static {
        MONGO.start();
    }

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> MONGO.getReplicaSetUrl("ride_app_test"));
    }

    @Value("${local.server.port}")
    private int port;

    @Autowired
    protected PassengerRepository passengerRepository;
    @Autowired
    protected DriverRepository driverRepository;
    @Autowired
    protected RideRepository rideRepository;

    private RestClient client;

    @BeforeEach
    void cleanDatabaseAndCreateClient() {
        rideRepository.deleteAll();
        passengerRepository.deleteAll();
        driverRepository.deleteAll();
        client = RestClient.create("http://localhost:" + port);
    }

    /** Status code plus the parsed ApiResponse JSON ({"successful": .., "data": ..}). */
    protected record Resp(int status, Map<String, Object> body) {
        public boolean successful() {
            return Boolean.TRUE.equals(body.get("successful"));
        }

        public Object data() {
            return body.get("data");
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> dataMap() {
            return (Map<String, Object>) body.get("data");
        }

        @SuppressWarnings("unchecked")
        public List<Map<String, Object>> dataList() {
            return (List<Map<String, Object>>) body.get("data");
        }

        public String dataString(String key) {
            return String.valueOf(dataMap().get(key));
        }
    }

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP = new ParameterizedTypeReference<>() {
    };

    protected Resp post(String path, Object body) {
        return client.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(body)
                .exchange((request, response) -> new Resp(response.getStatusCode().value(), response.bodyTo(JSON_MAP)));
    }

    protected Resp post(String path) {
        return client.post().uri(path)
                .exchange((request, response) -> new Resp(response.getStatusCode().value(), response.bodyTo(JSON_MAP)));
    }

    protected Resp patch(String path, Object body) {
        return client.patch().uri(path).contentType(MediaType.APPLICATION_JSON).body(body)
                .exchange((request, response) -> new Resp(response.getStatusCode().value(), response.bodyTo(JSON_MAP)));
    }

    protected Resp get(String path) {
        return client.get().uri(path)
                .exchange((request, response) -> new Resp(response.getStatusCode().value(), response.bodyTo(JSON_MAP)));
    }

    // ---- scenario helpers ----

    protected static Map<String, Object> registration(String name, String email) {
        return Map.of("name", name, "email", email, "phoneNumber", "08012345678", "password", "secret123");
    }

    /** Registers and logs in a passenger; returns the passenger id. */
    protected String loggedInPassenger(String name, String email) {
        String id = post("/api/passengers/register", registration(name, email)).dataString("id");
        post("/api/passengers/login", Map.of("email", email, "password", "secret123"));
        return id;
    }

    /** Registers and logs in a driver; returns the driver id. */
    protected String loggedInDriver(String name, String email) {
        String id = post("/api/drivers/register", registration(name, email)).dataString("id");
        post("/api/drivers/login", Map.of("email", email, "password", "secret123"));
        return id;
    }

    /** Passenger requests a ride; returns the full response. */
    protected Resp requestRide(String passengerId) {
        return post("/api/passengers/" + passengerId + "/rides",
                Map.of("pickUpLocation", "Yaba", "dropOffLocation", "Lekki"));
    }
}
