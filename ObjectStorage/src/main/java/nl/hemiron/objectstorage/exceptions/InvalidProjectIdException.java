package nl.hemiron.objectstorage.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class InvalidProjectIdException extends RuntimeException {
    public InvalidProjectIdException(String message) {
        super(message);
    }
}
