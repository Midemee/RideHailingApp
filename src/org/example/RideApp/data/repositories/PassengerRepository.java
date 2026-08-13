package org.example.RideApp.data.repositories;

import org.example.RideApp.data.models.Passenger;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PassengerRepository extends MongoRepository <Passenger, String> {
}
