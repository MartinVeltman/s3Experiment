package nl.hemiron.objectstorage.exceptions;

public class BucketNotFoundException extends RuntimeException {
    public BucketNotFoundException(String message) {
        super(message);
    }
}
