package org.example.RideApp.data.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A ride belongs to exactly one passenger and one driver (UML: * -> 1 each).
 * The relationships are stored as ids so that credentials are never embedded in ride documents.
 */
@Data
@Document(collection = "rides")
public class Ride {
    @Id
    private String id;
    private String passengerId;
    private String driverId;
    private String pickUpLocation;
    private String dropOffLocation;
    private double amountPaid;
    private RideStatus status;
}
