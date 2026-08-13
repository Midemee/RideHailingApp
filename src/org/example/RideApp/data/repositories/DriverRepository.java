package org.example.RideApp.data.repositories;

import org.example.RideApp.data.models.Driver;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DriverRepository extends MongoRepository <Driver, String> {
}
