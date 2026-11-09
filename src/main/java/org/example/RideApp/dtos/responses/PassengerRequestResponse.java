package org.example.RideApp.dtos.responses;

import lombok.Data;
import org.example.RideApp.data.models.RideStatus;

/** A pending ride request as seen by a driver. */
@Data
public class PassengerRequestResponse {
    private String rideId;
    private String passengerId;
    private String passengerName;
    private String passengerPhoneNumber;
    private String pickUpLocation;
    private String dropOffLocation;
    private RideStatus status;
}
