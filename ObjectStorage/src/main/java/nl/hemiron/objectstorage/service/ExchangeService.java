package nl.hemiron.objectstorage.service;

import org.springframework.context.annotation.Bean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;

@Service
public class ExchangeService {

    RestTemplate restTemplate;

    public ExchangeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void put(MultipartFile file, String url) throws HttpClientErrorException, HttpServerErrorException, IOException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentLength(file.getSize());

        RequestEntity<InputStreamResource> requestEntity = new RequestEntity<>(
                new InputStreamResource(file.getInputStream()),
                httpHeaders,
                HttpMethod.PUT,
                URI.create(url)
        );

        restTemplate.exchange(requestEntity, void.class);
    }
}
