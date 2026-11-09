package org.example.RideApp.dtos.requests;

import lombok.Data;

@Data
public class RequestRideRequest {
    private String pickUpLocation;
    private String dropOffLocation;
}
