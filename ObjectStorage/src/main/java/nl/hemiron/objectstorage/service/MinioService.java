package nl.hemiron.objectstorage.service;

import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.*;
import jakarta.annotation.PostConstruct;
import nl.hemiron.objectstorage.dao.BucketDAO;
import nl.hemiron.objectstorage.model.BucketDb;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
public class MinioService {

    @Value("${minio.builder.endpoint}")
    private String minioEndpoint;

    @Value("${minio.builder.access.key}")
    private String minioAccessKey;

    @Value("${minio.builder.secret.key}")
    private String minioSecretKey;

    private MinioClient minioClient;

    private final BucketDAO bucketDAO;

    public MinioService(BucketDAO bucketDAO) {
        this.bucketDAO = bucketDAO;
    }

    @PostConstruct
    public void initializeMinioClient() {
        this.minioClient = MinioClient.builder()
                .endpoint(minioEndpoint)
                .credentials(minioAccessKey, minioSecretKey)
                .build();
    }

    public BucketDb createBucket(String name) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException, IllegalArgumentException {
        minioClient.makeBucket(
                MakeBucketArgs.builder()
                .bucket(name).build()
        );

        return bucketDAO.save(
                new BucketDb(name)
        );
    }
}
