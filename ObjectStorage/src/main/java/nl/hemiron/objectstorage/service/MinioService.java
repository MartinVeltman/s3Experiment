package nl.hemiron.objectstorage.service;

import io.minio.BucketExistsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.errors.*;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import nl.hemiron.objectstorage.dao.BucketDAO;
import nl.hemiron.objectstorage.exceptions.BucketNotFoundException;
import nl.hemiron.objectstorage.model.BucketDb;
import nl.hemiron.objectstorage.model.response.GetBucketResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Service
public class MinioService {

    @Value("${minio.builder.endpoint}")
    private String minioEndpoint;

    @Value("${minio.builder.access.key}")
    private String minioAccessKey;

    @Value("${minio.builder.secret.key}")
    private String minioSecretKey;

    MinioClient minioClient;

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

    public List<GetBucketResponse> getBuckets() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        var buckets = minioClient.listBuckets();

        List<GetBucketResponse> responses = new ArrayList<>();
        for (Bucket bucket : buckets) {
            responses.add(getBucketByName(bucket.name()));
        }

        return responses;
    }

    public GetBucketResponse getBucketByName(final String name) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, BucketNotFoundException {
        var bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(name)
                .build());
        if (!bucketExists) {
            throw new BucketNotFoundException("Bucket with name " + name + " not found");
        }

        Iterable<Result<Item>> bucketObjects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(name)
                        .recursive(true)
                        .build());

        long totalSize = 0L;
        int amountOfObjects = 0;
        for (Result<Item> obj : bucketObjects) {
            totalSize += obj.get().size();
            amountOfObjects++;
        }

        return new GetBucketResponse(name, totalSize, amountOfObjects);
    }
}
