package org.example.RideApp.data.models;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Data
@Document
public class Ride {
    private String id;
    private Passenger passenger;
    private Driver driver;
    private String pickUpLocation;
    private String destination;
    private BigDecimal amount;
    private boolean isRideComplete;
}