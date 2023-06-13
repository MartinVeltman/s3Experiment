package nl.hemiron.objectstorage.controller;

import io.minio.Result;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import nl.hemiron.objectstorage.exceptions.BadRequestException;
import nl.hemiron.objectstorage.exceptions.BucketNotFoundException;
import nl.hemiron.objectstorage.exceptions.InternalServerErrorException;
import nl.hemiron.objectstorage.exceptions.NotFoundException;
import nl.hemiron.objectstorage.model.request.UploadFileToBucketRequest;
import nl.hemiron.objectstorage.model.response.DeleteFileResponse;
import nl.hemiron.objectstorage.model.response.ItemResponse;
import nl.hemiron.objectstorage.model.response.UploadFileToBucketResponse;
import nl.hemiron.objectstorage.service.ExchangeService;
import nl.hemiron.objectstorage.service.MinioService;
import nl.hemiron.objectstorage.service.StringUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

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

    @PostMapping(consumes = {"multipart/form-data"})
    @Operation(summary = "Upload multipart file to bucket", responses = {
            @ApiResponse(responseCode = "200", description = "Multipart file uploaded successfully to bucket"),
            @ApiResponse(responseCode = "400", description = "Specified object path is invalid"),
            @ApiResponse(responseCode = "404", description = "Bucket with specified name does not exist"),
            @ApiResponse(responseCode = "500", description = "Multipart file could not be uploaded due to an unexpected error")
    })
    public ResponseEntity<UploadFileToBucketResponse> uploadFileToBucket(
            @RequestHeader("Project-Id") UUID projectId,
            @PathVariable String bucketName,
            @ModelAttribute UploadFileToBucketRequest uploadFileToBucketRequest) {
        try {
            MultipartFile multipartFile = uploadFileToBucketRequest.getObject();

            String objectName = uploadFileToBucketRequest.getObjectPath() + "/" + multipartFile.getOriginalFilename();

            String uploadObjectURL = minioService.getUploadObjectURL(
                    bucketName,
                    objectName,
                    projectId
            );

            exchangeService.put(multipartFile, uploadObjectURL);

            return new ResponseEntity<>(
                    new UploadFileToBucketResponse()
                            .add(linkTo(methodOn(ObjectController.class).downloadObject(projectId, bucketName, StringUtils.encodeBase64(objectName))).withSelfRel()),
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

    @GetMapping(value = "/{objectName}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Download a single object", responses = {
            @ApiResponse(responseCode = "200", description = "Object downloaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid base64 object name"),
            @ApiResponse(responseCode = "404", description = "Could not find bucket and/or object with this name"),
            @ApiResponse(responseCode = "500", description = "Object could not be retrieved due to an unexpected error")
    })
    public ResponseEntity<InputStreamResource> downloadObject(
            @RequestHeader("Project-Id") UUID projectId,
            @PathVariable String bucketName,
            @PathVariable String objectName) throws IOException {
        try {
            var inputstream = this.minioService.getObject(bucketName, objectName, projectId);

            var filename = StringUtils.getFilenameFromBase64(objectName);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("Content-Disposition", "attachment; filename=" + filename);

            return new ResponseEntity<>(
                    new InputStreamResource(inputstream),
                    httpHeaders,
                    HttpStatus.OK
            );
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        } catch (ErrorResponseException e) {
            throw new NotFoundException(e.getMessage());
        } catch (ServerException | InternalException | XmlParserException | InvalidResponseException |
                 InvalidKeyException | NoSuchAlgorithmException | InsufficientDataException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    @DeleteMapping()
    @Operation(summary = "Remove object(s) from a bucket using base64 encoded names", responses = {
            @ApiResponse(responseCode = "200", description = "Objects removed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid base64 object name"),
            @ApiResponse(responseCode = "404", description = "Could not find bucket with this name"),
            @ApiResponse(responseCode = "500", description = "Object could not be removed due to an unexpected error")
    })
    public ResponseEntity<DeleteFileResponse> deleteObjects(
            @RequestHeader("Project-Id") UUID projectId,
            @PathVariable String bucketName,
            @RequestBody String[] objectNames) {
        try {
            Iterable<Result<DeleteError>> deleteResponses = minioService.deleteObjects(bucketName, objectNames, projectId);
            return new ResponseEntity<>(
                    new DeleteFileResponse(bucketName, deleteResponses),
                    HttpStatus.OK
            );
        } catch (BucketNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        } catch (HttpClientErrorException | IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        } catch (HttpServerErrorException | ServerException | ErrorResponseException |
                 InsufficientDataException | IOException | InvalidKeyException |
                 InvalidResponseException | XmlParserException | InternalException | NoSuchAlgorithmException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    @GetMapping(value = "/directory/{directoryName}")
    @Operation(summary = "Get directory contents", responses = {
            @ApiResponse(responseCode = "200", description = "Directory contents retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid base64 directory name"),
            @ApiResponse(responseCode = "404", description = "Could not find bucket or directory with this name"),
            @ApiResponse(responseCode = "500", description = "Directory contents could not be retrieved due to an unexpected error")
    })
    public ResponseEntity<List<ItemResponse>> getDirectory(
            @RequestHeader("Project-Id") UUID projectId,
            @PathVariable String bucketName,
            @PathVariable String directoryName) throws IOException {
        try {
            var directoryContents = this.minioService.getDirectoryContents(bucketName, directoryName, projectId);

            return new ResponseEntity<>(
                    directoryContents,
                    HttpStatus.OK
            );
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        } catch (ErrorResponseException e) {
            throw new NotFoundException(e.getMessage());
        } catch (ServerException | InternalException | XmlParserException | InvalidResponseException |
                 InvalidKeyException | NoSuchAlgorithmException | InsufficientDataException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }
}
