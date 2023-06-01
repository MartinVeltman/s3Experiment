package nl.hemiron.objectstorage.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
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

    public BucketDb createBucket(String bucketName) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException, IllegalArgumentException {
        minioClient.makeBucket(
                MakeBucketArgs.builder()
                .bucket(bucketName).build()
        );

        return bucketDAO.save(
                new BucketDb(bucketName)
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

    public GetBucketResponse getBucketByName(final String bucketName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, BucketNotFoundException {
        verifyBucketExists(bucketName);

        Iterable<Result<Item>> bucketObjects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .recursive(true)
                        .build());

        long totalSize = 0L;
        int amountOfObjects = 0;
        for (Result<Item> obj : bucketObjects) {
            totalSize += obj.get().size();
            amountOfObjects++;
        }

        return new GetBucketResponse(bucketName, totalSize, amountOfObjects);
    }

    public String getUploadObjectURL(String bucketName, String objectName) throws BucketNotFoundException, ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        verifyBucketExists(bucketName);

        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.PUT)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(60 * 60) // TODO: Discuss what would be a reasonable expiry time
                        .build()
        );
    }

    public GetObjectResponse getObject(String bucketName, String objectName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        verifyBucketExists(bucketName);

        var decodedName = StringUtils.decodeBase64(objectName);
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(decodedName)
                .build());
    }

    private void verifyBucketExists(String bucketName) throws BucketNotFoundException, ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build()
        );

        if (!bucketExists) {
            throw new BucketNotFoundException("Bucket with name " + bucketName + " not found");
        }
    }
}
