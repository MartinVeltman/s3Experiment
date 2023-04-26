package nl.hemiron.objectstorage.Service;

import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MinioService {

    @Value("${minio.builder.endpoint}")
    private String minioEndpoint;

    @Value("${minio.builder.access.key}")
    private String minioAccessKey;

    @Value("${minio.builder.secret.key}")
    private String minioSecretKey;

    private MinioClient minioClient;

    @PostConstruct
    public void initializeMinioClient() {
        this.minioClient = MinioClient.builder()
                .endpoint(minioEndpoint)
                .credentials(minioAccessKey, minioSecretKey)
                .build();
    }
}
