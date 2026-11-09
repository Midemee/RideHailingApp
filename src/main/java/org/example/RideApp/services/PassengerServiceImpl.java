package org.example.RideApp.services;

import lombok.RequiredArgsConstructor;
import org.example.RideApp.data.models.Driver;
import org.example.RideApp.data.models.Passenger;
import org.example.RideApp.data.models.Ride;
import org.example.RideApp.data.models.RideStatus;
import org.example.RideApp.data.repositories.DriverRepository;
import org.example.RideApp.data.repositories.PassengerRepository;
import org.example.RideApp.data.repositories.RideRepository;
import org.example.RideApp.dtos.requests.LoginRequest;
import org.example.RideApp.dtos.requests.LogoutRequest;
import org.example.RideApp.dtos.requests.PaymentRequest;
import org.example.RideApp.dtos.requests.RegisterRequest;
import org.example.RideApp.dtos.requests.RequestRideRequest;
import org.example.RideApp.dtos.responses.DriverInfoResponse;
import org.example.RideApp.dtos.responses.LoginResponse;
import org.example.RideApp.dtos.responses.RideResponse;
import org.example.RideApp.dtos.responses.UserResponse;
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
public class PassengerServiceImpl implements PassengerService {
    private static final Set<RideStatus> ACTIVE = Set.of(RideStatus.REQUESTED, RideStatus.ACCEPTED, RideStatus.ONGOING);

    private final PassengerRepository passengerRepository;
    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse register(RegisterRequest request) {
        Validator.validateRegistration(request);
        String email = Validator.normalizeEmail(request.getEmail());
        if (passengerRepository.existsByEmail(email)) throw new ConflictException("Email already exists");
        Passenger saved = passengerRepository.save(Mapper.mapToPassenger(request, passwordEncoder.encode(request.getPassword())));
        return Mapper.mapToUserResponse(saved);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        Validator.validateLogin(request);
        Passenger passenger = passengerRepository.findByEmail(Validator.normalizeEmail(request.getEmail()))
                .filter(p -> passwordEncoder.matches(request.getPassword(), p.getPassword()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        passenger.setLoggedIn(true);
        passengerRepository.save(passenger);
        return Mapper.mapToLoginResponse(passenger.getId(), passenger.getName(), passenger.getEmail(), "Login successful");
    }

    @Override
    public String logout(LogoutRequest request) {
        if (request == null) throw new InvalidRequestException("Request body is required");
        Validator.requireText(request.getUserId(), "UserId");
        Passenger passenger = findPassenger(request.getUserId());
        passenger.setLoggedIn(false);
        passengerRepository.save(passenger);
        return "Logout successful";
    }

    @Override
    public RideResponse requestRide(String passengerId, RequestRideRequest request) {
        Passenger passenger = findLoggedInPassenger(passengerId);
        if (request == null) throw new InvalidRequestException("Request body is required");
        Validator.requireText(request.getPickUpLocation(), "PickUpLocation");
        Validator.requireText(request.getDropOffLocation(), "DropOffLocation");
        if (rideRepository.existsByPassengerIdAndStatusIn(passenger.getId(), ACTIVE))
            throw new ConflictException("You already have an active ride");
        Driver driver = driverRepository.findFirstByAvailableTrueAndLoggedInTrue()
                .orElseThrow(() -> new ConflictException("No driver is currently available"));

        Ride ride = new Ride();
        ride.setPassengerId(passenger.getId());
        ride.setDriverId(driver.getId());
        ride.setPickUpLocation(request.getPickUpLocation().trim());
        ride.setDropOffLocation(request.getDropOffLocation().trim());
        ride.setAmountPaid(0.0);
        ride.setStatus(RideStatus.REQUESTED);
        return Mapper.mapToRideResponse(rideRepository.save(ride));
    }

    @Override
    public RideResponse cancelRide(String passengerId, String rideId) {
        findLoggedInPassenger(passengerId);
        Ride ride = findOwnedRide(passengerId, rideId);
        if (ride.getStatus() != RideStatus.REQUESTED && ride.getStatus() != RideStatus.ACCEPTED)
            throw new ConflictException("Ride can no longer be cancelled (status: " + ride.getStatus() + ")");
        if (ride.getStatus() == RideStatus.ACCEPTED) {
            // the driver was taken off the market when accepting; give them back
            driverRepository.findById(ride.getDriverId()).ifPresent(driver -> {
                driver.setAvailable(true);
                driverRepository.save(driver);
            });
        }
        ride.setStatus(RideStatus.CANCELLED);
        return Mapper.mapToRideResponse(rideRepository.save(ride));
    }

    @Override
    public DriverInfoResponse viewDriverInfo(String passengerId, String rideId) {
        findLoggedInPassenger(passengerId);
        Ride ride = findOwnedRide(passengerId, rideId);
        if (ride.getStatus() != RideStatus.ACCEPTED && ride.getStatus() != RideStatus.ONGOING
                && ride.getStatus() != RideStatus.COMPLETED)
            throw new ConflictException("A driver has not accepted this ride");
        Driver driver = driverRepository.findById(ride.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        return Mapper.mapToDriverInfoResponse(driver);
    }

    @Override
    public RideResponse makePayment(String passengerId, String rideId, PaymentRequest request) {
        findLoggedInPassenger(passengerId);
        Ride ride = findOwnedRide(passengerId, rideId);
        if (request == null || request.getAmount() == null || !Double.isFinite(request.getAmount())
                || request.getAmount() <= 0)
            throw new InvalidRequestException("Amount must be greater than zero");
        if (ride.getStatus() != RideStatus.COMPLETED)
            throw new ConflictException("Only a completed ride can be paid for (status: " + ride.getStatus() + ")");
        if (ride.getAmountPaid() > 0) throw new ConflictException("Ride has already been paid for");
        ride.setAmountPaid(request.getAmount());
        return Mapper.mapToRideResponse(rideRepository.save(ride));
    }

    @Override
    public List<RideResponse> viewRides(String passengerId) {
        findLoggedInPassenger(passengerId);
        return rideRepository.findByPassengerId(passengerId).stream().map(Mapper::mapToRideResponse).toList();
    }

    private Passenger findPassenger(String id) {
        return passengerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Passenger not found"));
    }

    private Passenger findLoggedInPassenger(String id) {
        Passenger passenger = findPassenger(id);
        if (!passenger.isLoggedIn()) throw new UnauthorizedException("Please log in first");
        return passenger;
    }

    /** Rides of other passengers are reported as not found rather than leaking their existence. */
    private Ride findOwnedRide(String passengerId, String rideId) {
        return rideRepository.findById(rideId)
                .filter(r -> passengerId.equals(r.getPassengerId()))
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found"));
    }
}
