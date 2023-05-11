package nl.hemiron.objectstorage.model.request;

import lombok.Getter;
import lombok.Setter;

public class CreateBucketRequest {

    @Getter
    @Setter
    private String name;
}
