package org.example.RideApp.services;

import org.example.RideApp.dtos.requests.LoginRequest;
import org.example.RideApp.dtos.requests.LogoutRequest;
import org.example.RideApp.dtos.requests.PaymentRequest;
import org.example.RideApp.dtos.requests.RegisterRequest;
import org.example.RideApp.dtos.requests.RequestRideRequest;
import org.example.RideApp.dtos.responses.DriverInfoResponse;
import org.example.RideApp.dtos.responses.LoginResponse;
import org.example.RideApp.dtos.responses.RideResponse;
import org.example.RideApp.dtos.responses.UserResponse;

import java.util.List;

/** Mirrors the Passenger class in the UML diagram. */
public interface PassengerService {
    UserResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    String logout(LogoutRequest request);
    RideResponse requestRide(String passengerId, RequestRideRequest request);
    RideResponse cancelRide(String passengerId, String rideId);
    DriverInfoResponse viewDriverInfo(String passengerId, String rideId);
    RideResponse makePayment(String passengerId, String rideId, PaymentRequest request);
    List<RideResponse> viewRides(String passengerId);
}
