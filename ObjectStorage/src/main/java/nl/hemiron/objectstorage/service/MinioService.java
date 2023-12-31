package nl.hemiron.objectstorage.service;

import com.google.common.collect.Iterables;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.extern.java.Log;
import nl.hemiron.objectstorage.exceptions.BucketNotEmptyException;
import nl.hemiron.objectstorage.exceptions.BucketNotFoundException;
import nl.hemiron.objectstorage.exceptions.InvalidProjectIdException;
import nl.hemiron.objectstorage.model.response.CreateBucketResponse;
import nl.hemiron.objectstorage.model.response.DeleteBucketResponse;
import nl.hemiron.objectstorage.model.response.GetBucketResponse;
import nl.hemiron.objectstorage.model.response.ItemResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Log
@Service
public class MinioService {

    @Value("${minio.builder.endpoint}")
    private String minioEndpoint;

    @Value("${minio.builder.access.key}")
    private String minioAccessKey;

    @Value("${minio.builder.secret.key}")
    private String minioSecretKey;

    MinioClient minioClient;

    public MinioService() {
    }

    @PostConstruct
    public void initializeMinioClient() {
        this.minioClient = MinioClient.builder()
                .endpoint(minioEndpoint)
                .credentials(minioAccessKey, minioSecretKey)
                .build();
    }

