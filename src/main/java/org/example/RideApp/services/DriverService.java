package org.example.RideApp.services;

import org.example.RideApp.dtos.requests.AvailabilityRequest;
import org.example.RideApp.dtos.requests.LoginRequest;
import org.example.RideApp.dtos.requests.LogoutRequest;
import org.example.RideApp.dtos.requests.RegisterRequest;
import org.example.RideApp.dtos.responses.DriverResponse;
import org.example.RideApp.dtos.responses.LoginResponse;
import org.example.RideApp.dtos.responses.PassengerRequestResponse;
import org.example.RideApp.dtos.responses.RideResponse;

import java.util.List;

/** Mirrors the Driver class in the UML diagram. */
public interface DriverService {
    DriverResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    String logout(LogoutRequest request);
    DriverResponse setAvailability(String driverId, AvailabilityRequest request);
    List<PassengerRequestResponse> viewPassengerRequest(String driverId);
    RideResponse acceptRide(String driverId, String rideId);
    RideResponse rejectRide(String driverId, String rideId);
    RideResponse startRide(String driverId, String rideId);
    RideResponse endRide(String driverId, String rideId);
}
