package org.example.RideApp.data.repositories;

import org.example.RideApp.data.models.Ride;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RideRepository extends MongoRepository <Ride, String> {
}