    public CompletableFuture<CreateBucketResponse> createBucketAsync(String bucketName, UUID projectId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName).build()
                );

                var tags = new HashMap<String, String>();
                tags.put("projectId", projectId.toString());

                minioClient.setBucketTags(
                        SetBucketTagsArgs.builder()
                                .bucket(bucketName)
                                .tags(tags)
                                .build());

                log.log(Level.INFO, "Created bucket " + bucketName);
                return new CreateBucketResponse(bucketName);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        });
    }

    public List<GetBucketResponse> getBuckets(UUID projectId) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        log.log(Level.INFO, "Getting all buckets for project with projectId: " + projectId);
        var buckets = minioClient.listBuckets();

        List<GetBucketResponse> responses = new ArrayList<>();
        for (Bucket bucket : buckets) {
            try {
                verifyBucketBelongsToProject(bucket.name(), projectId);
                responses.add(getBucketByName(bucket.name(), projectId));
            } catch (InvalidProjectIdException ignored) {
                // Don't add the bucket to the responses if the given ProjectId doesn't match
            }
        }

        return responses;
    }

    public GetBucketResponse getBucketByName(final String bucketName, final UUID projectId) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, BucketNotFoundException {
        verifyBucketExists(bucketName);
        verifyBucketBelongsToProject(bucketName, projectId);

        Iterable<Result<Item>> bucketObjects = getObjectsInBucket(bucketName);

        long totalSize = 0L;
        int amountOfObjects = 0;
        for (Result<Item> obj : bucketObjects) {
            totalSize += obj.get().size();
            amountOfObjects++;
        }

        return new GetBucketResponse(bucketName, totalSize, amountOfObjects);
    }

    public CompletableFuture<String> getUploadObjectURLAsync(String bucketName, String objectName, UUID projectId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                verifyBucketExists(bucketName);
                verifyBucketBelongsToProject(bucketName, projectId);

                log.log(Level.INFO, "Getting presigned object URL for object: " + objectName);

                return minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.PUT)
                                .bucket(bucketName)
                                .object(objectName)
                                .expiry(60 * 60) // TODO: Discuss what would be a reasonable expiry time
                                .build()
                );
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        });
    }

    public GetObjectResponse getObject(String bucketName, String objectName, UUID projectId) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        verifyBucketExists(bucketName);
        verifyBucketBelongsToProject(bucketName, projectId);

        var decodedName = StringUtils.decodeBase64(objectName);
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(decodedName)
                .build());
    }

    public Iterable<Result<DeleteError>> deleteObjects(String bucketName, String[] objectNames, UUID projectId) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        verifyBucketExists(bucketName);
        verifyBucketBelongsToProject(bucketName, projectId);

        for (String objectName : objectNames) {
            if (isDirectory(objectName)) {
                emptyDirectory(bucketName, objectName);
            }
        }

        List<DeleteObject> decodedObjectNames = Arrays.stream(objectNames)
                .map(Base64.getDecoder()::decode)
                .map(name -> new DeleteObject(new String(name)))
                .collect(Collectors.toCollection(ArrayList::new));

        var deleteErrors = minioClient.removeObjects(RemoveObjectsArgs.builder()
                .bucket(bucketName)
                .objects(decodedObjectNames)
                .build());

        log.log(Level.INFO, "Deleted objects " + Arrays.toString(objectNames));

        return deleteErrors;
    }

    public CompletableFuture<DeleteBucketResponse> deleteBucketAsync(final String bucketName, final boolean force, final UUID projectId) {
        return CompletableFuture.supplyAsync(() -> {
            try {

                verifyBucketExists(bucketName);

                verifyBucketBelongsToProject(bucketName, projectId);

                Iterable<Result<Item>> bucketObjects = getObjectsInBucket(bucketName);
                if (force) {
                    for (Result<Item> bucketObject : bucketObjects) {
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
                log.log(Level.INFO, "Deleted bucket " + bucketName);
                return new DeleteBucketResponse(bucketName);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        });
    }

    public List<ItemResponse> getDirectoryContents(String bucketName, String directoryName, UUID projectId) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        verifyBucketExists(bucketName);
        verifyBucketBelongsToProject(bucketName, projectId);

        var decodedName = StringUtils.decodeBase64(directoryName);

        log.log(Level.INFO, "Getting objects for directory " + decodedName + " (Decoded from " + directoryName + ")");

        Iterable<Result<Item>> directoryContents;

        if (decodedName.equals("/")) {
            directoryContents = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .recursive(false)
                    .build());
        } else {
            directoryContents = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(decodedName)
                    .recursive(false)
                    .build());
        }
        log.log(Level.INFO, "Contents of directory: ");

        List<ItemResponse> items = new ArrayList<>();
        for (Result<Item> itemResult : directoryContents) {
            var item = itemResult.get();
            log.log(Level.INFO, item.objectName());
            if (item.isDir()) {
                items.add(new ItemResponse(item.objectName(), item.isDir()));
                continue;
            }
            items.add(new ItemResponse(
                    item.etag(), item.objectName(), item.lastModified(), item.owner(), item.size(),
                    item.storageClass(), item.isLatest(), item.versionId(), item.userMetadata(), item.isDir())
            );
        }
        return items;
    }

    private void verifyBucketExists(String bucketName) throws BucketNotFoundException, ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build()
        );

        if (!bucketExists) {
            log.log(Level.WARNING, "Could not find bucket with name " + bucketName);
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

    private void verifyBucketBelongsToProject(String bucketName, UUID projectId) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        var tags = minioClient.getBucketTags(
                GetBucketTagsArgs.builder().bucket(bucketName).build()
        );
        var bucketTagProjectIdString = tags.get().get("projectId");

        try {
            UUID bucketTagProjectId = UUID.fromString(bucketTagProjectIdString);
            if (!projectId.equals(bucketTagProjectId)) {
                throw new InvalidProjectIdException(
                        "Invalid Project Id for bucket with name " + bucketName);
            }
        } catch (NullPointerException ignored) {
            throw new InvalidProjectIdException(
                    "Could not parse Project Id to UUID for bucket with name " + bucketName);
        }
    }

    private void emptyDirectory(String bucketName, String directoryName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        var decodedDirectoryName = StringUtils.decodeBase64(directoryName);

        Iterable<Result<Item>> directoryObjects = minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(decodedDirectoryName)
                .recursive(true)
                .build());

        for (Result<Item> directoryObject : directoryObjects) {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(directoryObject.get().objectName())
                    .build());
        }
    }

    private boolean isDirectory(String objectName) {
        return StringUtils.decodeBase64(objectName).endsWith("/");
    }
}
