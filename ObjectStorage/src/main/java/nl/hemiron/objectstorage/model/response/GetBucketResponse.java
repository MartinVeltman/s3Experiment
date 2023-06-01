package nl.hemiron.objectstorage.model.response;

import lombok.AllArgsConstructor;
import org.springframework.hateoas.RepresentationModel;

@AllArgsConstructor
public class GetBucketResponse extends RepresentationModel<GetBucketResponse> {
    /**
     * Name of the bucket
     */
    public final String name;

    /**
     * Total size of the bucket (in bytes)
     */
    public final long size;

    /**
     * Amount of objects in the bucket. Folders are not counted as objects
     */
    public final int amountOfObjects;
}
