package nl.hemiron.objectstorage.repository;

import nl.hemiron.objectstorage.model.BucketDb;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BucketRepository extends JpaRepository<BucketDb, UUID> {
    BucketDb findByName(String name);
}
