package nl.hemiron.objectstorage.controller;

import io.minio.errors.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import nl.hemiron.objectstorage.exceptions.*;
import nl.hemiron.objectstorage.model.BucketDb;
import nl.hemiron.objectstorage.model.request.CreateBucketRequest;
import nl.hemiron.objectstorage.model.response.CreateBucketResponse;
import nl.hemiron.objectstorage.model.response.DeleteBucketResponse;
import nl.hemiron.objectstorage.model.response.GetBucketResponse;
import nl.hemiron.objectstorage.service.MinioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/buckets")
@Tag(name = "BucketController", description = "Endpoints for everything related to MinIO buckets")
public class BucketController {

    private final MinioService minioService;

    public BucketController(MinioService minioService) {
        this.minioService = minioService;
    }

    @PostMapping()
    @Operation(summary = "Create bucket", responses = {
            @ApiResponse(responseCode = "201", description = "Bucket created successfully"),
            @ApiResponse(responseCode = "400", description = "Bucket name invalid"),
            @ApiResponse(responseCode = "409", description = "Bucket name already in use"),
            @ApiResponse(responseCode = "500", description = "Bucket could not be created due to an unexpected error")
    })
    public ResponseEntity<CreateBucketResponse> createBucket(@RequestBody CreateBucketRequest createBucketRequest) {
        try {
            BucketDb bucket = minioService.createBucket(
                    createBucketRequest.getName()
            );

            return new ResponseEntity<>(
                    new CreateBucketResponse(
                            bucket.getName()
                    ).add(linkTo(methodOn(BucketController.class).getBucket(bucket.getName())).withSelfRel()),
                    HttpStatus.CREATED
            );
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        } catch (ErrorResponseException e) {
            throw new ConflictException(e.getMessage());
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "Get bucket information of all buckets", responses = {
            @ApiResponse(responseCode = "200", description = "Bucket information retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Buckets could not be retrieved due to an unexpected error")
    })
    public ResponseEntity<List<GetBucketResponse>> getBuckets() {
        try {
            var response = this.minioService.getBuckets();
            return new ResponseEntity<>(
                    response,
                    HttpStatus.OK
            );
        } catch (ServerException | InsufficientDataException | ErrorResponseException | IOException |
                 InternalException | XmlParserException | InvalidResponseException | InvalidKeyException |
                 NoSuchAlgorithmException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    @GetMapping("/{bucketName}")
    @Operation(summary = "Get bucket information of a single bucket", responses = {
            @ApiResponse(responseCode = "200", description = "Bucket information retrieved succcessfully"),
            @ApiResponse(responseCode = "404", description = "Bucket with specified name does not exist"),
            @ApiResponse(responseCode = "500", description = "Bucket could not be retrieved due to an unexpected error")
    })
    public ResponseEntity<GetBucketResponse> getBucket(@PathVariable final String bucketName) {
        try {
            var response = this.minioService.getBucketByName(bucketName);
            return new ResponseEntity<>(
                    response,
                    HttpStatus.OK
            );
        } catch (BucketNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        } catch (ServerException | InsufficientDataException | ErrorResponseException | IOException |
                 InternalException | XmlParserException | InvalidResponseException | InvalidKeyException |
                 NoSuchAlgorithmException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    @DeleteMapping("/{bucketName}")
    @Operation(summary = "Delete a single bucket", responses = {
            @ApiResponse(responseCode = "200", description = "Bucket deleted successfully"),
            @ApiResponse(responseCode = "404", description = "No bucket found with this name"),
            @ApiResponse(responseCode = "409", description = "Bucket could not be deleted due to it not being empty"),
            @ApiResponse(responseCode = "500", description = "Bucket could not be deleted due to an unexpected error")
    })
    public ResponseEntity<DeleteBucketResponse> deleteBucket(
            @PathVariable final String bucketName,
            @RequestHeader(value = "force-delete", defaultValue = "false" ) boolean forceDelete) {
        try {
            var bucket = this.minioService.deleteBucket(bucketName, forceDelete);
            return new ResponseEntity<>(
                    new DeleteBucketResponse(
                            bucket.getName()
                    ),
                    HttpStatus.OK
            );
        } catch (BucketNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        } catch (BucketNotEmptyException e) {
            throw new ConflictException(e.getMessage());
        } catch (ServerException | InsufficientDataException | ErrorResponseException | IOException |
                 InternalException | XmlParserException | InvalidResponseException | InvalidKeyException |
                 NoSuchAlgorithmException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }
}
