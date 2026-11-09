package org.example.RideApp.dtos.responses;

import lombok.Data;
import org.example.RideApp.data.models.RideStatus;

@Data
public class RideResponse {
    private String id;
    private String passengerId;
    private String driverId;
    private String pickUpLocation;
    private String dropOffLocation;
    private double amountPaid;
    private RideStatus status;
}
