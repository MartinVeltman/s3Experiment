package nl.hemiron.objectstorage.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class DeleteBucketResponse {
    /**
     * Name of the bucket
     */
    @Getter
    @Setter
    public final String name;
}
