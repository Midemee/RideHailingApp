package org.example.RideApp.data.repositories;

import org.example.RideApp.data.models.Ride;
import org.example.RideApp.data.models.RideStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface RideRepository extends MongoRepository<Ride, String> {
    List<Ride> findByPassengerId(String passengerId);
    List<Ride> findByDriverIdAndStatus(String driverId, RideStatus status);
    boolean existsByPassengerIdAndStatusIn(String passengerId, Collection<RideStatus> statuses);
    boolean existsByDriverIdAndStatusIn(String driverId, Collection<RideStatus> statuses);
}
