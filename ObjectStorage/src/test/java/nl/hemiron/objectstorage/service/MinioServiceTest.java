package nl.hemiron.objectstorage.service;


import io.minio.MinioClient;
import io.minio.Result;
import io.minio.errors.*;
import io.minio.messages.Item;
import nl.hemiron.objectstorage.dao.BucketDAO;
import nl.hemiron.objectstorage.exceptions.BucketNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioServiceTest {

    private MinioService sut;

    @Mock
    private BucketDAO bucketDAO;

    @Mock
    private MinioClient minioClient;

    @BeforeEach
    void beforeEach() {
        sut = new MinioService(bucketDAO);
        sut.minioClient = this.minioClient;
    }

    @Test
    void getBucketByName_WhenBucketDoesNotExist_ThrowsBucketNotFoundException()
            throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        // Arrange
        var name = "mybucket";
        when(this.minioClient.bucketExists(any())).thenReturn(false);

        // Act
        var exception = assertThrows(BucketNotFoundException.class, () -> sut.getBucketByName(name));

        // Assert
        var actual = exception.getMessage();
        assertThat(actual, is("Bucket with name mybucket not found"));
    }

    @Test
    void getBucketByName_WithEmptyBucket_ReturnsResponseWithZeroAmountOfObjectsAndSize()
            throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        // Arrange
        var bucketObjects = new ArrayList<Result<Item>>();
        when(this.minioClient.bucketExists(any())).thenReturn(true);
        when(this.minioClient.listObjects(any())).thenReturn(bucketObjects);

        // Act
        var actual = sut.getBucketByName("lcab-bucket");

        // Assert
        assertThat(actual.size, is(0L));
        assertThat(actual.amountOfObjects, is(0));
    }

    @Test
    void getBucketByName_WithObjectInSubdirectory_CountsThatObjectInTotalSize()
            throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        // Arrange
        var bucketObjects = new ArrayList<Result<Item>>();
        var object = new Result<Item>(new Item() {
            @Override
            public long size() {
                return 53_567L;
            }

            @Override
            public String objectName() {
                return "customers.xlsx";
            }
        });
        var objectInSubdirectory = new Result<Item>(new Item() {
            @Override
            public long size() {
                return 4_572L;
            }

            @Override
            public String objectName() {
                return "hemiron/2023_05_11_logs.txt";
            }
        });

        bucketObjects.add(object);
        bucketObjects.add(objectInSubdirectory);

        when(this.minioClient.bucketExists(any())).thenReturn(true);
        when(this.minioClient.listObjects(any())).thenReturn(bucketObjects);

        // Act
        var actual = sut.getBucketByName("hemiron-bucket");

        // Assert
        assertThat(actual.size, is(58_139L));
        assertThat(actual.amountOfObjects, is(2));
    }

}
