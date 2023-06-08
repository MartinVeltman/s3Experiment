package nl.hemiron.objectstorage.model.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class UploadFileToBucketRequest {

    private String objectPath;

    private MultipartFile object;
}
