package nl.hemiron.objectstorage.dao;

import nl.hemiron.objectstorage.model.BucketDb;
import nl.hemiron.objectstorage.repository.BucketRepository;
import org.springframework.stereotype.Component;

@Component
public class BucketDAO {

    private final BucketRepository bucketRepository;

    public BucketDAO(BucketRepository bucketRepository) {
        this.bucketRepository = bucketRepository;
    }

    public BucketDb save(BucketDb bucketDb) {
        return this.bucketRepository.save(bucketDb);
    }

    public BucketDb getByBucketName(String bucketName) {
        return this.bucketRepository.findByName(bucketName);
    }

    public void remove(BucketDb bucketDb) {
        this.bucketRepository.delete(bucketDb);
    }
}
