package org.example.RideApp.services;

import lombok.RequiredArgsConstructor;
import org.example.RideApp.data.models.Driver;
import org.example.RideApp.data.models.Passenger;
import org.example.RideApp.data.models.Ride;
import org.example.RideApp.data.models.RideStatus;
import org.example.RideApp.data.repositories.DriverRepository;
import org.example.RideApp.data.repositories.PassengerRepository;
import org.example.RideApp.data.repositories.RideRepository;
import org.example.RideApp.dtos.requests.AvailabilityRequest;
import org.example.RideApp.dtos.requests.LoginRequest;
import org.example.RideApp.dtos.requests.LogoutRequest;
import org.example.RideApp.dtos.requests.RegisterRequest;
import org.example.RideApp.dtos.responses.DriverResponse;
import org.example.RideApp.dtos.responses.LoginResponse;
import org.example.RideApp.dtos.responses.PassengerRequestResponse;
import org.example.RideApp.dtos.responses.RideResponse;
import org.example.RideApp.exceptions.ConflictException;
import org.example.RideApp.exceptions.InvalidRequestException;
import org.example.RideApp.exceptions.ResourceNotFoundException;
import org.example.RideApp.exceptions.UnauthorizedException;
import org.example.RideApp.utils.Mapper;
import org.example.RideApp.utils.Validator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {
    private static final Set<RideStatus> IN_PROGRESS = Set.of(RideStatus.ACCEPTED, RideStatus.ONGOING);

    private final DriverRepository driverRepository;
    private final PassengerRepository passengerRepository;
    private final RideRepository rideRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public DriverResponse register(RegisterRequest request) {
        Validator.validateRegistration(request);
        String email = Validator.normalizeEmail(request.getEmail());
        if (driverRepository.existsByEmail(email)) throw new ConflictException("Email already exists");
        Driver saved = driverRepository.save(Mapper.mapToDriver(request, passwordEncoder.encode(request.getPassword())));
        return Mapper.mapToDriverResponse(saved);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        Validator.validateLogin(request);
        Driver driver = driverRepository.findByEmail(Validator.normalizeEmail(request.getEmail()))
                .filter(d -> passwordEncoder.matches(request.getPassword(), d.getPassword()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        driver.setLoggedIn(true);
        driverRepository.save(driver);
        return Mapper.mapToLoginResponse(driver.getId(), driver.getName(), driver.getEmail(), "Login successful");
    }

    @Override
    public String logout(LogoutRequest request) {
        if (request == null) throw new InvalidRequestException("Request body is required");
        Validator.requireText(request.getUserId(), "UserId");
        Driver driver = findDriver(request.getUserId());
        if (rideRepository.existsByDriverIdAndStatusIn(driver.getId(), IN_PROGRESS))
            throw new ConflictException("Finish your current ride before logging out");
        driver.setLoggedIn(false);
        driverRepository.save(driver);
        return "Logout successful";
    }

    @Override
    public DriverResponse setAvailability(String driverId, AvailabilityRequest request) {
        Driver driver = findLoggedInDriver(driverId);
        if (request == null || request.getAvailable() == null) throw new InvalidRequestException("Available is required");
        if (request.getAvailable() && rideRepository.existsByDriverIdAndStatusIn(driverId, IN_PROGRESS))
            throw new ConflictException("You cannot become available while on a ride");
        driver.setAvailable(request.getAvailable());
        return Mapper.mapToDriverResponse(driverRepository.save(driver));
    }

    @Override
    public List<PassengerRequestResponse> viewPassengerRequest(String driverId) {
        findLoggedInDriver(driverId);
        return rideRepository.findByDriverIdAndStatus(driverId, RideStatus.REQUESTED).stream()
                .map(ride -> {
                    Passenger passenger = passengerRepository.findById(ride.getPassengerId())
                            .orElseThrow(() -> new ResourceNotFoundException("Passenger not found"));
                    return Mapper.mapToPassengerRequestResponse(ride, passenger);
                })
                .toList();
    }

    @Override
    public RideResponse acceptRide(String driverId, String rideId) {
        Driver driver = findLoggedInDriver(driverId);
        Ride ride = findAssignedRide(driverId, rideId);
        requireStatus(ride, RideStatus.REQUESTED, "accepted");
        if (!driver.isAvailable()) throw new ConflictException("You are not available to take a ride");
        ride.setStatus(RideStatus.ACCEPTED);
        driver.setAvailable(false);
        driverRepository.save(driver);
        return Mapper.mapToRideResponse(rideRepository.save(ride));
    }

    @Override
    public RideResponse rejectRide(String driverId, String rideId) {
        findLoggedInDriver(driverId);
        Ride ride = findAssignedRide(driverId, rideId);
        requireStatus(ride, RideStatus.REQUESTED, "rejected");
        ride.setStatus(RideStatus.REJECTED);
        return Mapper.mapToRideResponse(rideRepository.save(ride));
    }

    @Override
    public RideResponse startRide(String driverId, String rideId) {
        findLoggedInDriver(driverId);
        Ride ride = findAssignedRide(driverId, rideId);
        requireStatus(ride, RideStatus.ACCEPTED, "started");
        ride.setStatus(RideStatus.ONGOING);
        return Mapper.mapToRideResponse(rideRepository.save(ride));
    }

    @Override
    public RideResponse endRide(String driverId, String rideId) {
        Driver driver = findLoggedInDriver(driverId);
        Ride ride = findAssignedRide(driverId, rideId);
        requireStatus(ride, RideStatus.ONGOING, "ended");
        ride.setStatus(RideStatus.COMPLETED);
        driver.setAvailable(true);
        driverRepository.save(driver);
        return Mapper.mapToRideResponse(rideRepository.save(ride));
    }

    private Driver findDriver(String id) {
        return driverRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
    }

    private Driver findLoggedInDriver(String id) {
        Driver driver = findDriver(id);
        if (!driver.isLoggedIn()) throw new UnauthorizedException("Please log in first");
        return driver;
    }

    /** Rides assigned to other drivers are reported as not found rather than leaking their existence. */
    private Ride findAssignedRide(String driverId, String rideId) {
        return rideRepository.findById(rideId)
                .filter(r -> driverId.equals(r.getDriverId()))
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found"));
    }

    private void requireStatus(Ride ride, RideStatus expected, String action) {
        if (ride.getStatus() != expected)
            throw new ConflictException("Ride cannot be " + action + " (status: " + ride.getStatus() + ")");
    }
}
