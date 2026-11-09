package org.example.RideApp.controllers;

import lombok.RequiredArgsConstructor;
import org.example.RideApp.dtos.requests.LoginRequest;
import org.example.RideApp.dtos.requests.LogoutRequest;
import org.example.RideApp.dtos.requests.PaymentRequest;
import org.example.RideApp.dtos.requests.RegisterRequest;
import org.example.RideApp.dtos.requests.RequestRideRequest;
import org.example.RideApp.dtos.responses.ApiResponse;
import org.example.RideApp.services.PassengerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/api/passengers")
@RequiredArgsConstructor
public class PassengerController {
    private final PassengerService passengerService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(CREATED).body(new ApiResponse(true, passengerService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.status(OK).body(new ApiResponse(true, passengerService.login(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@RequestBody LogoutRequest request) {
        return ResponseEntity.status(OK).body(new ApiResponse(true, passengerService.logout(request)));
    }

    @PostMapping("/{passengerId}/rides")
    public ResponseEntity<ApiResponse> requestRide(@PathVariable String passengerId,
                                                   @RequestBody RequestRideRequest request) {
        return ResponseEntity.status(CREATED)
                .body(new ApiResponse(true, passengerService.requestRide(passengerId, request)));
    }

    @GetMapping("/{passengerId}/rides")
    public ResponseEntity<ApiResponse> viewRides(@PathVariable String passengerId) {
        return ResponseEntity.ok(new ApiResponse(true, passengerService.viewRides(passengerId)));
    }

    @PostMapping("/{passengerId}/rides/{rideId}/cancel")
    public ResponseEntity<ApiResponse> cancelRide(@PathVariable String passengerId, @PathVariable String rideId) {
        return ResponseEntity.ok(new ApiResponse(true, passengerService.cancelRide(passengerId, rideId)));
    }

    @GetMapping("/{passengerId}/rides/{rideId}/driver")
    public ResponseEntity<ApiResponse> viewDriverInfo(@PathVariable String passengerId, @PathVariable String rideId) {
        return ResponseEntity.ok(new ApiResponse(true, passengerService.viewDriverInfo(passengerId, rideId)));
    }

    @PostMapping("/{passengerId}/rides/{rideId}/payment")
    public ResponseEntity<ApiResponse> makePayment(@PathVariable String passengerId, @PathVariable String rideId,
                                                   @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(new ApiResponse(true, passengerService.makePayment(passengerId, rideId, request)));
    }
}
