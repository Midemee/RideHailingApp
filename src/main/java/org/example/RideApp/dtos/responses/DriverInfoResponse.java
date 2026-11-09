package org.example.RideApp.dtos.responses;

import lombok.Data;

/** What a passenger may see about the driver of their ride (no credentials, no email). */
@Data
public class DriverInfoResponse {
    private String id;
    private String name;
    private String phoneNumber;
}
