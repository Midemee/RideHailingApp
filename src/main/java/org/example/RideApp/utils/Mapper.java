package org.example.RideApp.utils;

import org.example.RideApp.data.models.Driver;
import org.example.RideApp.data.models.Passenger;
import org.example.RideApp.data.models.Ride;
import org.example.RideApp.dtos.requests.RegisterRequest;
import org.example.RideApp.dtos.responses.DriverInfoResponse;
import org.example.RideApp.dtos.responses.DriverResponse;
import org.example.RideApp.dtos.responses.LoginResponse;
import org.example.RideApp.dtos.responses.PassengerRequestResponse;
import org.example.RideApp.dtos.responses.RideResponse;
import org.example.RideApp.dtos.responses.UserResponse;

public class Mapper {
    private Mapper() {
    }

    public static Passenger mapToPassenger(RegisterRequest request, String encodedPassword) {
        Passenger passenger = new Passenger();
        passenger.setName(request.getName().trim());
        passenger.setEmail(Validator.normalizeEmail(request.getEmail()));
        passenger.setPhoneNumber(request.getPhoneNumber().trim());
        passenger.setPassword(encodedPassword);
        return passenger;
    }

    public static Driver mapToDriver(RegisterRequest request, String encodedPassword) {
        Driver driver = new Driver();
        driver.setName(request.getName().trim());
        driver.setEmail(Validator.normalizeEmail(request.getEmail()));
        driver.setPhoneNumber(request.getPhoneNumber().trim());
        driver.setPassword(encodedPassword);
        driver.setAvailable(true);
        return driver;
    }

    public static UserResponse mapToUserResponse(Passenger passenger) {
        UserResponse response = new UserResponse();
        response.setId(passenger.getId());
        response.setName(passenger.getName());
        response.setEmail(passenger.getEmail());
        response.setPhoneNumber(passenger.getPhoneNumber());
        return response;
    }

    public static DriverResponse mapToDriverResponse(Driver driver) {
        DriverResponse response = new DriverResponse();
        response.setId(driver.getId());
        response.setName(driver.getName());
        response.setEmail(driver.getEmail());
        response.setPhoneNumber(driver.getPhoneNumber());
        response.setAvailable(driver.isAvailable());
        return response;
    }

    public static LoginResponse mapToLoginResponse(String id, String name, String email, String message) {
        LoginResponse response = new LoginResponse();
        response.setId(id);
        response.setName(name);
        response.setEmail(email);
        response.setMessage(message);
        return response;
    }

    public static RideResponse mapToRideResponse(Ride ride) {
        RideResponse response = new RideResponse();
        response.setId(ride.getId());
        response.setPassengerId(ride.getPassengerId());
        response.setDriverId(ride.getDriverId());
        response.setPickUpLocation(ride.getPickUpLocation());
        response.setDropOffLocation(ride.getDropOffLocation());
        response.setAmountPaid(ride.getAmountPaid());
        response.setStatus(ride.getStatus());
        return response;
    }

    public static DriverInfoResponse mapToDriverInfoResponse(Driver driver) {
        DriverInfoResponse response = new DriverInfoResponse();
        response.setId(driver.getId());
        response.setName(driver.getName());
        response.setPhoneNumber(driver.getPhoneNumber());
        return response;
    }

    public static PassengerRequestResponse mapToPassengerRequestResponse(Ride ride, Passenger passenger) {
        PassengerRequestResponse response = new PassengerRequestResponse();
        response.setRideId(ride.getId());
        response.setPassengerId(passenger.getId());
        response.setPassengerName(passenger.getName());
        response.setPassengerPhoneNumber(passenger.getPhoneNumber());
        response.setPickUpLocation(ride.getPickUpLocation());
        response.setDropOffLocation(ride.getDropOffLocation());
        response.setStatus(ride.getStatus());
        return response;
    }
}
