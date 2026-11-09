package org.example.RideApp;

import org.example.RideApp.data.models.Driver;
import org.example.RideApp.data.models.Ride;
import org.example.RideApp.data.models.RideStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercises the derived queries directly against the MongoDB container. */
class RideRepositoryIntegrationTest extends AbstractIntegrationTest {

    private Ride ride(String passengerId, String driverId, RideStatus status) {
        Ride ride = new Ride();
        ride.setPassengerId(passengerId);
        ride.setDriverId(driverId);
        ride.setPickUpLocation("A");
        ride.setDropOffLocation("B");
        ride.setStatus(status);
        return rideRepository.save(ride);
    }

    @Test
    void savedRideGetsGeneratedIdAndRoundTrips() {
        Ride saved = ride("p1", "d1", RideStatus.REQUESTED);

        assertThat(saved.getId()).isNotBlank();
        Ride loaded = rideRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getPassengerId()).isEqualTo("p1");
        assertThat(loaded.getStatus()).isEqualTo(RideStatus.REQUESTED);
        assertThat(loaded.getAmountPaid()).isZero();
    }

    @Test
    void findByPassengerIdAndByDriverIdAndStatus() {
        ride("p1", "d1", RideStatus.REQUESTED);
        ride("p1", "d1", RideStatus.COMPLETED);
        ride("p2", "d1", RideStatus.REQUESTED);
        ride("p2", "d2", RideStatus.REQUESTED);

        assertThat(rideRepository.findByPassengerId("p1")).hasSize(2);
        List<Ride> pending = rideRepository.findByDriverIdAndStatus("d1", RideStatus.REQUESTED);
        assertThat(pending).hasSize(2).allMatch(r -> "d1".equals(r.getDriverId()));
    }

    @Test
    void existsByStatusIn() {
        ride("p1", "d1", RideStatus.CANCELLED);
        ride("p1", "d1", RideStatus.ONGOING);

        assertThat(rideRepository.existsByPassengerIdAndStatusIn("p1", Set.of(RideStatus.ONGOING))).isTrue();
        assertThat(rideRepository.existsByPassengerIdAndStatusIn("p1", Set.of(RideStatus.REQUESTED))).isFalse();
        assertThat(rideRepository.existsByDriverIdAndStatusIn("d1", Set.of(RideStatus.ACCEPTED, RideStatus.ONGOING))).isTrue();
        assertThat(rideRepository.existsByDriverIdAndStatusIn("d2", Set.of(RideStatus.ONGOING))).isFalse();
    }

    @Test
    void findsOnlyAvailableLoggedInDrivers() {
        Driver offline = driver("offline@example.com", true, false);
        Driver busy = driver("busy@example.com", false, true);
        assertThat(driverRepository.findFirstByAvailableTrueAndLoggedInTrue()).isEmpty();

        Driver ready = driver("ready@example.com", true, true);

        assertThat(driverRepository.findFirstByAvailableTrueAndLoggedInTrue()).get()
                .extracting(Driver::getId).isEqualTo(ready.getId());
        assertThat(offline.getId()).isNotEqualTo(busy.getId());
    }

    private Driver driver(String email, boolean available, boolean loggedIn) {
        Driver driver = new Driver();
        driver.setName("D");
        driver.setEmail(email);
        driver.setPhoneNumber("1");
        driver.setPassword("x");
        driver.setAvailable(available);
        driver.setLoggedIn(loggedIn);
        return driverRepository.save(driver);
    }
}
