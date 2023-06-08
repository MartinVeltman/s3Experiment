package nl.hemiron.objectstorage.model.response;

import io.minio.messages.Owner;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * This class is a kind of wrapper class for io.minio.messages.Item.
 * Because the getters in the Item class follow unconventional naming (e.g. size() instead of getSize()),
 * Spring can't serialize all the fields when returning Item in a JSON response. As it's not possible to change the method names
 * or add JsonProperty annotations to the Item class, creating this new class was the best solution I could come up with.
 */
@AllArgsConstructor
@Getter
public class ItemResponse {

    public ItemResponse(String objectName, boolean isDir) {
        this.objectName = objectName;
        this.isDir = isDir;
    }

    private String etag;
    private String objectName;
    private ZonedDateTime lastModified;
    private Owner owner;
    private long size;
    private String storageClass;
    private boolean isLatest;
    private String versionId;
    private Map<String, String> userMetadata;
    private boolean isDir;
}
