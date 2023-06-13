package nl.hemiron.objectstorage.model.response;

import io.minio.Result;
import io.minio.messages.DeleteError;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class DeleteFileResponse {
    /**
     * Name of the bucket
     */
    @Getter
    public final String bucketName;

    @Getter
    public final Iterable<Result<DeleteError>> deleteErrors;
}
