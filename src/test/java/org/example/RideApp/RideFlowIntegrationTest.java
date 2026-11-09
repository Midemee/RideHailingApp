package org.example.RideApp;

import org.example.RideApp.data.models.Ride;
import org.example.RideApp.data.models.RideStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** End-to-end ride lifecycle: REQUESTED -> ACCEPTED -> ONGOING -> COMPLETED (+ REJECTED / CANCELLED). */
class RideFlowIntegrationTest extends AbstractIntegrationTest {

    private String ridePath(String passengerId, String rideId) {
        return "/api/passengers/" + passengerId + "/rides/" + rideId;
    }

    private String driverRidePath(String driverId, String rideId, String action) {
        return "/api/drivers/" + driverId + "/rides/" + rideId + "/" + action;
    }

    @Test
    void fullHappyPath() {
        String passengerId = loggedInPassenger("Ada", "ada@example.com");
        String driverId = loggedInDriver("Bola", "bola@example.com");

        // passenger requests a ride -> assigned to the only available driver
        Resp requested = requestRide(passengerId);
        assertThat(requested.status()).isEqualTo(201);
        assertThat(requested.dataString("status")).isEqualTo("REQUESTED");
        assertThat(requested.dataString("driverId")).isEqualTo(driverId);
        assertThat(requested.dataString("pickUpLocation")).isEqualTo("Yaba");
        String rideId = requested.dataString("id");

        // passenger cannot see the driver yet
        assertThat(get(ridePath(passengerId, rideId) + "/driver").status()).isEqualTo(409);

        // driver sees the request, including who is asking
        Resp pending = get("/api/drivers/" + driverId + "/requests");
        assertThat(pending.status()).isEqualTo(200);
        assertThat(pending.dataList()).hasSize(1);
        assertThat(pending.dataList().get(0).get("passengerName")).isEqualTo("Ada");
        assertThat(pending.dataList().get(0).get("rideId")).isEqualTo(rideId);

        // accept
        Resp accepted = post(driverRidePath(driverId, rideId, "accept"));
        assertThat(accepted.status()).isEqualTo(200);
        assertThat(accepted.dataString("status")).isEqualTo("ACCEPTED");
        assertThat(driverRepository.findById(driverId).orElseThrow().isAvailable()).isFalse();
        assertThat(get("/api/drivers/" + driverId + "/requests").dataList()).isEmpty();

        // passenger can now see the driver, without credentials
        Resp info = get(ridePath(passengerId, rideId) + "/driver");
        assertThat(info.status()).isEqualTo(200);
        assertThat(info.dataString("name")).isEqualTo("Bola");
        assertThat(info.dataMap()).doesNotContainKeys("password", "email");

        // start and end
        assertThat(post(driverRidePath(driverId, rideId, "start")).dataString("status")).isEqualTo("ONGOING");
        assertThat(post(driverRidePath(driverId, rideId, "end")).dataString("status")).isEqualTo("COMPLETED");
        assertThat(driverRepository.findById(driverId).orElseThrow().isAvailable()).isTrue();

        // pay
        Resp paid = post(ridePath(passengerId, rideId) + "/payment", Map.of("amount", 2500.50));
        assertThat(paid.status()).isEqualTo(200);
        assertThat(((Number) paid.dataMap().get("amountPaid")).doubleValue()).isEqualTo(2500.50);

        Ride stored = rideRepository.findById(rideId).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(stored.getAmountPaid()).isEqualTo(2500.50);

        // history
        Resp history = get("/api/passengers/" + passengerId + "/rides");
        assertThat(history.dataList()).hasSize(1);
    }

    @Test
    void requestFailsWhenNoDriverIsAvailable() {
        String passengerId = loggedInPassenger("Ada", "ada@example.com");

        Resp response = requestRide(passengerId);

        assertThat(response.status()).isEqualTo(409);
        assertThat(response.data()).isEqualTo("No driver is currently available");
        assertThat(rideRepository.count()).isZero();
    }

