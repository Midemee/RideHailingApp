package org.example.RideApp.dtos.responses;

import lombok.Data;

@Data
public class LoginResponse {
    private String id;
    private String name;
    private String email;
    private String message;
}
