package org.example.RideApp.dtos.responses;

import lombok.Data;

@Data
public class DriverResponse {
    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private boolean available;
}