    @Test
    void offlineOrBusyDriversAreNotAssigned() {
        String passengerId = loggedInPassenger("Ada", "ada@example.com");
        String driverId = loggedInDriver("Bola", "bola@example.com");
        patch("/api/drivers/" + driverId + "/availability", Map.of("available", false));
        assertThat(requestRide(passengerId).status()).isEqualTo(409);

        patch("/api/drivers/" + driverId + "/availability", Map.of("available", true));
        post("/api/drivers/logout", Map.of("userId", driverId));
        assertThat(requestRide(passengerId).status()).isEqualTo(409);
    }

    @Test
    void passengerCannotHaveTwoActiveRides() {
        String passengerId = loggedInPassenger("Ada", "ada@example.com");
        loggedInDriver("Bola", "bola@example.com");
        assertThat(requestRide(passengerId).status()).isEqualTo(201);

        Resp second = requestRide(passengerId);

        assertThat(second.status()).isEqualTo(409);
        assertThat(second.data()).isEqualTo("You already have an active ride");
        assertThat(rideRepository.count()).isEqualTo(1);
    }

    @Test
    void requestValidatesLocations() {
        String passengerId = loggedInPassenger("Ada", "ada@example.com");
        loggedInDriver("Bola", "bola@example.com");

        Resp response = post("/api/passengers/" + passengerId + "/rides", Map.of("pickUpLocation", "Yaba"));

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.data()).isEqualTo("DropOffLocation is required");
    }

    @Test
    void driverCanRejectAndPassengerMayRequestAgain() {
        String passengerId = loggedInPassenger("Ada", "ada@example.com");
        String driverId = loggedInDriver("Bola", "bola@example.com");
        String rideId = requestRide(passengerId).dataString("id");

        Resp rejected = post(driverRidePath(driverId, rideId, "reject"));

        assertThat(rejected.status()).isEqualTo(200);
        assertThat(rejected.dataString("status")).isEqualTo("REJECTED");
        assertThat(driverRepository.findById(driverId).orElseThrow().isAvailable()).isTrue();
        assertThat(post(driverRidePath(driverId, rideId, "accept")).status()).isEqualTo(409);
        assertThat(requestRide(passengerId).status()).isEqualTo(201);
    }

    @Test
    void passengerCanCancelRequestedRide() {
        String passengerId = loggedInPassenger("Ada", "ada@example.com");
        String driverId = loggedInDriver("Bola", "bola@example.com");
        String rideId = requestRide(passengerId).dataString("id");

        Resp cancelled = post(ridePath(passengerId, rideId) + "/cancel");

        assertThat(cancelled.status()).isEqualTo(200);
        assertThat(cancelled.dataString("status")).isEqualTo("CANCELLED");
        assertThat(post(driverRidePath(driverId, rideId, "accept")).status()).isEqualTo(409);
    }

    @Test
    void cancellingAnAcceptedRideFreesTheDriver() {
        String passengerId = loggedInPassenger("Ada", "ada@example.com");
        String driverId = loggedInDriver("Bola", "bola@example.com");
        String rideId = requestRide(passengerId).dataString("id");
        post(driverRidePath(driverId, rideId, "accept"));
        assertThat(driverRepository.findById(driverId).orElseThrow().isAvailable()).isFalse();

        Resp cancelled = post(ridePath(passengerId, rideId) + "/cancel");

        assertThat(cancelled.status()).isEqualTo(200);
        assertThat(driverRepository.findById(driverId).orElseThrow().isAvailable()).isTrue();
    }

    @Test
    void ongoingRideCannotBeCancelled() {
        String passengerId = loggedInPassenger("Ada", "ada@example.com");
        String driverId = loggedInDriver("Bola", "bola@example.com");
        String rideId = requestRide(passengerId).dataString("id");
        post(driverRidePath(driverId, rideId, "accept"));
        post(driverRidePath(driverId, rideId, "start"));

        assertThat(post(ridePath(passengerId, rideId) + "/cancel").status()).isEqualTo(409);
        assertThat(rideRepository.findById(rideId).orElseThrow().getStatus()).isEqualTo(RideStatus.ONGOING);
    }

    @Test
    void rideStepsCannotBeSkippedOrRepeated() {
        String passengerId = loggedInPassenger("Ada", "ada@example.com");
        String driverId = loggedInDriver("Bola", "bola@example.com");
        String rideId = requestRide(passengerId).dataString("id");

        assertThat(post(driverRidePath(driverId, rideId, "start")).status()).isEqualTo(409); // not accepted yet
        assertThat(post(driverRidePath(driverId, rideId, "end")).status()).isEqualTo(409);

        post(driverRidePath(driverId, rideId, "accept"));
        assertThat(post(driverRidePath(driverId, rideId, "accept")).status()).isEqualTo(409); // twice
        assertThat(post(driverRidePath(driverId, rideId, "end")).status()).isEqualTo(409);   // not started
    }

    @Test
    void anotherDriverCannotTouchTheRide() {
        String passengerId = loggedInPassenger("Ada", "ada@example.com");
        String driverId = loggedInDriver("Bola", "bola@example.com");
        String rideId = requestRide(passengerId).dataString("id");
        String otherDriverId = loggedInDriver("Chidi", "chidi@example.com");

        assertThat(post(driverRidePath(otherDriverId, rideId, "accept")).status()).isEqualTo(404);
        assertThat(get("/api/drivers/" + otherDriverId + "/requests").dataList()).isEmpty();
        assertThat(rideRepository.findById(rideId).orElseThrow().getDriverId()).isEqualTo(driverId);
    }

    @Test
    void anotherPassengerCannotSeeCancelOrPayForTheRide() {
        String passengerId = loggedInPassenger("Ada", "ada@example.com");
        String driverId = loggedInDriver("Bola", "bola@example.com");
        String rideId = requestRide(passengerId).dataString("id");
        post(driverRidePath(driverId, rideId, "accept"));
        String intruderId = loggedInPassenger("Eve", "eve@example.com");

        assertThat(get(ridePath(intruderId, rideId) + "/driver").status()).isEqualTo(404);
        assertThat(post(ridePath(intruderId, rideId) + "/cancel").status()).isEqualTo(404);
        assertThat(post(ridePath(intruderId, rideId) + "/payment", Map.of("amount", 10)).status()).isEqualTo(404);
        assertThat(rideRepository.findById(rideId).orElseThrow().getStatus()).isEqualTo(RideStatus.ACCEPTED);
    }

    @Test
    void paymentRules() {
        String passengerId = loggedInPassenger("Ada", "ada@example.com");
        String driverId = loggedInDriver("Bola", "bola@example.com");
        String rideId = requestRide(passengerId).dataString("id");
        String payment = ridePath(passengerId, rideId) + "/payment";

        // not completed yet
        assertThat(post(payment, Map.of("amount", 1000)).status()).isEqualTo(409);

        post(driverRidePath(driverId, rideId, "accept"));
        post(driverRidePath(driverId, rideId, "start"));
        post(driverRidePath(driverId, rideId, "end"));

        assertThat(post(payment, Map.of("amount", 0)).status()).isEqualTo(400);
        assertThat(post(payment, Map.of("amount", -5)).status()).isEqualTo(400);
        assertThat(post(payment, Map.of()).status()).isEqualTo(400);
        assertThat(post(payment, Map.of("amount", 1000)).status()).isEqualTo(200);
        Resp twice = post(payment, Map.of("amount", 1000));
        assertThat(twice.status()).isEqualTo(409);
        assertThat(twice.data()).isEqualTo("Ride has already been paid for");
    }

    @Test
    void driverCannotLogOutMidRide() {
        String passengerId = loggedInPassenger("Ada", "ada@example.com");
        String driverId = loggedInDriver("Bola", "bola@example.com");
        String rideId = requestRide(passengerId).dataString("id");
        post(driverRidePath(driverId, rideId, "accept"));

        assertThat(post("/api/drivers/logout", Map.of("userId", driverId)).status()).isEqualTo(409);
        assertThat(patch("/api/drivers/" + driverId + "/availability", Map.of("available", true)).status())
                .isEqualTo(409);
    }

    @Test
    void unknownRideIsNotFound() {
        String passengerId = loggedInPassenger("Ada", "ada@example.com");
        String driverId = loggedInDriver("Bola", "bola@example.com");

        assertThat(post(ridePath(passengerId, "nope") + "/cancel").status()).isEqualTo(404);
        assertThat(post(driverRidePath(driverId, "nope", "accept")).status()).isEqualTo(404);
    }
}
