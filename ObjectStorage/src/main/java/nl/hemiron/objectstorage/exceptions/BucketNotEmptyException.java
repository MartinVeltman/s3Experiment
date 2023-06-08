package nl.hemiron.objectstorage.exceptions;

public class BucketNotEmptyException extends RuntimeException {
    public BucketNotEmptyException(String message) {super(message);}
}
