package org.example.RideApp.data.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "passengers")
public class Passenger {
    @Id
    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    /** BCrypt hash, never the raw password. */
    private String password;
    /** Set by login(), cleared by logout(). Ride operations require it. */
    private boolean loggedIn;
}
