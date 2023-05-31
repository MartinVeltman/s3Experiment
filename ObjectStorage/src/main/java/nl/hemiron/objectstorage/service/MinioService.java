package nl.hemiron.objectstorage.service;

import com.google.common.collect.Iterables;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import nl.hemiron.objectstorage.dao.BucketDAO;
import nl.hemiron.objectstorage.exceptions.BucketNotEmptyException;
import nl.hemiron.objectstorage.exceptions.BucketNotFoundException;
import nl.hemiron.objectstorage.model.BucketDb;
import nl.hemiron.objectstorage.model.response.DeleteBucketResponse;
import nl.hemiron.objectstorage.model.response.GetBucketResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
        Iterable<Result<Item>> bucketObjects = getObjectsInBucket(bucketName);

        long totalSize = 0L;
        int amountOfObjects = 0;
        for (Result<Item> obj : bucketObjects) {
            totalSize += obj.get().size();
            amountOfObjects++;
        }

        return new GetBucketResponse(bucketName, totalSize, amountOfObjects);
    }

    public String getUploadObjectURL(String bucketName, String objectPath, MultipartFile multipartFile) throws BucketNotFoundException, ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        verifyBucketExists(bucketName);

        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.PUT)
                        .bucket(bucketName)
                        .object(objectPath + "/" + multipartFile.getOriginalFilename())
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

    public BucketDb deleteBucket(final String bucketName, final boolean force) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        verifyBucketExists(bucketName);
        Iterable<Result<Item>> bucketObjects = getObjectsInBucket(bucketName);
        if (force) {
            for (Result<Item > bucketObject : bucketObjects) {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(bucketObject.get().objectName())
                        .build());
            }
        }
        if (Iterables.size(bucketObjects) > 0)
            throw new BucketNotEmptyException("Bucket not empty, consider emptying it or adding 'force-delete' header to your request");
        minioClient.removeBucket(RemoveBucketArgs.builder()
                .bucket(bucketName)
                .build());
        try {
            BucketDb bucket = bucketDAO.getByBucketName(bucketName);
            bucketDAO.remove(bucket);
            return bucket;
        } catch (Exception ignored) {}
        return new BucketDb(bucketName);
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

    private Iterable<Result<Item>> getObjectsInBucket(final String bucketName) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .recursive(true)
                        .build());
    }
}
