package nl.hemiron.objectstorage.controller;

import io.minio.errors.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import nl.hemiron.objectstorage.exceptions.BadRequestException;
import nl.hemiron.objectstorage.exceptions.BucketNotFoundException;
import nl.hemiron.objectstorage.exceptions.InternalServerErrorException;
import nl.hemiron.objectstorage.exceptions.NotFoundException;
import nl.hemiron.objectstorage.model.request.UploadFileToBucketRequest;
import nl.hemiron.objectstorage.model.response.UploadFileToBucketResponse;
import nl.hemiron.objectstorage.service.ExchangeService;
import nl.hemiron.objectstorage.service.MinioService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/buckets/{bucketName}/objects")
@Tag(name = "ObjectController", description = "Endpoints for everything related to MinIO bucket objects")
public class ObjectController {

    private final MinioService minioService;

    private final ExchangeService exchangeService;

    public ObjectController(MinioService minioService, ExchangeService exchangeService) {
        this.minioService = minioService;
        this.exchangeService = exchangeService;
    }

    @PostMapping(consumes = { "multipart/form-data" })
    @Operation(summary = "Upload multipart file to bucket", responses = {
            @ApiResponse(responseCode = "200", description = "Multipart file uploaded successfully to bucket"),
            @ApiResponse(responseCode = "400", description = "Specified object path is invalid"),
            @ApiResponse(responseCode = "404", description = "Bucket with specified name does not exist"),
            @ApiResponse(responseCode = "500", description = "Multipart file could not be uploaded due to an unexpected error")
    })
    public ResponseEntity<UploadFileToBucketResponse> uploadFileToBucket(@PathVariable String bucketName, @ModelAttribute UploadFileToBucketRequest uploadFileToBucketRequest) {
        try {
            MultipartFile multipartFile = uploadFileToBucketRequest.getObject();

            String uploadObjectURL = minioService.getUploadObjectURL(
                    bucketName,
                    uploadFileToBucketRequest.getObjectPath(),
                    multipartFile
            );

            exchangeService.put(multipartFile, uploadObjectURL);

            return new ResponseEntity<>(
                    // TODO: See issue #33 - Add HATEOAS links for uploaded files
                    new UploadFileToBucketResponse(),
                    HttpStatus.OK
            );
        } catch (BucketNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        } catch (HttpClientErrorException | IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        } catch (HttpServerErrorException | ServerException | ErrorResponseException | InsufficientDataException | IOException |
                 InvalidKeyException | InvalidResponseException | XmlParserException | InternalException |
                 NoSuchAlgorithmException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

}
