package nl.hemiron.objectstorage.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class DeleteBucketResponse {
    /**
     * Name of the bucket
     */
    @Getter
    public final String name;
}
