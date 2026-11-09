package org.example.RideApp.dtos.responses;

import lombok.Data;

@Data
public class UserResponse {
    private String id;
    private String name;
    private String email;
    private String phoneNumber;
}
