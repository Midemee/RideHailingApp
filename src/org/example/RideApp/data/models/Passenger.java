package org.example.RideApp.data.models;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Passenger{
    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private String password;
    private String location;

}
