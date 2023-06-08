package nl.hemiron.objectstorage.service;


import com.google.common.collect.Iterables;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.errors.*;
import io.minio.messages.Item;
import nl.hemiron.objectstorage.dao.BucketDAO;
import nl.hemiron.objectstorage.exceptions.BucketNotEmptyException;
import nl.hemiron.objectstorage.exceptions.BucketNotFoundException;
import nl.hemiron.objectstorage.exceptions.NotFoundException;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
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

    @Test
    void deleteBucket_WithObjectsInBucketAndNoForceDelete_ThrowsBucketNotEmptyException() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        //Arrange
        var name = "mybucket";
        var bucketObjects = new ArrayList<Result<Item>>();
        bucketObjects.add(new Result<Item>(new Item() {}));

        boolean forceDelete = false;

        when(this.minioClient.bucketExists(any())).thenReturn(true);
        when(this.minioClient.listObjects(any())).thenReturn(bucketObjects);

        //Act
        var exception = assertThrows(BucketNotEmptyException.class, () -> sut.deleteBucket(name, forceDelete));

        //Assert
        var actual = exception.getMessage();
        assertThat(actual, is("Bucket not empty, consider emptying it or adding 'force-delete' header to your request"));
    }

    @Test
    void getObject_WithBucketNameAndEncodedObjectName_ReturnsObject()
            throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        // Arrange
        var headers = new Headers.Builder().build();
        var response = new GetObjectResponse(
                headers, "s1057392-bucket", "eu-west-3",
                "healthForYou/rightToAccess/a18d2f3b-7809-4efb-865c-113a1105fa98.pdf", null);
        when(this.minioClient.getObject(any())).thenReturn(response);
        when(this.minioClient.bucketExists(any())).thenReturn(true);

        // Act
        var actual = sut.getObject("s1057392-bucket", "aGVhbHRoRm9yWW91L3JpZ2h0VG9BY2Nlc3NzL2ExOGQyZjNiLTc4MDktNGVmYi04NjVjLTExM2ExMTA1ZmE5OC5wZGY=");
        var actualBucket = actual.bucket();
        var actualObject = actual.object();

        // Assert
        assertThat(actualBucket, is("s1057392-bucket"));
        assertThat(actualObject, is("healthForYou/rightToAccess/a18d2f3b-7809-4efb-865c-113a1105fa98.pdf"));
    }

    @Test
    void getDirectoryContents_WithDirectoryThatDoesNotExist_ThrowsNotFoundException()
            throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        //Arrange
        var bucketObjects = new ArrayList<Result<Item>>();

        when(this.minioClient.bucketExists(any())).thenReturn(true);
        when(this.minioClient.listObjects(any())).thenReturn(bucketObjects);

        //Act
        var exception = assertThrows(NotFoundException.class,
                () -> sut.getDirectoryContents("mybucket", "dGhpc0RpcmVjdG9yeURvZXNOb3RFeGlzdC8="));

        //Assert
        var actual = exception.getMessage();
        assertThat(actual, is("Directory with name thisDirectoryDoesNotExist/ not found"));
    }

    @Test
    void getDirectoryContents_WithDirectoryContainingFileAndSubdirectory_ReturnsBothAsItemResponse()
            throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        //Arrange
        var bucketObjects = new ArrayList<Result<Item>>();
        var object = new Result<Item>(new Item() {
            @Override
            public String objectName() {
                return "eeyore.png";
            }

            @Override
            public ZonedDateTime lastModified() {
                return ZonedDateTime.now();
            }
        });
        var subdirectory = new Result<Item>(new Item() {
            @Override
            public String objectName() {
                return "videos/";
            }

            @Override
            public boolean isDir() {
                return true;
            }
        });
        bucketObjects.add(object);
        bucketObjects.add(subdirectory);

        when(this.minioClient.bucketExists(any())).thenReturn(true);
        when(this.minioClient.listObjects(any())).thenReturn(bucketObjects);

        //Act
        var actual = sut.getDirectoryContents("mybucket", "Lw==");

        //Assert
        assertThat(actual.size(), is(2));
        var first = actual.get(0);
        assertThat(first.getObjectName(), is("eeyore.png"));
        var second = actual.get(1);
        assertThat(second.getObjectName(), is("videos/"));
        assertThat(second.isDir(), is(true));
    }

}
