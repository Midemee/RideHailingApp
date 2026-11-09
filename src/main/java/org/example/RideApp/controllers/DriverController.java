package org.example.RideApp.controllers;

import lombok.RequiredArgsConstructor;
import org.example.RideApp.dtos.requests.AvailabilityRequest;
import org.example.RideApp.dtos.requests.LoginRequest;
import org.example.RideApp.dtos.requests.LogoutRequest;
import org.example.RideApp.dtos.requests.RegisterRequest;
import org.example.RideApp.dtos.responses.ApiResponse;
import org.example.RideApp.services.DriverService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {
    private final DriverService driverService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(CREATED).body(new ApiResponse(true, driverService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.status(OK).body(new ApiResponse(true, driverService.login(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@RequestBody LogoutRequest request) {
        return ResponseEntity.status(OK).body(new ApiResponse(true, driverService.logout(request)));
    }

    @PatchMapping("/{driverId}/availability")
    public ResponseEntity<ApiResponse> setAvailability(@PathVariable String driverId,
                                                       @RequestBody AvailabilityRequest request) {
        return ResponseEntity.ok(new ApiResponse(true, driverService.setAvailability(driverId, request)));
    }

    @GetMapping("/{driverId}/requests")
    public ResponseEntity<ApiResponse> viewPassengerRequest(@PathVariable String driverId) {
        return ResponseEntity.ok(new ApiResponse(true, driverService.viewPassengerRequest(driverId)));
    }

    @PostMapping("/{driverId}/rides/{rideId}/accept")
    public ResponseEntity<ApiResponse> acceptRide(@PathVariable String driverId, @PathVariable String rideId) {
        return ResponseEntity.ok(new ApiResponse(true, driverService.acceptRide(driverId, rideId)));
    }

    @PostMapping("/{driverId}/rides/{rideId}/reject")
    public ResponseEntity<ApiResponse> rejectRide(@PathVariable String driverId, @PathVariable String rideId) {
        return ResponseEntity.ok(new ApiResponse(true, driverService.rejectRide(driverId, rideId)));
    }

    @PostMapping("/{driverId}/rides/{rideId}/start")
    public ResponseEntity<ApiResponse> startRide(@PathVariable String driverId, @PathVariable String rideId) {
        return ResponseEntity.ok(new ApiResponse(true, driverService.startRide(driverId, rideId)));
    }

    @PostMapping("/{driverId}/rides/{rideId}/end")
    public ResponseEntity<ApiResponse> endRide(@PathVariable String driverId, @PathVariable String rideId) {
        return ResponseEntity.ok(new ApiResponse(true, driverService.endRide(driverId, rideId)));
    }
}
