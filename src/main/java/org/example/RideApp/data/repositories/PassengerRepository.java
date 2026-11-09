package org.example.RideApp.data.repositories;

import org.example.RideApp.data.models.Passenger;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PassengerRepository extends MongoRepository<Passenger, String> {
    Optional<Passenger> findByEmail(String email);
    boolean existsByEmail(String email);
}
