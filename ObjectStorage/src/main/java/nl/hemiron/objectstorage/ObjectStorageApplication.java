package nl.hemiron.objectstorage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(value = "http://localhost:4200")
@SpringBootApplication
public class ObjectStorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(ObjectStorageApplication.class, args);
    }

}
