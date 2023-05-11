package nl.hemiron.objectstorage.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;

@AllArgsConstructor
public class CreateBucketResponse extends RepresentationModel<CreateBucketResponse> {

    @Getter
    @Setter
    private String name;
}
