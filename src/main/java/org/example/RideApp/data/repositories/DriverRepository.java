package org.example.RideApp.data.repositories;

import org.example.RideApp.data.models.Driver;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DriverRepository extends MongoRepository<Driver, String> {
    Optional<Driver> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Driver> findFirstByAvailableTrueAndLoggedInTrue();
}
