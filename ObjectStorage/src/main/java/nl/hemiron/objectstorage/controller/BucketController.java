package nl.hemiron.objectstorage.controller;

import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import nl.hemiron.objectstorage.exceptions.BadRequestException;
import nl.hemiron.objectstorage.exceptions.ConflictException;
import nl.hemiron.objectstorage.exceptions.InternalServerErrorException;
import nl.hemiron.objectstorage.model.BucketDb;
import nl.hemiron.objectstorage.model.request.CreateBucketRequest;
import nl.hemiron.objectstorage.model.response.CreateBucketResponse;
import nl.hemiron.objectstorage.service.MinioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

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

    // TODO: See issue #9 - Retrieve bucket
    @GetMapping("/{name}")
    public ResponseEntity<?> getBucket(@PathVariable String name) {
        return new ResponseEntity<>(
                name,
                HttpStatus.OK
        );
    }
}